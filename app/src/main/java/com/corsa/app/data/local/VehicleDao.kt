package com.corsa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity): Int

    @Query("SELECT * FROM vehicle WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VehicleEntity?

    @Query("SELECT * FROM vehicle WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle ORDER BY id ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE vehicle SET currentMileageKm = :newMileageKm WHERE id = :id AND currentMileageKm < :newMileageKm")
    suspend fun advanceMileageIfHigher(id: Long, newMileageKm: Long): Int
}
