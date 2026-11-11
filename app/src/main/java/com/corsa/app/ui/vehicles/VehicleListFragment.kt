package com.corsa.app.ui.vehicles

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class VehicleListFragment : Fragment(R.layout.fragment_vehicle_list) {
    private val viewModel by viewModels<VehicleListViewModel> {
        VehicleListViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<RecyclerView>(R.id.vehicleList)
        val status = view.findViewById<View>(R.id.statusPanel)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.progress)
        val title = view.findViewById<TextView>(R.id.statusTitle)
        val message = view.findViewById<TextView>(R.id.statusMessage)
        val action = view.findViewById<MaterialButton>(R.id.statusAction)
        val add = view.findViewById<ExtendedFloatingActionButton>(R.id.addVehicle)
        val adapter = VehicleListAdapter { id -> findNavController().navigate(R.id.vehicleDetailsFragment, Bundle().apply { putLong("vehicleId", id) }) }
        list.adapter = adapter
        add.setOnClickListener { findNavController().navigate(R.id.vehicleEditorFragment, Bundle().apply { putLong("vehicleId", 0L) }) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    list.isVisible = state is VehicleListUiState.Content
                    status.isVisible = state !is VehicleListUiState.Content
                    add.isVisible = state !is VehicleListUiState.Loading
                    progress.isVisible = state is VehicleListUiState.Loading
                    title.isVisible = state !is VehicleListUiState.Loading
                    message.isVisible = state is VehicleListUiState.Empty || state is VehicleListUiState.Error
                    action.isVisible = state is VehicleListUiState.Empty || state is VehicleListUiState.Error
                    when (state) {
                        VehicleListUiState.Loading -> Unit
                        VehicleListUiState.Empty -> { title.setText(R.string.empty_vehicles_title); message.setText(R.string.empty_vehicles_subtitle); action.setText(R.string.add_vehicle); action.setOnClickListener { add.performClick() } }
                        is VehicleListUiState.Content -> adapter.submitList(state.vehicles)
                        is VehicleListUiState.Error -> { title.setText(R.string.vehicle_load_error_title); message.text = state.message; action.setText(R.string.retry); action.setOnClickListener { viewModel.retry() } }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.vehicleList)?.adapter = null
        super.onDestroyView()
    }
}
