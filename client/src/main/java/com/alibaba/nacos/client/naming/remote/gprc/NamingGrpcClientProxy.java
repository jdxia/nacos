/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.client.naming.remote.gprc;

import com.alibaba.nacos.api.ability.constant.AbilityKey;
import com.alibaba.nacos.api.ability.constant.AbilityStatus;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.CommonParams;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.pojo.Service;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.naming.remote.NamingRemoteConstants;
import com.alibaba.nacos.api.naming.remote.request.AbstractNamingRequest;
import com.alibaba.nacos.api.naming.remote.request.BatchInstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.PersistentInstanceRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceListRequest;
import com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest;
import com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest;
import com.alibaba.nacos.api.naming.remote.response.BatchInstanceResponse;
import com.alibaba.nacos.api.naming.remote.response.QueryServiceResponse;
import com.alibaba.nacos.api.naming.remote.response.ServiceListResponse;
import com.alibaba.nacos.api.naming.remote.response.SubscribeServiceResponse;
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.api.remote.RemoteConstants;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.request.RequestMeta;
import com.alibaba.nacos.api.remote.response.Response;
import com.alibaba.nacos.api.remote.response.ResponseCode;
import com.alibaba.nacos.api.selector.AbstractSelector;
import com.alibaba.nacos.api.selector.SelectorType;
import com.alibaba.nacos.client.env.NacosClientProperties;
import com.alibaba.nacos.client.monitor.MetricsMonitor;
import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
import com.alibaba.nacos.client.naming.event.ServerListChangedEvent;
import com.alibaba.nacos.client.naming.remote.AbstractNamingClientProxy;
import com.alibaba.nacos.client.naming.remote.gprc.redo.NamingGrpcRedoService;
import com.alibaba.nacos.client.naming.remote.gprc.redo.data.BatchInstanceRedoData;
import com.alibaba.nacos.client.naming.remote.gprc.redo.data.InstanceRedoData;
import com.alibaba.nacos.client.security.SecurityProxy;
import com.alibaba.nacos.client.utils.AppNameUtils;
import com.alibaba.nacos.common.notify.Event;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.common.remote.ConnectionType;
import com.alibaba.nacos.common.remote.client.*;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.JacksonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.nacos.api.remote.RemoteConstants.MONITOR_LABEL_NONE;
import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;

/**
 * Naming grpc client proxy.
 *
 * @author xiweng.yy
 */
public class NamingGrpcClientProxy extends AbstractNamingClientProxy {

    private final String namespaceId;

    private final String uuid;

    private final Long requestTimeout;

    private final RpcClient rpcClient;

    private final NamingGrpcRedoService redoService;

    public NamingGrpcClientProxy(String namespaceId, SecurityProxy securityProxy, ServerListFactory serverListFactory,
            NacosClientProperties properties, ServiceInfoHolder serviceInfoHolder) throws NacosException {
        super(securityProxy);
        this.namespaceId = namespaceId;
        this.uuid = UUID.randomUUID().toString();
        this.requestTimeout = Long.parseLong(properties.getProperty(CommonParams.NAMING_REQUEST_TIMEOUT, "-1"));
        Map<String, String> labels = new HashMap<>();
        labels.put(RemoteConstants.LABEL_SOURCE, RemoteConstants.LABEL_SOURCE_SDK);
        labels.put(RemoteConstants.LABEL_MODULE, RemoteConstants.LABEL_MODULE_NAMING);
        labels.put(Constants.APPNAME, AppNameUtils.getAppName());
        this.rpcClient = RpcClientFactory.createClient(uuid, ConnectionType.GRPC, labels,
                RpcClientTlsConfigFactory.getInstance().createSdkConfig(properties.asProperties()));
        this.redoService = new NamingGrpcRedoService(this, properties);
        NAMING_LOGGER.info("Create naming rpc client for uuid->{}", uuid);

        /**
         * 启动grpc client 也就是和 baseRpcServer 建立连接
         *
         * 重要, 往下
         */
        start(serverListFactory, serviceInfoHolder);
    }

