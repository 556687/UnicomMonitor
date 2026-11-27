package com.unicom.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class MonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(2001, buildForegroundNotification())
        job = scope.launch {
            while (isActive) {
                try {
                    val repo = SettingsRepository(applicationContext)
                    val s = repo.settings.first()
                    if (s.cookie.isNotBlank()) {
                        val d = UnicomApi.fetchBalance(s.cookie, s.mobile)
                        val text = NotificationHelper.buildText(d, s.emojiSet)
                        NotificationHelper.notify(applicationContext, "联通余量", text, s.imagePath)
                    }
                } catch (e: Exception) {
                    // ignore
                }
                val minutes = 30L
                delay(minutes * 60 * 1000L)
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("unicom_service", "联通监控服务", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, "unicom_service")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("联通流量监控")
            .setContentText("后台监控运行中")
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
