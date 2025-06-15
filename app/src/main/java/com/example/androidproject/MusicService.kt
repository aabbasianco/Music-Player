package com.example.androidproject

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_FORWARD = "ACTION_FORWARD"
        const val ACTION_REWIND = "ACTION_REWIND"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ارسال Broadcast به اکتیویتی برای کنترل موزیک
        intent?.action?.let { action ->
            sendBroadcast(Intent(action))
        }

        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("در حال پخش موسیقی")
            .setContentText("کنترل از نوار وضعیت")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        // دکمه‌های کنترل نوتیفیکیشن
        builder.addAction(
            android.R.drawable.ic_media_previous, "قبلی",
            createPendingIntent(ACTION_PREV)
        )
        builder.addAction(
            android.R.drawable.ic_media_rew, "10 ثانیه عقب",
            createPendingIntent(ACTION_REWIND)
        )
        builder.addAction(
            android.R.drawable.ic_media_play, "پخش/مکث",
            createPendingIntent(ACTION_PLAY_PAUSE)
        )
        builder.addAction(
            android.R.drawable.ic_media_ff, "10 ثانیه جلو",
            createPendingIntent(ACTION_FORWARD)
        )
        builder.addAction(
            android.R.drawable.ic_media_next, "بعدی",
            createPendingIntent(ACTION_NEXT)
        )

        return builder.build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
