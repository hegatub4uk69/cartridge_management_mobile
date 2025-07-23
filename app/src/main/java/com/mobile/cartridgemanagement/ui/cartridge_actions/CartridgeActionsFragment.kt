package com.mobile.cartridgemanagement.ui.cartridge_actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mobile.cartridgemanagement.databinding.FragmentCartridgeActionsBinding

class CartridgeActionsFragment : Fragment() {

    private var _binding: FragmentCartridgeActionsBinding? = null

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

        val textView: TextView = binding.textCartridgeActions
        cartridgeActionsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}