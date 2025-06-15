package com.example.yourapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.androidproject.R

class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREV = "action_prev"
        const val ACTION_FORWARD = "action_forward"
        const val ACTION_REWIND = "action_rewind"
    }

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var notificationManager: NotificationManagerCompat
    private val handler = Handler()
    private val updateNotificationRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun run() {
            if (mediaPlayer.isPlaying) {
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
                handler.postDelayed(this, 1000) // هر ۱ ثانیه آپدیت
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        notificationManager = NotificationManagerCompat.from(this)

        // اینجا آهنگ نمونه از res/raw قرار بده، اسمش sample_music هست:
        mediaPlayer = MediaPlayer.create(this, R.raw.sample1)

        mediaPlayer.setOnCompletionListener {
            // وقتی آهنگ تمام شد سرویس را متوقف کن یا کار دلخواه انجام بده
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (mediaPlayer.isPlaying) pauseMusic() else playMusic()
                }
                ACTION_NEXT -> playNext()
                ACTION_PREV -> playPrevious()
                ACTION_FORWARD -> forward()
                ACTION_REWIND -> rewind()
            }
        }

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
        handler.removeCallbacks(updateNotificationRunnable)
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun playMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            handler.post(updateNotificationRunnable)
            updateNotification()
        }
    }


    private fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            handler.removeCallbacks(updateNotificationRunnable)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            updateNotification()
        }
    }

    private fun playNext() {
        // اینجا کد آهنگ بعدی را بگذار، برای نمونه ریست می‌کنیم
        mediaPlayer.seekTo(0)
        playMusic()
    }

    private fun playPrevious() {
        // اینجا کد آهنگ قبلی را بگذار، برای نمونه ریست می‌کنیم
        mediaPlayer.seekTo(0)
        playMusic()
    }

    private fun forward() {
        val newPos = mediaPlayer.currentPosition + 10000
        mediaPlayer.seekTo(if (newPos < mediaPlayer.duration) newPos else mediaPlayer.duration)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        updateNotification()
    }

    private fun rewind() {
        val newPos = mediaPlayer.currentPosition - 10000
        mediaPlayer.seekTo(if (newPos > 0) newPos else 0)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        updateNotification()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification() {
        val notification = buildNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("در حال پخش موسیقی")
            .setContentText("کنترل از نوار وضعیت")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(mediaPlayer.isPlaying)
            .setProgress(mediaPlayer.duration, mediaPlayer.currentPosition, false)

        // دکمه‌های نوتیفیکیشن
        builder.addAction(
            android.R.drawable.ic_media_previous,
            "قبلی",
            createPendingIntent(ACTION_PREV)
        )
        builder.addAction(
            android.R.drawable.ic_media_rew,
            "10 ثانیه عقب",
            createPendingIntent(ACTION_REWIND)
        )

        if (mediaPlayer.isPlaying) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "مکث",
                createPendingIntent(ACTION_PLAY_PAUSE)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "پخش",
                createPendingIntent(ACTION_PLAY_PAUSE)
            )
        }

        builder.addAction(
            android.R.drawable.ic_media_ff,
            "10 ثانیه جلو",
            createPendingIntent(ACTION_FORWARD)
        )
        builder.addAction(
            android.R.drawable.ic_media_next,
            "بعدی",
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
                "کانال موسیقی",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "کانال مربوط به نوتیفیکیشن موزیک پلیر"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
