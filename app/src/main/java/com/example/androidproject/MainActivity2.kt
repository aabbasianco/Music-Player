package com.example.androidproject

import android.content.ContentUris
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {

    private lateinit var imgAlbumArt: ImageView
    private lateinit var txtTitle: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnPlayPause: ImageButton

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var musicList: ArrayList<Music>
    private var position = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        initViews()

        musicList = intent.getParcelableArrayListExtra("MUSIC_LIST") ?: ArrayList()
        position = intent.getIntExtra("POSITION", 0)

        if (musicList.isNotEmpty()) {
            playMusic()
        }

        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer?.start()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnNext.setOnClickListener {
            nextMusic()
        }

        btnPrev.setOnClickListener {
            previousMusic()
        }

        btnRewind.setOnClickListener {
            mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0) - 10000)
        }

        btnForward.setOnClickListener {
            mediaPlayer?.seekTo((mediaPlayer?.currentPosition ?: 0) + 10000)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun initViews() {
        imgAlbumArt = findViewById(R.id.imgAlbumArt)
        txtTitle = findViewById(R.id.txtTitle)
        seekBar = findViewById(R.id.seekBar)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnRewind = findViewById(R.id.btnRewind)
        btnForward = findViewById(R.id.btnForward)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtTotalTime = findViewById(R.id.txtTotalTime)
    }

    private fun playMusic() {
        val music = musicList[position]
        txtTitle.text = music.title

        val totalDuration = mediaPlayer?.duration ?: 0
        txtTotalTime.text = formatTime(totalDuration)

        val albumArtUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            music.albumId
        )
        imgAlbumArt.setImageURI(albumArtUri)

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(music.data)
            prepare()
            start()
            setOnCompletionListener {
                nextMusic() // پخش خودکار ترک بعدی
            }
        }

        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        seekBar.max = mediaPlayer?.duration ?: 0
        updateSeekBar()
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    txtCurrentTime.text = formatTime(it.currentPosition)
                    handler.postDelayed(this, 500)
                }
            }
        }, 0)
    }


    private fun nextMusic() {
        position = (position + 1) % musicList.size
        playMusic()
    }

    private fun previousMusic() {
        position = if (position - 1 < 0) musicList.size - 1 else position - 1
        playMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

}
