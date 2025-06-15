package com.example.yourapp

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "UPDATE_NOTIFICATION") {
                val title = intent.getStringExtra("TITLE") ?: "Unknown"
                val artist = intent.getStringExtra("ARTIST") ?: "Unknown"
                val albumId = intent.getLongExtra("ALBUM_ID", -1L)
                updateNotification(title, artist, albumId)
            }
        }
    }

    private var intentBitmap: Bitmap? = null
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter("UPDATE_NOTIFICATION")
        registerReceiver(updateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val albumId = intent?.getLongExtra("ALBUM_ID", -1) ?: -1
        val title = intent?.getStringExtra("TITLE") ?: "Unknown"
        val artist = intent?.getStringExtra("ARTIST") ?: "Unknown"

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

        val notification = buildNotification(title, artist)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(updateReceiver)
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, artist: String): Notification {
        val bitmap = albumArtBitmap ?: BitmapFactory.decodeResource(resources, R.drawable.cover2)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(bitmap)
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

    fun updateNotification(title: String, artist: String, albumId: Long) {
        // بارگذاری عکس کاور
        val uri = Uri.parse("content://media/external/audio/albumart/$albumId")
        try {
            contentResolver.openInputStream(uri)?.use {
                albumArtBitmap = BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.cover2)
        }

        val notification = buildNotification(title, artist)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

}
