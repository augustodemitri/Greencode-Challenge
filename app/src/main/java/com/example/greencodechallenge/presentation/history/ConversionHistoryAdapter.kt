package com.example.greencodechallenge.presentation.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.greencodechallenge.R
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.databinding.ItemConversionHistoryBinding
import com.example.greencodechallenge.domain.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversionHistoryAdapter : ListAdapter<ConversionHistory, ConversionHistoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversionHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemConversionHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        fun bind(item: ConversionHistory) {
            val context = binding.root.context
            
            binding.apply {
                tvOriginalAmount.text = context.getString(
                    R.string.original_amount_format,
                    CurrencyFormatter.formatNumber(item.originalAmount),
                    item.fromCurrency
                )
                
                tvConvertedAmount.text = context.getString(
                    R.string.converted_amount_format,
                    CurrencyFormatter.formatNumber(item.convertedAmount),
                    item.toCurrency
                )
                
                tvConversionRate.text = context.getString(
                    R.string.conversion_rate_format,
                    item.fromCurrency,
                    CurrencyFormatter.formatNumber(item.conversionRate),
                    item.toCurrency
                )
                
                tvTimestamp.text = dateFormat.format(Date(item.timestamp))
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ConversionHistory>() {
            override fun areItemsTheSame(oldItem: ConversionHistory, newItem: ConversionHistory): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ConversionHistory, newItem: ConversionHistory): Boolean {
                return oldItem == newItem
            }
        }
    }
} 