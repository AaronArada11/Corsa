package com.corsa.app.ui.vehicles

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleListAdapterTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun bind_rendersAllFieldsAndHandlesNullPlateWithoutLeaking() {
        var clickedId = -1L
        val adapter = VehicleListAdapter { id -> clickedId = id }
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)

        val itemWithPlate = VehicleListItemUiModel(
            id = 10L,
            brand = "Honda",
            model = "Civic",
            year = 2020,
            plateNumber = "ABC-123",
            currentMileageKm = 30000L,
            mileageDisplay = "30,000 km"
        )
        holder.bind(itemWithPlate)

        val tvName = holder.itemView.findViewById<TextView>(R.id.tvVehicleName)
        val tvPlate = holder.itemView.findViewById<TextView>(R.id.tvPlateNumber)
        val tvMileage = holder.itemView.findViewById<TextView>(R.id.tvMileage)

        assertEquals("2020 Honda Civic", tvName.text.toString())
        assertEquals("ABC-123", tvPlate.text.toString())
        assertEquals(View.VISIBLE, tvPlate.visibility)
        assertEquals("30,000 km", tvMileage.text.toString())

        holder.itemView.findViewById<View>(R.id.cardVehicle).performClick()
        assertEquals(10L, clickedId)

        // Recycle/rebind to item without plate
        val itemWithoutPlate = VehicleListItemUiModel(
            id = 11L,
            brand = "Tesla",
            model = "Model 3",
            year = 2023,
            plateNumber = null,
            currentMileageKm = 5000L,
            mileageDisplay = "5,000 km"
        )
        holder.bind(itemWithoutPlate)

        assertEquals("2023 Tesla Model 3", tvName.text.toString())
        assertEquals(View.GONE, tvPlate.visibility)
        assertTrue(tvPlate.text.isNullOrEmpty())
    }
}
