package com.unicom.monitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class MonitorWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = SettingsRepository(applicationContext)
        val s = repo.settings.first()
        var ok = true
        if (s.cookie.isNotBlank()) {
            try {
                val data = UnicomApi.fetchBalance(s.cookie, s.mobile)
                val text = NotificationHelper.buildText(data, s.emojiSet)
                NotificationHelper.notify(applicationContext, "联通余量", text, s.imagePath)
            } catch (e: Exception) {
                ok = false
            }
        }
        // 自调度下一次（支持 1 分钟起）
        val minutes = s.customInterval.toIntOrNull() ?: (s.intervalHours * 60)
        val next = androidx.work.OneTimeWorkRequestBuilder<MonitorWorker>()
            .setInitialDelay(minutes.toLong().coerceAtLeast(1), java.util.concurrent.TimeUnit.MINUTES)
            .build()
        androidx.work.WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork("unicom_monitor", androidx.work.ExistingWorkPolicy.REPLACE, next)
        return if (ok) Result.success() else Result.retry()
    }
}
