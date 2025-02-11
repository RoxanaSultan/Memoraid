package com.example.memoraid.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.R
import com.example.memoraid.databinding.ItemJournalBinding
import android.view.LayoutInflater

class ModalAdapter(private val onItemSelected: (Int) -> Unit) : RecyclerView.Adapter<ModalAdapter.ModalViewHolder>() {

    private val images = listOf(
        R.drawable.journal_option_1,
        R.drawable.journal_option_2
    )

    class ModalViewHolder(val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModalViewHolder {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModalViewHolder, position: Int) {
        holder.binding.journalImageButton.setImageResource(images[position])
        holder.binding.journalTitle.visibility = RecyclerView.GONE
        holder.binding.journalRemoveButton.visibility = RecyclerView.GONE
        holder.binding.journalImageButton.setOnClickListener {
            onItemSelected(position)
        }
    }

    override fun getItemCount(): Int = images.size
}
