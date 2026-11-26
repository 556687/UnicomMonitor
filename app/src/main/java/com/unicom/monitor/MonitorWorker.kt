package com.unicom.monitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class MonitorWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repo = SettingsRepository(applicationContext)
        val s = repo.settings.first()
        if (s.cookie.isBlank()) return Result.success()
        return try {
            val data = UnicomApi.fetchBalance(s.cookie, s.mobile)
            val text = NotificationHelper.buildText(data, s.emojiSet)
            NotificationHelper.notify(applicationContext, "联通余量", text, s.imagePath)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
