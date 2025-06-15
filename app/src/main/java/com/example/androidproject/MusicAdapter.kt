package com.example.androidproject

import android.content.ContentUris
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MusicAdapter(
    private val musicList: List<Music>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtArtist: TextView = itemView.findViewById(R.id.txtArtist)
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun getItemCount(): Int = musicList.size

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
        holder.txtTitle.text = music.title
        holder.txtArtist.text = music.artist

        val context = holder.itemView.context
        val albumArtUri = Uri.parse("content://media/external/audio/albumart")
        val imageUri = ContentUris.withAppendedId(albumArtUri, music.albumId)

        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            if (inputStream != null) {
                holder.imgCover.setImageURI(imageUri)
                inputStream.close()
            } else {
                holder.imgCover.setImageResource(R.drawable.cover2)
            }
        } catch (e: Exception) {
            holder.imgCover.setImageResource(R.drawable.cover2)
        }

        holder.itemView.setOnClickListener { onItemClick(position) }
    }

}
