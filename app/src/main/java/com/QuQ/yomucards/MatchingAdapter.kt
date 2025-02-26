package com.QuQ.yomucards

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.QuQ.yomucards.databinding.ItemMatchingBinding

class MatchingAdapter(
    private val items: MutableList<String>,
    private val onItemClick: (String) -> Unit // Лямбда для обработки нажатий
) : RecyclerView.Adapter<MatchingAdapter.MatchingViewHolder>() {

    private var selectedItem: String? = null

    inner class MatchingViewHolder(val binding: ItemMatchingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchingViewHolder {
        val binding = ItemMatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchingViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textViewItem.text = item

        // Устанавливаем фон в зависимости от выбранного элемента
        holder.itemView.setBackgroundColor(
            if (item == selectedItem) Color.LTGRAY else Color.TRANSPARENT
        )

        // Обработка нажатия на элемент
        holder.itemView.setOnClickListener {
            val previousSelected = selectedItem
            selectedItem = item
            notifyItemChanged(items.indexOf(previousSelected)) // Сбросить предыдущий выбор
            notifyItemChanged(position) // Обновить текущий выбор
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(item: String) {
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Метод для сброса выбранного элемента
    fun clearSelection() {
        val previousSelected = selectedItem
        selectedItem = null
        notifyItemChanged(items.indexOf(previousSelected)) // Сбросить выделение
    }
}
