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

            // Удаляем предыдущие слушатели
            countInput.onFocusChangeListener = null
            countInput.removeTextChangedListener(textWatcher)

            // Устанавливаем текущее значение
            countInput.setText(item.count.toString())

            // Добавляем слушатели
            countInput.addTextChangedListener(textWatcher)
            countInput.onFocusChangeListener = focusListener
        }

        private val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) { // Когда поле теряет фокус
                val editText = v as EditText
                if (editText.text.isNullOrEmpty()) {
                    editText.setText("1")
                    currentItem?.let {
                        it.count = 1
                        onCountChanged(it, 1)
                    }
                }
            }
        }

        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Ничего не делаем при пустом значении (ждем потери фокуса)
                if (s.isNullOrEmpty()) return

                currentItem?.let { item ->
                    val newCount = s.toString().toIntOrNull() ?: 1
                    val clampedCount = newCount.coerceIn(1, 20)

                    if (newCount != clampedCount) {
                        s.replace(0, s.length, clampedCount.toString())
                    }

                    if (clampedCount != item.count) {
                        item.count = clampedCount
                        onCountChanged(item, clampedCount)
                    }
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