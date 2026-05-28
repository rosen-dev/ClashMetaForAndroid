package com.github.kr328.clash.service.myfeature.newdiyconfig

import com.github.kr328.clash.service.myfeature.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 远程配置下载器：负责从机场 URL 获取原始 YAML 内容。
 */
object Downloader {
    private val client = OkHttpClient()

    /**
     * 从给定的 URL 下载内容。
     * @param url 远程配置文件的 URL
     * @return Result<String> 包含成功下载的字符串或失败的异常
     */
    suspend fun download(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                AppLogger.d("DI_DOWNLOADER: 开始下载, URL: $url")
                val request = Request.Builder()
                    .url(url)
                    // 使用标准的 User-Agent，确保能正确获取到订阅
                    .header("User-Agent", "ClashMetaForAndroid/2.11.1")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("下载失败: HTTP ${response.code}")
                    }
                    val body = response.body?.string() ?: throw Exception("响应体为空")
                    AppLogger.d("DI_DOWNLOADER: 下载成功, 内容长度: ${body.length}")
                    body
                }
            }
        }
    }
}
