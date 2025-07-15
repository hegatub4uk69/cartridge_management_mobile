package com.mobile.cartridgemanagement.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.cartridgemanagement.R
import com.mobile.cartridgemanagement.ui.data.NewCartridgeRecViewDataItem

class NewCartridgeRecViewDataAdapter(var items: List<NewCartridgeRecViewDataItem>) :
    RecyclerView.Adapter<NewCartridgeRecViewDataAdapter.ViewHolder>() {

    // Хранит ссылки на View-элементы
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvId: TextView = view.findViewById(R.id.tvId)
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    // Создает новый элемент списка
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.new_cartridge_recyclerview_item, parent, false)
        return ViewHolder(view)
    }

    // Заполняет элемент данными
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvId.text = "ID: ${item.id}"
        holder.tvName.text = "Name: ${item.name}"
    }

    // Возвращает количество элементов
    override fun getItemCount() = items.size
}