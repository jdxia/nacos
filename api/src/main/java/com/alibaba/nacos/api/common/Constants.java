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

package com.alibaba.nacos.api.common;

import java.util.concurrent.TimeUnit;

/**
 * Nacos 客户端、服务端共享的协议常量.
 *
 * <p>这里同时包含 HTTP 参数名、HTTP 头、gRPC 模块标识、Naming 默认值以及少量历史兼容常量。
 * 其中很多字符串是对外协议的一部分，不能仅因为当前仓库没有直接调用方就修改或删除，
 * 否则可能破坏旧版客户端、插件或二方扩展的兼容性。</p>
 *
 * @author Nacos
 */
public class Constants {

    /**
     * 历史 Config HTTP 协议使用的客户端版本标识.
     *
     * <p>当前客户端的真实版本由 {@code VersionUtils} 生成，不应用该常量判断实际 SDK 版本；
     * 该常量在当前开源代码中主要作为历史兼容定义保留。</p>
     */
    public static final String CLIENT_VERSION = "3.0.0";

    /**
     * Config 长轮询协议从 2.0.4 开始将变更结果放入响应体的版本分界值，{@code 204} 表示 2.0.4.
     *
     * <p>旧客户端的变更结果放在 {@link #PROBE_MODIFY_RESPONSE} 等响应头中。
     * 当前 API 模块保留该值用于协议兼容。</p>
     */
    public static final int DATA_IN_BODY_VERSION = 204;

    /**
     * 未显式指定 group 时使用的默认分组，Config 和 Naming 共用.
     */
    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    /**
     * 应用名在 HTTP/gRPC 连接标签中的协议键.
     *
     * <p>该值用于识别配置或注册请求来自哪个应用，不是 Naming 的 {@code serviceName}。</p>
     */
    public static final String APPNAME = "AppName";

    /**
     * 服务端对外展示客户端版本时使用的连接标签键.
     */
    public static final String CLIENT_VERSION_KEY = "ClientVersion";

    /**
     * 无法识别请求应用名时使用的历史占位值.
     */
    public static final String UNKNOWN_APP = "UnknownApp";

    /**
     * 历史生产环境地址服务域名.
     *
     * <p>当前开源客户端应通过 {@code serverAddr} 或 {@code endpoint} 配置服务端，
     * 不要将该内部历史域名当作公网 Nacos 默认地址。</p>
     */
    public static final String DEFAULT_DOMAINNAME = "commonconfig.config-host.taobao.com";

    /**
     * 历史日常/测试环境地址服务域名；开源部署不应依赖该值.
     */
    public static final String DAILY_DOMAINNAME = "commonconfig.taobao.net";

    /**
     * 用空字符串表示“未指定”的共享占位值.
     *
     * <p>它不是 Java {@code null}，例如 Naming 订阅中可用它表示未限定集群。</p>
     */
    public static final String NULL = "";

    /**
     * Config HTTP 参数或鉴权资源中 Data ID 的键名.
     */
    public static final String DATA_ID = "dataId";

    /**
     * Config 旧版 HTTP 协议中 namespace 的参数名.
     *
     * <p>这里的 {@code tenant} 与 {@link #NAMESPACE_ID} 都可表示命名空间，但属于不同接口的协议键，
     * 不能在 HTTP 请求中随意互换。</p>
     */
    public static final String TENANT = "tenant";

    /**
     * Config/Naming HTTP 参数或鉴权资源中分组名的键名.
     */
    public static final String GROUP = "group";

    /**
     * 新版接口或鉴权解析中 namespace ID 的参数名.
     */
    public static final String NAMESPACE_ID = "namespaceId";

    /**
     * HTTP {@code Last-Modified} 响应头名，用于传递配置的最后修改时间.
     */
    public static final String LAST_MODIFIED = "Last-Modified";

