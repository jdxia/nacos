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

package com.alibaba.nacos.naming.core.v2.service.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.common.notify.Event;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.naming.core.v2.ServiceManager;
import com.alibaba.nacos.naming.core.v2.client.AbstractClient;
import com.alibaba.nacos.naming.core.v2.client.Client;
import com.alibaba.nacos.naming.core.v2.client.manager.ClientManager;
import com.alibaba.nacos.naming.core.v2.client.manager.ClientManagerDelegate;
import com.alibaba.nacos.naming.core.v2.event.client.ClientEvent;
import com.alibaba.nacos.naming.core.v2.event.client.ClientOperationEvent;
import com.alibaba.nacos.naming.core.v2.event.metadata.MetadataEvent;
import com.alibaba.nacos.naming.core.v2.index.ClientServiceIndexesManager;
import com.alibaba.nacos.naming.core.v2.pojo.BatchInstancePublishInfo;
import com.alibaba.nacos.naming.core.v2.pojo.InstancePublishInfo;
import com.alibaba.nacos.naming.core.v2.pojo.Service;
import com.alibaba.nacos.naming.core.v2.service.ClientOperationService;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.pojo.Subscriber;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation service for ephemeral clients and services.
 *
 * @author xiweng.yy
 */
@Component("ephemeralClientOperationService")
public class EphemeralClientOperationServiceImpl implements ClientOperationService {

    private final ClientManager clientManager;

    public EphemeralClientOperationServiceImpl(ClientManagerDelegate clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public void registerInstance(Service service, Instance instance, String clientId) throws NacosException {
        NamingUtils.checkInstanceIsLegal(instance);

        // 将 Service 保存到 NamespaceSingletonMaps 中, 一个namespace下可能有多个 service
        Service singleton = ServiceManager.getInstance().getSingleton(service);

        // 禁止向持久服务注册临时实例
        if (!singleton.isEphemeral()) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM,
                    String.format("Current service %s is persistent service, can't register ephemeral instance.",
                            singleton.getGroupedServiceName()));
        }

        /**
         * 根据 clientId 获取 Client 对象
         *
         * ClientManagerDelegate 内部会根据 clientId的格式来判断到底用 ConnectionBasedClientManager 还是 PersistentIpPortClientManager
         * 默认最终用的是 ConnectionBasedClientManager
         */
        Client client = clientManager.getClient(clientId);
        checkClientIsLegal(client, clientId);

        // 把 instance对象 封装成 InstancePublishInfo 对象
        InstancePublishInfo instanceInfo = getPublishInfo(instance);

        /**
         * <p>
         * 服务注册的关键, 将 InstancePublishInfo 保存到 Client 中
         *
         * 注意: 一个client可以注册多个服务, 但是每个服务只能有一个实例
         * 可以看 {@link AbstractClient} 这个的属性, 这个属性非常重要
         *
         * {@link AbstractClient#addServiceInstance(Service, InstancePublishInfo)}
         * 这个方法里面 发布了 一个 {@link ClientEvent.ClientChangedEvent} 事件, 同步给其他nacos节点
         * </p>
         */
        client.addServiceInstance(singleton, instanceInfo);
        client.setLastUpdatedTime();
        client.recalculateRevision();

