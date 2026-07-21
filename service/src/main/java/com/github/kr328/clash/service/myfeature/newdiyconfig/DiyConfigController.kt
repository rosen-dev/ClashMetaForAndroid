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
     * 职责：作为配置文件的“前置加工厂”，在 Clash Core 真正下载前介入。
     * 作用：
     * 1. 拦截远程 URL 并下载原始 YAML。
     * 2. 调用 ConfigModifier 注入自定义策略组、分流脚本和本地 RuleProvider 钩子。
     * 3. 将修改后的终极配置注册到本地 DiyRuleServer。
     *
     * 为什么在 ProfileProcessor 的 apply 和 update 中都要调用？
     * - 在 apply 中调用是为了“第一次洗礼”：确保新保存的配置即带自定义功能。
     * - 在 update 中调用是为了“生命延续”：确保后续自动更新不会抹除我们的自定义注入。
     *
     * @return 修改后的本地服务器 URL (或在失败时回退到原始 source)
     */
    suspend fun processProfile(context: Context, type: Profile.Type, source: String): String {
        // 仅处理远程 URL
        if (type != Profile.Type.Url || !source.startsWith("http")) {
            return source
        }

        return try {
            AppLogger.d("DIY_CONTROLLER: 开始拦截处理 URL: $source")

            // 1. 确保服务器启动 (如果尚未启动)
            DiyRuleServer.start()

            // 2. 确保规则已注册 (如果尚未注册)
            setupStaticRules(context)

            // 3. 下载原始配置
            val downloadResult = Downloader.download(source).getOrThrow()

            // 4. 使用排版引擎修改配置
            val modifiedYaml = ConfigModifier.modify(downloadResult.content, DiyRuleServer.baseUrl)

            // 5. 组装响应头（包含套餐到期时间和剩余流量信息）
            val responseHeaders = mutableMapOf<String, String>()
            downloadResult.subscriptionUserInfo?.let { responseHeaders["subscription-userinfo"] = it }
            downloadResult.profileUpdateInterval?.let { responseHeaders["profile-update-interval"] = it }

            // 6. 注入主配置及响应头到本地服务器
            DiyRuleServer.register("/config.yaml", responseHeaders) { modifiedYaml }

            // 返回本地生成的 URL
            val finalUrl = "${DiyRuleServer.baseUrl}/config.yaml"
            AppLogger.d("DIY_CONTROLLER: 处理成功, 最终 URL: $finalUrl")
            finalUrl
        } catch (e: Exception) {
            AppLogger.e("DIY_CONTROLLER: 处理失败，回退到原始 URL", e)
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
