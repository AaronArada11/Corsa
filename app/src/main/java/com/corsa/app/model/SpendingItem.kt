package com.corsa.app.model

/**
 * Origin source type for a unified spending entry.
 */
enum class SpendingSourceType {
    FUEL,
    MAINTENANCE,
    EXPENSE
}

/**
 * Read model projected from the UNION ALL query of fuel_log, maintenance_log, and expense.
 *
 * All financial amounts are stored and calculated in integer minor currency units
 * ([costMinor] in Philippine centavos, 100 centavos = ₱1.00).
 */
data class SpendingItem(
    val sourceType: SpendingSourceType,
    val sourceId: Long,
    val vehicleId: Long,
    val dateIso: String,
    val category: String,
    val costMinor: Long
)
