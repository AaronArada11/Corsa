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
import androidx.room.withTransaction
import com.corsa.app.data.local.VehicleDocumentDao
import com.corsa.app.data.local.VehicleDocumentEntity
import com.corsa.app.model.DocumentDraft
import com.corsa.app.model.DocumentType
import com.corsa.app.model.VehicleDocument
import com.corsa.app.model.parseDocumentDate
import java.math.BigDecimal

class CorsaRepository(
    private val vehicleDao: VehicleDao,
    private val currentYearProvider: () -> Int = { Calendar.getInstance().get(Calendar.YEAR) },
    private val documentDao: VehicleDocumentDao? = null,
    private val database: CorsaDatabase? = null
) {
    constructor(
        database: CorsaDatabase,
        currentYearProvider: () -> Int = { Calendar.getInstance().get(Calendar.YEAR) }
    ) : this(database.vehicleDao(), currentYearProvider, database.vehicleDocumentDao(), database)

    private fun documents() = checkNotNull(documentDao) { "Document storage is not configured" }

    private suspend fun <T> transaction(block: suspend () -> T): T =
        database?.withTransaction { block() } ?: block()

    suspend fun saveDocument(draft: DocumentDraft): SaveOutcome {
        val errors = mutableMapOf<String, String>()
        if (draft.title.isBlank()) errors["title"] = "Title is required"
        val issue = draft.issueDate.trim().ifEmpty { null }
        val expiry = draft.expirationDate.trim().ifEmpty { null }
        if (issue != null && parseDocumentDate(issue) == null) errors["issueDate"] = "Use a valid YYYY-MM-DD date"
        if (expiry != null && parseDocumentDate(expiry) == null) errors["expirationDate"] = "Use a valid YYYY-MM-DD date"
        if (issue != null && expiry != null && expiry < issue) errors["expirationDate"] = "Expiration cannot precede issue date"
        val cost = try {
            BigDecimal(draft.cost.trim().ifEmpty { "0" }).movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) { null } catch (_: ArithmeticException) { null }
        if (cost == null || cost < 0) errors["cost"] = "Enter a nonnegative PHP amount with at most two decimals"
        if (cost != null && cost > 0 && issue == null) errors["issueDate"] = "Issue date is required for a paid document"
        if (errors.isNotEmpty()) return SaveOutcome.ValidationErrors(errors)
        return transaction {
            val dao = documents()
            if (vehicleDao.getById(draft.vehicleId) == null) return@transaction SaveOutcome.MissingRecord
            if (draft.id != 0L && dao.getById(draft.id)?.vehicleId != draft.vehicleId) return@transaction SaveOutcome.MissingRecord
            val entity = VehicleDocumentEntity(draft.id, draft.vehicleId, draft.title.trim(), draft.documentType.name,
                draft.documentNumber.trim().ifEmpty { null }, issue, expiry, cost!!, draft.filePath,
                draft.physicalLocation.trim().ifEmpty { null }, draft.description.trim().ifEmpty { null })
            if (draft.id == 0L) SaveOutcome.Success(dao.insert(entity))
            else if (dao.update(entity) > 0) SaveOutcome.Success(draft.id) else SaveOutcome.MissingRecord
        }
    }

    suspend fun getDocument(id: Long): VehicleDocument? = documents().getById(id)?.toDomain()

    fun observeVehicleDocuments(vehicleId: Long): Flow<List<VehicleDocument>> =
        documents().observeForVehicle(vehicleId).map { rows -> rows.map { it.toDomain() } }

    suspend fun deleteDocument(id: Long): DeleteOutcome =
        if (documents().deleteById(id) > 0) DeleteOutcome.Deleted else DeleteOutcome.Missing

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
        fun VehicleDocumentEntity.toDomain() = VehicleDocument(id, vehicleId, title, DocumentType.valueOf(documentType),
            documentNumber, issueDate, expirationDate, costMinor, filePath, physicalLocation, description)
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
