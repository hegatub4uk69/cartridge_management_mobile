package com.mobile.cartridgemanagement.ui.new_cartridge

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mobile.cartridgemanagement.R
import com.mobile.cartridgemanagement.databinding.NewCartridgeBinding
import com.mobile.cartridgemanagement.ui.adapter.NewCartridgeRecViewDataAdapter
import com.mobile.cartridgemanagement.ui.data.NewCartridgeRecViewDataItem
import com.mobile.cartridgemanagement.ui.data.NewCartridgeSelectDataItem
import com.mobile.cartridgemanagement.ui.network.ApiClient
import com.mobile.cartridgemanagement.ui.network.ApiService
import kotlinx.coroutines.launch

class NewCartridgeFragment : Fragment() {

    private var _binding: NewCartridgeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var adapter = NewCartridgeRecViewDataAdapter(emptyList())
    private var selectedCartridgeModelId: Int? = null
    private var dialogAlreadyShown = false
    private var originalCartridgeModels: List<NewCartridgeSelectDataItem>? = null
    private var filteredCartridgeModels: List<NewCartridgeSelectDataItem>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val newCartridgeViewModel =
            ViewModelProvider(this).get(NewCartridgeViewModel::class.java)

        _binding = NewCartridgeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val cartridgeModelSelect = binding.cartridgeSelect
        cartridgeModelSelect.adapter = ArrayAdapter(requireContext(), R.layout.new_cartridge_select_item, mutableListOf<String>().apply {
            if (selectedCartridgeModelId == null) add("Выберите элемент")
        })

        cartridgeModelSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (position > 0) {
                    Toast.makeText(requireContext(), "ID: $selectedCartridgeModelId", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

//        cartridgeModelSelect.setOnTouchListener { _, _ ->
//            showCartridgeModelSelectDialog()
//            true
//        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButton()
        setupSelectCartridgeModelTouch()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSelectCartridgeModelTouch() =
        binding.cartridgeSelect.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && !dialogAlreadyShown) {
                dialogAlreadyShown = true
                showCartridgeModelSelectDialog()
                // Сбрасываем флаг через небольшой delay (чтобы избежать двойного вызова)
                binding.cartridgeSelect.postDelayed({ dialogAlreadyShown = false }, 300)
            }
            true
        }

    private fun showCartridgeModelSelectDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val dialogBuilder = AlertDialog.Builder(requireContext())
            val editText = EditText(requireContext()).apply { hint = "Поиск..." }

            val listView = ListView(requireContext())
            val response = ApiClient.retrofit.create(ApiService::class.java).getCartridgeModels()
            val items = response.result.map { NewCartridgeSelectDataItem(it.id, it.name) }
            originalCartridgeModels = items
            filteredCartridgeModels = originalCartridgeModels!!.toMutableList()
            val adapter = ArrayAdapter(requireContext(), R.layout.new_cartridge_select_item, filteredCartridgeModels!!.map { it.name })
            listView.adapter = adapter

            // Фильтрация списка при вводе текста
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filteredCartridgeModels = if (s.isNullOrEmpty()) {
                        originalCartridgeModels!!.toMutableList()
                    } else {
                        originalCartridgeModels!!.filter { it.name.contains(s, ignoreCase = true) }.toMutableList()
                    }
                    adapter.clear()
                    adapter.addAll((filteredCartridgeModels as MutableList<NewCartridgeSelectDataItem>).map { it.name })
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            dialogBuilder.setView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                addView(editText, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                addView(listView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })

            dialogBuilder.setTitle("Выберите элемент")
            dialogBuilder.setNegativeButton("Отмена") {_, _ -> dialogAlreadyShown = false}
            val alertDialog = dialogBuilder.create()

            // Обработка выбора элемента
            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = (filteredCartridgeModels as MutableList<NewCartridgeSelectDataItem>)[position]
                selectedCartridgeModelId = selectedItem.id

                // Обновляем Spinner (отображаем выбранное название)
                (binding.cartridgeSelect.adapter as ArrayAdapter<String>).apply {
                    clear()
                    add(selectedItem.name)
                }

                Toast.makeText(
                    requireContext(),
                    "Выбрано: ${selectedItem.name} (ID: ${selectedItem.id})",
                    Toast.LENGTH_SHORT
                ).show()
                alertDialog.dismiss()
            }

            alertDialog.setOnDismissListener { dialogAlreadyShown = false }
            alertDialog.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = NewCartridgeRecViewDataAdapter(emptyList())
        binding.recyclerViewTest.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NewCartridgeFragment.adapter
        }
    }

    private fun setupButton() {
        binding.btnLoadData.setOnClickListener {
            loadDataFromApi()
        }
    }

    private fun loadDataFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.retrofit.create(ApiService::class.java).getCartridgeModels()
                val items = response.result.map { NewCartridgeRecViewDataItem(it.id, it.name) }
                adapter.items = items
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Ошибка загрузки: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}