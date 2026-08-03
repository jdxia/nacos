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

package com.alibaba.nacos.api;

/**
 * Nacos 客户端初始化属性的键名集合.
 *
 * <p>这些常量只是传入 {@link java.util.Properties} 的键，不会主动读取或校验配置。客户端通常通过
 * {@code NacosClientProperties} 按“构造参数、JVM 系统属性、操作系统环境变量”的顺序查找同名配置；该顺序可由
 * {@code nacos.env.first} 调整。因此，同一个键同时出现在多个配置源时，必须先确认实际生效来源。</p>
 *
 * <p>所有数值最终都以字符串传入。除非单项注释另有说明，时间配置的单位均为毫秒。配置错误可能在客户端初始化时才表现为
 * 数字转换异常、线程池创建失败或连接超时，生产环境应在发布前完成取值范围校验。</p>
 *
 * @author Nacos
 */
public class PropertyKeyConst {

    /**
     * 是否启用云环境命名空间解析，布尔值，默认值为 {@code true}.
     *
     * <p>启用后，Config 和 Naming 会优先从 ACM/ANS 云环境信息及
     * {@link SystemEnv#ALIBABA_ALIWARE_NAMESPACE} 获取命名空间；只有这些值为空时才回退到
     * {@link #NAMESPACE}。关闭后不再读取上述云环境命名空间。</p>
     *
     * <p>生产风险：默认开启意味着容器或宿主机中遗留的云环境变量可能覆盖代码显式传入的 namespace。</p>
     */
    public static final String IS_USE_CLOUD_NAMESPACE_PARSING = "isUseCloudNamespaceParsing";

    /**
     * 是否启用 endpoint 占位符及云环境解析规则，布尔值，默认值为 {@code true}.
     *
     * <p>启用后，{@link #ENDPOINT} 可写成 {@code ${propertyName:defaultValue}}；客户端会解析指定属性，
     * 并以 {@link SystemEnv#ALIBABA_ALIWARE_ENDPOINT_URL} 作为云环境回退来源。即使 endpoint 不是占位符，
     * 非空的云环境 endpoint 也可能覆盖它。关闭后 endpoint 按普通字符串使用。</p>
     */
    public static final String IS_USE_ENDPOINT_PARSING_RULE = "isUseEndpointParsingRule";

    /**
     * 地址服务器的域名或 IP，不是最终 Nacos Server 的地址.
     *
     * <p>客户端会访问该地址下的 server-list 接口，动态获取真正的 Nacos Server 列表。端口、上下文路径和列表名分别由
     * {@link #ENDPOINT_PORT}、{@link #ENDPOINT_CONTEXT_PATH} 和 {@link #ENDPOINT_CLUSTER_NAME} 控制。</p>
     *
     * <p>不要与 {@link #SERVER_ADDR} 同时配置：当前版本中 Config 优先使用 serverAddr，而 Naming 优先使用 endpoint，
     * 同一客户端可能因此连接到两个不同集群。</p>
     */
    public static final String ENDPOINT = "endpoint";

    /**
     * Config 地址服务器请求所附加的原始查询参数，例如 {@code key=value&region=cn-hangzhou}.
     *
     * <p>值中不要包含开头的 {@code ?}。当前版本仅 Config 的地址服务器路径消费该配置，Naming 不消费；参数会直接拼接，
     * 客户端不负责 URL 编码。当前 2.4.3 在已追加 namespace 时存在只追加分隔符而漏掉参数值的实现缺陷，组合使用前必须验证
     * 最终请求 URL。不要放入密码、Token 等敏感信息，因为完整地址可能出现在日志和网关访问记录中。</p>
     */
    public static final String ENDPOINT_QUERY_PARAMS = "endpointQueryParams";

    /**
     * 地址服务器的 HTTP 端口，整数，默认值为 {@code 8080}.
     *
     * <p>该端口属于 {@link #ENDPOINT}，不是 Nacos 默认 HTTP 端口 8848，也不是 gRPC 端口 9848/9849。
     * {@link SystemEnv#ALIBABA_ALIWARE_ENDPOINT_PORT} 非空时优先于本配置。</p>
     */
    public static final String ENDPOINT_PORT = "endpointPort";

