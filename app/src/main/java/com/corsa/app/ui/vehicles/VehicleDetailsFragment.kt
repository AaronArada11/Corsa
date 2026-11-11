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
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.corsa.app.model.Vehicle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.text.NumberFormat

class VehicleDetailsFragment : Fragment(R.layout.fragment_vehicle_details) {
    private val viewModel by viewModels<VehicleDetailsViewModel> {
        VehicleDetailsViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { findNavController().navigateUp() }
        val content = view.findViewById<View>(R.id.detailsContent)
        val status = view.findViewById<View>(R.id.detailsStatus)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.detailsProgress)
        val statusTitle = view.findViewById<TextView>(R.id.detailsStatusTitle)
        val statusMessage = view.findViewById<TextView>(R.id.detailsStatusMessage)
        val statusAction = view.findViewById<MaterialButton>(R.id.detailsStatusAction)
        val name = view.findViewById<TextView>(R.id.vehicleName)
        val plate = view.findViewById<TextView>(R.id.vehiclePlate)
        val mileage = view.findViewById<TextView>(R.id.vehicleMileage)
        val deleteError = view.findViewById<TextView>(R.id.deleteError)
        val edit = view.findViewById<MaterialButton>(R.id.editVehicle)
        val delete = view.findViewById<MaterialButton>(R.id.deleteVehicle)
        fun showDeleteDialog(vehicle: Vehicle) {
            MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message, vehicle.displayName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteConfirmed() }.show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    content.isVisible = state is VehicleDetailsUiState.Content
                    status.isVisible = state !is VehicleDetailsUiState.Content && state !is VehicleDetailsUiState.Deleted
                    progress.isVisible = state is VehicleDetailsUiState.Loading
                    statusTitle.isVisible = state !is VehicleDetailsUiState.Loading
                    statusMessage.isVisible = state is VehicleDetailsUiState.Missing
                    statusAction.isVisible = state is VehicleDetailsUiState.Error || state is VehicleDetailsUiState.Missing
                    when (state) {
                        VehicleDetailsUiState.Loading -> Unit
                        VehicleDetailsUiState.Missing -> { statusTitle.setText(R.string.missing_vehicle_title); statusMessage.setText(R.string.missing_vehicle_subtitle); statusAction.setText(R.string.back_to_list); statusAction.setOnClickListener { findNavController().popBackStack(R.id.vehicleListFragment, false) } }
                        is VehicleDetailsUiState.Error -> { statusTitle.text = state.message; statusAction.setText(R.string.retry); statusAction.setOnClickListener { viewModel.retry() } }
                        is VehicleDetailsUiState.Content -> {
                            name.text = state.vehicle.displayName
                            plate.text = state.vehicle.plateNumber?.let { getString(R.string.details_plate, it) } ?: getString(R.string.details_no_plate)
                            mileage.text = getString(R.string.details_mileage, NumberFormat.getIntegerInstance().format(state.vehicle.currentMileageKm))
                            deleteError.text = state.deleteError.orEmpty(); deleteError.isVisible = state.deleteError != null
                            edit.isEnabled = !state.deleting; delete.isEnabled = !state.deleting
                            edit.setOnClickListener { findNavController().navigate(R.id.vehicleEditorFragment, Bundle().apply { putLong("vehicleId", state.vehicle.id) }) }
                            delete.setOnClickListener { showDeleteDialog(state.vehicle) }
                        }
                        VehicleDetailsUiState.Deleted -> findNavController().popBackStack(R.id.vehicleListFragment, false)
                    }
                }
            }
        }
    }
}
