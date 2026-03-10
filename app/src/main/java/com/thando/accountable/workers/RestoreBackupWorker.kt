package com.thando.accountable.workers

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thando.accountable.AccountableRepository.Companion.copyAppMediaToExternalFolder
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.database.AccountableDatabase
import com.thando.accountable.ui.AccountableNotification
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class RestoreBackupWorker(
    val context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context,workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val uri: Uri = (inputData.getString("uri") ?: run {
                return Result.failure()
            }).toUri()

            val docFile = DocumentFile.fromTreeUri(context, uri) ?: run {
                AccountableNotification.createMessageNotification(
                    context,
                    "Reading Downloads Directory",
                    "Can't access Accountable directory!"
                )
                return Result.failure()
            }
            if (!docFile.canRead()) {
                AccountableNotification.createMessageNotification(
                    context,
                    "Reading Downloads Directory",
                    "Can't access Accountable directory!"
                )
                return Result.failure()
            }

            AccountableDatabase.closeDatabase()
            val inputDbFile = docFile.findFile("accountable_database.db")
            val dbFile: File = context.getDatabasePath("accountable_database")
            context.contentResolver.openOutputStream(dbFile.toUri()).use { output ->
                inputDbFile?.uri?.let { inputFile ->
                    context.contentResolver.openInputStream(inputFile).use { input ->
                        if (output != null) {
                            input?.copyTo(output)
                        }
                    }
                }
            }

            val saveFile = DocumentFile.fromFile(context.filesDir)
            val imageFile = docFile.findFile(AppResources.ImageResource.DESTINATION_FOLDER)
            val videoFile = docFile.findFile(AppResources.VideoResource.DESTINATION_FOLDER)
            val audioFile = docFile.findFile(AppResources.AudioResource.DESTINATION_FOLDER)
            val documentFile =
                docFile.findFile(AppResources.DocumentResource.DESTINATION_FOLDER)
            val itemsSaved = AtomicInteger(0)
            val totalItems = (
                    imageFile?.listFiles()?.size?.plus(
                        videoFile?.listFiles()?.size?.plus(
                            audioFile?.listFiles()?.size?.plus(
                                documentFile?.listFiles()?.size!!
                            ) ?: 0
                        ) ?: 0
                    ) ?: 0)
            AccountableNotification.createProgressNotification(
                context,
                "Restoring Back Up"
            ) { notification ->
                withContext(MainActivity.IO) {
                    copyAppMediaToExternalFolder(
                        context,
                        AppResources.ImageResource.DESTINATION_FOLDER,
                        imageFile,
                        saveFile,
                        notification,
                        itemsSaved,
                        totalItems
                    )
                    copyAppMediaToExternalFolder(
                        context,
                        AppResources.VideoResource.DESTINATION_FOLDER,
                        videoFile,
                        saveFile,
                        notification,
                        itemsSaved,
                        totalItems
                    )
                    copyAppMediaToExternalFolder(
                        context,
                        AppResources.AudioResource.DESTINATION_FOLDER,
                        audioFile,
                        saveFile,
                        notification,
                        itemsSaved,
                        totalItems
                    )
                    copyAppMediaToExternalFolder(
                        context,
                        AppResources.DocumentResource.DESTINATION_FOLDER,
                        documentFile,
                        saveFile,
                        notification,
                        itemsSaved,
                        totalItems
                    )
                }
            }

            AccountableNotification.createMessageNotification(
                context,
                "Restoring Back Up",
                "Restoration Complete"
            )

            Result.success()
        } catch (_: Exception) {
            AccountableNotification.createMessageNotification(
                context,
                "Restoring Back Up",
                "Something Went Wrong!"
            )

            Result.failure()
        }
    }

}