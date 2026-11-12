package com.corsa.app.ui.dashboard

import android.graphics.Bitmap
import android.content.res.Configuration
import android.widget.TextView
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.CorsaApplication
import com.corsa.app.MainActivity
import com.corsa.app.R
import com.corsa.app.model.VehicleDraft
import com.corsa.app.model.SaveOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PopulatedDashboardTest {
    @Test fun switching_edits_optionalPlate_fallback_andMissingData(): Unit = runBlocking {
        val app = ApplicationProvider.getApplicationContext<CorsaApplication>()
        val repo = app.container.repository
        val ids = mutableListOf<Long>()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val initiallyEmpty = repo.observeAllVehicles().first().isEmpty()
                if (initiallyEmpty) onView(withId(R.id.emptyTitle)).check(matches(withText(R.string.dashboard_empty_title)))
                fun save(draft: VehicleDraft): Long =
                    runBlocking { (repo.saveVehicle(draft) as SaveOutcome.Success).id }
                val first = save(VehicleDraft(brand = "Dashboard A", model = "One", year = "2024", currentMileageKm = "12345", plateNumber = "TEST 101"))
                ids.add(first)
                if (initiallyEmpty) onView(withId(R.id.selectedBrand)).perform(awaitText("Dashboard A"))
                val second = save(VehicleDraft(brand = "Dashboard B", model = "Two", year = "2025", currentMileageKm = "987", photoPath = File(app.filesDir, "missing-photo").path))
                ids.add(second)
                onView(withId(R.id.vehicleSelector)).perform(click())
                onView(withText("2025 Dashboard B Two")).perform(click())
                onView(withId(R.id.selectedBrand)).check(matches(withText("Dashboard B")))
                onView(withId(R.id.selectedPlate)).check(matches(withEffectiveVisibility(Visibility.GONE)))
                onView(withId(R.id.selectedOdometer)).check(matches(withText("987 km")))
                onView(withId(R.id.selectedPhoto)).check(matches(withContentDescription(app.getString(R.string.dashboard_photo_fallback, "2025 Dashboard B Two"))))
                onView(withId(R.id.vehicleSelector)).perform(click())
                onView(withText("2024 Dashboard A One")).perform(click())
                onView(withId(R.id.selectedPlate)).check(matches(withText("TEST 101")))
                scenario.onActivity { activity ->
                    val root = activity.findViewById<View>(R.id.main)
                    val image = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                    root.draw(android.graphics.Canvas(image))
                    File(app.getExternalFilesDir(null), "dashboard-populated-check.png").outputStream().use {
                        image.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                save(VehicleDraft(id = first, brand = "Changed", model = "One", year = "2024", currentMileageKm = "12346"))
                onView(withId(R.id.selectedBrand)).perform(awaitText("Changed"))
                onView(withText(R.string.dashboard_insufficient)).perform(scrollTo()).check(matches(isDisplayed()))
                onView(withText(R.string.dashboard_spending_explanation)).perform(scrollTo()).check(matches(isDisplayed()))
                // Inflate at 200% without changing the emulator's global settings.
                scenario.onActivity { activity ->
                    val config = Configuration(activity.resources.configuration).apply { fontScale = 2f }
                    val context = activity.createConfigurationContext(config)
                    val large = android.view.LayoutInflater.from(context).cloneInContext(
                        android.view.ContextThemeWrapper(context, R.style.Theme_Corsa_EmptyDashboard)
                    ).inflate(R.layout.dashboard_populated, null)
                    large.findViewById<TextView>(R.id.selectedBrand).text = "Dashboard vehicle"
                    large.findViewById<TextView>(R.id.selectedModel).text = "2025 Long model name"
                    large.findViewById<TextView>(R.id.selectedOdometer).text = "123,456 km"
                    large.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1400, View.MeasureSpec.EXACTLY))
                    large.layout(0, 0, 1080, 1400)
                    val body = large.findViewById<TextView>(R.id.selectedModel)
                    assertTrue(body.textSize > 16 * context.resources.displayMetrics.density)
                    assertTrue((large as android.widget.ScrollView).getChildAt(0).measuredHeight > 1400)
                    val image = Bitmap.createBitmap(1080, 1400, Bitmap.Config.ARGB_8888)
                    large.draw(android.graphics.Canvas(image))
                    File(app.getExternalFilesDir(null), "dashboard-large-font-check.png").outputStream().use {
                        image.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
                repo.deleteVehicle(first)
                onView(withId(R.id.vehicleSelector)).perform(scrollTo(), click())
                onView(withText("2025 Dashboard B Two")).perform(click())
                onView(withId(R.id.selectedBrand)).check(matches(withText("Dashboard B")))
            }
        } finally { ids.forEach { repo.deleteVehicle(it) } }
    }

    @Test fun photo_privateFile_decodesWithAspectRatio_andInvalidFilesFallback() {
        val app = ApplicationProvider.getApplicationContext<CorsaApplication>()
        val file = File.createTempFile("dashboard-photo-", ".png", app.filesDir)
        try {
            val source = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val decoded = DashboardPhoto.load(app.filesDir, file.path)!!
            assertEquals(2f, decoded.width.toFloat() / decoded.height, 0.01f)
            assertNull(DashboardPhoto.load(app.filesDir, "/sdcard/external.png"))
            assertNull(DashboardPhoto.load(app.filesDir, null))
            file.writeText("unreadable image")
            assertNull(DashboardPhoto.load(app.filesDir, file.path))
        } finally { file.delete() }
    }

    private fun awaitText(expected: String) = object : androidx.test.espresso.ViewAction {
        override fun getConstraints() = isAssignableFrom(TextView::class.java)
        override fun getDescription() = "Wait for observed Room text: $expected"
        override fun perform(controller: androidx.test.espresso.UiController, view: View) {
            val deadline = android.os.SystemClock.uptimeMillis() + 5_000
            while ((view as TextView).text.toString() != expected &&
                android.os.SystemClock.uptimeMillis() < deadline) {
                controller.loopMainThreadForAtLeast(50)
            }
            assertEquals(expected, (view as TextView).text.toString())
        }
    }
}
