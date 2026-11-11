package com.corsa.app.model

import com.corsa.app.calculation.FuelSummary
import com.corsa.app.calculation.MaintenanceStatus
import com.corsa.app.calculation.TankState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationOutcomesTest {

    @Test
    fun saveOutcome_distinguishesSuccessValidationAndMissing() {
        val success: SaveOutcome = SaveOutcome.Success(42L)
        val errors: SaveOutcome = SaveOutcome.ValidationErrors(mapOf("brand" to "Brand is required"))
        val missing: SaveOutcome = SaveOutcome.MissingRecord

        assertTrue(success is SaveOutcome.Success)
        assertEquals(42L, (success as SaveOutcome.Success).id)

        assertTrue(errors is SaveOutcome.ValidationErrors)
        assertEquals("Brand is required", (errors as SaveOutcome.ValidationErrors).errors["brand"])

        assertEquals(SaveOutcome.MissingRecord, missing)
    }

    @Test
    fun deleteOutcome_distinguishesDeletedAndMissing() {
        val deleted: DeleteOutcome = DeleteOutcome.Deleted
        val missing: DeleteOutcome = DeleteOutcome.Missing

        assertEquals(DeleteOutcome.Deleted, deleted)
        assertEquals(DeleteOutcome.Missing, missing)
    }

    @Test
    fun contracts_enumsAndModelsInstantiate() {
        assertEquals(5, MaintenanceStatus.entries.size)
        assertEquals(3, TankState.entries.size)
        assertEquals(3, SpendingSourceType.entries.size)

        val summary = FuelSummary(totalDistanceKm = 450L, totalLiters = 30.0, efficiencyKmL = 15.0)
        assertEquals(450L, summary.totalDistanceKm)
        assertEquals(30.0, summary.totalLiters, 0.0001)
        assertEquals(15.0, summary.efficiencyKmL!!, 0.0001)

        val spending = SpendingItem(
            sourceType = SpendingSourceType.FUEL,
            sourceId = 1L,
            vehicleId = 10L,
            dateIso = "2026-11-11",
            category = "Fuel",
            costMinor = 150000L
        )
        assertEquals(150000L, spending.costMinor)
    }
}