    private void start(ServerListFactory serverListFactory, ServiceInfoHolder serviceInfoHolder) throws NacosException {
        rpcClient.serverListFactory(serverListFactory);
        rpcClient.registerConnectionListener(redoService);

        /**
         * NamingPushRequestHandler 就是用来处理 baseRpcServer 发给 GrpcClient的请求的, 比如服务实例变更
         *
         * 利用双端流接受服务端发送的数据, 比如 {@link NamingPushRequestHandler#requestReply(Request, Connection)} 就是处理服务推送下来的实例变更
         *
         * 重要, 这个是利用双端流接受服务端发送的数据, 也要重点看下
         */
        rpcClient.registerServerRequestHandler(new NamingPushRequestHandler(serviceInfoHolder));

        /**
         * 开启心跳的定时任务, 确定9848端口, 创建socket连接, serverCheck请求, 创建一个双端流
         * 接受服务端nacos server的数据, 这个数据谁来处理? 就是上面的 NamingPushRequestHandler
         */
        rpcClient.start();
        NotifyCenter.registerSubscriber(this);
    }

    @Override
    public void onEvent(ServerListChangedEvent event) {
        rpcClient.onServerListChange();
    }

    @Override
    public Class<? extends Event> subscribeType() {
        return ServerListChangedEvent.class;
    }

    @Override
    public void registerService(String serviceName, String groupName, Instance instance) throws NacosException {
        NAMING_LOGGER.info("[REGISTER-SERVICE] {} registering service {} with instance {}", namespaceId, serviceName,
                instance);
        /**
         * 本质做的事情是
         * 1. 添加实例信息到 Client的 publishers中
         * 2. 发布 ClientChangedEvent, 用在集群情况下, DistroProtocol, 同步实例信息的
         * 3. 发布 ClientRegisterServiceEvent, 添加 ClientId 信息到 publisherIndexes
         * 4. 发布 ServiceChangedEvent --> 异步的, 推送最新的服务实例信息给所有订阅了当前服务的服务订阅者
         */


        // 是临时实例
        if (instance.isEphemeral()) {
            // 注册临时实例
            registerServiceForEphemeral(serviceName, groupName, instance);
        } else {
            // 持久实例
            doRegisterServiceForPersistent(serviceName, groupName, instance);
        }
    }

    private void registerServiceForEphemeral(String serviceName, String groupName, Instance instance)
            throws NacosException {
        /**
         * 如果要注册是先缓存一下, 然后 redoService 里面会进行注册, 失败了会重试
         * 断连/超时等场景下，请求可能没成功或成功但客户端未感知；先写缓存可让后续“定时重放任务”在连接恢复后自动补注册，保证最终与“期望状态”一致
         *
         * 先看下 redoService 这个变量 的构造函数 {@link NamingGrpcRedoService#NamingGrpcRedoService(NamingGrpcClientProxy, NacosClientProperties)}
         *
         * redoService 是客户端的, 用来帮客户端在 gRPC 连接抖动/重连后，把“期望的注册/订阅状态”自动补偿回服务端，避免你在业务代码里自己管理断连后的重试和状态对齐
         */
        redoService.cacheInstanceForRedo(serviceName, groupName, instance);

        // 往下, 发 RPC给server
        doRegisterService(serviceName, groupName, instance);
    }

    @Override
    public void batchRegisterService(String serviceName, String groupName, List<Instance> instances)
            throws NacosException {
        redoService.cacheInstanceForRedo(serviceName, groupName, instances);
        doBatchRegisterService(serviceName, groupName, instances);
    }

    @Override
    public void batchDeregisterService(String serviceName, String groupName, List<Instance> instances)
            throws NacosException {
        synchronized (redoService.getRegisteredInstances()) {
            List<Instance> retainInstance = getRetainInstance(serviceName, groupName, instances);
            batchRegisterService(serviceName, groupName, retainInstance);
        }
    }

