package com.roxanasultan.memoraid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.enums.AlbumType
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.ItemAlbumBinding
import com.roxanasultan.memoraid.models.Album
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AlbumAdapter(
    private val context: Context,
    private val albums: MutableList<Album>,
    private val onAlbumClick: (Album) -> Unit,
    private val onAlbumDelete: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    inner class AlbumViewHolder(private val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(album: Album) {
            when (album.type) {
                AlbumType.ALBUM_GREEN.type -> binding.albumImageButton.setImageResource(R.drawable.album_option_1)
                AlbumType.ALBUM_BLUE.type -> binding.albumImageButton.setImageResource(R.drawable.album_option_2)
                AlbumType.ALBUM_PINK.type -> binding.albumImageButton.setImageResource(R.drawable.album_option_3)
            }

            binding.albumTitle.text = album.title

            binding.albumImageButton.setOnClickListener {
                onAlbumClick(album)
            }

            binding.albumRemoveButton.setOnClickListener {
                deleteAlbum(album, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(albums[position])
    }

    override fun getItemCount(): Int = albums.size

    private fun deleteAlbum(album: Album, position: Int) {
        val alertDialog = android.app.AlertDialog.Builder(context)
            .setTitle("Delete Album")
            .setMessage("Are you sure you want to delete this album?")
            .setPositiveButton("Yes") { _, _ ->
                album.images?.let { removeImagesFromStorage(it) }

                db.collection("albums").document(album.id)
                    .delete()
                    .addOnSuccessListener {
                        albums.removeAt(position)
                        notifyItemRemoved(position)
                        onAlbumDelete(album)
                    }
            }
            .setNegativeButton("No", null)
            .create()

        alertDialog.show()
    }

    private fun removeImagesFromStorage(imageUris: List<String>) {
        for (imageUri in imageUris) {
            val fileReference = storage.getReferenceFromUrl(imageUri)

            fileReference.delete()
                .addOnSuccessListener {

                }
                .addOnFailureListener { exception ->
                }
        }
    }

}
