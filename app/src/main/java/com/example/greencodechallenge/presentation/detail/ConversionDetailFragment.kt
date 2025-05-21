package com.example.greencodechallenge.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.example.greencodechallenge.R
import com.example.greencodechallenge.databinding.FragmentConversionDetailBinding
import com.example.greencodechallenge.domain.utils.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ConversionDetailFragment : Fragment() {

    private var _binding: FragmentConversionDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ConversionDetailViewModel by viewModels()
    private val args: ConversionDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        viewModel.loadConversion(args.conversionId)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.conversion.collect { conversion ->
                    conversion?.let { displayConversion(it) }
                }
            }
        }
    }

    private fun displayConversion(conversion: ConversionDetailUiState) {
        binding.apply {
            tvOriginalAmount.text = getString(
                R.string.original_amount_format,
                CurrencyFormatter.formatNumber(conversion.originalAmount),
                conversion.fromCurrency
            )
            
            tvConvertedAmount.text = getString(
                R.string.converted_amount_format,
                CurrencyFormatter.formatNumber(conversion.convertedAmount),
                conversion.toCurrency
            )
            
            tvConversionRate.text = getString(
                R.string.conversion_rate_format,
                conversion.fromCurrency,
                CurrencyFormatter.formatNumber(conversion.conversionRate),
                conversion.toCurrency
            )
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvTimestamp.text = dateFormat.format(Date(conversion.timestamp))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 