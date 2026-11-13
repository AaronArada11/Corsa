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
import android.widget.LinearLayout
import com.google.android.material.tabs.TabLayout
import com.corsa.app.model.*
import java.text.SimpleDateFormat
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.content.ActivityNotFoundException
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider

class VehicleDetailsFragment : Fragment(R.layout.fragment_vehicle_details) {
    private var selectedTab = 0
    private val viewModel by viewModels<VehicleDetailsViewModel> {
        VehicleDetailsViewModel.provideFactory((requireActivity().application as CorsaApplication).container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedTab = savedInstanceState?.getInt("documentTab") ?: selectedTab
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.navigationContentDescription = getString(R.string.navigate_up)
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        val content = view.findViewById<View>(R.id.detailsContent)
        val history = view.findViewById<View>(R.id.documentHistoryPanel)
        val tabs = view.findViewById<TabLayout>(R.id.detailsTabs)
        tabs.addTab(tabs.newTab().setText(R.string.document_profile))
        tabs.addTab(tabs.newTab().setText(R.string.document_history))
        tabs.selectTab(tabs.getTabAt(selectedTab))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.position
                content.isVisible = selectedTab == 0 && viewModel.uiState.value is VehicleDetailsUiState.Content
                history.isVisible = selectedTab == 1 && viewModel.uiState.value is VehicleDetailsUiState.Content
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        view.findViewById<View>(R.id.addDocument).setOnClickListener {
            findNavController().navigate(R.id.documentEditorFragment, Bundle().apply { putLong("vehicleId", requireArguments().getLong("vehicleId")) })
        }
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
                    tabs.isVisible = state is VehicleDetailsUiState.Content
                    content.isVisible = state is VehicleDetailsUiState.Content && selectedTab == 0
                    history.isVisible = state is VehicleDetailsUiState.Content && selectedTab == 1
                    status.isVisible = state !is VehicleDetailsUiState.Content && state !is VehicleDetailsUiState.Deleted
                    progress.isVisible = state is VehicleDetailsUiState.Loading
                    statusTitle.isVisible = state !is VehicleDetailsUiState.Loading
                    statusMessage.isVisible = state is VehicleDetailsUiState.Missing
                    statusAction.isVisible = state is VehicleDetailsUiState.Error || state is VehicleDetailsUiState.Missing
                    when (state) {
                        VehicleDetailsUiState.Loading -> Unit
                        VehicleDetailsUiState.Missing -> { statusTitle.setText(R.string.missing_vehicle_title); statusMessage.setText(R.string.missing_vehicle_subtitle); statusAction.setText(R.string.back_to_list); statusAction.setOnClickListener { returnToVehicles() } }
                        is VehicleDetailsUiState.Error -> { statusTitle.text = state.message; statusAction.setText(R.string.retry); statusAction.setOnClickListener { viewModel.retry() } }
                        is VehicleDetailsUiState.Content -> {
                            renderDocuments(view, state)
                            name.text = state.vehicle.displayName
                            plate.text = state.vehicle.plateNumber?.let { getString(R.string.details_plate, it) } ?: getString(R.string.details_no_plate)
                            mileage.text = getString(R.string.details_mileage, NumberFormat.getIntegerInstance().format(state.vehicle.currentMileageKm))
                            deleteError.text = state.deleteError.orEmpty(); deleteError.isVisible = state.deleteError != null
                            edit.isEnabled = !state.deleting; delete.isEnabled = !state.deleting
                            edit.setOnClickListener { findNavController().navigate(R.id.vehicleEditorFragment, Bundle().apply { putLong("vehicleId", state.vehicle.id) }) }
                            delete.setOnClickListener { showDeleteDialog(state.vehicle) }
                        }
                        VehicleDetailsUiState.Deleted -> returnToVehicles()
                    }
                }
            }
        }
    }

    private fun returnToVehicles() {
        if (!findNavController().popBackStack(R.id.vehicleListFragment, false)) {
            findNavController().popBackStack(R.id.dashboardFragment, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("documentTab", selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        val state = viewModel.uiState.value as? VehicleDetailsUiState.Content ?: return
        view?.let { renderDocuments(it, state) }
    }

    private fun renderDocuments(view: View, state: VehicleDetailsUiState.Content) {
        val status = view.findViewById<TextView>(R.id.documentHistoryStatus)
        status.text = state.documentError ?: if (state.documentsLoading) "Loading documents…" else if (state.documents.isEmpty()) getString(R.string.document_empty) else ""
        status.setOnClickListener { viewModel.retryDocuments() }
        val rows = view.findViewById<LinearLayout>(R.id.documentRows)
        rows.removeAllViews()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        state.documents.forEach { document ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 24, 0, 24)
            }
            row.addView(TextView(requireContext()).apply {
                text = listOfNotNull(document.title, resources.getStringArray(R.array.document_types)[document.documentType.ordinal],
                    document.documentNumber, document.issueDate?.let { "Issued $it" }, document.expirationDate?.let { "Expires $it" },
                    "₱${BigDecimal.valueOf(document.costMinor, 2).toPlainString()}", document.physicalLocation?.let { "Physical copy: $it" }, document.description).joinToString("\n")
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            })
            val warning = when (document.expiration(today)) {
                DocumentExpiration.EXPIRED -> "Expired"
                DocumentExpiration.EXPIRES_TODAY -> "Expires today"
                DocumentExpiration.EXPIRING_SOON -> "Expires within 30 days"
                DocumentExpiration.CURRENT -> null
            }
            if (warning != null) row.addView(TextView(requireContext()).apply { text = warning; setTextColor(requireContext().getColor(R.color.corsa_warning)) })
            document.filePath?.let { path ->
                val files = (requireActivity().application as CorsaApplication).container.documentFiles
                val file = files.managedFile(path)?.takeIf { it.isFile }
                if (file == null) row.addView(TextView(requireContext()).apply { setText(R.string.document_unavailable) })
                else {
                    val preview = ImageView(requireContext()).apply {
                        contentDescription = getString(R.string.document_preview)
                        adjustViewBounds = true
                        maxHeight = 320
                    }
                    row.addView(preview)
                    viewLifecycleOwner.lifecycleScope.launch {
                        try { preview.setImageBitmap(files.thumbnail(path)) }
                        catch (error: kotlinx.coroutines.CancellationException) { throw error }
                        catch (_: Exception) { preview.visibility = View.GONE }
                    }
                    row.addView(MaterialButton(requireContext()).apply {
                        text = if (file.extension == "pdf") "Open PDF attachment" else getString(R.string.document_open)
                        setOnClickListener {
                            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.documents", file)
                            val mime = when (file.extension) { "pdf" -> "application/pdf"; "png" -> "image/png"; else -> "image/jpeg" }
                            try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (_: ActivityNotFoundException) { Toast.makeText(requireContext(), "No app can open this attachment", Toast.LENGTH_LONG).show() }
                        }
                    })
                }
            }
            row.addView(MaterialButton(requireContext()).apply {
                setText(R.string.document_edit)
                setOnClickListener { findNavController().navigate(R.id.documentEditorFragment, Bundle().apply {
                    putLong("vehicleId", document.vehicleId); putLong("documentId", document.id)
                }) }
            })
            row.addView(MaterialButton(requireContext()).apply {
                setText(R.string.delete)
                contentDescription = "Delete document ${document.title}"
                setOnClickListener { MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.document_delete_title)
                    .setMessage(R.string.document_delete_message).setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteDocument(document.id) }.show() }
            })
            rows.addView(row)
        }
    }
}