    /**
     * 地址服务器接口的上下文路径.
     *
     * <p>它只决定客户端到地址服务器的查询路径；为空时回退到 {@link #CONTEXT_PATH}。云环境中的
     * {@link SystemEnv#ALIBABA_ALIWARE_ENDPOINT_CONTEXT_PATH} 非空时优先。该值与 Nacos Server 自身部署路径是两个概念。</p>
     */
    public static final String ENDPOINT_CONTEXT_PATH = "endpointContextPath";

    /**
     * 地址服务器上的 server-list 名称，默认值为 {@code serverlist}.
     *
     * <p>它参与组成地址服务器查询 URL，用来选择要返回的服务器列表。该值不是 Naming 服务实例的 clusterName，
     * 也不会把客户端注册到某个业务集群。新配置应使用本键，不要再使用兼容键 {@link #CLUSTER_NAME}。</p>
     */
    public static final String ENDPOINT_CLUSTER_NAME = "endpointClusterName";

    /**
     * Config 客户端的内部服务器列表标识.
     *
     * <p>未配置时客户端根据固定地址或 endpoint 自动生成；该标识会出现在日志中，并参与本地配置快照的隔离。
     * 当前版本 Naming 不消费此键。生产环境若手工指定，必须保证不同 Nacos 集群使用不同值，否则可能读取到其他集群的旧快照。</p>
     */
    public static final String SERVER_NAME = "serverName";

    /**
     * Nacos 命名空间 ID，而不是控制台展示名称.
     *
     * <p>它用于隔离 Config 的 dataId/group 和 Naming 的服务/实例。未配置时 Config 使用空 tenant 语义，Naming 最终回退到
     * {@code public}。当 {@link #IS_USE_CLOUD_NAMESPACE_PARSING} 为 {@code true} 时，云环境命名空间可能优先于本值。</p>
     */
    public static final String NAMESPACE = "namespace";

    /**
     * Nacos 内置用户名/密码鉴权的用户名.
     *
     * <p>用户名为空时，内置鉴权客户端不会发起登录；配置用户名后应同时配置 {@link #PASSWORD}。它与 RAM 的
     * {@link #ACCESS_KEY} 是两套独立凭据，不可互换。</p>
     */
    public static final String USERNAME = "username";

    /**
     * 与 {@link #USERNAME} 配套的 Nacos 登录密码.
     *
     * <p>属于敏感信息，不得硬编码、打印或写入可提交的配置文件。尤其不能在生产环境开启
     * {@link #LOG_ALL_PROPERTIES}，否则该值会未经脱敏直接写入客户端初始化日志。</p>
     */
    public static final String PASSWORD = "password";

    /**
     * 阿里云 RAM/STS 请求签名使用的 AccessKey ID.
     *
     * <p>它是凭据标识，不是 Nacos 内置用户名。未显式配置且 {@link #IS_USE_RAM_INFO_PARSING} 开启时，客户端会尝试从
     * 运行环境的 RAM 信息中读取。虽然不是私钥，仍不应无必要地对外暴露或写入业务日志。</p>
     */
    public static final String ACCESS_KEY = "accessKey";

    /**
     * 与 {@link #ACCESS_KEY} 配套的阿里云 RAM/STS SecretKey，用于生成请求签名.
     *
     * <p>这是高敏感密钥，必须通过密钥管理系统或受控环境变量注入，禁止提交到仓库。未显式配置且
     * {@link #IS_USE_RAM_INFO_PARSING} 开启时，客户端会尝试从运行环境读取。</p>
     */
    public static final String SECRET_KEY = "secretKey";

    /**
     * 从实例元数据服务获取 STS 临时凭据时使用的 RAM 角色名.
     *
     * <p>配置后，RAM 鉴权实现会设置该角色并优先使用获取到的 STS 临时凭据进行签名。生产环境需同时确认元数据服务可达、
     * 角色最小权限和凭据刷新链路；不要把角色名误当成 namespace 或 Nacos 用户名。</p>
     */
    public static final String RAM_ROLE_NAME = "ramRoleName";

    /**
     * 固定 Nacos Server 地址列表，例如 {@code 10.0.0.1:8848,10.0.0.2:8848}.
     *
     * <p>Config 支持逗号或分号分隔，并可识别 HTTP/HTTPS 前缀；Naming 的当前实现按逗号分隔。未写端口时 Config 会补
     * 默认端口 8848。不要与 {@link #ENDPOINT} 同时配置，因为当前版本 Config 和 Naming 对两者的优先级不同。</p>
     */
    public static final String SERVER_ADDR = "serverAddr";