    /**
     * HTTP {@code Accept-Encoding} 请求头名，客户端用它声明可接受的压缩编码.
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * HTTP {@code Content-Encoding} 响应头名，用于识别响应体是否经过 gzip 等压缩.
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /**
     * Config HTTP 长轮询表单参数名，其值携带客户端正在监听的 group key 及 MD5 列表.
     */
    public static final String PROBE_MODIFY_REQUEST = "Listening-Configs";

    /**
     * 2.0.4 以前 Config 短轮询响应中承载旧格式变更列表的 HTTP 头名.
     */
    public static final String PROBE_MODIFY_RESPONSE = "Probe-Modify-Response";

    /**
     * 2.0.4 以前 Config 短轮询响应中承载新格式变更列表的 HTTP 头名.
     */
    public static final String PROBE_MODIFY_RESPONSE_NEW = "Probe-Modify-Response-New";

    /**
     * 历史 Config HTTP 协议中表示“启用压缩”的字符串布尔值.
     */
    public static final String USE_ZIP = "true";

    /**
     * Config HTTP 响应中配置内容 MD5 的头名，用于变更检测和一致性校验.
     */
    public static final String CONTENT_MD5 = "Content-MD5";

    /**
     * 历史 Config HTTP 协议中的配置版本头名；当前开源主流路径主要使用 MD5 判断变更.
     */
    public static final String CONFIG_VERSION = "Config-Version";

    /**
     * Config HTTP 响应中配置格式的头名，例如 {@code properties}、{@code yaml} 或 {@code json}.
     */
    public static final String CONFIG_TYPE = "Config-Type";

    /**
     * Config 加密插件传递“加密数据密钥”的头/字段名.
     *
     * <p>该值属于密文元数据，不是 Nacos 鉴权用的 access token。</p>
     */
    public static final String ENCRYPTED_DATA_KEY = "Encrypted-Data-Key";

    /**
     * 历史 Config HTTP 条件请求使用的 {@code If-Modified-Since} 头名.
     */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * 历史地址服务通过 HTTP 头返回客户端下次拉取间隔时使用的键名.
     */
    public static final String SPACING_INTERVAL = "client-spacing-interval";

    /**
     * Config v1 HTTP API 的根路径.
     */
    public static final String BASE_PATH = "/v1/cs";

    /**
     * Config v1 配置发布、查询、删除 API 路径，即 {@code /v1/cs/configs}.
     */
    public static final String CONFIG_CONTROLLER_PATH = BASE_PATH + "/configs";

    /**
     * 历史通用 token 字段名；当前默认鉴权登录响应使用 {@link #ACCESS_TOKEN}.
     */
    public static final String TOKEN = "token";

    /**
     * 默认 Nacos 鉴权登录响应和后续 HTTP 请求携带 JWT 时使用的参数名.
     */
    public static final String ACCESS_TOKEN = "accessToken";

    /**
     * 登录响应中 token 剩余有效时长的字段名，当前默认鉴权插件以秒为单位.
     */
    public static final String TOKEN_TTL = "tokenTtl";

    /**
     * 登录响应中标识当前用户是否具有全局管理员角色的布尔字段名.
     */
    public static final String GLOBAL_ADMIN = "globalAdmin";

    /**
     * 登录、请求上下文和审计中用户名的参数/字段名.
     */
    public static final String USERNAME = "username";

    /**
     * 历史登录响应中 token 预刷新窗口的字段名；当前默认登录响应未直接返回该字段.
     */
    public static final String TOKEN_REFRESH_WINDOW = "tokenRefreshWindow";

    /**
     * SDK gRPC 端口相对 HTTP 主端口的默认偏移量；主端口 8848 时默认为 9848.
     */
    public static final Integer SDK_GRPC_PORT_DEFAULT_OFFSET = 1000;

    /**
     * 集群节点间 gRPC 端口相对 HTTP 主端口的默认偏移量；主端口 8848 时默认为 9849.
     */
    public static final Integer CLUSTER_GRPC_PORT_DEFAULT_OFFSET = 1001;

    /**
     * * 历史地址列表异步刷新的默认间隔，单位：秒.
     */
    public static final int ASYNC_UPDATE_ADDRESS_INTERVAL = 300;

