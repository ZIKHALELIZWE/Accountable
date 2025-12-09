package com.thando.accountable.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.thando.accountable.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class AccountableNotification private constructor(
    val context: Context,
    private val pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
    private var title:String,
    pushNotificationUnit: AtomicReference<(() -> Unit)?>,
    executeUnit: (AccountableNotification) -> Unit
): AutoCloseable {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var builtNotification: NotificationCompat.Builder? = null
    private var previousVal = 0

    init {
        pushNotificationUnit.set {
            buildProgressNotification(title)
            executeUnit.invoke(this)
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @SuppressLint("InlinedApi")
    fun buildProgressNotification(title:String) {
        this.title = title

        // Build the notification
        builtNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stars_black_24dp) // Notification icon
            .setContentTitle(title) // Title displayed in the notification
            .setContentText("Initializing...") // Text displayed in the notification
            .setPriority(NotificationCompat.PRIORITY_LOW) // Notification priority for better visibility
            .setProgress(100, 0, false)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
        previousVal = 0

        // Display the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        notificationManager.notify(NOTIFICATION_ID, builtNotification!!.build())
    }

    @SuppressLint("MissingPermission")
    fun updateNotification(progress: Int,progressMax: Int){
        builtNotification?.let {
            val newVal = Math.round((progress/progressMax.toFloat())*100)
            if (newVal == previousVal) return
            else previousVal = newVal
            if (newVal == 100) {
                it.setContentText("$title Complete")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(NOTIFICATION_ID, it.build())
                notificationManager.cancel(NOTIFICATION_ID)
                builtNotification = null
            } else {
                it.setContentText("${newVal}%")
                    .setProgress(100, newVal, false)
                notificationManager.notify(NOTIFICATION_ID, it.build())
            }
        }
    }

    companion object {
        // Unique channel ID for notifications
        const val CHANNEL_ID = "i.apps.notifications"

        // Unique identifier for the notification
        const val NOTIFICATION_ID = 1234

        // Description for the notification channel
        const val CHANNEL_NAME = "Test notification"

        @SuppressLint("InlinedApi")
        suspend fun createProgressNotification(
            context: Context,
            title: String,
            pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
            pushNotificationUnit: AtomicReference<(() -> Unit)?>,
            executeUnit: (AccountableNotification) -> Unit
        ):AccountableNotification{
            val accountableNotification = AccountableNotification(
                context,
                pushNotificationPermissionLauncher,
                title,
                pushNotificationUnit,
                executeUnit
            )
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else {
                withContext(Dispatchers.Main){
                    accountableNotification.buildProgressNotification(title)
                }
                executeUnit.invoke(accountableNotification)
            }
            return accountableNotification
        }
    }

    override fun close() {
        builtNotification?.setOngoing(false)
            ?.setProgress(0, 0, false)
            ?.setAutoCancel(true)
        notificationManager.notify(NOTIFICATION_ID, builtNotification?.build())
        notificationManager.cancel(NOTIFICATION_ID)
        builtNotification = null
    }
}