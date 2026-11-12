package com.corsa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VehicleEntity::class, VehicleDocumentEntity::class],
    version = 2,
    exportSchema = true
)
abstract class CorsaDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun vehicleDocumentDao(): VehicleDocumentDao

    companion object {
        const val DATABASE_NAME = "corsa.db"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS vehicle_document (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, vehicleId INTEGER NOT NULL, title TEXT NOT NULL, documentType TEXT NOT NULL, documentNumber TEXT, issueDate TEXT, expirationDate TEXT, costMinor INTEGER NOT NULL, filePath TEXT, physicalLocation TEXT, description TEXT, FOREIGN KEY(vehicleId) REFERENCES vehicle(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vehicle_document_vehicleId ON vehicle_document(vehicleId)")
            }
        }

        fun buildFileDatabase(context: Context, databaseName: String = DATABASE_NAME): CorsaDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CorsaDatabase::class.java,
                databaseName
            ).addMigrations(MIGRATION_1_2).build()
        }

        fun buildInMemoryDatabase(context: Context): CorsaDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                CorsaDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
