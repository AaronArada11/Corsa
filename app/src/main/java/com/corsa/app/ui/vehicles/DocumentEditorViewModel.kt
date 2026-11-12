package com.corsa.app.ui.vehicles

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.corsa.app.data.CorsaRepository
import com.corsa.app.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class DocumentEditorState(val draft: DocumentDraft, val loading: Boolean = true,
    val saving: Boolean = false, val saved: Boolean = false, val error: String? = null,
    val errors: Map<String, String> = emptyMap())

class DocumentEditorViewModel(private val repository: CorsaRepository, private val savedState: SavedStateHandle) : ViewModel() {
    private var originalPath: String? = null
    private val pendingPaths = mutableSetOf<String>()
    private val vehicleId = savedState.get<Long>("vehicleId") ?: 0L
    private val documentId = savedState.get<Long>("documentId") ?: 0L
    private val mutableState = MutableStateFlow(DocumentEditorState(DocumentDraft(id = documentId, vehicleId = vehicleId)))
    val uiState = mutableState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                val stored = if (documentId > 0) repository.getDocument(documentId) else null
                originalPath = stored?.filePath
                if (repository.getVehicle(vehicleId) == null || (documentId > 0 && stored?.vehicleId != vehicleId)) {
                    mutableState.value = mutableState.value.copy(loading = false, error = "Vehicle or document no longer exists")
                    return@launch
                }
                val draft = stored?.let { DocumentDraft(it.id, it.vehicleId, it.title, it.documentType,
                    it.documentNumber.orEmpty(), it.issueDate.orEmpty(), it.expirationDate.orEmpty(),
                    BigDecimal.valueOf(it.costMinor, 2).toPlainString(), it.filePath, it.physicalLocation.orEmpty(), it.description.orEmpty()) }
                    ?: mutableState.value.draft
                val values = savedState.get<ArrayList<String>>("documentFields")
                val restored = if (values == null) draft else draft.copy(title = values[0], documentType = DocumentType.valueOf(values[1]),
                    documentNumber = values[2], issueDate = values[3], expirationDate = values[4], cost = values[5],
                    physicalLocation = values[6], description = values[7], filePath = savedState["documentPath"])
                mutableState.value = DocumentEditorState(restored, loading = false, saved = savedState["documentSaved"] ?: false)
                if (restored.filePath != null && restored.filePath != originalPath && !mutableState.value.saved) pendingPaths.add(restored.filePath)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, error = "Unable to load document. Try again.") }
        }
    }

    fun update(draft: DocumentDraft) {
        if (mutableState.value.saving) return
        if (draft.filePath != null && draft.filePath != originalPath) pendingPaths.add(draft.filePath)
        savedState["documentFields"] = arrayListOf(draft.title, draft.documentType.name, draft.documentNumber,
            draft.issueDate, draft.expirationDate, draft.cost, draft.physicalLocation, draft.description)
        savedState["documentPath"] = draft.filePath
        mutableState.value = mutableState.value.copy(draft = draft, errors = emptyMap(), error = null)
    }

    fun save() {
        val state = mutableState.value
        if (state.loading || state.saving || state.saved) return
        mutableState.value = state.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                when (val result = repository.saveDocument(state.draft)) {
                    is SaveOutcome.Success -> {
                        pendingPaths.remove(state.draft.filePath)
                        savedState["documentSaved"] = true
                        mutableState.value = state.copy(saved = true)
                    }
                    is SaveOutcome.ValidationErrors -> mutableState.value = state.copy(errors = result.errors)
                    SaveOutcome.MissingRecord -> mutableState.value = state.copy(error = "Vehicle or document no longer exists")
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = state.copy(error = "Couldn’t save document. Try again.") }
        }
    }

    override fun onCleared() {
        val paths = pendingPaths.toList()
        if (paths.isNotEmpty()) CoroutineScope(Dispatchers.IO).launch {
            paths.forEach { repository.discardDocumentAttachment(it) }
        }
        super.onCleared()
    }

    companion object {
        fun factory(repository: CorsaRepository) = viewModelFactory {
            initializer { DocumentEditorViewModel(repository, createSavedStateHandle()) }
        }
    }
}
