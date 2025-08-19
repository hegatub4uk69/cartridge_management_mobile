package com.mobile.cartridgemanagement.ui.cartridge_actions

import com.mobile.cartridgemanagement.BarcodeScannerActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mobile.cartridgemanagement.R
import com.mobile.cartridgemanagement.databinding.FragmentCartridgeActionsBinding
import com.mobile.cartridgemanagement.ui.data.CartridgeActionsDepSelectDataItem
import com.mobile.cartridgemanagement.ui.network.ApiClient
import com.mobile.cartridgemanagement.ui.network.ApiService
import com.mobile.cartridgemanagement.ui.network.requests.GetCartridgeInfo
import com.mobile.cartridgemanagement.ui.network.responses.CartridgeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class CartridgeActionsFragment : Fragment() {

    private var _binding: FragmentCartridgeActionsBinding? = null

    private val CAMERA_PERMISSION_REQUEST = 1002  // Для запроса разрешения

    companion object {
        private const val BARCODE_SCAN_REQUEST = 1001  // Для сканера
    }

    private var selectedDepartmentId: Int? = null
    private var dialogAlreadyShown = false
    private var originalDepartments: List<CartridgeActionsDepSelectDataItem>? = null
    private var filteredDepartments: List<CartridgeActionsDepSelectDataItem>? = null
    private var cartridgeInfo: CartridgeInfo? by Delegates.observable(null) { _, _, newValue ->
        updateSubmitButtonState()
        updateCartridgeInfo(newValue)
    }

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val cartridgeActionsViewModel = ViewModelProvider(this).get(CartridgeActionsViewModel::class.java)
        _binding = FragmentCartridgeActionsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val cartridgeModelSelect = binding.departmentSelect
        cartridgeModelSelect.adapter = ArrayAdapter(requireContext(), R.layout.new_cartridge_select_item, mutableListOf<String>().apply {
            if (selectedDepartmentId == null) add("Выберите подразделение")
        })

        binding.out.isChecked = true
        binding.radioActions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.`in` -> {
                    updateDepartmentSelectState("disable")
                }
                R.id.out -> {
                    updateDepartmentSelectState("enable")
                }
                R.id.decommiss -> {
                    updateDepartmentSelectState("disable")
                }
            }
        }
        binding.manualInputCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.barcodeScan.hint = if (isChecked) {
                "Введите номер картриджа"
            } else { "Отсканировать штрих-код" }
            binding.barcodeScan.requestFocus()
        }
        binding.barcodeScan.requestFocus()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateSubmitButtonState()
        setupSelectDepartmentTouch()
        setupInputField()
    }

    private fun setupInputField() {
        binding.barcodeScan.setOnClickListener {
            if (binding.manualInputCheckbox.isChecked) {
                // Ручной режим - просто фокусируемся
                binding.barcodeScan.run {
                    requestFocus()
                    showKeyboard()
                }
            } else {
                // Режим сканера - запускаем сканирование
                checkCameraPermission()
            }
        }

        // Слушатель окончания ввода (для ручного режима)
        binding.barcodeScan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                try {
                    if (binding.barcodeScan.text.isNotEmpty()) {
                        if (binding.barcodeScan.text.matches(Regex("^\\d+$"))) {
                            fetchData(binding.barcodeScan.text.toString())
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Текст должен содержать только цифры!",
                                Toast.LENGTH_LONG
                            ).show()
                            binding.barcodeScan.setText("")
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Поле не может быть пустым!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Обработка любых исключений
                    Toast.makeText(requireContext(), "Ошибка обработки текста!", Toast.LENGTH_SHORT).show()
                    // Очищаем поле
                    binding.barcodeScan.setText("")
                }
                true
            } else false
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startBarcodeScanner()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun startBarcodeScanner() {
        val scannerIntent = Intent(requireContext(), BarcodeScannerActivity::class.java)
        startActivityForResult(scannerIntent, BARCODE_SCAN_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BARCODE_SCAN_REQUEST && resultCode == RESULT_OK) {
            try {
                val barcode = data?.getStringExtra("SCAN_RESULT")
                if (!barcode.isNullOrBlank()) {
                    // Проверяем, что штрихкод состоит только из цифр (целое число)
                    if (barcode.matches(Regex("^\\d+$"))) {
                        binding.barcodeScan.setText(barcode)
                        fetchData(barcode)  // Автоматический запрос после сканирования
                    } else {
                        // Выводим Toast с предупреждением
                        Toast.makeText(requireContext(), "Штрихкод должен содержать только цифры!", Toast.LENGTH_LONG).show()
                        // Очищаем поле
                        binding.barcodeScan.setText("")
                    }
                } else {
                    // Пустой штрих-код
                    Toast.makeText(requireContext(), "Не удалось распознать штрих-код!", Toast.LENGTH_SHORT).show()
                    // Очищаем поле
                    binding.barcodeScan.setText("")
                }
            } catch (e: Exception) {
                // Обработка любых исключений
                Toast.makeText(requireContext(), "Ошибка обработки штрих-кода!", Toast.LENGTH_SHORT).show()
                // Очищаем поле
                binding.barcodeScan.setText("")
            }
        }
    }

    private fun fetchData(code: String) {
        val request = GetCartridgeInfo(id = code.toInt())

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.retrofit.create(ApiService::class.java)
                        .getCartridgeInfo(request)
                }
                cartridgeInfo = response // автоматически вызовет updateUI()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                cartridgeInfo = null // сбрасываем данные при ошибке
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCartridgeInfo(info: CartridgeInfo?) {
        with(binding) {
            info?.let {
                // Заполняем поля данными
                binding.cartridgeNumber.text = it.id.toString()
                binding.cartridgeModel.text = it.model
                binding.cartridgeDepartment.text = it.department
                binding.cartridgeDepartmentDatetime.text = it.department_date
            } ?: run {
                // Скрываем данные если null
                binding.cartridgeNumber.text = ""
                binding.cartridgeModel.text = ""
                binding.cartridgeDepartment.text = ""
                binding.cartridgeDepartmentDatetime.text = ""
            }
        }
    }

    private fun showKeyboard() {
        binding.barcodeScan.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.barcodeScan, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBarcodeScanner()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupSelectDepartmentTouch() =
        binding.departmentSelect.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && !dialogAlreadyShown) {
                dialogAlreadyShown = true
                showDepartmentSelectDialog()
                // Сбрасываем флаг через небольшой delay (чтобы избежать двойного вызова)
                binding.departmentSelect.postDelayed({ dialogAlreadyShown = false }, 300)
            }
            true
        }

    private fun showDepartmentSelectDialog() {
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

            val response = ApiClient.retrofit.create(ApiService::class.java).getDepartments()
            val items = response.result.map { CartridgeActionsDepSelectDataItem(it.id, it.name) }
            originalDepartments = items
            filteredDepartments = originalDepartments!!.toMutableList()
            val adapter = ArrayAdapter(requireContext(), R.layout.new_cartridge_model_select_item, filteredDepartments!!.map { it.name })
            listView.adapter = adapter

            // Фильтрация списка при вводе текста
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filteredDepartments = if (s.isNullOrEmpty()) {
                        originalDepartments!!.toMutableList()
                    } else {
                        originalDepartments!!.filter { it.name.contains(s, ignoreCase = true) }.toMutableList()
                    }
                    adapter.clear()
                    adapter.addAll((filteredDepartments as MutableList<CartridgeActionsDepSelectDataItem>).map { it.name })
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            dialogBuilder.setView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                addView(editText)
                addView(listView)
            })

            dialogBuilder.setNegativeButton("Отмена") {_, _ -> dialogAlreadyShown = false}
            val alertDialog = dialogBuilder.create()

            // Обработка выбора элемента
            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedItem = (filteredDepartments as MutableList<CartridgeActionsDepSelectDataItem>)[position]
                selectedDepartmentId = selectedItem.id

                // Обновляем Spinner (отображаем выбранное название)
                (binding.departmentSelect.adapter as ArrayAdapter<String>).apply {
                    clear()
                    add(selectedItem.name)
                }

                Toast.makeText(requireContext(), "ID: $selectedDepartmentId, NAME: ${selectedItem.name}", Toast.LENGTH_SHORT).show()
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

    private fun updateDepartmentSelectState(option: String) {
        if ( option === "disable" ) {
            binding.departmentSelect.isEnabled = false
            binding.departmentSelect.visibility = View.GONE
        } else if (option === "enable") {
            binding.departmentSelect.isEnabled = true
            binding.departmentSelect.visibility = View.VISIBLE
        }
    }

    private fun updateSubmitButtonState() {
        binding.btnSubmit.isEnabled = cartridgeInfo != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}