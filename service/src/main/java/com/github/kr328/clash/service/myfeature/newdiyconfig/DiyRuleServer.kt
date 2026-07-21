package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger
import fi.iki.elonen.NanoHTTPD
import java.util.concurrent.ConcurrentHashMap

/**
 * 路由条目定义：包含内容生成函数和可选的 HTTP 响应头
 */
class RouteEntry(
    val provider: () -> String,
    val headers: Map<String, String> = emptyMap()
)

/**
 * 规则分发基站：长效本地 HTTP 服务器。
 * 使用固定端口 6789，为 Clash 提供 RuleProvider 下载。
 */
object DiyRuleServer {
    private const val PORT = 6789
    val baseUrl = "http://127.0.0.1:$PORT"

    // 路由注册表: 路径 -> 路由条目
    private val routes = ConcurrentHashMap<String, RouteEntry>()

    private val server = object : NanoHTTPD(PORT) {
        override fun serve(session: IHTTPSession): Response {
            val entry = routes[session.uri]
            return if (entry != null) {
                AppLogger.d("DI_RULE_SERVER: 响应请求: ${session.uri}")
                val response = newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", entry.provider())
                entry.headers.forEach { (key, value) ->
                    response.addHeader(key, value)
                }
                response
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
     * @param headers 可选的 HTTP 响应头
     * @param provider 返回文件内容的 Lambda 函数
     */
    fun register(path: String, headers: Map<String, String> = emptyMap(), provider: () -> String) {
        routes[path] = RouteEntry(provider, headers)
    }
}
