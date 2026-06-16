package com.jacobleighty.musictracker.notification

import android.content.Context
import androidx.work.*
import com.jacobleighty.musictracker.Constants
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    const val CAT_MUSIC = "music"
    const val CAT_CONCERTS = "concerts"
    const val CAT_TV = "tv"

    private const val BACKUP_WORK = "monthly_backup"

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(Constants.NOTIF_PREFS, Context.MODE_PRIVATE)
        scheduleNotificationCategory(
            context, CAT_MUSIC,
            prefs.getInt(Constants.PREF_NOTIF_HOUR_MUSIC, Constants.DEFAULT_NOTIF_HOUR),
            ExistingPeriodicWorkPolicy.KEEP,
        )
        scheduleNotificationCategory(
            context, CAT_CONCERTS,
            prefs.getInt(Constants.PREF_NOTIF_HOUR_CONCERTS, Constants.DEFAULT_NOTIF_HOUR),
            ExistingPeriodicWorkPolicy.KEEP,
        )
        scheduleNotificationCategory(
            context, CAT_TV,
            prefs.getInt(Constants.PREF_NOTIF_HOUR_TV, Constants.DEFAULT_NOTIF_HOUR),
            ExistingPeriodicWorkPolicy.KEEP,
        )
        scheduleBackup(context)
    }

    fun rescheduleCategory(context: Context, category: String, hour: Int) {
        scheduleNotificationCategory(context, category, hour, ExistingPeriodicWorkPolicy.REPLACE)
    }

    private fun scheduleNotificationCategory(
        context: Context,
        category: String,
        hour: Int,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        val now = LocalDateTime.now()
        val nextTime = now.toLocalDate().atTime(LocalTime.of(hour, 0)).let {
            if (it.isAfter(now)) it else it.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextTime).seconds

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.SECONDS)
            .setInputData(workDataOf(NotificationWorker.KEY_CATEGORY to category))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "notifications_$category",
            policy,
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
