package com.jacobleighty.musictracker.notification

import android.content.Context
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    private const val NOTIFICATION_WORK = "daily_notifications"
    private const val BACKUP_WORK = "monthly_backup"

    fun schedule(context: Context) {
        scheduleNotifications(context)
        scheduleBackup(context)
    }

    private fun scheduleNotifications(context: Context) {
        val now = LocalDateTime.now()
        val next7am = now.toLocalDate().atTime(LocalTime.of(7, 0)).let {
            if (it.isAfter(now)) it else it.plusDays(1)
        }
        val initialDelay = Duration.between(now, next7am).seconds

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleBackup(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(14, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BACKUP_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
