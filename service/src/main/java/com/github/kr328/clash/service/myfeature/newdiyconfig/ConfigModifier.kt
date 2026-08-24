package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger

/**
 * 业务配置生成引擎：负责生成本地纯网关配置（包含基础设置、本地链式代理节点、策略组和规则）。
 */
object ConfigModifier {

    // 本地链式代理节点名称与目标端口（指向本机运行的专用客户端）
    const val LOCAL_PROXY_NAME = "Local-Chain-Proxy"
    const val LOCAL_PROXY_PORT = 7890
    const val GATEWAY_MIXED_PORT = 8899

    // 核心策略组常量定义（消除魔法字符串）
    private const val WHITELIST_GROUP = "Whitelist"
    private const val PRIORITY_WHITELIST_GROUP = "Priority-Whitelist"
    private const val DIRECT_GROUP = "Direct"
    private const val BLACKLIST_GROUP = "Blacklist"

    /**
     * 生成完整的本地网关 YAML 配置。
     * @param baseUrl 本地规则服务器基础地址，例如 http://127.0.0.1:6789
     */
    fun generate(baseUrl: String): String {
        AppLogger.d("CONFIG_MODIFIER: 开始生成本地网关配置")

        val sections = LinkedHashMap<String, Any>()

        // 1. 基础网络设置 (监听 8899 并允许局域网 PC 接入，开启 Meta 性能优化)
        sections.putAll(createBaseSettings())

        // 2. 本地链式代理节点定义 (SOCKS5 指向 127.0.0.1:7890)
        sections["proxies"] = createProxies()

        // 3. 策略组定义
        sections["proxy-groups"] = createProxyGroups()

        // 4. 规则链定义 (严格按照优先级排序)
        sections["rules"] = createRules()

        // 5. 规则数据源定义 (全部指向本地 NanoHTTPD 基站)
        sections["rule-providers"] = createRuleProviders(baseUrl)

        // 6. 通过排版引擎生成规范的 YAML
        return DiyYamlEngine.dump(sections)
    }

    /**
     * 构建基础网络设置。
     */
    private fun createBaseSettings(): LinkedHashMap<String, Any> {
        val base = LinkedHashMap<String, Any>()
        base["mixed-port"] = GATEWAY_MIXED_PORT
        base["allow-lan"] = true
        base["bind-address"] = "*"
        base["mode"] = "rule"
        base["log-level"] = "info"
        base["ipv6"] = false
        base["find-process-mode"] = "off"
        base["tcp-concurrent"] = true
        return base
    }

    /**
     * 构建本地链式代理节点。
     */
    private fun createProxies(): List<Map<String, Any>> {
        return listOf(
            mapOf(
                "name" to LOCAL_PROXY_NAME,
                "type" to "socks5",
                "server" to "127.0.0.1",
                "port" to LOCAL_PROXY_PORT,
                "skip-cert-verify" to true
            )
        )
    }

    /**
     * 构建自定义策略组。
     */
    private fun createProxyGroups(): List<Map<String, Any>> {
        return listOf(
            mapOf("name" to WHITELIST_GROUP, "type" to "select", "proxies" to listOf(LOCAL_PROXY_NAME)),
            // Priority-Whitelist 联动主白名单出口
            mapOf("name" to PRIORITY_WHITELIST_GROUP, "type" to "select", "proxies" to listOf(WHITELIST_GROUP)),
            mapOf("name" to DIRECT_GROUP, "type" to "select", "proxies" to listOf("DIRECT")),
            mapOf("name" to BLACKLIST_GROUP, "type" to "select", "proxies" to listOf("REJECT"))
        )
    }

    /**
     * 构建规则链列表（顺序自上而下匹配）。
     */
    private fun createRules(): List<String> {
        return listOf(
            // 1. 拦截Windows 电脑192.168.10.50对Googlevideo.com域名的访问
            "AND,((SRC-IP-CIDR,192.168.10.50/32),(DOMAIN-SUFFIX,googlevideo.com)),REJECT",
            // 2. 先匹配高优先级白名单 (黑名单例外)
            "RULE-SET,$PRIORITY_WHITELIST_GROUP,$PRIORITY_WHITELIST_GROUP",
            // 3. 再匹配黑名单
            "RULE-SET,$BLACKLIST_GROUP,$BLACKLIST_GROUP",
            // 4. 匹配常规直连与代理
            "RULE-SET,$DIRECT_GROUP,$DIRECT_GROUP",
            "RULE-SET,$WHITELIST_GROUP,$WHITELIST_GROUP",
            // 5. 兜底逻辑
            "MATCH,$BLACKLIST_GROUP"
        )
    }

    /**
     * 构建规则数据源定义（全部指向本地基站）。
     */
    private fun createRuleProviders(baseUrl: String): Map<String, Any> {
        val now = System.currentTimeMillis()
        return mapOf(
            WHITELIST_GROUP to mapOf("type" to "http", "behavior" to "classical", "format" to "yaml", "url" to "$baseUrl/RuleSet_Whitelist.yaml?v=$now", "path" to "./rules/RuleSet_Whitelist", "interval" to 86400),
            PRIORITY_WHITELIST_GROUP to mapOf("type" to "http", "behavior" to "classical", "format" to "yaml", "url" to "$baseUrl/RuleSet_Priority_Whitelist.yaml?v=$now", "path" to "./rules/RuleSet_Priority_Whitelist", "interval" to 86400),
            DIRECT_GROUP to mapOf("type" to "http", "behavior" to "classical", "format" to "yaml", "url" to "$baseUrl/RuleSet_Direct.yaml?v=$now", "path" to "./rules/RuleSet_Direct", "interval" to 86400),
            BLACKLIST_GROUP to mapOf("type" to "http", "behavior" to "classical", "format" to "yaml", "url" to "$baseUrl/RuleSet_Blacklist.yaml?v=$now", "path" to "./rules/RuleSet_Blacklist", "interval" to 86400)
        )
    }
}
