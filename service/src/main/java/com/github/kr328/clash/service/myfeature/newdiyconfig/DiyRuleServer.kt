package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.ConcurrentHashMap

/**
 * 规则分发基站：长效本地 HTTP 服务器。
 * 使用固定端口 6789，为 Clash 提供 RuleProvider 下载。
 */
object DiyRuleServer {
    private const val PORT = 6789
    val baseUrl = "http://127.0.0.1:$PORT"

    // 路由注册表: 路径 -> 内容生成函数
    private val routes = ConcurrentHashMap<String, () -> String>()

    private val server = object : NanoHTTPD(PORT) {
        override fun serve(session: IHTTPSession): Response {
            val provider = routes[session.uri]
            return if (provider != null) {
                AppLogger.d("DI_RULE_SERVER: 响应请求: ${session.uri}")
                newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", provider())
            } else {
                AppLogger.d("DI_RULE_SERVER: 路径未找到: ${session.uri}")
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
            }
        }
    }

    /**
     * 启动服务器。
     */
    fun start() {
        if (!server.isAlive) {
            try {
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                AppLogger.d("DI_RULE_SERVER: 服务器已在端口 $PORT 启动")
            } catch (e: Exception) {
                AppLogger.e("DI_RULE_SERVER: 启动失败", e)
            }
        }
    }

    /**
     * 停止服务器。
     */
    fun stop() {
        if (server.isAlive) {
            server.stop()
            AppLogger.d("DI_RULE_SERVER: 服务器已停止")
        }
    }

    /**
     * 注册一个动态或静态文件的路径。
     * @param path 请求路径，如 "/Gemini.yaml"
     * @param provider 返回文件内容的 Lambda 函数
     */
    fun register(path: String, provider: () -> String) {
        routes[path] = provider
    }
}
