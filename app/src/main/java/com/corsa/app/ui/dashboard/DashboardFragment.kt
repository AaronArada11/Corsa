package com.corsa.app.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private val viewModel by viewModels<DashboardViewModel> { DashboardViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository) }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.dashboardStatusTitle)
        val message = view.findViewById<TextView>(R.id.dashboardStatusMessage)
        val action = view.findViewById<MaterialButton>(R.id.dashboardAction)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.dashboardProgress)
        ViewCompat.setAccessibilityHeading(title, true)
        viewLifecycleOwner.lifecycleScope.launch { viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect { state ->
            progress.isVisible = state is DashboardUiState.Loading
            title.isVisible = state !is DashboardUiState.Loading; message.isVisible = state !is DashboardUiState.Loading; action.isVisible = state !is DashboardUiState.Loading
            when (state) {
                DashboardUiState.Loading -> Unit
                DashboardUiState.Empty -> { title.setText(R.string.dashboard_empty_title); message.setText(R.string.dashboard_empty_message); action.setText(R.string.dashboard_add_vehicle); action.setOnClickListener { findNavController().navigate(R.id.vehicleEditorFragment, Bundle().apply { putLong("vehicleId", 0L) }) } }
                is DashboardUiState.Garage -> { title.setText(R.string.dashboard_garage_title); message.text = getString(R.string.dashboard_garage_message, state.vehicleCount); action.setText(R.string.dashboard_view_cars); action.setOnClickListener { findNavController().navigate(R.id.vehicleListFragment) } }
                DashboardUiState.Error -> { title.setText(R.string.dashboard_load_error_title); message.setText(R.string.vehicle_load_error_title); action.setText(R.string.retry); action.setOnClickListener { viewModel.retry() } }
            }
        } } }
    }
}
