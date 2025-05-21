package com.example.greencodechallenge.presentation.conversion

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greencodechallenge.R
import com.example.greencodechallenge.databinding.FragmentConversionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConversionFragment : Fragment() {

    private val viewModel by viewModels<ConversionViewModel>()

    private var _binding: FragmentConversionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
        val tooltip = getString(R.string.exchange_rate_info)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.ivInfo.tooltipText = tooltip
        }

        viewModel.fetchExchangeRates()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ConversionUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnConvert.isEnabled = true
                            binding.spinnerFrom.isEnabled = true
                            binding.spinnerTo.isEnabled = true
                            
                            setupSpinners(state.currencies, state.fromCurrency, state.toCurrency)
                            
                            if (state.ratio.isNotEmpty()) {
                                binding.tvRatio.text = state.ratio
                                binding.tvRatio.visibility = View.VISIBLE
                            } else {
                                binding.tvRatio.visibility = View.GONE
                            }
                            
                            if (state.lastUpdated.isNotEmpty()) {
                                binding.tvLastUpdated.text = state.lastUpdated
                                binding.tvLastUpdated.visibility = View.VISIBLE
                            } else {
                                binding.tvLastUpdated.visibility = View.GONE
                            }
                            
                            if (state.result.isNotEmpty()) {
                                binding.tvResult.text = state.result
                                binding.tvResult.visibility = View.VISIBLE
                            } else {
                                binding.tvResult.text = getString(R.string.result_label)
                                binding.tvResult.visibility = View.VISIBLE
                            }

                            val amount = binding.etAmount.text.toString()
                            if (amount.isNotBlank() && binding.spinnerFrom.selectedItem != null && binding.spinnerTo.selectedItem != null) {
                                val from = binding.spinnerFrom.selectedItem as String
                                val to = binding.spinnerTo.selectedItem as String
                                viewModel.convertCurrency(amount, from, to)
                            }
                        }
                        is ConversionUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnConvert.isEnabled = true
                            binding.spinnerFrom.isEnabled = true
                            binding.spinnerTo.isEnabled = true
                            
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        is ConversionUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnConvert.isEnabled = false
                            binding.spinnerFrom.isEnabled = false
                            binding.spinnerTo.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnConvert.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            val from = binding.spinnerFrom.selectedItem as? String ?: return@setOnClickListener
            val to = binding.spinnerTo.selectedItem as? String ?: return@setOnClickListener
            viewModel.convertCurrency(amount, from, to)
        }

        binding.btnSwap.setOnClickListener {
            val fromCurrency = binding.spinnerFrom.selectedItem as? String
            val toCurrency = binding.spinnerTo.selectedItem as? String
            
            if (fromCurrency != null && toCurrency != null) {
                viewModel.swapCurrencies()

                val adapter = binding.spinnerFrom.adapter
                val fromPosition = (0 until adapter.count).indexOfFirst { adapter.getItem(it) == toCurrency }
                val toPosition = (0 until adapter.count).indexOfFirst { adapter.getItem(it) == fromCurrency }
                
                binding.spinnerFrom.setSelection(fromPosition)
                binding.spinnerTo.setSelection(toPosition)
            }
        }
    }

    private fun setupSpinners(currencies: List<String>, fromCurrency: String, toCurrency: String) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            currencies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerFrom.adapter = adapter
        binding.spinnerTo.adapter = adapter

        val fromPosition = currencies.indexOf(fromCurrency).takeIf { it >= 0 } ?: 0
        val toPosition = currencies.indexOf(toCurrency).takeIf { it >= 0 } ?: 0

        binding.spinnerFrom.setSelection(fromPosition)
        binding.spinnerTo.setSelection(toPosition)

        binding.spinnerFrom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val from = binding.spinnerFrom.selectedItem as? String ?: return
                val to = binding.spinnerTo.selectedItem as? String ?: return
                viewModel.updateExchangeRate(from, to)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val from = binding.spinnerFrom.selectedItem as? String ?: return
                val to = binding.spinnerTo.selectedItem as? String ?: return
                viewModel.updateExchangeRate(from, to)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 