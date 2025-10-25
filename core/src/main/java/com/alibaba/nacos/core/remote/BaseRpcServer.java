/*
 * Copyright 1999-2023 Alibaba Group Holding Ltd.
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

package com.alibaba.nacos.core.remote;

import com.alibaba.nacos.common.remote.ConnectionType;
import com.alibaba.nacos.common.remote.PayloadRegistry;
import com.alibaba.nacos.core.cluster.lookup.AddressServerMemberLookup;
import com.alibaba.nacos.core.remote.tls.RpcServerSslContextRefresherHolder;
import com.alibaba.nacos.core.utils.Loggers;
import com.alibaba.nacos.sys.env.EnvUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * abstract rpc server .
 *
 * @author liuzunfei
 * @version $Id: BaseRpcServer.java, v 0.1 2020年07月13日 3:41 PM liuzunfei Exp $
 */
public abstract class BaseRpcServer {

    static {
        // / 类加载时扫描并注册所有RPC请求/响应类型
        PayloadRegistry.init();
    }

    /**
     * Start sever.
     */
    @PostConstruct
    public void start() throws Exception {
        // 获取实际运行类的简单类名(如 GrpcSdkServer 或 GrpcClusterServer)
        String serverName = getClass().getSimpleName();
        Loggers.REMOTE.info("Nacos {} Rpc server starting at port {}", serverName, getServicePort());

        // 抽象方法,由子类实现具体的服务器启动逻辑
        startServer();

        if (RpcServerSslContextRefresherHolder.getSdkInstance() != null) {
            RpcServerSslContextRefresherHolder.getSdkInstance().refresh(this);
        }

        if (RpcServerSslContextRefresherHolder.getClusterInstance() != null) {
            RpcServerSslContextRefresherHolder.getClusterInstance().refresh(this);
        }

        /**
         * (获取主端口8848) + rpcPortOffset() (子类实现的偏移量)
         * - GrpcSdkServer: 8848 + 1000 = 9848 (客户端SDK通信)
         * - GrpcClusterServer: 8848 + 1001 = 9849 (集群节点间通信)
         */
        Loggers.REMOTE.info("Nacos {} Rpc server started at port {}", serverName, getServicePort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Loggers.REMOTE.info("Nacos {} Rpc server stopping", serverName);
            try {
                BaseRpcServer.this.stopServer();
                Loggers.REMOTE.info("Nacos {} Rpc server stopped successfully...", serverName);
            } catch (Exception e) {
                Loggers.REMOTE.error("Nacos {} Rpc server stopped fail...", serverName, e);
            }
        }));

    }

    /**
     * get connection type.
     *
     * @return connection type.
     */
    public abstract ConnectionType getConnectionType();

    /**
     * Reload protocol context if necessary.
     *
     * <p>
     * protocol like:
     * <li>Tls</li>
     * </p>
     */
    public abstract void reloadProtocolContext();

    /**
     * Start sever.
     *
     * @throws Exception exception throw if start server fail.
     */
    public abstract void startServer() throws Exception;

    /**
     * the increase offset of nacos server port for rpc server port.
     *
     * @return delta port offset of main port.
     */
    public abstract int rpcPortOffset();

    /**
     * get service port.
     *
     * @return service port.
     */
    public int getServicePort() {
        // (获取主端口8848) + rpcPortOffset() (子类实现的偏移量)
        return EnvUtil.getPort() + rpcPortOffset();
    }

    /**
     * Stop Server.
     *
     * @throws Exception throw if stop server fail.
     */
    public final void stopServer() throws Exception {
        shutdownServer();
    }

    /**
     * the increase offset of nacos server port for rpc server port.
     */
    @PreDestroy
    public abstract void shutdownServer();

}
