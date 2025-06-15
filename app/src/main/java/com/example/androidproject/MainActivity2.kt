package com.example.androidproject

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.yourapp.MusicService

class MainActivity2 : AppCompatActivity() {

    private lateinit var imgAlbumArt: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var txtArtist: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnPlayPause: ImageButton


    private lateinit var musicList: ArrayList<Music>
    private var position = 0
    private var mediaPlayer: MediaPlayer? = null

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                MusicService.ACTION_PLAY_PAUSE -> togglePlayPause()
                MusicService.ACTION_NEXT -> playNext()
                MusicService.ACTION_PREV -> playPrevious()
                MusicService.ACTION_FORWARD -> forward()
                MusicService.ACTION_REWIND -> rewind()
            }
        }
    }

    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private val handler = android.os.Handler()
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                txtCurrentTime.text = formatTime(it.currentPosition)
                handler.postDelayed(this, 500)
            }
        }
    }

    private lateinit var seekBarVolume: SeekBar
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        initViews()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                return
            }
        }

        seekBarVolume = findViewById(R.id.seekBarVolume)

// گرفتن AudioManager برای کنترل صدا
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        seekBarVolume.max = maxVolume
        seekBarVolume.progress = currentVolume

        seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        musicList = intent.getParcelableArrayListExtra("MUSIC_LIST") ?: ArrayList()
        position = intent.getIntExtra("POSITION", 0)

        registerReceiverWithExportFlag()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        if (musicList.isNotEmpty()) {
            showMusicInfo()
            playMusic()
            startMusicService() // فقط برای نوتیفیکیشن
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    txtCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnNext.setOnClickListener { playNext() }
        btnPrev.setOnClickListener { playPrevious() }
        btnForward.setOnClickListener { forward() }
        btnRewind.setOnClickListener { rewind() }

        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            playNext()
        }

    }

    private fun initViews() {
        imgAlbumArt = findViewById(R.id.imgAlbumArt)
        txtTitle = findViewById(R.id.txtTitle)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnRewind = findViewById(R.id.btnRewind)
        btnForward = findViewById(R.id.btnForward)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        seekBar = findViewById(R.id.seekBar)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)
        txtArtist = findViewById(R.id.txtArtist)
    }

    private fun registerReceiverWithExportFlag() {
        val filter = IntentFilter().apply {
            addAction(MusicService.ACTION_PLAY_PAUSE)
            addAction(MusicService.ACTION_NEXT)
            addAction(MusicService.ACTION_PREV)
            addAction(MusicService.ACTION_FORWARD)
            addAction(MusicService.ACTION_REWIND)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter,Context.RECEIVER_NOT_EXPORTED)
        }
    }

    private fun showMusicInfo() {
        val music = musicList[position]
        txtTitle.text = music.title
        txtArtist.text = music.artist
        val albumArtUri = Uri.parse("content://media/external/audio/albumart")
        imgAlbumArt.setImageURI(Uri.withAppendedPath(albumArtUri, music.albumId.toString()))
    }

    private fun playMusic() {
        mediaPlayer?.release()
        val music = musicList[position]
        mediaPlayer = MediaPlayer().apply {
            setDataSource(music.data)
            prepare()
            start()
            setOnCompletionListener {
                playNext()
            }
        }

        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)

        val duration = mediaPlayer?.duration ?: 0
        seekBar.max = duration
        txtTotalTime.text = formatTime(duration)

        handler.post(updateSeekBarRunnable)
    }



    private fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                it.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun playNext() {
        position = (position + 1) % musicList.size
        showMusicInfo()
        playMusic()
    }

    private fun playPrevious() {
        position = if (position - 1 < 0) musicList.size - 1 else position - 1
        showMusicInfo()
        playMusic()
    }

    private fun forward() {
        mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0) + 10000)
    }

    private fun rewind() {
        mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0) - 10000)
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(controlReceiver)
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }


}
