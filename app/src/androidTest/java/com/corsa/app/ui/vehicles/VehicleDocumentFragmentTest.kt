package com.corsa.app.ui.vehicles

import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.navigation.fragment.NavHostFragment
import com.corsa.app.*
import com.corsa.app.model.*
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleDocumentFragmentTest {
    @Test fun createDocumentShowsLocationNotesAndExpiryInHistory() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val repository = (activity.application as CorsaApplication).container.repository
                val id = runBlocking { (repository.saveVehicle(VehicleDraft(brand = "Document test", model = "Car", year = "2020", currentMileageKm = "0")) as SaveOutcome.Success).id }
                (activity.supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment).navController.navigate(R.id.vehicleDetailsFragment, Bundle().apply { putLong("vehicleId", id) })
            }
            onView(withText(R.string.document_history)).perform(click())
            onView(withId(R.id.addDocument)).perform(click())
            onView(withId(R.id.documentTitle)).perform(replaceText("Registration copy"))
            onView(withId(R.id.documentExpirationDate)).perform(scrollTo(), replaceText("2020-01-01"))
            onView(withId(R.id.documentLocation)).perform(scrollTo(), replaceText("Home safe"))
            onView(withId(R.id.documentDescription)).perform(scrollTo(), replaceText("Proof for resale"), closeSoftKeyboard())
            scenario.recreate()
            onView(withId(R.id.documentDescription)).check(matches(withText("Proof for resale")))
            onView(withId(R.id.documentSave)).perform(scrollTo(), click())
            onView(withText(containsString("Physical copy: Home safe"))).check(matches(isDisplayed()))
            onView(withText(containsString("Proof for resale"))).check(matches(isDisplayed()))
            onView(withText("Expired")).check(matches(isDisplayed()))
            onView(withText(R.string.document_edit)).perform(scrollTo(), click())
            onView(withId(R.id.documentTitle)).perform(replaceText("Updated registration"), closeSoftKeyboard())
            onView(withId(R.id.documentSave)).perform(scrollTo(), click())
            onView(withText(containsString("Updated registration"))).check(matches(isDisplayed()))
            onView(withContentDescription("Delete document Updated registration")).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            onView(withText(R.string.document_empty)).check(matches(isDisplayed()))
        }
    }
}
