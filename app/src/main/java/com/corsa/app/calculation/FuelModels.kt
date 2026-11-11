package com.corsa.app.calculation

/**
 * Fuel tank fill status recorded at fuel purchase.
 */
enum class TankState {
    /**
     * Tank filled completely to the brim; acts as a calculation anchor.
     */
    FULL,

    /**
     * Tank partially filled; liters absorbed into next full fill calculation.
     */
    PARTIAL,

    /**
     * Declares missing or unrecorded fuel history; breaks the efficiency segment.
     */
    GAP
}

/**
 * Calculated fuel summary over a sequence of fuel entries or a time period.
 */
data class FuelSummary(
    val totalDistanceKm: Long,
    val totalLiters: Double,
    val efficiencyKmL: Double?
)
