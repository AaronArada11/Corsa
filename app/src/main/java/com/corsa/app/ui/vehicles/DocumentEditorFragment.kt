package com.corsa.app.ui.vehicles

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.corsa.app.model.DocumentType
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellationException

class DocumentEditorFragment : Fragment(R.layout.fragment_document_editor) {
    private var importing = false
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewLifecycleOwner.lifecycleScope.launch {
            importing = true
            view?.findViewById<View>(R.id.documentSave)?.isEnabled = false
            try {
                val container = (requireActivity().application as CorsaApplication).container
                val path = container.documentFiles.import(uri)
                viewModel.update(viewModel.uiState.value.draft.copy(filePath = path))
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { view?.findViewById<TextView>(R.id.documentError)?.text = error.message ?: "Unable to import attachment" }
            finally { importing = false; view?.findViewById<View>(R.id.documentSave)?.isEnabled = true }
        }
    }
    private val viewModel by viewModels<DocumentEditorViewModel> {
        val container = (requireActivity().application as CorsaApplication).container
        DocumentEditorViewModel.factory(container.repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<MaterialToolbar>(R.id.toolbar).apply {
            navigationContentDescription = getString(R.string.navigate_up)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
        val ids = listOf(R.id.documentTitle, R.id.documentNumber, R.id.documentIssueDate,
            R.id.documentExpirationDate, R.id.documentCost, R.id.documentLocation, R.id.documentDescription)
        val fields = ids.map { view.findViewById<EditText>(it) }
        val type = view.findViewById<Spinner>(R.id.documentType)
        type.adapter = ArrayAdapter.createFromResource(requireContext(), R.array.document_types, android.R.layout.simple_spinner_dropdown_item)
        var rendering = false
        fun update() {
            if (rendering || viewModel.uiState.value.loading) return
            val old = viewModel.uiState.value.draft
            viewModel.update(old.copy(title = fields[0].text.toString(), documentNumber = fields[1].text.toString(),
                issueDate = fields[2].text.toString(), expirationDate = fields[3].text.toString(), cost = fields[4].text.toString(),
                physicalLocation = fields[5].text.toString(), description = fields[6].text.toString(),
                documentType = DocumentType.entries[type.selectedItemPosition.coerceAtLeast(0)]))
        }
        fields.forEach { it.doAfterTextChanged { update() } }
        type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) { update() }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        view.findViewById<View>(R.id.documentSave).setOnClickListener { update(); viewModel.save() }
        view.findViewById<View>(R.id.documentRetry).setOnClickListener { viewModel.load() }
        view.findViewById<View>(R.id.documentAttach).setOnClickListener { if (!importing) picker.launch(arrayOf("image/jpeg", "image/png", "application/pdf")) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.saved) { findNavController().popBackStack(); return@collect }
                    rendering = true
                    val d = state.draft
                    val values = listOf(d.title, d.documentNumber, d.issueDate, d.expirationDate, d.cost, d.physicalLocation, d.description)
                    fields.zip(values).forEach { (field, value) -> if (field.text.toString() != value) field.setText(value) }
                    type.setSelection(d.documentType.ordinal)
                    val keys = listOf("title", "documentNumber", "issueDate", "expirationDate", "cost", "physicalLocation", "description")
                    fields.zip(keys).forEach { (field, key) -> field.error = state.errors[key]; field.isEnabled = !state.loading && !state.saving }
                    type.isEnabled = !state.loading && !state.saving
                    view.findViewById<View>(R.id.documentSave).isEnabled = !state.loading && !state.saving && !importing
                    view.findViewById<View>(R.id.documentAttach).isEnabled = !state.loading && !state.saving && !importing
                    view.findViewById<TextView>(R.id.documentError).text = state.error ?: state.errors["filePath"].orEmpty()
                    view.findViewById<View>(R.id.documentRetry).visibility = if (state.error == null) View.GONE else View.VISIBLE
                    view.findViewById<TextView>(R.id.documentAttachment).setText(if (d.filePath == null) R.string.document_none else R.string.document_attached)
                    rendering = false
                }
            }
        }
    }
}