    /**
     * * 历史短轮询的默认间隔，单位：秒.
     */
    public static final int POLLING_INTERVAL_TIME = 15;

    /**
     * * 历史 Config HTTP 单次网络操作的默认超时，单位：毫秒.
     */
    public static final int ONCE_TIMEOUT = 2000;

    /**
     * * 历史 Config HTTP Socket 读超时，单位：毫秒.
     */
    public static final int SO_TIMEOUT = 60000;

    /**
     * * Config 客户端长轮询默认挂起时间，单位：毫秒.
     */
    public static final int CONFIG_LONG_POLL_TIMEOUT = 30000;

    /**
     * * Config 客户端允许的最小长轮询时间，单位：毫秒；传入更小值会被抬高到此值.
     */
    public static final int MIN_CONFIG_LONG_POLL_TIMEOUT = 10000;

    /**
     * * Config 监听任务失败后的默认惩罚/重试延迟，单位：毫秒.
     */
    public static final int CONFIG_RETRY_TIME = 2000;

    /**
     * Config 传输请求的默认最大重试次数.
     *
     * <p>生产环境不应盲目调大：故障时每个客户端的重试会叠加，可能放大服务端压力。</p>
     */
    public static final int MAX_RETRY = 3;

    /**
     * * 历史 Config 接收操作最大等待时间，默认为 {@link #ONCE_TIMEOUT} 的 5 倍，单位：毫秒.
     */
    public static final int RECV_WAIT_TIMEOUT = ONCE_TIMEOUT * 5;

    /**
     * 历史字符集名称，表示 UTF-8；新代码优先使用 {@code StandardCharsets.UTF_8}.
     */
    public static final String ENCODE = "UTF-8";

    /**
     * 历史 Config 本地映射文件名；当前 API 模块保留作为兼容常量.
     */
    public static final String MAP_FILE = "map-file.js";

    /**
     * 历史 Config 客户端流控阈值；当前 API 模块中无直接消费方.
     */
    public static final int FLOW_CONTROL_THRESHOLD = 20;

    /**
     * 历史 Config 流控时间槽数；需与具体流控实现一起解读，不是线程数.
     */
    public static final int FLOW_CONTROL_SLOT = 10;

    /**
     * 历史 Config 流控统计窗口间隔，单位：毫秒.
     */
    public static final int FLOW_CONTROL_INTERVAL = 1000;

    /**
     * Naming 服务保护阈值的默认值.
     *
     * <p>{@code 0.0F} 是最低阈值，但并不等于完全禁用保护：源码判断条件为
     * {@code healthyRatio <= threshold}，因此存在实例但健康实例数为 0 时仍会达到保护阈值。</p>
     */
    public static final float DEFAULT_PROTECT_THRESHOLD = 0.0F;

    /**
     * Config 监听协议的记录分隔符，字符值为 ASCII 1（SOH）.
     */
    public static final String LINE_SEPARATOR = Character.toString((char) 1);

    /**
     * Config 协议内部字段/内容分隔符，字符值为 ASCII 2（STX）.
     *
     * <p>增量发布内容不能包含该字符，否则解析时会把内容误切分。</p>
     */
    public static final String WORD_SEPARATOR = Character.toString((char) 2);

    /**
     * Config 长轮询文本协议的 CRLF 行分隔符.
     */
    public static final String LONGPOLLING_LINE_SEPARATOR = "\r\n";

    /**
     * Config HTTP 请求中客户端应用名的头名.
     */
    public static final String CLIENT_APPNAME_HEADER = "Client-AppName";

    /**
     * Config HTTP 请求中客户端时间戳的头名，当前值为 Unix 毫秒时间戳.
     */
    public static final String CLIENT_REQUEST_TS_HEADER = "Client-RequestTS";

    /**
     * Config HTTP 历史请求签名头名.
     *
     * <p>客户端当前以 {@code MD5(timestamp + appKey)} 生成该值；它不等价于 JWT 鉴权，
     * 不应单独当作互联网边界的安全机制。</p>
     */
    public static final String CLIENT_REQUEST_TOKEN_HEADER = "Client-RequestToken";

