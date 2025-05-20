package com.example.greencodechallenge.presentation.conversion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.example.greencodechallenge.databinding.FragmentConversionBinding

@AndroidEntryPoint
class ConversionFragment : Fragment() {

    private val viewModel by viewModels<ConversionViewModel>()

    private var _binding: FragmentConversionBinding? = null
    private val binding get() = _binding!!

    private var fromCurrency: String = ""
    private var toCurrency: String = ""

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

        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchExchangeRates()
    }

    private fun setupObservers() {
        viewModel.availableCurrencies.observe(viewLifecycleOwner) { currencies ->
            if (currencies.isNotEmpty()) {
                // Inicializar las monedas con las primeras disponibles
                if (fromCurrency.isEmpty()) {
                    fromCurrency = currencies.first()
                }
                if (toCurrency.isEmpty() && currencies.size > 1) {
                    toCurrency = currencies[1]
                } else if (toCurrency.isEmpty()) {
                    toCurrency = currencies.first()
                }
                
                setupSpinners(currencies)
            }
        }

        viewModel.conversionResult.observe(viewLifecycleOwner) { result ->
            binding.tvResult.text = result
        }
        
        viewModel.conversionRatio.observe(viewLifecycleOwner) { ratio ->
            if (ratio.isNotEmpty()) {
                binding.tvRatio.text = ratio
                binding.tvRatio.visibility = View.VISIBLE
            } else {
                binding.tvRatio.visibility = View.GONE
            }
        }

        viewModel.lastUpdated.observe(viewLifecycleOwner) { lastUpdated ->
            if (lastUpdated.isNotEmpty()) {
                binding.tvLastUpdated.text = lastUpdated
                binding.tvLastUpdated.visibility = View.VISIBLE
            } else {
                binding.tvLastUpdated.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConvert.isEnabled = !isLoading

            binding.spinnerFrom.isEnabled = !isLoading
            binding.spinnerTo.isEnabled = !isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnConvert.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            
            viewModel.convertCurrency(amount, fromCurrency, toCurrency)
        }
    }

    private fun setupSpinners(currencies: List<String>) {
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
                val newCurrency = currencies[position]
                if (fromCurrency != newCurrency) {
                    fromCurrency = newCurrency
                    viewModel.updateExchangeRate(fromCurrency, toCurrency)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.spinnerTo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newCurrency = currencies[position]
                if (toCurrency != newCurrency) {
                    toCurrency = newCurrency
                    viewModel.updateExchangeRate(fromCurrency, toCurrency)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 