package com.corsa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDocumentDao {
    @Insert suspend fun insert(document: VehicleDocumentEntity): Long
    @Update suspend fun update(document: VehicleDocumentEntity): Int
    @Query("SELECT * FROM vehicle_document WHERE id = :id")
    suspend fun getById(id: Long): VehicleDocumentEntity?
    @Query("SELECT * FROM vehicle_document WHERE vehicleId = :vehicleId ORDER BY issueDate DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<VehicleDocumentEntity>>
    @Query("DELETE FROM vehicle_document WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    @Query("SELECT filePath FROM vehicle_document WHERE vehicleId = :vehicleId AND filePath IS NOT NULL")
    suspend fun attachmentPaths(vehicleId: Long): List<String>
    @Query("SELECT COUNT(*) FROM vehicle_document WHERE filePath = :path")
    suspend fun countReferences(path: String): Int
}
