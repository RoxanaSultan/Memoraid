package com.example.memoraid.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.memoraid.databinding.ItemDayBinding
import com.example.memoraid.models.CardGame
import java.text.SimpleDateFormat
import java.util.Locale

class CardGameAdapter(private val games: MutableList<CardGame>) :
    RecyclerView.Adapter<CardGameAdapter.CardGameDayViewHolder>() {

    inner class CardGameDayViewHolder(val binding: ItemDayBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardGameDayViewHolder {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CardGameDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardGameDayViewHolder, position: Int) {
        val item = games[position]
        holder.binding.dayOfWeek.text = item.dayOfWeek
        holder.binding.date.text = item.date
        holder.binding.moves.text = "Moves: ${item.moves}"
        holder.binding.time.text = "Time: ${item.time}"
        holder.binding.score.text = "Score: ${item.score}"
    }

    override fun getItemCount() = games.size

    fun sortGamesByDateAndTime() {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        games.sortWith { game1, game2 ->
            val date1 = dateFormat.parse(game1.date)
            val date2 = dateFormat.parse(game2.date)

            if (date1 != null && date2 != null) {
                val dateComparison = date1.compareTo(date2)
                if (dateComparison != 0) {
                    return@sortWith dateComparison
                }
            }

            val time1 = timeFormat.parse(game1.startTime)
            val time2 = timeFormat.parse(game2.startTime)

            if (time1 != null && time2 != null) {
                return@sortWith time1.compareTo(time2)
            }
            return@sortWith 0
        }
        notifyDataSetChanged()
    }
}
