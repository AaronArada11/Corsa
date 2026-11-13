package com.corsa.app.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var photoJob: Job? = null
    private val viewModel by viewModels<DashboardViewModel> {
        DashboardViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.emptyTitle)
        val message = view.findViewById<TextView>(R.id.emptyMessage)
        val action = view.findViewById<MaterialButton>(R.id.addVehicleButton)
        val vehicleImage = view.findViewById<ImageView>(R.id.emptyVehicleImage)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.dashboardProgress)

        ViewCompat.setAccessibilityHeading(title, true)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    view.findViewById<View>(R.id.dashboardContent).isVisible = state !is DashboardUiState.Garage
                    view.findViewById<View>(R.id.populatedDashboard).isVisible = state is DashboardUiState.Garage
                    progress.isVisible = state is DashboardUiState.Loading
                    title.isVisible = state !is DashboardUiState.Loading
                    message.isVisible = state !is DashboardUiState.Loading
                    action.isVisible = state !is DashboardUiState.Loading
                    vehicleImage.isVisible = state is DashboardUiState.Empty || state is DashboardUiState.Garage
                    when (state) {
                        DashboardUiState.Loading -> Unit
                        DashboardUiState.Empty -> {
                            title.setText(R.string.dashboard_empty_title)
                            message.setText(R.string.dashboard_empty_message)
                            action.setText(R.string.dashboard_add_vehicle)
                            action.setOnClickListener {
                                findNavController().navigate(
                                    R.id.vehicleEditorFragment,
                                    Bundle().apply { putLong("vehicleId", 0L) },
                                )
                            }
                        }
                        is DashboardUiState.Garage -> {
                            renderGarage(view, state)
                        }
                        DashboardUiState.Error -> {
                            title.setText(R.string.dashboard_load_error_title)
                            message.setText(R.string.vehicle_load_error_title)
                            action.setText(R.string.retry)
                            action.setOnClickListener { viewModel.retry() }
                        }
                    }
                }
            }
        }
    }

    private fun addVehicle() {
        findNavController().navigate(R.id.vehicleEditorFragment, Bundle().apply { putLong("vehicleId", 0L) })
    }

    private fun renderGarage(view: View, state: DashboardUiState.Garage) {
        val car = state.selected
        view.findViewById<TextView>(R.id.brandBadge).text =
            car.brand.trim().take(1).uppercase(Locale.getDefault())
        view.findViewById<TextView>(R.id.selectedBrand).text = car.brand
        view.findViewById<TextView>(R.id.selectedModel).text = getString(R.string.dashboard_model_year, car.year, car.model)
        view.findViewById<TextView>(R.id.selectedPlate).apply {
            text = car.plateNumber
            isVisible = !car.plateNumber.isNullOrBlank()
        }
        view.findViewById<TextView>(R.id.selectedOdometer).text =
            getString(R.string.dashboard_km, NumberFormat.getIntegerInstance().format(car.currentMileageKm))
        view.findViewById<View>(R.id.dashboardAddVehicle).setOnClickListener { addVehicle() }
        view.findViewById<View>(R.id.dashboardViewCars).setOnClickListener {
            findNavController().navigate(R.id.vehicleListFragment)
        }
        view.findViewById<View>(R.id.vehicleSelector).apply {
            contentDescription = getString(R.string.dashboard_vehicle_description, car.displayName, car.plateNumber.orEmpty())
            setOnClickListener {
                val current = viewModel.uiState.value as? DashboardUiState.Garage ?: return@setOnClickListener
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dashboard_choose_vehicle)
                    .setSingleChoiceItems(current.vehicles.map { it.displayName }.toTypedArray(),
                        current.vehicles.indexOfFirst { it.id == current.selected.id }) { dialog, index ->
                        viewModel.selectVehicle(current.vehicles[index].id)
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.dashboard_add_another) { _, _ -> addVehicle() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
        val photo = view.findViewById<ImageView>(R.id.selectedPhoto)
        photoJob?.cancel()
        photo.setImageResource(R.drawable.dashboard_car_placeholder)
        photo.contentDescription = getString(R.string.dashboard_photo_fallback, car.displayName)
        val privateRoot = requireContext().filesDir
        photoJob = viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                DashboardPhoto.load(privateRoot, car.photoPath)
            }
            if (bitmap != null) {
                photo.setImageBitmap(bitmap)
                photo.contentDescription = getString(R.string.dashboard_photo, car.displayName)
            }
        }
    }

    override fun onDestroyView() {
        photoJob?.cancel()
        photoJob = null
        super.onDestroyView()
    }
}