        /**
         * 发布 ClientRegisterServiceEvent 事件
         * 发布服务注册事件, 从而更新 publisherIndexes, 并发布 ServiceChangedEvent 事件
         *
         * 在 {@link ClientServiceIndexesManager#handleClientOperation(ClientOperationEvent)} 这个地方用的
         */
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientRegisterServiceEvent(singleton, clientId));
        NotifyCenter
                .publishEvent(new MetadataEvent.InstanceMetadataEvent(singleton, instanceInfo.getMetadataId(), false));
    }

    @Override
    public void batchRegisterInstance(Service service, List<Instance> instances, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingleton(service);
        if (!singleton.isEphemeral()) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM,
                    String.format("Current service %s is persistent service, can't batch register ephemeral instance.",
                            singleton.getGroupedServiceName()));
        }
        Client client = clientManager.getClient(clientId);
        checkClientIsLegal(client, clientId);
        BatchInstancePublishInfo batchInstancePublishInfo = new BatchInstancePublishInfo();
        List<InstancePublishInfo> resultList = new ArrayList<>();
        for (Instance instance : instances) {
            InstancePublishInfo instanceInfo = getPublishInfo(instance);
            resultList.add(instanceInfo);
        }
        batchInstancePublishInfo.setInstancePublishInfos(resultList);
        client.addServiceInstance(singleton, batchInstancePublishInfo);
        client.setLastUpdatedTime();
        client.recalculateRevision();
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientRegisterServiceEvent(singleton, clientId));
        NotifyCenter.publishEvent(
                new MetadataEvent.InstanceMetadataEvent(singleton, batchInstancePublishInfo.getMetadataId(), false));
    }

    @Override
    public void deregisterInstance(Service service, Instance instance, String clientId) {
        if (!ServiceManager.getInstance().containSingleton(service)) {
            Loggers.SRV_LOG.warn("remove instance from non-exist service: {}", service);
            return;
        }
        Service singleton = ServiceManager.getInstance().getSingleton(service);
        Client client = clientManager.getClient(clientId);
        checkClientIsLegal(client, clientId);

        // client对象里面移除这个, 把服务信息从当前client对象中移除掉, 从 publishers中移除掉
        InstancePublishInfo removedInstance = client.removeServiceInstance(singleton);
        client.setLastUpdatedTime();
        client.recalculateRevision();
        if (null != removedInstance) {
            /**
             * 发布 ClientDeregisterServiceEvent 事件
             *
             * 在 {@link ClientServiceIndexesManager#handleClientOperation(ClientOperationEvent)} 消费的
             */
            NotifyCenter.publishEvent(new ClientOperationEvent.ClientDeregisterServiceEvent(singleton, clientId));
            NotifyCenter.publishEvent(
                    new MetadataEvent.InstanceMetadataEvent(singleton, removedInstance.getMetadataId(), true));
        }
    }

    @Override
    public void subscribeService(Service service, Subscriber subscriber, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingletonIfExist(service).orElse(service);
        Client client = clientManager.getClient(clientId);
        checkClientIsLegal(client, clientId);

        /**
         * 往client中添加一个 Subscriber
         *
         * {@link AbstractClient#addServiceSubscriber(Service, Subscriber)}
         */
        client.addServiceSubscriber(singleton, subscriber);
        client.setLastUpdatedTime();

        /**
         * 发布了一个 ClientSubscribeServiceEvent 事件, ClientServiceIndexesManager 会进行消费
         *
         * {@link ClientServiceIndexesManager#onEvent(Event)}
         */
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientSubscribeServiceEvent(singleton, clientId));
    }

    @Override
    public void unsubscribeService(Service service, Subscriber subscriber, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingletonIfExist(service).orElse(service);
        Client client = clientManager.getClient(clientId);
        checkClientIsLegal(client, clientId);

        // 在 map里面移除, 这个map是  client 订阅了哪些服务的实例
        client.removeServiceSubscriber(singleton);
        client.setLastUpdatedTime();

        // 发布解约事件, 那边 index也移除这个
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientUnsubscribeServiceEvent(singleton, clientId));
    }

    private void checkClientIsLegal(Client client, String clientId) {
        if (client == null) {
            Loggers.SRV_LOG.warn("Client connection {} already disconnect", clientId);
            throw new NacosRuntimeException(NacosException.CLIENT_DISCONNECT,
                    String.format("Client [%s] connection already disconnect, can't register ephemeral instance.",
                            clientId));
        }
        if (!client.isEphemeral()) {
            Loggers.SRV_LOG.warn("Client connection {} type is not ephemeral", clientId);
            throw new NacosRuntimeException(NacosException.INVALID_PARAM,
                    String.format("Current client [%s] is persistent client, can't register ephemeral instance.",
                            clientId));
        }
    }
}
