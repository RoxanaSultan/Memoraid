package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.databinding.ItemJournalBinding
import com.example.memoraid.models.Journal
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class JournalAdapter(
    private val journals: MutableList<Journal>,
    private val onJournalClick: (Journal) -> Unit
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class JournalViewHolder(private val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(journal: Journal) {
            binding.journalImageButton.setImageResource(R.drawable.journal)
            binding.journalTitle.text = journal.title

            // Open journal on click
            binding.journalImageButton.setOnClickListener {
                onJournalClick(journal)
            }

            // Remove journal on click
            binding.journalRemoveButton.setOnClickListener {
                deleteJournal(journal, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(journals[position])
    }

    override fun getItemCount(): Int = journals.size

    // 🔥 Function to delete the journal
    private fun deleteJournal(journal: Journal, position: Int) {
        // First, remove images from Firebase Storage
        journal.imageUris?.let { removeImagesFromStorage(it) }

        // Then, delete the journal from Firestore
        db.collection("journals").document(journal.id)
            .delete()
            .addOnSuccessListener {
                // Remove from list and refresh UI
                journals.removeAt(position)
                notifyItemRemoved(position)
//                Toast.makeText(binding.root.context, "Journal deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
//                Toast.makeText(binding.root.context, "Failed to delete journal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to delete images from Firebase Storage
    private fun removeImagesFromStorage(imageUris: List<String>) {
        val storageReference = FirebaseStorage.getInstance().reference.child("journal_images")
        for (imageUri in imageUris) {
            val fileReference = storageReference.child(imageUri)
            fileReference.delete()
                .addOnSuccessListener {
                    // Successfully deleted image from storage
                    println("Image deleted from storage: $imageUri")
                }
                .addOnFailureListener { exception ->
                    // Log error or show message
                    exception.printStackTrace()
                }
        }
    }

}
