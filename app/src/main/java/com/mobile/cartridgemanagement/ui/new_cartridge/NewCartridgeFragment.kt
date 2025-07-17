package com.mobile.cartridgemanagement.ui.new_cartridge

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var adapterRecView: NewCartridgeRecViewDataAdapter
    private lateinit var recyclerView: RecyclerView
    private var selectedCartridgeModels = mutableListOf<NewCartridgeRecViewDataItem>()
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
            if (selectedCartridgeModelId == null) add("Выберите модель картриджа")
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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
//        setupButton()
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
            val editText = EditText(requireContext()).apply {
                hint = "Поиск..."
                gravity = Gravity.CENTER
                maxLines = 1
                inputType = InputType.TYPE_CLASS_TEXT
                isSingleLine = true
                setLines(1)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(5, 15, 5, 5)
                }
            }
            val listView = ListView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    setMargins(5, 0, 5, 5)
                }
            }

            val response = ApiClient.retrofit.create(ApiService::class.java).getCartridgeModels()
            val items = response.result.map { NewCartridgeSelectDataItem(it.id, it.name) }
            originalCartridgeModels = items
            filteredCartridgeModels = originalCartridgeModels!!.toMutableList()
            val adapter = ArrayAdapter(requireContext(), R.layout.new_cartridge_model_select_item, filteredCartridgeModels!!.map { it.name })
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
                addView(editText)
                addView(listView)
            })

            //dialogBuilder.setTitle("Модели картриджей")
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
                onItemSelected(selectedItem.id, selectedItem.name)

                alertDialog.dismiss()
            }

            alertDialog.setOnDismissListener { dialogAlreadyShown = false }
            alertDialog.setOnShowListener {
                editText.requestFocus()
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
            alertDialog.show()
            val displayMetrics = DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
            val width = (displayMetrics.widthPixels * 0.9).toInt()
            val height = (displayMetrics.heightPixels * 0.4).toInt()
            alertDialog.window?.setLayout(width, height)
        }
    }

    private fun onItemSelected(id: Int, name: String) {
        if (selectedCartridgeModels.any {it.id == id}) {
            Toast.makeText(requireContext(), "$name уже добавлен", Toast.LENGTH_SHORT).show()
            return
        }

        selectedCartridgeModels.add(NewCartridgeRecViewDataItem(id, name))
        adapterRecView.updateItems(selectedCartridgeModels.toList())
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerViewTest
        adapterRecView = NewCartridgeRecViewDataAdapter(
            onItemRemoved = { item ->
                selectedCartridgeModels.remove(item)
                adapterRecView.updateItems(selectedCartridgeModels)
            },
            onCountChanged = { item, newCount ->
                item.count = newCount
                // Можно обновить данные где-то еще, если нужно
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapterRecView
    }
//
//    private fun setupButton() {
//        binding.btnLoadData.setOnClickListener {
//            loadDataFromApi()
//        }
//    }
//
//    private fun loadDataFromApi() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val response = ApiClient.retrofit.create(ApiService::class.java).getCartridgeModels()
//                val items = response.result.map { NewCartridgeRecViewDataItem(it.id, it.name) }
//                adapter.items = items
//                adapter.notifyDataSetChanged()
//            } catch (e: Exception) {
//                Toast.makeText(
//                    requireContext(),
//                    "Ошибка загрузки: ${e.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}