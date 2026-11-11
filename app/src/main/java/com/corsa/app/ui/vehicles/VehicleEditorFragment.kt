package com.corsa.app.ui.vehicles

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.corsa.app.model.VehicleDraft
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class VehicleEditorFragment : Fragment(R.layout.fragment_vehicle_editor) {
    private val viewModel by viewModels<VehicleEditorViewModel> {
        VehicleEditorViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(if (arguments?.getLong("vehicleId", 0L) == 0L) R.string.create_vehicle else R.string.edit_vehicle)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        val content = view.findViewById<View>(R.id.editorContent)
        val status = view.findViewById<View>(R.id.editorStatus)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.editorProgress)
        val statusTitle = view.findViewById<TextView>(R.id.editorStatusTitle)
        val statusMessage = view.findViewById<TextView>(R.id.editorStatusMessage)
        val statusAction = view.findViewById<MaterialButton>(R.id.editorStatusAction)
        val storageError = view.findViewById<TextView>(R.id.storageError)
        val save = view.findViewById<MaterialButton>(R.id.saveVehicle)
        val brand = view.findViewById<TextInputEditText>(R.id.brandInput)
        val model = view.findViewById<TextInputEditText>(R.id.modelInput)
        val year = view.findViewById<TextInputEditText>(R.id.yearInput)
        val plate = view.findViewById<TextInputEditText>(R.id.plateInput)
        val mileage = view.findViewById<TextInputEditText>(R.id.mileageInput)
        fun update() {
            val old = (viewModel.uiState.value as? VehicleEditorUiState.Form)?.draft ?: return
            viewModel.updateDraft(VehicleDraft(old.id, brand.text.toString(), model.text.toString(), year.text.toString(), plate.text.toString(), mileage.text.toString(), old.photoPath))
        }
        listOf(brand, model, year, plate, mileage).forEach { it.doAfterTextChanged { update() } }
        save.setOnClickListener { viewModel.save() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    content.isVisible = state is VehicleEditorUiState.Form
                    status.isVisible = state !is VehicleEditorUiState.Form && state !is VehicleEditorUiState.Saved
                    progress.isVisible = state is VehicleEditorUiState.Loading
                    statusTitle.isVisible = state !is VehicleEditorUiState.Loading
                    statusMessage.isVisible = state is VehicleEditorUiState.Missing
                    statusAction.isVisible = state is VehicleEditorUiState.LoadError || state is VehicleEditorUiState.Missing
                    when (state) {
                        VehicleEditorUiState.Loading -> Unit
                        VehicleEditorUiState.Missing -> { statusTitle.setText(R.string.missing_vehicle_title); statusMessage.setText(R.string.missing_vehicle_subtitle); statusAction.setText(R.string.back_to_list); statusAction.setOnClickListener { findNavController().popBackStack(R.id.vehicleListFragment, false) } }
                        is VehicleEditorUiState.LoadError -> { statusTitle.text = state.message; statusAction.setText(R.string.retry); statusAction.setOnClickListener { viewModel.retryLoad() } }
                        is VehicleEditorUiState.Form -> {
                            fun TextInputEditText.show(value: String) { if (text.toString() != value) setText(value) }
                            brand.show(state.draft.brand); model.show(state.draft.model); year.show(state.draft.year); plate.show(state.draft.plateNumber); mileage.show(state.draft.currentMileageKm)
                            view.findViewById<TextInputLayout>(R.id.brandLayout).error = state.errors["brand"]
                            view.findViewById<TextInputLayout>(R.id.modelLayout).error = state.errors["model"]
                            view.findViewById<TextInputLayout>(R.id.yearLayout).error = state.errors["year"]
                            view.findViewById<TextInputLayout>(R.id.mileageLayout).error = state.errors["currentMileageKm"]
                            storageError.text = state.storageError.orEmpty(); storageError.isVisible = state.storageError != null
                            save.isEnabled = !state.saving
                            listOf(brand, model, year, plate, mileage).forEach { it.isEnabled = !state.saving }
                        }
                        is VehicleEditorUiState.Saved -> {
                            findNavController().popBackStack(R.id.vehicleListFragment, false)
                            findNavController().navigate(R.id.vehicleDetailsFragment, Bundle().apply { putLong("vehicleId", state.id) })
                        }
                    }
                }
            }
        }
    }
}
