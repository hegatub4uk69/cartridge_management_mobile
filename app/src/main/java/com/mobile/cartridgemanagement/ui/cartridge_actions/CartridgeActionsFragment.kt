package com.mobile.cartridgemanagement.ui.cartridge_actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mobile.cartridgemanagement.R
import com.mobile.cartridgemanagement.databinding.FragmentCartridgeActionsBinding
import com.mobile.cartridgemanagement.ui.network.responses.CartridgeInfo

class CartridgeActionsFragment : Fragment() {

    private var _binding: FragmentCartridgeActionsBinding? = null

    private var selectedDepartmentId: Int? = null
    private var cartridgeInfo = mutableListOf<CartridgeInfo>()

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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateSubmitButtonState()
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
        val isButtonEnabled = cartridgeInfo.isNotEmpty()
        binding.btnSubmit.isEnabled = isButtonEnabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}