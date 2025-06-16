    package com.QuQ.yomucards

    import android.graphics.Color
    import android.view.LayoutInflater
    import android.view.ViewGroup
    import androidx.core.content.ContextCompat
    import androidx.recyclerview.widget.RecyclerView
    import com.QuQ.yomucards.databinding.ItemMatchingBinding

    class MatchingAdapter(
        private val items: MutableList<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<MatchingAdapter.MatchingViewHolder>() {

        private var selectedPosition: Int = -1

        inner class MatchingViewHolder(val binding: ItemMatchingBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchingViewHolder {
            val binding = ItemMatchingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MatchingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: MatchingViewHolder, position: Int) {
            val item = items[position]
            holder.binding.textViewItem.text = item

            // Устанавливаем фон в зависимости от выбранной позиции
            val context = holder.itemView.context
            val selectedColor = ContextCompat.getColor(context, R.color.selected_item)

            if (position == selectedPosition) {
                holder.itemView.setBackgroundResource(R.drawable.item_selected_background)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.item_no_selected_background)
            }

            holder.itemView.setOnClickListener {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                if (selectedPosition == adapterPosition) {
                    selectedPosition = -1
                    notifyItemChanged(adapterPosition)
                } else {
                    val previousSelected = selectedPosition
                    selectedPosition = adapterPosition
                    if (previousSelected != -1) notifyItemChanged(previousSelected)
                    notifyItemChanged(adapterPosition)
                }
                onItemClick(items[adapterPosition])
            }
        }

        override fun getItemCount(): Int = items.size

        fun removeItem(item: String) {
            val position = items.indexOf(item)
            if (position != -1) {
                // Если удаляем выбранный элемент - сбрасываем выбор
                if (position == selectedPosition) {
                    selectedPosition = -1
                }
                items.removeAt(position)
                notifyItemRemoved(position)
                // Обновляем оставшиеся элементы, так как их позиции изменились
                notifyItemRangeChanged(position, items.size - position)
            }
        }

        fun clearSelection() {
            val previousSelected = selectedPosition
            selectedPosition = -1
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }
        }

        fun getSelectedItem(): String? = if (selectedPosition != -1 && selectedPosition < items.size) items[selectedPosition] else null
    }