    /**
     * Nacos HTTP API 的上下文路径，默认值为 {@code nacos}.
     *
     * <p>当 {@link #ENDPOINT_CONTEXT_PATH} 为空时，它也作为地址服务器上下文路径的回退值。客户端会统一路径格式，
     * 但连续的 {@code //} 会被判定为非法；反向代理改写路径时必须确保客户端、网关和服务端三者一致。</p>
     */
    public static final String CONTEXT_PATH = "contextPath";

    /**
     * 地址服务器列表名的旧兼容键.
     *
     * <p>当前版本仅 Config 的地址管理器在 {@link #ENDPOINT_CLUSTER_NAME} 缺失时回退读取该键，Naming 不读取。
     * 它不是业务服务的 Naming clusterName。新代码必须使用 {@link #ENDPOINT_CLUSTER_NAME}，避免两个模块行为不一致。</p>
     */
    public static final String CLUSTER_NAME = "clusterName";

    /**
     * Config 请求声明的字符编码，默认值为 {@code UTF-8}.
     *
     * <p>该值会写入 Config 请求的 charset 头。只有在服务端、网关和配置内容确实使用同一编码时才应修改；编码不一致会导致
     * 中文乱码、MD5 比对异常或配置内容无法正确解析。</p>
     */
    public static final String ENCODE = "encode";

    /**
     * Config 长轮询超时时间，单位毫秒，历史默认值为 {@code 30000}，最小值为 {@code 10000}.
     *
     * <p>该键源自 1.x HTTP 长轮询模型。当前 2.4.3 gRPC {@code ClientWorker} 仍会解析并保存它，但主流程没有继续消费该字段，
     * 因此不能依赖它调整 gRPC 配置监听时延；使用其他客户端版本时需重新核对实现。</p>
     */
    public static final String CONFIG_LONG_POLL_TIMEOUT = "configLongPollTimeout";

    /**
     * Config 监听失败后的历史重试间隔，单位毫秒，默认值为 {@code 2000}.
     *
     * <p>当前 2.4.3 gRPC {@code ClientWorker} 会解析该值，但主流程没有继续消费对应字段。它不等同于 RPC 自身的重试次数或
     * {@link #CONFIG_REQUEST_TIMEOUT}；不要以为修改本项一定能改变 2.x 客户端的故障恢复速度。</p>
     */
    public static final String CONFIG_RETRY_TIME = "configRetryTime";

    /**
     * Config gRPC 请求超时时间，单位毫秒，默认值为 {@code -1}.
     *
     * <p>负值表示不覆盖 RPC 客户端默认超时；当前默认 RPC 超时为 3000 毫秒。该值会影响查询、发布、删除和监听相关请求。
     * 设置过小会在网络抖动时制造大量失败与重试，设置过大则会延长调用线程阻塞和故障发现时间。</p>
     */
    public static final String CONFIG_REQUEST_TIMEOUT = "configRequestTimeout";

    /**
     * Config Worker 自动计算线程数时允许使用的最大值.
     *
     * <p>未配置时按 CPU 核数向上取 2 的幂；该上限应用后仍保证至少 2 个线程。它只限制自动计算结果，显式设置的
     * {@link #CLIENT_WORKER_THREAD_COUNT} 会在最后覆盖该结果，因此不会受本上限约束。</p>
     */
    public static final String CLIENT_WORKER_MAX_THREAD_COUNT = "clientWorkerMaxThreadCount";

    /**
     * Config Worker 线程池的最终线程数.
     *
     * <p>配置后直接覆盖自动计算值，也会绕过 {@link #CLIENT_WORKER_MAX_THREAD_COUNT} 和最小线程数保护。应配置为合理的正整数；
     * 过大会增加线程与上下文切换成本，非法值可能导致客户端初始化失败。</p>
     */
    public static final String CLIENT_WORKER_THREAD_COUNT = "clientWorkerThreadCount";

    /**
     * Config 传输层的历史最大重试次数，历史默认值为 {@code 3}.
     *
     * <p>当前 2.4.3 {@code ConfigTransportClient} 中初始化该值的方法未接入主流程，字段也没有实际消费点，因此本配置目前
     * 不能作为有效的 gRPC 重试控制项。若升级、降级客户端，需按对应版本源码重新确认。</p>
     */
    public static final String MAX_RETRY = "maxRetry";

