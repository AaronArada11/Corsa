package com.corsa.app.model

/**
 * Result of saving a domain entity or draft through [CorsaRepository].
 */
sealed interface SaveOutcome {
    /**
     * Successfully persisted with the generated or updated [id].
     */
    data class Success(val id: Long) : SaveOutcome

    /**
     * Validation failed before persistence.
     * [errors] maps field names to localized error messages or string resource keys.
     */
    data class ValidationErrors(val errors: Map<String, String>) : SaveOutcome

    /**
     * Target record to update was not found in persistence.
     */
    data object MissingRecord : SaveOutcome
}

/**
 * Result of deleting a domain entity by ID through [CorsaRepository].
 */
sealed interface DeleteOutcome {
    /**
     * Record existed and was deleted.
     */
    data object Deleted : DeleteOutcome

    /**
     * Record did not exist.
     */
    data object Missing : DeleteOutcome
}
