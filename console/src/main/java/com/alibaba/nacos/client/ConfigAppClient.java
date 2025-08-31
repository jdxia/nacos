package com.alibaba.nacos.client;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;

public class ConfigAppClient {

    public static void main(String[] args) throws NacosException, IOException {
        String serverAddr = "127.0.0.1:8848";
        String dataId = "order.properties";
        String group = "DEFAULT_GROUP";

        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        ConfigService configService = NacosFactory.createConfigService(properties);

        // 获取配置文件数据
//        getConfigData(configService, dataId, group);

        // 配置监听
//        configListener(configService, dataId, group);

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
        configService.addListener(dataId, group, new Listener() {

            public void receiveConfigInfo(String configInfo) {
                System.out.println("====> 配置新内容:" + configInfo);
            }

            // 用于指定配置变更通知的执行器（线程池）
            public Executor getExecutor() {
                return null;
            }
        });

        // 阻塞等待
        System.in.read();
    }

    private static void getConfigData(ConfigService configService, String dataId, String group) throws NacosException {
        String content = configService.getConfig(dataId, group, 5000);
        System.out.println("==========> " + content);
    }
}
