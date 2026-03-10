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
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

class AccountableNotification private constructor(
    val context: Context,
    private val notificationID: Int,
    private var title:String
): AutoCloseable {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var builtNotification: NotificationCompat.Builder? = null
    private var previousVal = 0

    init {
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
    fun buildMessageNotification(title:String, message:String) {
        builtNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stars_black_24dp) // Notification icon
            .setContentTitle(title) // Title displayed in the notification
            .setContentText(message) // Text displayed in the notification
            .setPriority(NotificationCompat.PRIORITY_LOW) // Notification priority for better visibility
            .setOnlyAlertOnce(true)

        // Display the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(notificationID, builtNotification!!.build())
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
            return
        }
        notificationManager.notify(notificationID, builtNotification!!.build())
    }

    @SuppressLint("MissingPermission")
    fun updateNotification(progress: Int,progressMax: Int){
        builtNotification?.let {
            val newVal = ((progress / progressMax.toFloat()) * 100).roundToInt()
            if (newVal == previousVal) return
            else previousVal = newVal
            if (newVal == 100) {
                it.setContentText("$title Complete")
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                notificationManager.notify(notificationID, it.build())
                notificationManager.cancel(notificationID)
                builtNotification = null
            } else {
                it.setContentTitle("$title ${newVal}%").setContentText(null)
                    .setProgress(100, newVal, false)
                notificationManager.notify(notificationID, it.build())
            }
        }
    }

    companion object {
        // Unique channel ID for notifications
        const val CHANNEL_ID = "i.apps.notifications"

        // Description for the notification channel
        const val CHANNEL_NAME = "Test notification"

        @SuppressLint("InlinedApi")
        suspend fun createMessageNotification(
            context: Context,
            title: String,
            message: String
        ): AccountableNotification {
            val accountableNotification = AccountableNotification(
                context,
                Random.nextInt(),
                title
            )
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                withContext(MainActivity.Main){
                    accountableNotification.buildMessageNotification(title,message)
                }
            }
            return accountableNotification
        }

        @SuppressLint("InlinedApi")
        suspend fun createProgressNotification(
            context: Context,
            title: String,
            executeUnit: suspend (AccountableNotification) -> Unit
        ):AccountableNotification{
            val accountableNotification = AccountableNotification(
                context,
                Random.nextInt(),
                title
            )
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                withContext(MainActivity.Main){
                    accountableNotification.buildProgressNotification(title)
                }
                executeUnit.invoke(accountableNotification)
            }
            return accountableNotification
        }

        @SuppressLint("InlinedApi")
        suspend fun canPushNotifications(
            context: Context,
            pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
            work: () -> Unit
        ){
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pushNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else {
                withContext(MainActivity.Main){
                    work.invoke()
                }
            }
        }
    }

    override fun close() {
        builtNotification?.setOngoing(false)
            ?.setProgress(0, 0, false)
            ?.setAutoCancel(true)
        notificationManager.notify(notificationID, builtNotification?.build())
        notificationManager.cancel(notificationID)
        builtNotification = null
    }
}