    /**
     * 历史 Config 原子/批量操作的默认条目上限；当前 API 模块中无直接消费方.
     */
    public static final int ATOMIC_MAX_SIZE = 1000;

    /**
     * Naming 默认实例 ID 的字段分隔符，默认格式为 {@code ip#port#cluster#serviceName}.
     */
    public static final String NAMING_INSTANCE_ID_SPLITTER = "#";

    /**
     * 默认 Naming 实例 ID 的历史字段数，对应 {@code ip#port#cluster#serviceName} 的 4 段.
     *
     * <p>Snowflake 生成器输出的是 {@code id#cluster#serviceName} 三段格式，不能用该常量统一拆分所有生成器结果。</p>
     */
    public static final int NAMING_INSTANCE_ID_SEG_COUNT = 4;

    /**
     * Naming HTTP 健康检查配置中多个请求头的分隔正则，字面值为竖线 {@code |}.
     */
    public static final String NAMING_HTTP_HEADER_SPLITTER = "\\|";

    /**
     * Naming 实例未指定 cluster 时的默认集群名.
     */
    public static final String DEFAULT_CLUSTER_NAME = "DEFAULT";

    /**
     * Naming 临时实例默认心跳超时，单位：毫秒.
     *
     * <p>实例元数据可覆盖该值；超时后实例先变为不健康，不是立即删除。</p>
     */
    public static final long DEFAULT_HEART_BEAT_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    /**
     * Naming 临时实例默认删除超时，单位：毫秒.
     *
     * <p>连续无更新超过该时间后才进入过期删除路径，与“不健康”阈值不是同一个概念。</p>
     */
    public static final long DEFAULT_IP_DELETE_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    /**
     * Naming 临时实例默认心跳间隔，单位：毫秒.
     */
    public static final long DEFAULT_HEART_BEAT_INTERVAL = TimeUnit.SECONDS.toMillis(5);

    /**
     * Naming HTTP API 未指定 namespace 时使用的公共命名空间 ID.
     */
    public static final String DEFAULT_NAMESPACE_ID = "public";

    /**
     * 客户端是否默认尝试从阿里云/ANS 环境信息解析 namespace.
     *
     * <p>如果显式传入 {@code namespace}，仍需注意云环境来源的优先级；
     * 希望完全以显式配置为准时应关闭对应的 cloud namespace parsing 开关。</p>
     */
    public static final boolean DEFAULT_USE_CLOUD_NAMESPACE_PARSING = true;

    /**
     * 集群写请求重定向使用的 HTTP 307 状态码；307 会保留原请求方法和请求体.
     */
    public static final int WRITE_REDIRECT_CODE = 307;

    /**
     * Naming 组合名和 ServiceInfo 缓存 key 的分隔符.
     *
     * <p>常见格式为 {@code groupName@@serviceName@@clusters}；业务名称不应嵌入 {@code @@}，
     * 否则组合名拆分会产生歧义。</p>
     */
    public static final String SERVICE_INFO_SPLITER = "@@";

    /**
     * 仅包含 {@code groupName@@serviceName} 时的历史分段数；ServiceInfo 缓存 key 还可能包含第三段 clusters.
     */
    public static final int SERVICE_INFO_SPLIT_COUNT = 2;

    /**
     * Naming 开关配置中将状态显式重置为空字符串时使用的文本占位值 {@code "null"}.
     */
    public static final String NULL_STRING = "null";

    /**
     * Naming 参数校验中表示“只允许非负整数字符”的正则.
     */
    public static final String NUMBER_PATTERN_STRING = "^\\d+$";

    /**
     * 正则表达式中匹配任意字符串的片段，常用于 Naming 模糊查询.
     */
    public static final String ANY_PATTERN = ".*";

    /**
     * 默认 Naming 实例 ID 生成器类型，结果由 IP、端口、集群和服务名组成.
     */
    public static final String DEFAULT_INSTANCE_ID_GENERATOR = "simple";

