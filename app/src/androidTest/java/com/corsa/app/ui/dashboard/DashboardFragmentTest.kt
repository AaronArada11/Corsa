package com.corsa.app.ui.dashboard

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.MainActivity
import com.corsa.app.CorsaApplication
import com.corsa.app.R
import com.corsa.app.model.SaveOutcome
import com.corsa.app.model.VehicleDraft
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardFragmentTest {
    @Test
    fun bottomNavigation_showsDashboardAndSettings_only() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.dashboardFragment)).check(matches(isDisplayed()))
            onView(withId(R.id.toolbar)).check(doesNotExist())
            onView(withId(R.id.settingsFragment)).check(matches(isDisplayed())).perform(click())
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
            onView(withId(R.id.analyticsFragment)).check(doesNotExist())
            onView(withId(R.id.vehicleListFragment)).check(doesNotExist())
        }
    }

    @Test
    fun dashboardOpensVehicleListWithoutBottomTab(): Unit = runBlocking {
        val repository = ApplicationProvider.getApplicationContext<CorsaApplication>().container.repository
        val id = (repository.saveVehicle(VehicleDraft(brand = "Navigation", model = "Test", year = "2024", currentMileageKm = "100")) as SaveOutcome.Success).id
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                onView(withText(R.string.dashboard_view_cars)).perform(scrollTo(), click())
                onView(withId(R.id.addVehicle)).check(matches(isDisplayed()))
            }
        } finally {
            repository.deleteVehicle(id)
        }
    }

    @Test
    fun savedVehicleReturnsToDashboardAfterDetailsBack(): Unit = runBlocking {
        val repository = ApplicationProvider.getApplicationContext<CorsaApplication>().container.repository
        val seedId = (repository.saveVehicle(VehicleDraft(brand = "Navigation Seed", model = "Test", year = "2024", currentMileageKm = "100")) as SaveOutcome.Success).id
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { it.findViewById<android.view.View>(R.id.dashboardAddVehicle).performClick() }
                onView(withId(R.id.brandInput)).perform(replaceText("Nav Flow"))
                onView(withId(R.id.modelInput)).perform(replaceText("Test"))
                onView(withId(R.id.yearInput)).perform(replaceText("2024"))
                onView(withId(R.id.mileageInput)).perform(replaceText("100"))
                onView(withId(R.id.saveVehicle)).perform(click())
                onView(withId(R.id.deleteVehicle)).check(matches(isDisplayed()))
                pressBack()
                onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
            }
        } finally {
            repository.observeAllVehicles().first().filter { it.brand == "Nav Flow" }.forEach { repository.deleteVehicle(it.id) }
            repository.deleteVehicle(seedId)
        }
    }
}
