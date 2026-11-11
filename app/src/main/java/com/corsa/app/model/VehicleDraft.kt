package com.corsa.app.model

/**
 * Raw string input draft for creating or editing a vehicle profile in UI forms.
 */
data class VehicleDraft(
    val id: Long = 0L,
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val plateNumber: String = "",
    val currentMileageKm: String = "",
    val photoPath: String? = null
)
