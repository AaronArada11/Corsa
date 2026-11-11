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

class DocumentEditorFragment : Fragment(R.layout.fragment_document_editor) {
    private val viewModel by viewModels<DocumentEditorViewModel> {
        DocumentEditorViewModel.factory((requireActivity().application as CorsaApplication).container.repository)
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
        view.findViewById<View>(R.id.documentAttach).visibility = View.GONE
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
                    view.findViewById<View>(R.id.documentSave).isEnabled = !state.loading && !state.saving
                    view.findViewById<TextView>(R.id.documentError).text = state.error.orEmpty()
                    view.findViewById<TextView>(R.id.documentAttachment).setText(if (d.filePath == null) R.string.document_none else R.string.document_attached)
                    rendering = false
                }
            }
        }
    }
}