    /**
     * Get instance list that need to be Retained.
     *
     * @param serviceName         service name
     * @param groupName           group name
     * @param deRegisterInstances deregister instance list
     * @return instance list that need to be retained.
     */
    private List<Instance> getRetainInstance(String serviceName, String groupName, List<Instance> deRegisterInstances)
            throws NacosException {
        if (CollectionUtils.isEmpty(deRegisterInstances)) {
            throw new NacosException(NacosException.INVALID_PARAM,
                    String.format("[Batch deRegistration] need deRegister instance is empty, instances: %s,",
                            deRegisterInstances));
        }
        String combinedServiceName = NamingUtils.getGroupedName(serviceName, groupName);
        InstanceRedoData instanceRedoData = redoService.getRegisteredInstancesByKey(combinedServiceName);
        if (!(instanceRedoData instanceof BatchInstanceRedoData)) {
            throw new NacosException(NacosException.INVALID_PARAM, String.format(
                    "[Batch deRegistration] batch deRegister is not BatchInstanceRedoData type , instances: %s,",
                    deRegisterInstances));
        }

        BatchInstanceRedoData batchInstanceRedoData = (BatchInstanceRedoData) instanceRedoData;
        List<Instance> allRedoInstances = batchInstanceRedoData.getInstances();
        if (CollectionUtils.isEmpty(allRedoInstances)) {
            throw new NacosException(NacosException.INVALID_PARAM, String.format(
                    "[Batch deRegistration] not found all registerInstance , serviceName：%s , groupName: %s",
                    serviceName, groupName));
        }

        Map<Instance, Instance> deRegisterInstanceMap = deRegisterInstances.stream()
                .collect(Collectors.toMap(Function.identity(), Function.identity()));
        List<Instance> retainInstances = new ArrayList<>();
        for (Instance redoInstance : allRedoInstances) {
            boolean needRetained = true;
            Iterator<Map.Entry<Instance, Instance>> it = deRegisterInstanceMap.entrySet().iterator();
            while (it.hasNext()) {
                Instance deRegisterInstance = it.next().getKey();
                // only compare Ip & Port because redoInstance's instanceId or serviceName might be null but deRegisterInstance's might not be null.
                if (compareIpAndPort(deRegisterInstance, redoInstance)) {
                    needRetained = false;
                    // clear current entry to speed up next redoInstance comparing.
                    it.remove();
                    break;
                }
            }
            if (needRetained) {
                retainInstances.add(redoInstance);
            }
        }
        return retainInstances;
    }

    private boolean compareIpAndPort(Instance deRegisterInstance, Instance redoInstance) {
        return ((deRegisterInstance.getIp().equals(redoInstance.getIp())) && (deRegisterInstance.getPort()
                == redoInstance.getPort()));
    }

    /**
     * Execute batch register operation.
     *
     * @param serviceName service name
     * @param groupName   group name
     * @param instances   instances
     * @throws NacosException NacosException
     */
    public void doBatchRegisterService(String serviceName, String groupName, List<Instance> instances)
            throws NacosException {
        BatchInstanceRequest request = new BatchInstanceRequest(namespaceId, serviceName, groupName,
                NamingRemoteConstants.BATCH_REGISTER_INSTANCE, instances);
        requestToServer(request, BatchInstanceResponse.class);
        redoService.instanceRegistered(serviceName, groupName);
    }

    /**
     * Execute register operation.
     *
     * @param serviceName name of service
     * @param groupName   group of service
     * @param instance    instance to register
     * @throws NacosException nacos exception
     */
    public void doRegisterService(String serviceName, String groupName, Instance instance) throws NacosException {

        // 构造 InstanceRequest 对象, type为 REGISTER_INSTANCE
        InstanceRequest request = new InstanceRequest(namespaceId, serviceName, groupName,
                NamingRemoteConstants.REGISTER_INSTANCE, instance);

        /**
         * 发送给nacos server
         * 这个request是被 {@link com.alibaba.nacos.naming.remote.rpc.handler.InstanceRequestHandler} 处理
         * InstanceRequestHandler 是 server的逻辑了
         */
        requestToServer(request, Response.class);

        // 如果执行到这一步, 那 nacos server是执行成功了, 到 redoService里面标记一下不用进行重试了
        redoService.instanceRegistered(serviceName, groupName);
    }

