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

package com.alibaba.nacos;

import com.alibaba.nacos.api.grpc.auto.Payload;
import com.alibaba.nacos.core.cluster.ServerMemberManager;
import com.alibaba.nacos.core.code.SpringApplicationRunListener;
import com.alibaba.nacos.core.remote.grpc.BaseGrpcServer;
import com.alibaba.nacos.core.remote.grpc.GrpcClusterServer;
import com.alibaba.nacos.core.remote.grpc.GrpcSdkServer;
import com.alibaba.nacos.sys.filter.NacosTypeExcludeFilter;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import io.grpc.util.MutableHandlerRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

/**
 * Nacos starter.
 * <p>
 * Use @SpringBootApplication and @ComponentScan at the same time, using CUSTOM type filter to control module enabled.
 * </p>
 *
 * @author nacos
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.alibaba.nacos", excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = {NacosTypeExcludeFilter.class}),
        @Filter(type = FilterType.CUSTOM, classes = {TypeExcludeFilter.class}),
        @Filter(type = FilterType.CUSTOM, classes = {AutoConfigurationExcludeFilter.class})})
@ServletComponentScan
public class Nacos {
    /**
     * 启动先找 spring.factories 中的自动配置类 还有 org.springframework.boot.autoconfigure.AutoConfiguration.imports 这个文件
     *
     * 启动的时候也会 执行 {@link SpringApplicationRunListener}
     *
     * 集群走 {@link ServerMemberManager}
     *
     * grpc service的初始化是被bean扫描到的, 初始化是在他们的父类 {@link BaseGrpcServer#start()}
     *  {@link GrpcSdkServer}   客户端 SDK（业务应用） 8848 + 1000 = 9848
     *  {@link GrpcClusterServer}  集群内部节点（Nacos server 之间） 8848 + 1001 = 9849
     *  里面有核心的
     *  {@link BaseGrpcServer#addServices(MutableHandlerRegistry, ServerInterceptor...)} 单个请求和双端流 定义的方法都在这里
     */

    /**
     * 启动类配置VM options添加参数，设置成单机启动：
     * -Dnacos.standalone=true -Dnacos.server.ip=127.0.0.1 -Dserver.port=8848
     * -Dnacos.home=/Users/xjd/Desktop/study/javaframework/nacos/home/standalone/
     * -Dnacos.logs.path=/Users/xjd/Desktop/study/javaframework/nacos/home/standalone/logs
     *
     * http://127.0.0.1:8848/nacos 账密都是nacos
     */


    public static void main(String[] args) {

        /**
         * # mvn仓库指向aliyun
         * mvn clean compile -Dmaven.test.skip=true -DskipTests=true -Dmaven.javadoc.skip=true -DskipSpotless=true --settings ~/.m2/settings.xml.aliyun
         */
        SpringApplication.run(Nacos.class, args);
    }

    /**
     * # Step 1: 拿到登入的token
     * curl -X POST 'http://127.0.0.1:8848/nacos/v1/auth/login' -d 'username=nacos&password=nacos'
     *
     * # Step 2: 把 token 存到变量,后续命令直接复用
     * TOKEN=$(curl -s -X POST 'http://127.0.0.1:8848/nacos/v1/auth/login' -d 'username=nacos&password=nacos' | python3 -c "import json,sys;print(json.load(sys.stdin)['accessToken'])")
     *
     * # A. 注册临时实例
     * curl -X POST "http://127.0.0.1:8848/nacos/v1/ns/instance?serviceName=demo&ip=127.0.0.1&port=8080&accessToken=$TOKEN"
     *
     * # B. 查列表
     * curl "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=demo&accessToken=$TOKEN"
     *
     * # C. 注册持久实例
     * #  如果没有指定ip, rm -rf  /Users/xjd/Desktop/study/javaframework/nacos/home/standalone/data/protocol
     * curl -X POST "http://127.0.0.1:8848/nacos/v1/ns/instance?serviceName=demo-persistent&ip=127.0.0.1&port=8081&ephemeral=false&accessToken=$TOKEN"
     *
     * # D. 查持久实例
     * curl "http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=demo-persistent&accessToken=$TOKEN"
     *
     * # E. 发布配置
     * curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs?accessToken=$TOKEN"  -d 'dataId=app.yaml&group=DEFAULT_GROUP&content=foo: bar'
     *
     * # F. 拉配置
     * curl "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=app.yaml&group=DEFAULT_GROUP&accessToken=$TOKEN"
     *
     * # G. 注销临时实例
     * curl -X DELETE "http://127.0.0.1:8848/nacos/v1/ns/instance?serviceName=demo&ip=127.0.0.1&port=8080&accessToken=$TOKEN"
     */


}

