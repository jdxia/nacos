package com.alibaba.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.alibaba.nacos.client.naming.listener.AbstractNamingChangeListener;
import com.alibaba.nacos.client.naming.listener.NamingChangeEvent;
import com.alibaba.nacos.client.naming.remote.gprc.NamingPushRequestHandler;
import com.alibaba.nacos.common.remote.client.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServiceAppClient {

    public static void main(String[] args) throws NacosException, IOException, InterruptedException {

        /**
         * 配置和注册, 2个不一样
         * nacos client是有缓存的, 简单理解为md5, 判断推过来的md5对比有没有变化, 有就触发 listener监听器, 然后更新本地的md5
         *
         * 如果有 A, B , C这3个监听器
         * B的listener抛了一场 ,在配置client里面, C的listener还是会执行
         * 并且B的 本地的 lastmd5 不会更新, 是要在listener执行成功后才更新
         *
         * 但是注册中心的client, md写的人,脑子不正常吧? 抄配置client的都不会抄?
         * 推过来, 直接先更新缓存, 然后挨个去调用所有监听器, 不加try catch, A, B , C这3个监听器
         * B抛了异常, C不会执行
         *
         * config client默认会定时同步, 注册中心client不会, 要手动开启
         */

        /**
         * 和 spring cloud common和spring cloud alibaba集成
         * 首先是 spring cloud common 里面的 ServiceRegistry (服务注册抽象) / DiscoveryClient (服务发现抽象)
         * 在 spring cloud alibaba里面 是 NacosServiceRegistry 和 NacosDiscoveryClient
         * 自动装配是 NacosServiceRegistryAutoConfiguration  和 NacosDiscoveryClientConfiguration
         *
         * 谁调用register?
         * 是 Spring Cloud Commons 的生命周期, AbstractAutoServiceRegistration 类 public void onApplicationEvent(WebServerInitializedEvent event)
         * 在web服务初始化后, 然后里面的 start 再到里面的 register方法
         *
         * 服务发现 failover：~/nacos/naming/{namespace}/failover
         *
         * 默认是没有定时和服务端校对注册数据, 除非开启 {@link PropertyKeyConst#NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE}
         *
         *
         * 服务器推送过来的逻辑是在 {@link NamingPushRequestHandler#requestReply(Request, Connection)}
         */


        // 注：服务端默认已开启鉴权（见 application.properties: nacos.core.auth.enabled=true）。
        // 因此客户端需要携带用户名/密码，否则 2.x gRPC 注册会返回 403: user not found。
        // 优先读取 JVM -D 参数，其次读取环境变量，最后回落到本地开发默认值 "nacos/nacos"
        Properties properties = getProperties();

        /**
         * 如需指定命名空间，可同时设置 properties.setProperty("namespace", "public");
         *
         * 这个里面还创建了链接,
         * 这个里面非常重要
         */
        NamingService naming = NamingFactory.createNamingService(properties);

        // 注册单个服务带一些权重信息
//        registerSingleService(naming);

        // 注册多个服务
//        registerService(naming);

        // 获取服务注册的实例信息
//        getNamInstance(naming);

        // 注销实例
//        deRegisterService(naming);

        // 订阅服务
        subscribeNormalInstance(naming);

        // 服务变化监听器, 这个里面的写法可以自定义监听器
//        changeListener(naming);
    }

    private static Properties getProperties() {
        String userFromProp = System.getProperty("nacos.username");
        String userFromEnv = System.getenv("NACOS_USERNAME");
        String passFromProp = System.getProperty("nacos.password");
        String passFromEnv = System.getenv("NACOS_PASSWORD");

        String username = (userFromProp != null && !userFromProp.isEmpty()) ? userFromProp
                : (userFromEnv != null && !userFromEnv.isEmpty()) ? userFromEnv : "nacos";
        String password = (passFromProp != null && !passFromProp.isEmpty()) ? passFromProp
                : (passFromEnv != null && !passFromEnv.isEmpty()) ? passFromEnv : "nacos";

        Properties properties = new Properties();
        properties.setProperty("serverAddr", "127.0.0.1:8848");
        properties.setProperty("username", username);
        properties.setProperty("password", password);

        /**
         * 推空保护
         * 当注册中心进行变更或遇到突发情况， 或服务提供者与注册中心间的链接因网络、CPU等其他因素发生抖动时，可能会导致订阅异常，从而使服务消费者获取到空的服务提供者实例列表
         *
         * Spring Cloud Alibaba框架 需要这样配置
         * spring.cloud.nacos.discovery.namingPushEmptyProtection=true
         *
         * 若您使用的是Dubbo框架，请在Dubbo配置文件的注册中心链接URL中添加下列配置
         * dubbo.registry.address=nacos://${mseNacos实例域名}:8848?namingPushEmptyProtection=true
         *
         * 会触发推空保护的场景
         * 1. Dubbo 2.7旧版本（2.7.6之前的版本）所注册的服务名格式与Dubbo 2.7新版本的服务名格式不同
         * 2. Dubbo 3支持应用级服务发现，所注册的服务名不再是接口名而是应用名。为了支持旧版本平滑升级，同时订阅应用级和接口级的服务名。当所有提供者均为Dubbo 3版本时，接口级服务必定不存在，因此订阅接口级服务时会触发推空保护
         * 3. Spring Cloud Alibaba在新版本中新增功能NacosWatch，用于监听自身服务状态，该功能会在启动应用时监听自身服务。当该功能首次启动且没有其他实例副本时，由于注册中心没有该服务，订阅会触发推空保护，应用启动完成后恢复
         * 4. Spring Cloud Gateway会在启动时查询注册中心中目前所有的服务列表，并订阅所有服务。例如，某个服务在Gateway启动后彻底下线（全部实例移除），是因为注册中心自动摘除了下线服务，但Spring Cloud Gateway不感知，仍然继续订阅，导致触发推空保护。
         * 4.1 目前Spring Cloud Gateway不支持动态感知某个服务完全移除后取消订阅，建议您重启Spring Cloud Gateway
         *
         */
        properties.put(PropertyKeyConst.NAMING_PUSH_EMPTY_PROTECTION, Boolean.TRUE.toString());

        /**
         * 定时补拉：需要开启 namingAsyncQuerySubscribeService=true。
         * 默认是 false，因此虽然订阅方法会调用 scheduleUpdateIfAbsent()，但默认会直接返回，不创建周期查询任务
         * 间隔时间默认不可调
         *
         * 但是注意这个会对服务端造成压力, 要注意好
         */
        properties.put(PropertyKeyConst.NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE, Boolean.TRUE.toString());
        return properties;
    }

    private static void changeListener(NamingService naming) throws NacosException, InterruptedException, IOException {
        naming.registerInstance("order", "192.169.1.111", 8888);

        TimeUnit.SECONDS.sleep(3);
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        EventListener serviceListener = new AbstractNamingChangeListener() {
            @Override
            public void onChange(NamingChangeEvent event) {
                if (event.isAdded()) {
                    System.out.println("======>add " + event.getAddedInstances());
                }
                if (event.isRemoved()) {
                    // ======>remove [Instance{instanceId='192.169.1.111#8888#DEFAULT#DEFAULT_GROUP@@order', ip='192.169.1.111', port=8888, weight=1.0, healthy=true, enabled=true, ephemeral=true, clusterName='DEFAULT', serviceName='DEFAULT_GROUP@@order', metadata={}}]
                    System.out.println("======>remove " + event.getRemovedInstances());
                }
                if (event.isModified()) {
                    System.out.println("======>modify " + event.getModifiedInstances());
                }
            }

            @Override
            public Executor getExecutor() {
                return executorService;
            }
        };
        naming.subscribe("order", serviceListener);

        System.in.read();
    }

    private static void subscribeNormalInstance(NamingService naming) throws NacosException, InterruptedException, IOException {
        naming.registerInstance("order", "192.169.1.111", 8888);

        TimeUnit.SECONDS.sleep(3);

        /**
         * 这是普通的监听器
         *
         * 如果抛异常, 不会再执行, 只打印日志
         * 并且要注意, 遍历同一服务、同一分组的监听器的, 如果 A, B, C 都有变更, B抛异常了, C是不会执行的
         *
         * 还要注意: 这边是先更新本地缓存, 再通知下面的 监听器的, 如果监听器出现了异常也是不会回滚本地缓存的.
         * 因此可能出现：Nacos 缓存已经是新实例列表，但你在回调里维护的业务状态还停留在旧版本。之后即使再次拉到相同列表，也不会仅仅因为上次回调失败就重新通知
         *
         * 但是配置中心的, 缓存的md5, 是监听器更新成功才改的
         */
        EventListener listener = event -> {
            if (event instanceof NamingEvent) {
                // order
                System.out.println("=======> " + ((NamingEvent) event).getServiceName());
                // =======>[Instance{instanceId='192.169.1.111#8888#DEFAULT#DEFAULT_GROUP@@order', ip='192.169.1.111', port=8888, weight=1.0, healthy=true, enabled=true, ephemeral=true, clusterName='DEFAULT', serviceName='DEFAULT_GROUP@@order', metadata={}}]
                System.out.println("=======> " + ((NamingEvent) event).getInstances());
            }
        };

        // 这是 可以自定义处理器的
        // EventListener serviceListener = new AbstractNamingChangeListener() {

        /**
         * {@link NacosNamingService#subscribe(String, EventListener)}
         */
        naming.subscribe("order", listener);

        // 取消订阅
        naming.unsubscribe("order", listener);

        System.in.read();
    }

    private static void getNamInstance(NamingService naming) throws NacosException, InterruptedException, IOException {
        registerSingleService(naming);

        TimeUnit.SECONDS.sleep(3);

        // 获取所有服务的实例
        System.out.println("=========> " + naming.getAllInstances("order"));

        // 筛选一些服务的实例
        System.out.println("=========> " + naming.selectInstances("order", true));

        System.in.read();
    }

    private static void registerSingleService(NamingService naming) throws NacosException, IOException {
        // 注册服务实例
        // naming.registerInstance("order", "192.169.1.111", 8888);

        Instance instance = new Instance();
        instance.setIp("192.168.1.111");
        instance.setPort(8888);
        // 临时实例 是 true, false是永久
        instance.setEphemeral(true);
        // 不健康
        instance.setHealthy(false);
        // 权重
        instance.setWeight(2.0);
        // 元数据
        Map<String, String> instanceMeta = new HashMap<String, String>();
        instanceMeta.put("mac", "111");
        instance.setMetadata(instanceMeta);

        naming.registerInstance("order", instance);

        System.in.read();
    }

    private static void registerService(NamingService naming) throws NacosException, IOException {
        /**
         * 服务列表里面就1个, 然后服务名是order
         * order里面集群条目是2个 bj 和 sh
         * bj集群里面有2个
         */
        naming.registerInstance("order", "192.169.1.111", 8888, "bj");
        // 这个没有用, 一个客户端对一个服务只能发布一个实例
        naming.registerInstance("order", "192.169.1.111", 8887, "bj");

        NamingService naming1 = NamingFactory.createNamingService("localhost:8848");
        naming1.registerInstance("order", "192.169.1.112", 8888, "bj");

        NamingService naming2 = NamingFactory.createNamingService("localhost:8848");
        naming2.registerInstance("order", "192.169.1.112", 8888, "sh");

        System.in.read();
    }

    private static void deRegisterService(NamingService naming) throws IOException, NacosException {
        Instance instance = new Instance();
        instance.setIp("192.168.1.111");
        instance.setPort(8888);
        // 临时实例 是 true, false是永久
        instance.setEphemeral(true);
        // 不健康
        instance.setHealthy(false);
        // 权重
        instance.setWeight(2.0);
        // 元数据
        Map<String, String> instanceMeta = new HashMap<String, String>();
        instanceMeta.put("mac", "111");
        instance.setMetadata(instanceMeta);
        // 注册
        naming.registerInstance("order", instance);

        /**
         * 注销
         * {@link com.alibaba.nacos.client.naming.NacosNamingService#deregisterInstance(java.lang.String, com.alibaba.nacos.api.naming.pojo.Instance)}
         */
        naming.deregisterInstance("order", instance);

        System.in.read();
    }

}
