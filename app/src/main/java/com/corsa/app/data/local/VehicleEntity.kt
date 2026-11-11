package com.corsa.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "brand")
    val brand: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "plateNumber")
    val plateNumber: String? = null,

    @ColumnInfo(name = "currentMileageKm")
    val currentMileageKm: Long,

    @ColumnInfo(name = "photoPath")
    val photoPath: String? = null
)