    /**
     * Execute register operation for persistent instance.
     *
     * @param serviceName name of service
     * @param groupName   group of service
     * @param instance    instance to register
     * @throws NacosException nacos exception
     */
    public void doRegisterServiceForPersistent(String serviceName, String groupName, Instance instance)
            throws NacosException {

        /**
         * 服务端处理的
         * {@link com.alibaba.nacos.naming.remote.rpc.handler.PersistentInstanceRequestHandler#handle(PersistentInstanceRequest, RequestMeta)}
         */
        PersistentInstanceRequest request = new PersistentInstanceRequest(namespaceId, serviceName, groupName,
                NamingRemoteConstants.REGISTER_INSTANCE, instance);
        requestToServer(request, Response.class);
    }

    @Override
    public void deregisterService(String serviceName, String groupName, Instance instance) throws NacosException {
        NAMING_LOGGER
                .info("[DEREGISTER-SERVICE] {} deregistering service {} with instance: {}", namespaceId, serviceName,
                        instance);

        // 分为临时实例和持久实例
        if (instance.isEphemeral()) {
            // 往下
            deregisterServiceForEphemeral(serviceName, groupName, instance);
        } else {
            doDeregisterServiceForPersistent(serviceName, groupName, instance);
        }
    }

    private void deregisterServiceForEphemeral(String serviceName, String groupName, Instance instance)
            throws NacosException {
        String key = NamingUtils.getGroupedName(serviceName, groupName);
        InstanceRedoData instanceRedoData = redoService.getRegisteredInstancesByKey(key);
        if (instanceRedoData instanceof BatchInstanceRedoData) {
            List<Instance> instances = new ArrayList<>();
            if (null != instance) {
                instances.add(instance);
            }
            batchDeregisterService(serviceName, groupName, instances);
        } else {
            redoService.instanceDeregister(serviceName, groupName);

            // 往下
            doDeregisterService(serviceName, groupName, instance);
        }
    }

    /**
     * Execute deregister operation.
     *
     * @param serviceName service name
     * @param groupName   group name
     * @param instance    instance
     * @throws NacosException nacos exception
     */
    public void doDeregisterService(String serviceName, String groupName, Instance instance) throws NacosException {
        // 类是 InstanceRequest,  参数类型是 deregisterInstance
        InstanceRequest request = new InstanceRequest(namespaceId, serviceName, groupName,
                NamingRemoteConstants.DE_REGISTER_INSTANCE, instance);

        /**
         * 服务端处理是 {@link com.alibaba.nacos.naming.remote.rpc.handler.InstanceRequestHandler#handle(InstanceRequest, RequestMeta)}
         */
        requestToServer(request, Response.class);
        redoService.instanceDeregistered(serviceName, groupName);
    }

    /**
     * Execute deregister operation for persistent instance.
     *
     * @param serviceName service name
     * @param groupName   group name
     * @param instance    instance
     * @throws NacosException nacos exception
     */
    public void doDeregisterServiceForPersistent(String serviceName, String groupName, Instance instance)
            throws NacosException {
        PersistentInstanceRequest request = new PersistentInstanceRequest(namespaceId, serviceName, groupName,
                NamingRemoteConstants.DE_REGISTER_INSTANCE, instance);
        requestToServer(request, Response.class);
    }

    @Override
    public void updateInstance(String serviceName, String groupName, Instance instance) throws NacosException {
    }

