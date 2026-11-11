package com.corsa.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {

    private lateinit var context: Context
    private lateinit var db: CorsaDatabase
    private lateinit var dao: VehicleDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = CorsaDatabase.buildInMemoryDatabase(context)
        dao = db.vehicleDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val entity = VehicleEntity(
            brand = "Toyota",
            model = "Corolla",
            year = 2022,
            plateNumber = "ABC-1234",
            currentMileageKm = 15000L
        )
        val id = dao.insert(entity)
        val loaded = dao.getById(id)

        assertNotNull(loaded)
        assertEquals("Toyota", loaded?.brand)
        assertEquals("Corolla", loaded?.model)
        assertEquals(2022, loaded?.year)
        assertEquals("ABC-1234", loaded?.plateNumber)
        assertEquals(15000L, loaded?.currentMileageKm)
        assertNull(loaded?.photoPath)
    }

    @Test
    fun observeAll_returnsVehiclesInOrder() = runBlocking {
        val id1 = dao.insert(
            VehicleEntity(brand = "Honda", model = "Civic", year = 2020, currentMileageKm = 30000L)
        )
        val id2 = dao.insert(
            VehicleEntity(brand = "Mazda", model = "3", year = 2021, currentMileageKm = 20000L)
        )

        val list = dao.observeAll().first()
        assertEquals(2, list.size)
        assertEquals(id1, list[0].id)
        assertEquals(id2, list[1].id)
    }

    @Test
    fun update_modifiesFieldsWithoutChangingId() = runBlocking {
        val id = dao.insert(
            VehicleEntity(brand = "Ford", model = "Focus", year = 2019, currentMileageKm = 50000L)
        )
        val updated = VehicleEntity(
            id = id,
            brand = "Ford",
            model = "Focus ST",
            year = 2019,
            plateNumber = "ST-999",
            currentMileageKm = 52000L
        )
        val rows = dao.update(updated)
        assertEquals(1, rows)

        val reloaded = dao.getById(id)
        assertEquals("Focus ST", reloaded?.model)
        assertEquals("ST-999", reloaded?.plateNumber)
        assertEquals(52000L, reloaded?.currentMileageKm)
    }

    @Test
    fun deleteById_removesOnlyTargetVehicle() = runBlocking {
        val id1 = dao.insert(
            VehicleEntity(brand = "Nissan", model = "Sentra", year = 2018, currentMileageKm = 60000L)
        )
        val id2 = dao.insert(
            VehicleEntity(brand = "Subaru", model = "BRZ", year = 2022, currentMileageKm = 10000L)
        )

        val deletedRows = dao.deleteById(id1)
        assertEquals(1, deletedRows)
        assertNull(dao.getById(id1))
        assertNotNull(dao.getById(id2))
    }

    @Test
    fun observeById_andAdvanceMileage_emitOnlyHigherMileage() = runBlocking {
        val id = dao.insert(VehicleEntity(brand = "Kia", model = "Rio", year = 2020, currentMileageKm = 100L))
        assertEquals(100L, dao.observeById(id).first()?.currentMileageKm)
        assertEquals(0, dao.advanceMileageIfHigher(id, 99L))
        assertEquals(1, dao.advanceMileageIfHigher(id, 101L))
        assertEquals(101L, dao.observeById(id).first()?.currentMileageKm)
    }

    @Test
    fun fileBackedDatabase_persistsAcrossReopen() = runBlocking {
        val dbFile = File(context.filesDir, "test_file_backed.db")
        if (dbFile.exists()) dbFile.delete()

        val fileDb1 = Room.databaseBuilder(context, CorsaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        val id = fileDb1.vehicleDao().insert(
            VehicleEntity(brand = "Hyundai", model = "Elantra", year = 2023, plateNumber = "ABC-123", currentMileageKm = 5000L, photoPath = "/photo/one")
        )
        val secondId = fileDb1.vehicleDao().insert(
            VehicleEntity(brand = "Suzuki", model = "Swift", year = 2021, currentMileageKm = 42000L)
        )
        fileDb1.close()

        val fileDb2 = Room.databaseBuilder(context, CorsaDatabase::class.java, dbFile.absolutePath)
            .allowMainThreadQueries()
            .build()
        val loaded = fileDb2.vehicleDao().getById(id)
        assertNotNull(loaded)
        assertEquals("Hyundai", loaded?.brand)
        assertEquals("Elantra", loaded?.model)
        assertEquals(2023, loaded?.year)
        assertEquals("ABC-123", loaded?.plateNumber)
        assertEquals(5000L, loaded?.currentMileageKm)
        assertEquals("/photo/one", loaded?.photoPath)
        assertEquals("Suzuki", fileDb2.vehicleDao().getById(secondId)?.brand)
        fileDb2.close()
        dbFile.delete()
        Unit
    }
}