    /**
     * 添加 Config Listener 时是否先同步拉取一次远端配置，布尔值，默认值为 {@code false}.
     *
     * <p>开启后，添加监听器的路径会进行同步网络请求，并用返回内容建立本地基线；当前值通常不会作为一次变更回调通知监听器。
     * 这会增加初始化阻塞和对 Nacos 可用性的依赖，还需注意“业务先读取旧值、监听器随后以新值建立基线”造成的时序窗口。</p>
     *
     * 一般不要开启
     * 可能产生“业务对象还是旧值，但监听器已经把新值当基线”的隐蔽时序问题
     */
    public static final String ENABLE_REMOTE_SYNC_CONFIG = "enableRemoteSyncConfig";

    /**
     * Naming 客户端启动时是否从磁盘加载服务实例缓存，布尔值，默认值为 {@code false}.
     *
     * <p>开启可在 Nacos 暂时不可用时更快获得上次实例列表，但缓存可能已经过期。生产环境必须配合实例健康检查和缓存目录隔离，
     * 不能把本地快照当成实时服务发现结果。</p>
     */
    public static final String NAMING_LOAD_CACHE_AT_START = "namingLoadCacheAtStart";

    /**
     * Naming 本地缓存根目录下的附加目录名.
     *
     * <p>最终路径位于 {@code ${JM.SNAPSHOT.PATH}/nacos/<value>/naming/<namespace>} 或
     * {@code ${user.home}/nacos/<value>/naming/<namespace>}。当前实现直接拼接该值，生产配置应只使用受控的相对目录名，
     * 禁止传入绝对路径、{@code ..} 或路径分隔符，以免目录穿越、缓存串用或覆盖非预期文件。</p>
     */
    public static final String NAMING_CACHE_REGISTRY_DIR = "namingCacheRegistryDir";

    /**
     * Naming 1.x 客户端心跳线程数的历史兼容键.
     *
     * <p>当前 2.4.3 客户端已经移除旧 UDP/Beat 线程池，主代码中没有该常量的消费点，配置它不会改变 2.x gRPC 临时实例的
     * 心跳或保活行为。不要用它排查 gRPC 注册掉线问题。</p>
     */
    public static final String NAMING_CLIENT_BEAT_THREAD_COUNT = "namingClientBeatThreadCount";

    /**
     * Naming 订阅服务异步轮询线程数的自动计算上限.
     *
     * <p>默认线程数约为 CPU 相关适配值的一半，且至少为 1。本项只约束自动计算结果；显式配置
     * {@link #NAMING_POLLING_THREAD_COUNT} 后会直接覆盖，因此不会受该上限保护。</p>
     */
    public static final String NAMING_POLLING_MAX_THREAD_COUNT = "namingPollingMaxThreadCount";

    /**
     * Naming 订阅服务异步轮询线程池的最终线程数.
     *
     * <p>仅在 {@link #NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE} 开启后承担周期查询任务。该值直接覆盖自动计算结果，必须为正整数；
     * 过小会使大量订阅排队，过大会增加 Nacos 查询压力，非法值可能导致线程池初始化失败。</p>
     */
    public static final String NAMING_POLLING_THREAD_COUNT = "namingPollingThreadCount";

    /**
     * Naming HTTP 客户端以单域名访问 Nacos 时的最大尝试次数，默认值为 {@code 3}.
     *
     * <p>该配置只作用于 HTTP domain 模式，2.x gRPC 请求和显式多地址轮询不使用它。值为 0 时不会真正发出请求；过大会放大
     * 故障期间的请求耗时和服务端压力，因此应与单次 HTTP 超时一起评估。</p>
     */
    public static final String NAMING_REQUEST_DOMAIN_RETRY_COUNT = "namingRequestDomainMaxRetryCount";

    /**
     * Naming 推送空结果保护开关，布尔值，默认值为 {@code false}.
     *
     * <p>开启后，如果新服务列表中不存在“健康且权重大于 0”的实例，客户端会忽略该次更新并继续保留旧列表。它能降低异常空推送
     * 引发的流量中断，但也可能掩盖合法的全量下线，使流量继续访问陈旧实例；必须配合告警和故障摘除策略使用。</p>
     */
    public static final String NAMING_PUSH_EMPTY_PROTECTION = "namingPushEmptyProtection";

