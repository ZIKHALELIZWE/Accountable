package com.thando.accountable.workers

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thando.accountable.AccountableRepository.Companion.copyAppMediaToExternalFolder
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.ui.AccountableNotification
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class BackupWorker(
    val context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {
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

            val dbFile: File = context.getDatabasePath("accountable_database")
            val fileName = "Backup (${getDateTimeFromMillis()})"
            var saveFile = docFile.findFile(fileName)
            saveFile?.delete()
            saveFile = docFile.createDirectory(fileName)
            saveFile?.createFile("application/x-sqlite3", dbFile.name + ".db")
                ?.let { outputFile ->
                    context.contentResolver.openOutputStream(outputFile.uri)
                        .use { output ->
                            context.contentResolver.openInputStream(dbFile.toUri())
                                .use { input ->
                                    if (output != null) {
                                        input?.copyTo(output)
                                    }
                                }
                        }
                }

            val imageFile =
                DocumentFile.fromFile(File(context.filesDir.toString() + File.separator + AppResources.ImageResource.DESTINATION_FOLDER))
            val videoFile =
                DocumentFile.fromFile(File(context.filesDir.toString() + File.separator + AppResources.VideoResource.DESTINATION_FOLDER))
            val audioFile =
                DocumentFile.fromFile(File(context.filesDir.toString() + File.separator + AppResources.AudioResource.DESTINATION_FOLDER))
            val documentFile =
                DocumentFile.fromFile(File(context.filesDir.toString() + File.separator + AppResources.DocumentResource.DESTINATION_FOLDER))
            val itemsSaved = AtomicInteger(0)
            val totalItems = imageFile.listFiles().size.plus(
                videoFile.listFiles().size.plus(
                    audioFile.listFiles().size.plus(
                        documentFile.listFiles().size
                    )
                )
            )
            AccountableNotification.createProgressNotification(
                context,
                "Backing Up"
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
                "Backing Up Accountable Data",
                "Backup Complete"
            )

            Result.success()
        } catch (_: Exception) {
            AccountableNotification.createMessageNotification(
                context,
                "Backing Up Accountable Data",
                "Something Went Wrong!"
            )
            Result.failure()
        }
    }

    private fun getDateTimeFromMillis(): String {
        val simpleDateFormat =
            SimpleDateFormat("dd-MM-yyyy-hh:mm", Locale.getDefault()).format(Date())
        return simpleDateFormat.format(System.currentTimeMillis())
    }
}