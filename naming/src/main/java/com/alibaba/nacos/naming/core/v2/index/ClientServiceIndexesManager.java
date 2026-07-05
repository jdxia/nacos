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

package com.alibaba.nacos.naming.core.v2.index;

import com.alibaba.nacos.common.notify.Event;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.common.notify.listener.SmartSubscriber;
import com.alibaba.nacos.common.trace.DeregisterInstanceReason;
import com.alibaba.nacos.common.trace.event.naming.DeregisterInstanceTraceEvent;
import com.alibaba.nacos.common.utils.ConcurrentHashSet;
import com.alibaba.nacos.naming.core.v2.client.Client;
import com.alibaba.nacos.naming.core.v2.event.client.ClientOperationEvent;
import com.alibaba.nacos.naming.core.v2.event.publisher.NamingEventPublisherFactory;
import com.alibaba.nacos.naming.core.v2.event.service.ServiceEvent;
import com.alibaba.nacos.naming.core.v2.pojo.InstancePublishInfo;
import com.alibaba.nacos.naming.core.v2.pojo.Service;
import com.alibaba.nacos.naming.push.v2.NamingSubscriberServiceV2Impl;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Client and service index manager.
 *
 * @author xiweng.yy
 */
@Component
public class ClientServiceIndexesManager extends SmartSubscriber {

    // index就是用来查询的, 这个类的作用就是用来查询的

    /**
     * 记录服务有哪些提供者, 也就是一个服务有哪些客户端注册了实例
     * <service, clientId>
     * 这边找到clientId就能找到client, 找到client 就能找到 这个 <service, InstancePublishInfo>
     */
    private final ConcurrentMap<Service, Set<String>> publisherIndexes = new ConcurrentHashMap<>();

    /**
     *  记录服务有哪些消费者 clientId
     *  也就是一个服务有哪些客户端订阅了实例, 有变化方便找他们然后推送过去
     */
    private final ConcurrentMap<Service, Set<String>> subscriberIndexes = new ConcurrentHashMap<>();

    public ClientServiceIndexesManager() {
        NotifyCenter.registerSubscriber(this, NamingEventPublisherFactory.getInstance());
    }

    public Collection<String> getAllClientsRegisteredService(Service service) {
        return publisherIndexes.containsKey(service) ? publisherIndexes.get(service) : new ConcurrentHashSet<>();
    }

    public Collection<String> getAllClientsSubscribeService(Service service) {
        return subscriberIndexes.containsKey(service) ? subscriberIndexes.get(service) : new ConcurrentHashSet<>();
    }

    public Collection<Service> getSubscribedService() {
        return subscriberIndexes.keySet();
    }

    /**
     * Clear the service index without instances.
     *
     * @param service The service of the Nacos.
     */
    public void removePublisherIndexesByEmptyService(Service service) {
        if (publisherIndexes.containsKey(service) && publisherIndexes.get(service).isEmpty()) {
            publisherIndexes.remove(service);
        }
    }

