package com.alibaba.nacos.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;

public class ConfigAppClient {

    public static void main(String[] args) throws NacosException, IOException {
        String userFromProp = System.getProperty("nacos.username");
        String userFromEnv = System.getenv("NACOS_USERNAME");
        String passFromProp = System.getProperty("nacos.password");
        String passFromEnv = System.getenv("NACOS_PASSWORD");

        String username = (userFromProp != null && !userFromProp.isEmpty()) ? userFromProp
                : (userFromEnv != null && !userFromEnv.isEmpty()) ? userFromEnv : "nacos";
        String password = (passFromProp != null && !passFromProp.isEmpty()) ? passFromProp
                : (passFromEnv != null && !passFromEnv.isEmpty()) ? passFromEnv : "nacos";

        String serverAddr = "127.0.0.1:8848";
        // namespace 用于租户/环境隔离，取值为命名空间的 ID（不是名称）。
        // 默认命名空间 public 传空字符串即可。
        String namespace = "dev";
        String dataId = "order.properties";
        String group = "DEFAULT_GROUP";

        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        properties.put("namespace", namespace);
        properties.setProperty("username", username);
        properties.setProperty("password", password);

        /**
         * 开启定时任务和一些初始化
         * 往下
         */
        ConfigService configService = NacosFactory.createConfigService(properties);

        /**
         * 发布配置 {@link NacosConfigService#publishConfig(String, String, String, String)}
         * 往下
         */
        configService.publishConfig(dataId, group, "k1=v1", ConfigType.PROPERTIES.getType());

        // 获取配置文件数据
        getConfigData(configService, dataId, group);

        // 配置监听
        configListener(configService, dataId, group);

//        modifyCAS(configService, dataId, group);


    }

    private static void modifyCAS(ConfigService configService, String dataId, String group) throws NacosException {
        // 首次发布，casMd5传入null。 md5可以网页拿到
        boolean isPublishOk = configService.publishConfigCas(dataId, group, "content", "604df5cf9526f6ece3ebc27c6d28d38e");
        System.out.println("======> " + isPublishOk);
        // old Md5 正确，变成成功
//        isPublishOk = configService.publishConfigCas(dataId, group, "newContent", oldContentMd5);
//        System.out.println(isPublishOk);
    }

    private static void configListener(ConfigService configService, String dataId, String group) throws NacosException, IOException {

        /**
         * {@link NacosConfigService#addListener(String, String, Listener)}
         * 往下
         */
        configService.addListener(dataId, group, new Listener() {

            public void receiveConfigInfo(String configInfo) {
                System.out.println("====> 配置有新内容:" + configInfo);
            }

            // 用于指定配置变更通知的执行器（线程池）
            public Executor getExecutor() {
                return null;
            }
        });

        // 获取配置并注册监听器
//        configService.getConfigAndSignListener()

        // 阻塞等待
        System.in.read();
    }

    private static void getConfigData(ConfigService configService, String dataId, String group) throws NacosException {
        // 往下
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println("==========> " + content);
    }
}
