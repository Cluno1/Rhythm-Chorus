package io.github.cluno1.sonorus.features.catalog.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.cluno1.sonorus.features.catalog.di.CatalogModule
import io.github.cluno1.sonorus.features.catalog.domain.CatalogFailure
import io.github.cluno1.sonorus.features.catalog.domain.RhythmNowPlayingItem
import java.util.concurrent.TimeUnit

/** Invisible one-way cache fill started when a Catalog rendition begins playing. */
class CatalogAutoCacheWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val workId = inputData.getString(KEY_WORK_ID) ?: return Result.failure()
        val arrangementId = inputData.getString(KEY_ARRANGEMENT_ID) ?: return Result.failure()
        val renditionId = inputData.getString(KEY_RENDITION_ID) ?: return Result.failure()
        return CatalogModule.repository(applicationContext)
            .cachePlaybackAndLatestScore(workId, arrangementId, renditionId)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    Log.w(TAG, "Catalog automatic offline cache failed for $renditionId", error)
                    if (error is CatalogFailure.Unreachable && runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                },
            )
    }

    companion object {
        private const val TAG = "CatalogAutoCache"
        private const val KEY_WORK_ID = "work_id"
        private const val KEY_ARRANGEMENT_ID = "arrangement_id"
        private const val KEY_RENDITION_ID = "rendition_id"
        private const val MAX_RETRIES = 4

        fun enqueue(context: Context, item: RhythmNowPlayingItem) {
            val input = Data.Builder()
                .putString(KEY_WORK_ID, item.workId)
                .putString(KEY_ARRANGEMENT_ID, item.arrangementId)
                .putString(KEY_RENDITION_ID, item.renditionId)
                .build()
            val request = OneTimeWorkRequestBuilder<CatalogAutoCacheWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "catalog-offline-${item.renditionId}",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