    @Override
    public List<Class<? extends Event>> subscribeTypes() {
        List<Class<? extends Event>> result = new LinkedList<>();
        result.add(ClientOperationEvent.ClientRegisterServiceEvent.class);
        result.add(ClientOperationEvent.ClientDeregisterServiceEvent.class);
        result.add(ClientOperationEvent.ClientSubscribeServiceEvent.class);
        result.add(ClientOperationEvent.ClientUnsubscribeServiceEvent.class);
        result.add(ClientOperationEvent.ClientReleaseEvent.class);
        return result;
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof ClientOperationEvent.ClientReleaseEvent) {

            // 客户端释放
            handleClientDisconnect((ClientOperationEvent.ClientReleaseEvent) event);
        } else if (event instanceof ClientOperationEvent) {
            // 往下
            handleClientOperation((ClientOperationEvent) event);
        }
    }

    private void handleClientDisconnect(ClientOperationEvent.ClientReleaseEvent event) {
        Client client = event.getClient();

        // 拿到这个client所有订阅的
        for (Service each : client.getAllSubscribeService()) {
            // 移除订阅的
            removeSubscriberIndexes(each, client.getClientId());
        }
        DeregisterInstanceReason reason = event.isNative()
                ? DeregisterInstanceReason.NATIVE_DISCONNECTED : DeregisterInstanceReason.SYNCED_DISCONNECTED;
        long currentTimeMillis = System.currentTimeMillis();

        // 拿到client所有注册的服务
        for (Service each : client.getAllPublishedService()) {

            // 移除
            removePublisherIndexes(each, client.getClientId());
            InstancePublishInfo instance = client.getInstancePublishInfo(each);
            NotifyCenter.publishEvent(new DeregisterInstanceTraceEvent(currentTimeMillis,
                    "", false, reason, each.getNamespace(), each.getGroup(), each.getName(),
                    instance.getIp(), instance.getPort()));
        }
    }

    private void handleClientOperation(ClientOperationEvent event) {
        Service service = event.getService();
        String clientId = event.getClientId();

        /**
         * 先看下这个类一开始的属性
         * 重点
         */

        if (event instanceof ClientOperationEvent.ClientRegisterServiceEvent) {
            /**
             *  服务注册, 添加 {@link ClientServiceIndexesManager#publisherIndexes} 也就是添加索引
             *  记录服务有哪些提供者, 也就是一个服务有哪些客户端注册了实例
             *
             * 重点
             *  然后里面还会发个事件, 目的是服务订阅者需要知道这个服务注册了, 延迟通知
             */
            addPublisherIndexes(service, clientId);
        } else if (event instanceof ClientOperationEvent.ClientDeregisterServiceEvent) {
            // 服务注销, 删除 publisherIndexes
            removePublisherIndexes(service, clientId);
        } else if (event instanceof ClientOperationEvent.ClientSubscribeServiceEvent) {
            // 服务订阅, 添加 subscriberIndexes
            addSubscriberIndexes(service, clientId);
        } else if (event instanceof ClientOperationEvent.ClientUnsubscribeServiceEvent) {
            // 服务解约, 移除 subscriberIndexes
            removeSubscriberIndexes(service, clientId);
        }
    }

    private void addPublisherIndexes(Service service, String clientId) {
        /**
         * 先看下这个类一开始的属性
         * 重点
         */

        // 把clientId 添加到 <service, clientId>
        publisherIndexes.computeIfAbsent(service, key -> new ConcurrentHashSet<>()).add(clientId);

        /**
         * 发布事件, 服务改变事件, 目的是服务订阅者需要知道这个服务注册了, 延迟通知
         * {@link NamingSubscriberServiceV2Impl#onEvent(Event)}
         */
        NotifyCenter.publishEvent(new ServiceEvent.ServiceChangedEvent(service, true));
    }

    private void removePublisherIndexes(Service service, String clientId) {
        publisherIndexes.computeIfPresent(service, (s, ids) -> {
            ids.remove(clientId);
            NotifyCenter.publishEvent(new ServiceEvent.ServiceChangedEvent(service, true));
            return ids.isEmpty() ? null : ids;
        });
    }

    private void addSubscriberIndexes(Service service, String clientId) {
        // 利用 subscriberIndexes 记录一下服务有哪些订阅者clientId, 后续服务实例信息发生了变化就会给这些client发通知
        Set<String> clientIds = subscriberIndexes.computeIfAbsent(service, key -> new ConcurrentHashSet<>());
        // Fix #5404, Only first time add need notify event.
        if (clientIds.add(clientId)) {
            /**
             * 发送事件
             * 这个事件是在 {@link NamingSubscriberServiceV2Impl#onEvent(Event)} 这里处理的
             */
            NotifyCenter.publishEvent(new ServiceEvent.ServiceSubscribedEvent(service, clientId));
        }
    }

    private void removeSubscriberIndexes(Service service, String clientId) {
        Set<String> clientIds = subscriberIndexes.get(service);
        if (clientIds == null) {
            return;
        }
        clientIds.remove(clientId);
        if (clientIds.isEmpty()) {
            subscriberIndexes.remove(service);
        }
    }
}
