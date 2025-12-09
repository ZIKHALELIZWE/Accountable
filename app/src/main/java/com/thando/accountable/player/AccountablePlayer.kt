package com.thando.accountable.player

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.thando.accountable.database.tables.Content

@OptIn(UnstableApi::class)
class AccountablePlayer : MediaSessionService() {
    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var sessionToken: SessionToken
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    var audioOrVideo: Triple<Long,Content,PlayerView>? = null

    private var isStarted = false

    private lateinit var notificationManager: MediaNotificationManager

    companion object{
        const val NOW_PLAYING_CHANNEL_ID = "media.NOW_PLAYING"
        const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification
        private const val TAG = "Media3AppTag"
        const val SESSION_INTENT_REQUEST_CODE = 0
    }

    fun init(context: Context, mediaSessionId:String = "AccountablePlayer"){
        player = ExoPlayer.Builder(context).build()
        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            context.packageManager?.getLaunchIntentForPackage(context.packageName)
                ?.let { sessionIntent ->
                    PendingIntent.getActivity(
                        context,
                        SESSION_INTENT_REQUEST_CODE,
                        sessionIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }
        mediaSession = MediaSession.Builder(context, player).setId(mediaSessionId)
            .setSessionActivity(sessionActivityPendingIntent!!).build()
        sessionToken = SessionToken(context, ComponentName(context, AccountablePlayer::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player.setAudioAttributes(audioAttributes, true)
        player.repeatMode = Player.REPEAT_MODE_OFF

        player.addListener(playerListener)
    }

    @OptIn(UnstableApi::class)
    fun setPlayerView(inputPlayerView: PlayerView){
        controllerFuture.addListener(
            {
                // Call controllerFuture.get() to retrieve the MediaController.
                // MediaController implements the Player interface, so it can be
                // attached to the PlayerView UI component.
                inputPlayerView.setPlayer(controllerFuture.get())
            },
            MoreExecutors.directExecutor()
        )
        inputPlayerView.apply{
            this@apply.player = this@AccountablePlayer.player
            hideController()
        }
    }

    override fun onCreate() {
        super.onCreate()
        init(this.applicationContext,"ServiceAccountablePlayer")
    }

    private fun onStart(context: Context) {
        if (isStarted) return

        isStarted = true

        notificationManager =
            MediaNotificationManager(
                context,
                mediaSession.token,
                player,
                PlayerNotificationListener()
            )


        notificationManager.showNotificationForPlayer(player)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        onDestroy()
    }

    fun addAndPlay(content: Content, playerView: PlayerView,context: Context){
        content.trackItem.value?.let {
            setPlayerView(playerView)
            if (!contains(content)) {
                closeAudioOrVideo()
                audioOrVideo = Triple(it.id,content,playerView)
                onStart(context)

                player.playWhenReady = true
                player.addMediaItem(MediaItem.fromUri(it.audioUrl))
                player.prepare()
            }
            else{
                content.id?.let { id -> audioOrVideo = Triple(id,content,playerView) }
            }
        }
    }

    fun contains(content: Content) = if (audioOrVideo==null) false else audioOrVideo!!.first == content.id

    fun isContentPlaying(content: Content) = contains(content) && audioOrVideo!!.second.isPlaying.value

    fun close(content: Content){
        if (contains(content)) close(content,null)
    }

    fun close(content: Content, playerView: PlayerView?){
        playerView?.player = null
        if (audioOrVideo != null && content.id == audioOrVideo!!.second.id){
            player.clearMediaItems()
            content.isPlaying.value = false
            audioOrVideo = null
            onClose()
        }
    }

    private fun onClose() {
        if (!isStarted) return

        isStarted = false

        // Hide notification
        notificationManager.hideNotification()
    }

    private fun closeAudioOrVideo(){
        audioOrVideo?.second?.let { close(it,audioOrVideo?.third) }
    }

    override fun onDestroy() {
        closeAudioOrVideo()
        release()
        super.onDestroy()
    }

    private fun release(){
        // Free ExoPlayer resources.
        player.removeListener(playerListener)
        player.release()
        mediaSession.release()
        MediaController.releaseFuture(controllerFuture)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(player)
                }

                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Log.e(TAG, "Error: ${error.message}")
        }
    }

    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {

        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            closeAudioOrVideo()
        }
    }
}