    @Override
    public ServiceInfo queryInstancesOfService(String serviceName, String groupName, String clusters,
            boolean healthyOnly) throws NacosException {
        ServiceQueryRequest request = new ServiceQueryRequest(namespaceId, serviceName, groupName);
        request.setCluster(clusters);
        request.setHealthyOnly(healthyOnly);
        QueryServiceResponse response = requestToServer(request, QueryServiceResponse.class);
        return response.getServiceInfo();
    }

    @Override
    public Service queryService(String serviceName, String groupName) throws NacosException {
        return null;
    }

    @Override
    public void createService(Service service, AbstractSelector selector) throws NacosException {
    }

    @Override
    public boolean deleteService(String serviceName, String groupName) throws NacosException {
        return false;
    }

    @Override
    public void updateService(Service service, AbstractSelector selector) throws NacosException {
    }

    @Override
    public ListView<String> getServiceList(int pageNo, int pageSize, String groupName, AbstractSelector selector)
            throws NacosException {
        ServiceListRequest request = new ServiceListRequest(namespaceId, groupName, pageNo, pageSize);
        if (selector != null) {
            if (SelectorType.valueOf(selector.getType()) == SelectorType.label) {
                request.setSelector(JacksonUtils.toJson(selector));
            }
        }
        ServiceListResponse response = requestToServer(request, ServiceListResponse.class);
        ListView<String> result = new ListView<>();
        result.setCount(response.getCount());
        result.setData(response.getServiceNames());
        return result;
    }

    @Override
    public ServiceInfo subscribe(String serviceName, String groupName, String clusters) throws NacosException {
        NAMING_LOGGER.info("[GRPC-SUBSCRIBE] service:{}, group:{}, cluster:{} ", serviceName, groupName, clusters);
        redoService.cacheSubscriberForRedo(serviceName, groupName, clusters);

        // 往下
        return doSubscribe(serviceName, groupName, clusters);
    }

    /**
     * Execute subscribe operation.
     *
     * @param serviceName service name
     * @param groupName   group name
     * @param clusters    clusters, current only support subscribe all clusters, maybe deprecated
     * @return current service info of subscribe service
     * @throws NacosException nacos exception
     */
    public ServiceInfo doSubscribe(String serviceName, String groupName, String clusters) throws NacosException {

        // 请求对象
        SubscribeServiceRequest request = new SubscribeServiceRequest(namespaceId, groupName, serviceName, clusters,
                true);

        /**
         * 发送请求
         *
         * 根据 SubscribeServiceRequest 去找 服务端处理流程
         * 服务端处理是在 {@link com.alibaba.nacos.naming.remote.rpc.handler.SubscribeServiceRequestHandler#handle(SubscribeServiceRequest, RequestMeta)}
         */
        SubscribeServiceResponse response = requestToServer(request, SubscribeServiceResponse.class);
        redoService.subscriberRegistered(serviceName, groupName, clusters);
        return response.getServiceInfo();
    }

    @Override
    public void unsubscribe(String serviceName, String groupName, String clusters) throws NacosException {
        NAMING_LOGGER.info("[GRPC-UNSUBSCRIBE] service:{}, group:{}, cluster:{} ", serviceName, groupName, clusters);
        redoService.subscriberDeregister(serviceName, groupName, clusters);

        // 往下
        doUnsubscribe(serviceName, groupName, clusters);
    }

    @Override
    public boolean isSubscribed(String serviceName, String groupName, String clusters) throws NacosException {
        return redoService.isSubscriberRegistered(serviceName, groupName, clusters);
    }

    /**
     * Execute unsubscribe operation.
     *
     * @param serviceName service name
     * @param groupName   group name
     * @param clusters    clusters, current only support subscribe all clusters, maybe deprecated
     * @throws NacosException nacos exception
     */
    public void doUnsubscribe(String serviceName, String groupName, String clusters) throws NacosException {

        // 注意, 最后一个参数是 false
        SubscribeServiceRequest request = new SubscribeServiceRequest(namespaceId, groupName, serviceName, clusters,
                false);

        /**
         * 发送给服务端
         *
         * 服务端处理逻辑是 {@link com.alibaba.nacos.naming.remote.rpc.handler.SubscribeServiceRequestHandler#handle(SubscribeServiceRequest, RequestMeta)}
         */
        requestToServer(request, SubscribeServiceResponse.class);
        redoService.removeSubscriberForRedo(serviceName, groupName, clusters);
    }

