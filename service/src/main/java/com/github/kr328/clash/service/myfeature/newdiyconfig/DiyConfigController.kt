package com.github.kr328.clash.service.myfeature.newdiyconfig

import android.content.Context
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.myfeature.AppLogger

/**
 * 业务总调度：协调各个模块完成配置修改和规则下发。
 */
object DiyConfigController {
    // 静态规则文件映射：本地请求路径 -> Assets 文件夹下的文件名
    private val STATIC_FILE_MAP = mapOf(
        "/RuleSet_Whitelist.yaml" to "RuleSet_Whitelist.yaml",
        "/RuleSet_Blacklist.yaml" to "RuleSet_Blacklist.yaml",
        "/RuleSet_Direct.yaml" to "RuleSet_Direct.yaml",
        "/RuleSet_Priority_Whitelist.yaml" to "RuleSet_Priority_Whitelist.yaml"
    )

    /**
     * 伴随 VPN 启动的初始化。
     */
    fun initialize(context: Context) {
        DiyRuleServer.start()
        setupStaticRules(context)
        AppLogger.d("DIY_CONTROLLER: 初始化完成")
    }

    /**
     * 停止所有 Diy 相关服务。
     */
    fun destroy() {
        DiyRuleServer.stop()
        AppLogger.d("DIY_CONTROLLER: 已停止服务")
    }

    /**
     * 处理配置文件的核心入口。
     *
     * 职责：拦截配置导入/更新，直接在本地生成纯网关配置。
     * 无论 source 是什么 URL，均忽略外部网络下载，直接在本地秒级就绪。
     *
     * @return 修改后的本地服务器 URL (http://127.0.0.1:6789/config.yaml)
     */
    suspend fun processProfile(context: Context, type: Profile.Type, source: String): String {
        // 仅处理远程 URL 类型的导入与更新
        if (type != Profile.Type.Url) {
            return source
        }

        return try {
            AppLogger.d("DIY_CONTROLLER: 收到配置导入/更新请求 (源: $source)，直接生成本地纯网关配置")

            // 1. 确保服务器启动 (如果尚未启动)
            DiyRuleServer.start()

            // 2. 确保规则已注册 (如果尚未注册)
            setupStaticRules(context)

            // 3. 直接生成本地网关配置 (含 8899 端口、Local-Chain-Proxy 与白名单规则)
            val generatedYaml = ConfigModifier.generate(DiyRuleServer.baseUrl)

            // 4. 注入主配置到本地服务器
            DiyRuleServer.register("/config.yaml") { generatedYaml }

            // 返回本地生成的 URL
            val finalUrl = "${DiyRuleServer.baseUrl}/config.yaml"
            AppLogger.d("DIY_CONTROLLER: 本地配置就绪, 最终 URL: $finalUrl")
            finalUrl
        } catch (e: Exception) {
            AppLogger.e("DIY_CONTROLLER: 生成本地配置失败，回退到原始 URL", e)
            source
        }
    }

    /**
     * 将 Assets 中的静态规则注册到本地服务器。
     */
    private fun setupStaticRules(context: Context) {
        STATIC_FILE_MAP.forEach { (path, assetName) ->
            DiyRuleServer.register(path) {
                try {
                    context.assets.open(assetName).bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    AppLogger.e("DIY_CONTROLLER: 无法读取 Asset 文件: $assetName", e)
                    "payload: [] # 错误: 无法加载资源"
                }
            }
        }
    }
}
