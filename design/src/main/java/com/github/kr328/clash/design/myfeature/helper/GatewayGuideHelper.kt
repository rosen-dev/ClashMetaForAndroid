package com.github.kr328.clash.design.myfeature.helper

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.DialogGatewayGuideBinding
import com.github.kr328.clash.design.util.layoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 局域网网关使用指南弹窗辅助类：负责装配数据并弹出 Material 风格的使用指引对话框。
 */
object GatewayGuideHelper {
    suspend fun showDialog(context: Context) {
        withContext(Dispatchers.Main) {
            val wifiIp = GatewayHelper.getWifiIpAddress()
                ?: context.getString(R.string.gateway_guide_ip_unknown)

            val binding = DialogGatewayGuideBinding.inflate(context.layoutInflater).apply {
                this.ipAddress = wifiIp
                this.gatewayPort = GatewayHelper.GATEWAY_PORT
                this.upstreamPort = GatewayHelper.UPSTREAM_PORT
            }

            val dialog = AlertDialog.Builder(context)
                .setView(binding.root)
                .show()

            binding.btnOk.setOnClickListener {
                dialog.dismiss()
            }
        }
    }
}
