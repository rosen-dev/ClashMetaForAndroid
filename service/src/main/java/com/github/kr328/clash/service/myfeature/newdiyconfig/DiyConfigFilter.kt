package com.github.kr328.clash.service.myfeature.newdiyconfig

import org.yaml.snakeyaml.Yaml

/**
 * 配置过滤器：负责从原始机场配置中提取关键信息并清理多余段落。
 * 剥离目的：使 ConfigModifier 专注于“如何构建新配置”，而不必关心“如何解析旧配置”。
 */
object DiyConfigFilter {

    /**
     * 从原始配置中提取所有节点的名称。
     */
    fun extractNodeNames(config: String): List<String> {
        val yaml = Yaml()
        return try {
            val configMap = yaml.load<Map<String, Any>>(config)
            val proxies = configMap["proxies"] as? List<Map<String, Any>> ?: emptyList()
            proxies.mapNotNull { it["name"] as? String }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保留原始配置的基础设置，剔除我们会重写的 rules, proxy-groups, rule-providers 和 script 块。
     */
    fun preserveBaseConfig(config: String): String {
        val discardKeywords = setOf(
            "proxy-groups:",
            "rules:",
            "rule-providers:",
            "script:"
        )

        return buildString {
            var inSectionToDiscard = false
            for (line in config.lines()) {
                // 判断是否为顶级键（非缩进、非列表项）
                val isTopLevel = line.isNotEmpty()
                        && !line.startsWith(" ")
                        && !line.startsWith("\t")
                        && !line.startsWith("-")

                if (isTopLevel) {
                    val trimmedLine = line.trim()
                    if (!trimmedLine.startsWith("#")) {
                        // 检查是否命中黑名单关键字
                        val matchesDiscardKey = discardKeywords.any { keyword -> trimmedLine.startsWith(keyword) }
                        if (matchesDiscardKey) {
                            inSectionToDiscard = true
                        } else if (trimmedLine.contains(":")) {
                            // 遇到其他顶级键，停止丢弃
                            inSectionToDiscard = false
                        }
                    }
                }
                if (!inSectionToDiscard) {
                    appendLine(line)
                }
            }
        }.trimEnd()
    }
}