    /**
     * 是否为已订阅服务启动异步周期查询，布尔值，默认值为 {@code false}.
     *
     * <p>开启后，客户端除服务端推送外还会按服务缓存时间调度查询，用于刷新订阅数据；失败时会退避，最长约 60 秒。
     * 大量服务订阅会额外增加客户端线程队列、网络请求和服务端 QPS，需结合轮询线程数与容量评估。</p>
     */
    public static final String NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE = "namingAsyncQuerySubscribeService";

    /**
     * Naming gRPC 重连后重新注册、重新订阅任务的执行间隔，单位毫秒，默认值为 {@code 3000}.
     *
     * <p>该值同时用作首次延迟和固定延迟。必须大于 0；过小会在网络抖动或服务端故障期间形成重放风暴，过大则延长实例和订阅恢复时间。</p>
     */
    public static final String REDO_DELAY_TIME = "redoDelayTime";

    /**
     * Naming gRPC 重做任务线程池的核心线程数，默认值为 {@code 1}.
     *
     * <p>任务负责连接恢复后的注册和订阅重放。通常单线程即可维持顺序并限制冲击；盲目增大可能并发冲击 Nacos Server，
     * 非法负值会导致客户端初始化失败。</p>
     */
    public static final String REDO_DELAY_THREAD_COUNT = "redoDelayThreadCount";

    /**
     * RAM 请求签名使用的地域 ID，例如 {@code cn-hangzhou}.
     *
     * <p>非空时 Config 和 Naming 的 RAM 鉴权会派生 V4 签名密钥，并声明 V4 签名版本；为空时沿用旧签名方式。
     * 地域必须与实际云资源和服务端鉴权配置一致，否则请求会因签名不匹配被拒绝。</p>
     */
    public static final String SIGNATURE_REGION_ID = "signatureRegionId";

    /**
     * 是否在客户端初始化日志中输出全部 Properties，布尔值，默认值为 {@code false}.
     *
     * <p>仅允许在隔离的本地调试环境短时开启。开启后代码会原样输出所有键值，不会对 password、secretKey、Token 或业务自定义
     * 敏感项做脱敏；生产环境开启会造成凭据泄露。排障结束后必须删除该配置并清理已产生的日志。</p>
     */
    public static final String LOG_ALL_PROPERTIES = "logAllProperties";

    /**
     * 是否允许从运行环境自动解析 RAM AccessKey/SecretKey，布尔值，自 2.3.3 起提供，默认值为 {@code true}.
     *
     * <p>只有显式 {@link #ACCESS_KEY} 或 {@link #SECRET_KEY} 为空时才尝试环境回退。设为 {@code false} 仅关闭自动回退，
     * 不会禁用显式传入的 AK/SK 签名；适用于 Java Agent 等不应读取宿主机 RAM 信息的隔离场景。</p>
     */
    public static final String IS_USE_RAM_INFO_PARSING = "isUseRamInfoParsing";

    /**
     * 阿里云环境约定的属性键.
     *
     * <p>这些键通常来自操作系统环境变量，但客户端通过统一属性抽象读取，也可能从构造 Properties 或 JVM 系统属性取得同名值。
     * 默认搜索顺序仍受 {@code nacos.env.first} 影响，不应仅凭类名假定值一定来自环境变量。</p>
     */
    public static class SystemEnv {

        /**
         * 云地址服务器端口，优先于 {@link PropertyKeyConst#ENDPOINT_PORT}；值必须是合法整数.
         */
        public static final String ALIBABA_ALIWARE_ENDPOINT_PORT = "ALIBABA_ALIWARE_ENDPOINT_PORT";

        /**
         * 云地址服务器上下文路径，优先于 {@link PropertyKeyConst#ENDPOINT_CONTEXT_PATH}.
         */
        public static final String ALIBABA_ALIWARE_ENDPOINT_CONTEXT_PATH = "ALIBABA_ALIWARE_ENDPOINT_CONTEXT_PATH";

        /**
         * 云环境命名空间；仅在 {@link PropertyKeyConst#IS_USE_CLOUD_NAMESPACE_PARSING} 开启时参与解析，
         * 并可优先于显式 {@link PropertyKeyConst#NAMESPACE}.
         */
        public static final String ALIBABA_ALIWARE_NAMESPACE = "ALIBABA_ALIWARE_NAMESPACE";

        /**
         * 云地址服务器 URL；在 endpoint 解析规则开启时可作为占位符回退值或覆盖普通 endpoint.
         */
        public static final String ALIBABA_ALIWARE_ENDPOINT_URL = "ALIBABA_ALIWARE_ENDPOINT_URL";
    }

}
