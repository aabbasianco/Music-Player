package com.example.yourapp

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.androidproject.R
import android.provider.MediaStore
import android.net.Uri


class MusicService : Service() {

    private var albumArtBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREV = "action_prev"
        const val ACTION_FORWARD = "action_forward"
        const val ACTION_REWIND = "action_rewind"
    }
    private var intentBitmap: Bitmap? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val albumId = intent?.getLongExtra("ALBUM_ID", -1) ?: -1
        if (albumId != -1L) {
            val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
            try {
                contentResolver.openInputStream(uri)?.use {
                    albumArtBitmap = BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.cover2)
            }
        }

        // نمایش نوتیف
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val albumArtBitmap = intentBitmap ?: BitmapFactory.decodeResource(resources, R.drawable.cover2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("در حال پخش موسیقی")
            .setContentText("برای کنترل پخش، از نوار وضعیت استفاده کنید")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(albumArtBitmap)
            .setLargeIcon(albumArtBitmap)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_previous, "قبلی", createPendingIntent(ACTION_PREV))
            .addAction(android.R.drawable.ic_media_pause, "مکث/پخش", createPendingIntent(ACTION_PLAY_PAUSE))
            .addAction(android.R.drawable.ic_media_next, "بعدی", createPendingIntent(ACTION_NEXT))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .build()
    }


    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "کانال موسیقی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نوتیفیکیشن مربوط به پخش موسیقی"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
