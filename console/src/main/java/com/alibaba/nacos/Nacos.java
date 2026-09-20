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
     * 启动类配置VM options添加参数，设置成单机启动：
     * -Dnacos.standalone=true -Dnacos.server.ip=127.0.0.1 -Dserver.port=8848
     * -Dnacos.home=/Users/xjd/Desktop/study/javaframework/nacos/home/standalone/
     * -Dnacos.logs.path=/Users/xjd/Desktop/study/javaframework/nacos/home/standalone/logs
     *
     * http://127.0.0.1:8848/nacos 账密都是nacos
     */
    public static void main(String[] args) {

        /**
         * curl调用方式在下面
         * 日志查看也在下面
         *
         * 临时实例用 AP（Distro）, 临时实例（量大、高频、可丢）, owner 单点写 → 异步推副本, 无全局 leader，每条数据各自的路由 owner
         * 持久实例用 CP（JRaft）, 配置、持久实例、元数据（必须正确）, 有全局 leader，所有写经 leader, 线性一致（强一致）
         *
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

    /**
     * 日志, 默认日志目录是 ${nacos.home}/logs，主要日志可以通过 nacos.logs.path 调整；start.out 是当前启动脚本重定向的输出，仍放在安装目录的 logs 下
     *
     * 场景                                      主要日志                                                   重点看什么
     *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
     *    启动失败、启动卡住                        start.out、nacos.log                                       启动异常栈、Spring 初始化失败、端口占用、配置加载异常
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    注册失败、实例异常上下线、服务发现异常    naming-server.log                                          Naming 主流程、服务/实例处理、客户端状态相关异常
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    实例健康状态变化、Naming 事件问题         naming-event.log                                           健康检查状态变化、事件处理异常；与 naming-server.log 配合
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    2.x SDK 连接失败、频繁断连                remote.log                                                 gRPC 连接建立/关闭、连接管理、请求处理异常；可按客户端 IP、connectionId 关联
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    服务实例变了，订阅方没收到更新            naming-push.log、remote-push.log                           服务变更推送，以及底层 RPC 推送失败、超时
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    配置中心初始化失败或内部异常              config-server.log、config-fatal.log                        配置模块初始化、配置处理中的错误
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    配置发布后未生效、节点读到旧配置          config-trace.log、config-dump.log、config-notify.log       配置变更轨迹、服务端本地配置文件/缓存更新、变更通知
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    客户端配置拉取异常                        config-client-request.log、config-pull.log                 配置请求与拉取相关记录；2.x 连接/推送问题结合 remote*.log
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    集群节点失联、成员列表异常                nacos-cluster.log                                          集群成员发现、成员状态变化、节点间探测异常
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    临时实例在不同节点上不一致                protocol-distro.log、naming-distro.log                     Distro 数据同步、校验、加载失败
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    Raft 无 Leader、选举或日志复制异常        protocol-raft.log、alipay-jraft.log                        Nacos 的 Raft 协议层与底层 JRaft 选举、复制、快照异常
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    数据库访问、持久化异常                    nacos-persistence.log，结合 nacos.log、config-fatal.log    数据源初始化、数据库操作和持久化相关异常
     *   ────────────────────────────────────────  ─────────────────────────────────────────────────────────  ──────────────────────────────────────────────────────────────────────────────
     *    登录、鉴权失败                            core-auth.log，结合 nacos.log                              鉴权相关信息；具体输出还取决于使用的鉴权插件
     *
     *    - 服务注册不上： naming-server.log → remote.log → 鉴权失败时看 core-auth.log。
     *   - 实例频繁掉线： remote.log + naming-server.log，先关联断连时间与实例移除时间；健康检查问题再看 naming-event.log。
     *   - 控制台已有实例，但消费端没更新： naming-push.log → remote-push.log → 消费端自身的 Nacos 客户端日志。
     *   - 连不同 Nacos 节点看到的实例不同： nacos-cluster.log → protocol-distro.log / naming-distro.log。
     */


}