    @Override
    public boolean serverHealthy() {
        return rpcClient.isRunning();
    }

    /**
     * Determine whether nacos-server supports the capability.
     *
     * @param abilityKey ability key
     * @return true if supported, otherwise false
     */
    public boolean isAbilitySupportedByServer(AbilityKey abilityKey) {
        return rpcClient.getConnectionAbility(abilityKey) == AbilityStatus.SUPPORTED;
    }

    private <T extends Response> T requestToServer(AbstractNamingRequest request, Class<T> responseClass)
            throws NacosException {
        Response response = null;
        try {
            request.putAllHeader(
                    getSecurityHeaders(request.getNamespace(), request.getGroupName(), request.getServiceName()));

            /**
             * 这是客户端
             * gRPC 通信
             * 利用grp client, 底层就是 GrpcSdkClient, 发送request
             *
             * 服务端是 {@link com.alibaba.nacos.core.remote.BaseRpcServer}
             */
            response = requestTimeout < 0 ? rpcClient.request(request) : rpcClient.request(request, requestTimeout);
            if (ResponseCode.SUCCESS.getCode() != response.getResultCode()) {
                throw new NacosException(response.getErrorCode(), response.getMessage());
            }
            if (responseClass.isAssignableFrom(response.getClass())) {
                return (T) response;
            }
            NAMING_LOGGER.error("Server return unexpected response '{}', expected response should be '{}'",
                    response.getClass().getName(), responseClass.getName());
            throw new NacosException(NacosException.SERVER_ERROR, "Server return invalid response");
        } catch (NacosException e) {
            // 失败指标记录
            recordRequestFailedMetrics(request, e, response);
            throw e;
        } catch (Exception e) {
            recordRequestFailedMetrics(request, e, response);
            throw new NacosException(NacosException.SERVER_ERROR, "Request nacos server failed: ", e);
        }
    }

    /**
     * Records registration metrics for a service instance.
     *
     * @param request   The registration request object.
     * @param exception The Exception encountered during the registration process, or null if registration was
     *                  successful.
     * @param response  The response object containing registration result information, or null if registration failed.
     */
    private void recordRequestFailedMetrics(AbstractNamingRequest request, Exception exception, Response response) {
        if (Objects.isNull(response)) {
            MetricsMonitor.getNamingRequestFailedMonitor(request.getClass().getSimpleName(), MONITOR_LABEL_NONE,
                    MONITOR_LABEL_NONE, exception.getClass().getSimpleName()).inc();
        } else {
            MetricsMonitor.getNamingRequestFailedMonitor(request.getClass().getSimpleName(),
                    String.valueOf(response.getResultCode()), String.valueOf(response.getErrorCode()),
                    MONITOR_LABEL_NONE).inc();
        }
    }

    @Override
    public void shutdown() throws NacosException {
        NAMING_LOGGER.info("Shutdown naming grpc client proxy for  uuid->{}", uuid);
        redoService.shutdown();
        shutDownAndRemove(uuid);
        NotifyCenter.deregisterSubscriber(this);
    }

    private void shutDownAndRemove(String uuid) {
        synchronized (RpcClientFactory.getAllClientEntries()) {
            try {
                RpcClientFactory.destroyClient(uuid);
                NAMING_LOGGER.info("shutdown and remove naming rpc client  for uuid ->{}", uuid);
            } catch (NacosException e) {
                NAMING_LOGGER.warn("Fail to shutdown naming rpc client  for uuid ->{}", uuid);
            }
        }
    }

    public boolean isEnable() {
        return rpcClient.isRunning();
    }
}
