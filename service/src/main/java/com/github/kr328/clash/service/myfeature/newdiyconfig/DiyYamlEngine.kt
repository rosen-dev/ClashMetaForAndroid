package com.github.kr328.clash.service.myfeature.newdiyconfig

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer

/**
 * YAML 排版引擎：负责将 Map 数据转换为像素级一致的 YAML 字符串。
 * 剥离目的：封装 SnakeYAML 的复杂配置，确保排版风格（缩进、换行、排序）在全项目统一。
 */
object DiyYamlEngine {

    /**
     * 将配置字典转换为 YAML 字符串。
     */
    fun dump(sections: Map<String, Any>): String {
        val dumperOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0 // 使用紧凑风格：- name
            width = 4096       // 防止长字符串被强制换行
        }

        val representer = object : Representer(dumperOptions) {
            init {
                // 确保字符串中的换行符（如脚本或多行文本）能被正确识别并使用双引号包裹
                this.representers[String::class.java] = Represent { data ->
                    val value = data as String
                    val style = if (value.contains('\n')) DumperOptions.ScalarStyle.DOUBLE_QUOTED
                    else DumperOptions.ScalarStyle.PLAIN
                    representScalar(getTag(data.javaClass, Tag.STR), value, style)
                }
            }
        }

        return Yaml(representer, dumperOptions).dump(sections)
    }
}