    /**
     * Snowflake Naming 实例 ID 生成器的 SPI 类型名.
     */
    public static final String SNOWFLAKE_INSTANCE_ID_GENERATOR = "snowflake";

    /**
     * 历史 HTTP scheme 文本，值中不包含 {@code ://}；新 URL 组装代码应使用 RequestUrlConstants.
     */
    public static final String HTTP_PREFIX = "http";

    /**
     * 通配文本 {@code *}；不是 Java 正则，需由具体协议层解释.
     */
    public static final String ALL_PATTERN = "*";

    /**
     * 主机和端口组装时使用的冒号分隔符.
     */
    public static final String COLON = ":";

    /**
     * Unix 换行符 LF.
     */
    public static final String LINE_BREAK = "\n";

    /**
     * 井号字符，与 {@link #NAMING_INSTANCE_ID_SPLITTER} 字面值相同，但这里作为通用文本常量保留.
     */
    public static final String POUND = "#";

    /**
     * 地址服务下发的自动 Config Tag 头/连接标签名.
     *
     * <p>该 Tag 可用于选择 Config Tag 版本，与 {@link #CONFIG_GRAY_LABEL} 的应用自定义灰度标签不是同一机制。</p>
     */
    public static final String VIPSERVER_TAG = "Vipserver-Tag";

    /**
     * 地址服务响应中的历史 Amory 环境标签名，客户端会透传到 Config gRPC 连接标签.
     */
    public static final String AMORY_TAG = "Amory-Tag";

    /**
     * 地址服务响应中的地域/位置标签名，客户端会透传到 Config gRPC 连接标签.
     */
    public static final String LOCATION_TAG = "Location-Tag";

    /**
     * Config HTTP 请求中声明字符集的头/参数键，默认通常为 UTF-8.
     */
    public static final String CHARSET_KEY = "charset";

    /**
     * Naming cluster 名校验正则：只允许英文字母、数字和连字符 {@code -}.
     */
    public static final String CLUSTER_NAME_PATTERN_STRING = "^[0-9a-zA-Z-]+$";

    /**
     * * Naming gRPC 断线重连后重做注册/订阅的默认延迟，单位：毫秒.
     */
    public static final long DEFAULT_REDO_DELAY_TIME = 3000L;

    /**
     * Naming gRPC redo 任务的默认工作线程数.
     */
    public static final int DEFAULT_REDO_THREAD_COUNT = 1;

    /**
     * Config 客户端自定义应用连接标签的配置键.
     *
     * <p>值格式为 {@code k1=v1,k2=v2}，支持通过 ConfigService {@code Properties}、JVM 参数或环境变量提供。
     * 标签 key/value 只允许字母、数字、下划线、连字符和点，单项最长 128 字符；非法项会被忽略。
     * 这些标签会出现在 INFO 日志和连接元数据中，禁止放入密码、token 等敏感信息。</p>
     */
    public static final String APP_CONN_LABELS_KEY = "nacos.app.conn.labels";

    /**
     * 点号分隔符，当前主要用于将标签配置键转换为环境变量名.
     */
    public static final String DOT = ".";

    /**
     * 历史标签来源权重配置的字段名；当前收集器已改为显式优先级，未直接使用该常量.
     */
    public static final String WEIGHT = "weight";

    /**
     * 标签来源“ConfigService Properties”的文本名；当前作为协议/兼容常量保留.
     */
    public static final String PROPERTIES_KEY = "properties";

    /**
     * {@link #APP_CONN_LABELS_PREFERRED} 中表示优先使用 JVM system properties 标签的值.
     */
    public static final String JVM_KEY = "jvm";

    /**
     * {@link #APP_CONN_LABELS_PREFERRED} 中表示优先使用环境变量标签的值.
     */
    public static final String ENV_KEY = "env";

