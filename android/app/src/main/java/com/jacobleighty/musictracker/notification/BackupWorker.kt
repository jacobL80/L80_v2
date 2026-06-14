package com.jacobleighty.musictracker.notification

import android.content.Context
import android.os.Environment
import androidx.work.*
import com.google.gson.GsonBuilder
import com.jacobleighty.musictracker.data.*
import java.io.File
import java.time.LocalDate

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private data class BackupData(
        val exportedAt: String,
        val artists: List<Artist>,
        val history: List<HistoryEntry>,
        val concerts: List<Concert>,
        val tvShows: List<TvShow>,
        val runningWeeks: List<RunningWeek>,
    )

    override suspend fun doWork(): Result {
        val api = ApiService.create()
        return try {
            val backup = BackupData(
                exportedAt = LocalDate.now().toString(),
                artists = api.getArtists(),
                history = api.getHistory(),
                concerts = api.getConcerts(),
                tvShows = api.getTvShows(),
                runningWeeks = api.getRunningWeeks(),
            )
            val json = GsonBuilder().setPrettyPrinting().create().toJson(backup)
            val dir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: applicationContext.filesDir
            dir.listFiles { f -> f.name.startsWith("l80_backup_") && f.name.endsWith(".json") }
                ?.forEach { it.delete() }
            File(dir, "l80_backup_${LocalDate.now()}.json").writeText(json)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
