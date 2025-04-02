package com.example.memoraid.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.databinding.ItemJournalBinding
import android.view.LayoutInflater

class AlbumModalAdapter(private val onItemSelected: (Int) -> Unit) : RecyclerView.Adapter<AlbumModalAdapter.AlbumModalViewHolder>() {

    private val images = listOf(
        R.drawable.album_option_1,
        R.drawable.album_option_2,
        R.drawable.album_option_3
    )

    class AlbumModalViewHolder(val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumModalViewHolder {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumModalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumModalViewHolder, position: Int) {
        holder.binding.journalImageButton.setImageResource(images[position])
        holder.binding.journalTitle.visibility = RecyclerView.GONE
        holder.binding.journalRemoveButton.visibility = RecyclerView.GONE
        holder.binding.journalImageButton.setOnClickListener {
            onItemSelected(position)
        }
    }

    override fun getItemCount(): Int = images.size
}
