package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger

/**
 * 业务配置生成引擎：负责生成本地纯网关配置（包含基础设置、本地链式代理节点、策略组和规则）。
 */
object ConfigModifier {

    // 本地链式代理节点名称与目标端口（指向本机运行的专用客户端）
    const val LOCAL_PROXY_NAME = "Local-Chain-Proxy"
    const val LOCAL_PROXY_PORT = 7890

    /**
     * 生成完整的本地网关 YAML 配置。
     * @param baseUrl 本地规则服务器基础地址，例如 http://127.0.0.1:6789
     */
    fun generate(baseUrl: String): String {
        AppLogger.d("CONFIG_MODIFIER: 开始生成本地网关配置")

        val sections = LinkedHashMap<String, Any>()

        // 1. 基础网络设置 (监听 8899 并允许局域网 PC 接入)
        sections["mixed-port"] = 8899
        sections["allow-lan"] = true
        sections["bind-address"] = "*"
        sections["mode"] = "rule"
        sections["log-level"] = "info"
        sections["ipv6"] = false

        // 2. 本地链式代理节点定义 (SOCKS5 指向 127.0.0.1:7890)
        val proxies = listOf(
            mapOf(
                "name" to LOCAL_PROXY_NAME,
                "type" to "socks5",
                "server" to "127.0.0.1",
                "port" to LOCAL_PROXY_PORT,
                "skip-cert-verify" to true
            )
        )
        sections["proxies"] = proxies

        // 3. 策略组、规则链与数据源
        val myOwnRulesSections = createMyOwnRules(baseUrl)
        sections.putAll(myOwnRulesSections)

        // 4. 通过排版引擎生成规范的 YAML
        return DiyYamlEngine.dump(sections)
    }

    /**
     * 构建自定义的策略组、规则与数据源部分。
     */
    private fun createMyOwnRules(baseUrl: String): LinkedHashMap<String, Any> {
        val whitelistName = "Whitelist"
        val priorityWhitelistName = "Priority-Whitelist"
        val directName = "Direct"
        val blacklistName = "Blacklist"

        // A. 策略组定义 (Whitelist 走 Local-Chain-Proxy)
        val proxyGroups = listOf(
            mapOf("name" to whitelistName, "type" to "select", "proxies" to listOf(LOCAL_PROXY_NAME)),
            // Priority-Whitelist 联动 Whitelist
            mapOf("name" to priorityWhitelistName, "type" to "select", "proxies" to listOf(whitelistName)),
            mapOf("name" to directName, "type" to "select", "proxies" to listOf("DIRECT")),
            mapOf("name" to blacklistName, "type" to "select", "proxies" to listOf("REJECT"))
        )

        // B. 规则链定义 (顺序决定优先级)
        val rules = listOf(
            // 1. 拦截特定设备的广告或视频请求
            "AND,((SRC-IP-CIDR,192.168.10.50/32),(DOMAIN-SUFFIX,googlevideo.com)),REJECT",
            // 2. 先匹配高优先级白名单 (黑名单例外)
            "RULE-SET,$priorityWhitelistName,$priorityWhitelistName",
            // 3. 再匹配黑名单
            "RULE-SET,$blacklistName,$blacklistName",
            // 4. 匹配常规直连与代理
            "RULE-SET,$directName,$directName",
            "RULE-SET,$whitelistName,$whitelistName",
            // 5. 兜底逻辑
            "MATCH,$blacklistName"
        )

        // C. 规则数据源定义 (全部指向本地基站)
        val now = System.currentTimeMillis()
        val ruleProviders = mapOf(
            whitelistName to mapOf("type" to "http", "behavior" to "classical", "url" to "$baseUrl/RuleSet_Whitelist.yaml?v=$now", "path" to "./rules/RuleSet_Whitelist", "interval" to 86400),
            priorityWhitelistName to mapOf("type" to "http", "behavior" to "classical", "url" to "$baseUrl/RuleSet_Priority_Whitelist.yaml?v=$now", "path" to "./rules/RuleSet_Priority_Whitelist", "interval" to 86400),
            directName to mapOf("type" to "http", "behavior" to "classical", "url" to "$baseUrl/RuleSet_Direct.yaml?v=$now", "path" to "./rules/RuleSet_Direct", "interval" to 86400),
            blacklistName to mapOf("type" to "http", "behavior" to "classical", "url" to "$baseUrl/RuleSet_Blacklist.yaml?v=$now", "path" to "./rules/RuleSet_Blacklist", "interval" to 86400)
        )

        // 使用 LinkedHashMap 保证 YAML 中的 Section 顺序：策略组 -> 规则 -> 数据源
        val sections = LinkedHashMap<String, Any>()
        sections["proxy-groups"] = proxyGroups
        sections["rules"] = rules
        sections["rule-providers"] = ruleProviders

        return sections
    }
}
