package com.mobile.cartridgemanagement.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.cartridgemanagement.R
import com.mobile.cartridgemanagement.ui.data.NewCartridgeRecViewDataItem

class NewCartridgeRecViewDataAdapter(private val onItemRemoved: (NewCartridgeRecViewDataItem) -> Unit,
                                     private val onCountChanged: (NewCartridgeRecViewDataItem, Int) -> Unit) :
    RecyclerView.Adapter<NewCartridgeRecViewDataAdapter.ViewHolder>() {

    private val items = mutableListOf<NewCartridgeRecViewDataItem>()

    fun updateItems(newItems: List<NewCartridgeRecViewDataItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.new_cartridge_recyclerview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.clearListeners() //
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.item_name)
        private val countInput: EditText = itemView.findViewById(R.id.item_count)
        private val removeButton: ImageButton = itemView.findViewById(R.id.remove_button)
        private var currentItem: NewCartridgeRecViewDataItem? = null //

        fun bind(item: NewCartridgeRecViewDataItem) {
            currentItem = item
            nameTextView.text = item.name
            // Устанавливаем значение без вызова слушателя
            countInput.removeTextChangedListener(textWatcher)
            countInput.setText(item.count.toString())
            countInput.addTextChangedListener(textWatcher)
        }

        fun clearListeners() {
            countInput.removeTextChangedListener(textWatcher)
            currentItem = null
        }

        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                currentItem?.let { item ->
                    when {
                        s.isNullOrEmpty() -> {
                            // Если поле пустое, устанавливаем минимальное значение
                            val minCount = 1
                            s?.replace(0, s.length, minCount.toString())
                            updateItemCount(item, minCount)
                        }
                        else -> {
                            val newCount = s.toString().toIntOrNull() ?: 1
                            val clampedCount = newCount.coerceIn(1, 20) // Минимум 1, максимум 100

                            if (newCount != clampedCount) {
                                s.replace(0, s.length, clampedCount.toString())
                            }

                            updateItemCount(item, clampedCount)
                        }
                    }
                }
            }

            private fun updateItemCount(item: NewCartridgeRecViewDataItem, newCount: Int) {
                if (newCount != item.count) {
                    item.count = newCount
                    onCountChanged(item, newCount)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        init {
            removeButton.setOnClickListener {
                currentItem?.let { item ->
                    onItemRemoved(item)
                }
            }
        }
    }
}