package com.corsa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class CorsaDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    companion object {
        const val DATABASE_NAME = "corsa.db"

        fun buildFileDatabase(context: Context, databaseName: String = DATABASE_NAME): CorsaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CorsaDatabase::class.java,
                databaseName
            ).build()
        }

        fun buildInMemoryDatabase(context: Context): CorsaDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                CorsaDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
