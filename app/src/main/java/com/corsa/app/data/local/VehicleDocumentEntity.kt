package com.corsa.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vehicle_document", foreignKeys = [ForeignKey(entity = VehicleEntity::class,
    parentColumns = ["id"], childColumns = ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")])
data class VehicleDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val title: String,
    val documentType: String,
    val documentNumber: String? = null,
    val issueDate: String? = null,
    val expirationDate: String? = null,
    val costMinor: Long = 0,
    val filePath: String? = null,
    val physicalLocation: String? = null,
    val description: String? = null
)
