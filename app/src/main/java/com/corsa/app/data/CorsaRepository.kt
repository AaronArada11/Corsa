package com.corsa.app.data

import com.corsa.app.data.local.CorsaDatabase
import com.corsa.app.data.local.VehicleDao
import com.corsa.app.data.local.VehicleEntity
import com.corsa.app.model.DeleteOutcome
import com.corsa.app.model.SaveOutcome
import com.corsa.app.model.Vehicle
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class CorsaRepository(
    private val vehicleDao: VehicleDao,
    private val currentYearProvider: () -> Int = { Calendar.getInstance().get(Calendar.YEAR) }
) {
    constructor(
        database: CorsaDatabase,
        currentYearProvider: () -> Int = { Calendar.getInstance().get(Calendar.YEAR) }
    ) : this(database.vehicleDao(), currentYearProvider)

    suspend fun saveVehicle(draft: VehicleDraft): SaveOutcome {
        val errors = mutableMapOf<String, String>()

        val trimmedBrand = draft.brand.trim()
        if (trimmedBrand.isEmpty()) {
            errors["brand"] = "Brand is required"
        }

        val trimmedModel = draft.model.trim()
        if (trimmedModel.isEmpty()) {
            errors["model"] = "Model is required"
        }

        val maxAllowedYear = currentYearProvider() + 1
        val parsedYear = draft.year.trim().toIntOrNull()
        if (parsedYear == null) {
            errors["year"] = "Valid year is required"
        } else if (parsedYear < 1886 || parsedYear > maxAllowedYear) {
            errors["year"] = "Year must be between 1886 and $maxAllowedYear"
        }

        val parsedMileage = draft.currentMileageKm.trim().toLongOrNull()
        if (parsedMileage == null) {
            errors["currentMileageKm"] = "Valid mileage is required"
        } else if (parsedMileage < 0L) {
            errors["currentMileageKm"] = "Mileage cannot be negative"
        }

        if (errors.isNotEmpty()) {
            return SaveOutcome.ValidationErrors(errors)
        }

        val normalizedPlate = draft.plateNumber.trim().ifEmpty { null }
        val entity = VehicleEntity(
            id = draft.id,
            brand = trimmedBrand,
            model = trimmedModel,
            year = parsedYear!!,
            plateNumber = normalizedPlate,
            currentMileageKm = parsedMileage!!,
            photoPath = draft.photoPath
        )

        return if (draft.id == 0L) {
            val newId = vehicleDao.insert(entity)
            SaveOutcome.Success(newId)
        } else {
            val rowsUpdated = vehicleDao.update(entity)
            if (rowsUpdated > 0) {
                SaveOutcome.Success(draft.id)
            } else {
                SaveOutcome.MissingRecord
            }
        }
    }

    suspend fun deleteVehicle(id: Long): DeleteOutcome {
        val rowsDeleted = vehicleDao.deleteById(id)
        return if (rowsDeleted > 0) {
            DeleteOutcome.Deleted
        } else {
            DeleteOutcome.Missing
        }
    }

    suspend fun getVehicle(id: Long): Vehicle? {
        return vehicleDao.getById(id)?.toDomain()
    }

    fun observeVehicle(id: Long): Flow<Vehicle?> {
        return vehicleDao.observeById(id).map { it?.toDomain() }
    }

    fun observeAllVehicles(): Flow<List<Vehicle>> {
        return vehicleDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    companion object {
        fun VehicleEntity.toDomain(): Vehicle = Vehicle(
            id = id,
            brand = brand,
            model = model,
            year = year,
            plateNumber = plateNumber,
            currentMileageKm = currentMileageKm,
            photoPath = photoPath
        )

        fun Vehicle.toDraft(): VehicleDraft = VehicleDraft(
            id = id,
            brand = brand,
            model = model,
            year = year.toString(),
            plateNumber = plateNumber.orEmpty(),
            currentMileageKm = currentMileageKm.toString(),
            photoPath = photoPath
        )
    }
}
