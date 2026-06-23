package com.jacobleighty.musictracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.jacobleighty.musictracker.MainActivity
import com.jacobleighty.musictracker.data.ApiService
import com.jacobleighty.musictracker.ui.DateUtils
import java.time.LocalDate

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val category = inputData.getString(KEY_CATEGORY) ?: return Result.success()
        val api = ApiService.create()
        val today = LocalDate.now()
        val notifs = mutableListOf<Pair<String, String>>()

        when (category) {
            WorkerScheduler.CAT_MUSIC -> try {
                api.getArtists()
                    .filter { !it.hiatus && DateUtils.hasFullDate(it.nextRelease) && DateUtils.parseDate(it.nextRelease) == today }
                    .forEach { notifs.add("New Release Today" to "${it.name} – ${it.albumTitle}") }
            } catch (_: Exception) {}

            WorkerScheduler.CAT_CONCERTS -> try {
                api.getConcerts()
                    .filter { !it.attended && DateUtils.hasFullDate(it.date) && DateUtils.parseDate(it.date) == today }
                    .forEach { notifs.add("Your Concert is Today!" to "${it.band} @ ${it.venue}") }
            } catch (_: Exception) {}

            WorkerScheduler.CAT_TV -> try {
                api.getTvShows()
                    .filter { !it.watched && DateUtils.hasFullDate(it.date) && DateUtils.parseDate(it.date) == today }
                    .forEach { notifs.add("New Release Today" to it.programName) }
            } catch (_: Exception) {}

            else -> return Result.success()
        }

        val notifIdBase = when (category) {
            WorkerScheduler.CAT_CONCERTS -> 200
            WorkerScheduler.CAT_TV -> 300
            else -> 100
        }

        if (notifs.isNotEmpty()) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            ensureChannel(nm)
            val intent = PendingIntent.getActivity(
                applicationContext, 0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            notifs.forEachIndexed { i, (title, text) ->
                val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .build()
                nm.notify(notifIdBase + i, notif)
            }
        }

        return Result.success()
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Upcoming Events", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "upcoming_events"
        const val KEY_CATEGORY = "category"
    }
}
