package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.JournalType
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
    private val storage = FirebaseStorage.getInstance()

    inner class JournalViewHolder(private val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(journal: Journal) {
            when (journal.type) {
                JournalType.JOURNAL_PINK.type -> binding.journalImageButton.setImageResource(R.drawable.journal_option_1)
                JournalType.JOURNAL_BLUE.type -> binding.journalImageButton.setImageResource(R.drawable.journal_option_2)
            }

            binding.journalTitle.text = journal.title

            binding.journalImageButton.setOnClickListener {
                onJournalClick(journal)
            }

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

    private fun deleteJournal(journal: Journal, position: Int) {
        journal.imageUris?.let { removeImagesFromStorage(it) }

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
        for (imageUri in imageUris) {
            val fileReference = storage.getReferenceFromUrl(imageUri)

            fileReference.delete()
                .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Image deleted from storage", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
//                Log.e("JournalDetailsFragment", "Failed to delete image: ${exception.message}")
//                    Toast.makeText(requireContext(), "Failed to delete image from storage", Toast.LENGTH_SHORT).show()
                }
        }
    }

}