    /**
     * 调整应用连接标签来源优先级的环境变量名.
     *
     * <p>默认优先级为 {@code Properties > JVM > ENV}。设为 {@code jvm} 时为
     * {@code JVM > Properties > ENV}；设为 {@code env} 时为 {@code ENV > Properties > JVM}。
     * 其他值不会切换优先来源。</p>
     */
    public static final String APP_CONN_LABELS_PREFERRED = "nacos_app_conn_labels_preferred";

    /**
     * 客户端向 gRPC 连接传输应用自定义标签时增加的内部前缀.
     *
     * <p>例如 {@code nacos.config.gray.label=gray-01} 在连接底层传输为
     * {@code app_nacos.config.gray.label=gray-01}；服务端 {@code ConnectionMeta#getAppLabels()} 会去掉该前缀。
     * 业务代码不应自行在 {@link #APP_CONN_LABELS_KEY} 的 key 上添加该前缀。</p>
     */
    public static final String APP_CONN_PREFIX = "app_";

    /**
     * Config 客户端内置的单值灰度标签配置键.
     *
     * <p>例如 {@code nacos.config.gray.label=gray-01} 表示当前 ConfigService 节点属于 {@code gray-01}
     * 标签组。它只是连接标签，不是开启灰度的开关；服务端还必须存在匹配该 key/value
     * 的灰度配置版本才会下发不同内容。Properties 方式只对使用该对象创建的 ConfigService 生效，
     * 且必须在创建 ConfigService 前设置。</p>
     *
     * <p>开源 nacos-client 2.3.2+ 具备标签采集和上传能力；当前开源 2.4.3 服务端仓库中只能确认连接标签的接收、保存和读取方法，
     * 未包含 MSE 控制台的“基于标签灰度发布”完整服务端实现。
     * 自建开源 Nacos 时不能仅凭设置该键就断言灰度已生效。</p>
     */
    public static final String CONFIG_GRAY_LABEL = "nacos.config.gray.label";

    /**
     * 是否默认尝试从运行环境解析 RAM AK/SK 的字符串布尔默认值.
     *
     * <p>自 2.3.3 起可通过 {@code isUseRamInfoParsing=false} 关闭环境解析，用于 Java Agent 等
     * 不应读取宿主环境 RAM 信息的场景。生产中应明确凭据来源，避免误读环境凭据。</p>
     */
    public static final String DEFAULT_USE_RAM_INFO_PARSING = "true";

    /**
     * The constants in config directory.
     */
    public static class Config {

        /**
         * gRPC 请求分发、鉴权资源类型和模块健康状态中代表 Config 模块的标识.
         */
        public static final String CONFIG_MODULE = "config";

        /**
         * Config gRPC 查询请求的 notify 头名，用于区分变更通知后的拉取场景.
         */
        public static final String NOTIFY_HEADER = "notify";
    }

    /**
     * The constants in naming directory.
     */
    public static class Naming {

        /**
         * gRPC 请求分发、鉴权资源类型和模块健康状态中代表 Naming 模块的标识.
         */
        public static final String NAMING_MODULE = "naming";

        /**
         * Naming CMDB 选择器上下文的类型名，用于按实例/消费者元数据执行标签选择.
         */
        public static final String CMDB_CONTEXT_TYPE = "CMDB";
    }

    /**
     * The constants in remote directory.
     */
    public static class Remote {

        /**
         * 连接检测、重置、Setup ACK 等 gRPC 内部控制请求的模块标识.
         */
        public static final String INTERNAL_MODULE = "internal";
    }

    /**
     * The constants in exception directory.
     */
    public static class Exception {

        /**
         * API 序列化失败的 Nacos 运行时异常错误码.
         */
        public static final int SERIALIZE_ERROR_CODE = 100;

        /**
         * API 反序列化失败的 Nacos 运行时异常错误码.
         */
        public static final int DESERIALIZE_ERROR_CODE = 101;

        /**
         * 数据源类型为空或未找到对应 Mapper 时的插件错误码.
         */
        public static final int FIND_DATASOURCE_ERROR_CODE = 102;

        /**
         * 数据源插件中未找到目标表 Mapper 时的错误码.
         */
        public static final int FIND_TABLE_ERROR_CODE = 103;
    }
}
