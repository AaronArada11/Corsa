package com.corsa.app.model

/**
 * Immutable domain read model representing a vehicle profile.
 */
data class Vehicle(
    val id: Long,
    val brand: String,
    val model: String,
    val year: Int,
    val plateNumber: String? = null,
    val currentMileageKm: Long,
    val photoPath: String? = null
) {
    val displayName: String
        get() = "$year $brand $model"
}
