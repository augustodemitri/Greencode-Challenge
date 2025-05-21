package com.example.greencodechallenge.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.greencodechallenge.R
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.databinding.FragmentHistoryBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: ConversionHistoryAdapter

    private var currentStateFlow: StateFlow<List<ConversionHistory>>? = null

    private enum class TimeFilter {
        TODAY, WEEK, ALL
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        
        // Por defecto, mostrar las conversiones de hoy
        binding.chipToday.isChecked = true
        loadConversions(TimeFilter.TODAY)
    }

    private fun setupRecyclerView() {
        adapter = ConversionHistoryAdapter()
        binding.recyclerViewHistory.adapter = adapter
    }
    
    private fun loadConversions(timeFilter: TimeFilter) {
        currentStateFlow = when (timeFilter) {
            TimeFilter.TODAY -> viewModel.todayConversions
            TimeFilter.WEEK -> viewModel.lastWeekConversions
            TimeFilter.ALL -> viewModel.conversions
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                currentStateFlow?.collectLatest { conversions ->
                    adapter.submitList(conversions)

                    if (conversions.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewHistory.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.recyclerViewHistory.visibility = View.VISIBLE
                    }

                    updateCounter(conversions.size, timeFilter)
                }
            }
        }
    }
    
    private fun updateCounter(count: Int, timeFilter: TimeFilter) {
        val filterName = when (timeFilter) {
            TimeFilter.TODAY -> getString(R.string.filter_today).lowercase()
            TimeFilter.WEEK -> getString(R.string.filter_week).lowercase()
            TimeFilter.ALL -> ""
        }

        binding.tvCountInfo.text = when {
            count == 0 -> getString(R.string.empty_history)
            timeFilter == TimeFilter.ALL -> resources.getQuantityString(R.plurals.conversions_count_total, count, count)
            timeFilter == TimeFilter.TODAY -> resources.getQuantityString(R.plurals.conversions_count_today, count, count, filterName)
            else -> resources.getQuantityString(R.plurals.conversions_count_last_week, count, count, filterName)
        }
    }
    
    private fun setupListeners() {
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
        
        binding.chipToday.setOnClickListener {
            loadConversions(TimeFilter.TODAY)
        }
        
        binding.chipWeek.setOnClickListener {
            loadConversions(TimeFilter.WEEK)
        }
        
        binding.chipAll.setOnClickListener {
            loadConversions(TimeFilter.ALL)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 