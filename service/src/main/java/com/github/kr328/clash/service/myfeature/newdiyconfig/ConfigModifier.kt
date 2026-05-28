package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger

/**
 * 业务配置修改引擎：负责将自定义的策略组和规则缝合进原始配置。
 * 现已重构为“指挥官”角色，具体脏活累活委派给 Filter 和 Engine。
 */
object ConfigModifier {

    /**
     * 修改 YAML 配置的唯一入口。
     */
    fun modify(originalYaml: String, baseUrl: String): String {
        AppLogger.d("CONFIG_MODIFIER: 开始重组配置")

        // 1. 委派过滤器提取原始节点和基础配置
        val nodeNames = DiyConfigFilter.extractNodeNames(originalYaml)
        val baseConfig = DiyConfigFilter.preserveBaseConfig(originalYaml)

        // 2. 定义并组装我们要注入的内容 (策略组、规则、数据源)
        val myOwnRulesSections = createMyOwnRules(nodeNames, baseUrl)

        // 3. 委派排版引擎生成对应的 YAML 片段
        val myRulesYaml = DiyYamlEngine.dump(myOwnRulesSections)

        // 4. 拼接头部基础设置和我们生成的新段落
        return "${baseConfig.trimEnd()}\n\n${myRulesYaml}"
    }

    /**
     * 构建自定义的规则部分。
     * 这里是业务逻辑最集中的地方：定义组、定义规则、定义数据源。
     */
    private fun createMyOwnRules(nodeNames: List<String>, baseUrl: String): LinkedHashMap<String, Any> {
        val whitelistName = "Whitelist"
        val priorityWhitelistName = "Priority-Whitelist"
        val directName = "Direct"
        val blacklistName = "Blacklist"

        // A. 策略组定义
        val proxyGroups = listOf(
            mapOf("name" to whitelistName, "type" to "select", "proxies" to nodeNames),
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
