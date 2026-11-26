package com.unicom.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "unicom_monitor"

    private val EMOJI_SETS = mapOf(
        "classic" to listOf("💔", "❤️", "🌸", "🍃", "🌙", "☀️", "✨", "⏳"),
        "animal" to listOf("🐶", "🐱", "🐼", "🐸", "🦊", "🐰", "🐻", "🕐"),
        "color" to listOf("🟥", "🟩", "🟦", "🟨", "🟪", "🟧", "🟫", "⬛"),
        "tech" to listOf("⚡", "💧", "📡", "🔋", "🛰", "📶", "💾", "🕒"),
    )

    fun emojis(set: String): List<String> = EMOJI_SETS[set] ?: EMOJI_SETS["classic"]!!

    fun buildText(data: BalanceData, emojiSet: String): String {
        val e = emojis(emojiSet)
        return buildString {
            appendLine(data.packageName)
            appendLine()
            appendLine("${e[0]} 跳 ➠ ${data.dailyMb}M")
            appendLine("${e[1]} 免 ➠ 0M")
            appendLine("${e[2]} 总流量 ➠ ${"%.2f".format(data.totalGb)}G")
            appendLine("${e[3]} 已消耗 ➠ ${"%.2f".format(data.usedGb)}G")
            appendLine("${e[4]} 免流已用 ➠ ${"%.2f".format(data.freeUsedGb)}G")
            appendLine("${e[5]} 今日跳量 ➠ ${data.dailyMb}M")
            appendLine("${e[6]} 剩余流量 ➠ ${"%.2f".format(data.remainGb)}G")
            appendLine("————————————————")
            appendLine("${e[7]} 联通时间 ➠ ${data.updateTime}")
            appendLine()
            appendLine("💰 剩余话费 ➠ ${data.fee}元")
            appendLine("📞 剩余语音 ➠ ${data.voice}分钟")
            appendLine("⭐ 可用积分 ➠ ${data.point}")
        }
    }

    fun notify(context: Context, title: String, text: String, imagePath: String? = null) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "联通流量监控", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText("点击查看流量详情")
            .setContentIntent(pi)
            .setAutoCancel(true)
        if (imagePath != null && imagePath.isNotEmpty()) {
            val f = java.io.File(imagePath)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(imagePath)
                if (bmp != null) {
                    builder.setLargeIcon(bmp)
                    builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bmp).bigText(text))
                } else {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
                }
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
            }
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }
        val n = builder.build()
        manager.notify(1001, n)
    }
}
