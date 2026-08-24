package com.github.kr328.clash.design.myfeature.helper

import com.github.kr328.clash.service.myfeature.AppLogger
import com.github.kr328.clash.service.myfeature.newdiyconfig.ConfigModifier
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 局域网网关辅助工具：维护端口常量并动态解析当前连接的 Wi-Fi / 热点 IP 地址。
 */
object GatewayHelper {
    const val GATEWAY_PORT = ConfigModifier.GATEWAY_MIXED_PORT
    const val UPSTREAM_PORT = ConfigModifier.LOCAL_PROXY_PORT

    /**
     * 仅获取当前 Wi-Fi / 热点局域网 IPv4 地址。
     * 未连接 Wi-Fi 或热点时返回 null。
     */
    fun getWifiIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (nif in interfaces) {
                if (nif.isLoopback || !nif.isUp) continue
                // 仅匹配 wlan (Wi-Fi) 或 ap (热点) 网卡
                if (nif.name.startsWith("wlan") || nif.name.startsWith("ap")) {
                    for (addr in nif.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (!ip.isNullOrBlank()) {
                                return ip
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("GATEWAY_HELPER: 获取 Wi-Fi IP 失败", e)
        }
        return null
    }
}
