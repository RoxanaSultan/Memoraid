package com.roxanasultan.memoraid.patient.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.roxanasultan.memoraid.R
import com.roxanasultan.memoraid.databinding.ItemJournalBinding
import android.view.LayoutInflater

class JournalModalAdapter(private val onItemSelected: (Int) -> Unit) : RecyclerView.Adapter<JournalModalAdapter.JournalModalViewHolder>() {

    private val images = listOf(
        R.drawable.journal_option_1,
        R.drawable.journal_option_2,
        R.drawable.journal_option_3
    )

    class JournalModalViewHolder(val binding: ItemJournalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalModalViewHolder {
        val binding = ItemJournalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JournalModalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalModalViewHolder, position: Int) {
        holder.binding.journalImageButton.setImageResource(images[position])
        holder.binding.journalTitle.visibility = RecyclerView.GONE
        holder.binding.journalRemoveButton.visibility = RecyclerView.GONE
        holder.binding.journalImageButton.setOnClickListener {
            onItemSelected(position)
        }
    }

    override fun getItemCount(): Int = images.size
}
