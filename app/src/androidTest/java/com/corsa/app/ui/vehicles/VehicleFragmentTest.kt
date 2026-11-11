package com.corsa.app.ui.vehicles

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.corsa.app.MainActivity
import com.corsa.app.R
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleListFragmentTest {
    @Test
    fun addAction_opensBlankEditor() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addVehicle)).perform(click())
            onView(withText(R.string.create_vehicle)).check(matches(isDisplayed()))
            onView(withId(R.id.brandInput)).check(matches(withText("")))
        }
    }
}

@RunWith(AndroidJUnit4::class)
class VehicleEditorFragmentTest {
    @Test
    fun draftSurvivesRecreation_andValidationStaysInline() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.addVehicle)).perform(click())
            onView(withId(R.id.brandInput)).perform(replaceText("Toyota"))
            onView(withId(R.id.modelInput)).perform(replaceText("Vios"))
            onView(withId(R.id.yearInput)).perform(replaceText("1800"))
            onView(withId(R.id.mileageInput)).perform(replaceText("-1"))

            scenario.recreate()

            onView(withId(R.id.brandInput)).check(matches(withText("Toyota")))
            onView(withId(R.id.modelInput)).check(matches(withText("Vios")))
            onView(withId(R.id.saveVehicle)).perform(click())
            onView(withText(containsString("Year must be between"))).check(matches(isDisplayed()))
            onView(withText("Mileage cannot be negative")).check(matches(isDisplayed()))
            onView(withId(R.id.brandInput)).check(matches(withText("Toyota")))
        }
    }
}

@RunWith(AndroidJUnit4::class)
class VehicleDetailsFragmentTest {
    @Test
    fun editVehicle_updatesObservedDetails() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addVehicle)).perform(click())
            onView(withId(R.id.brandInput)).perform(replaceText("Edit Test"))
            onView(withId(R.id.modelInput)).perform(replaceText("Observed"))
            onView(withId(R.id.yearInput)).perform(replaceText("2022"))
            onView(withId(R.id.mileageInput)).perform(replaceText("100"))
            onView(withId(R.id.saveVehicle)).perform(click())

            onView(withId(R.id.editVehicle)).perform(click())
            onView(withId(R.id.mileageInput)).perform(replaceText("250"))
            onView(withId(R.id.saveVehicle)).perform(click())

            onView(withText("Current odometer: 250 km")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun createThenCancelAndConfirmDelete_returnsToList() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.addVehicle)).perform(click())
            onView(withId(R.id.brandInput)).perform(replaceText("Delete Test"))
            onView(withId(R.id.modelInput)).perform(replaceText("Only This"))
            onView(withId(R.id.yearInput)).perform(replaceText("2024"))
            onView(withId(R.id.mileageInput)).perform(replaceText("42"))
            onView(withId(R.id.saveVehicle)).perform(click())
            onView(withText("2024 Delete Test Only This")).check(matches(isDisplayed()))

            onView(withId(R.id.deleteVehicle)).perform(click())
            onView(withText(R.string.cancel)).perform(click())
            onView(withText("2024 Delete Test Only This")).check(matches(isDisplayed()))

            onView(withId(R.id.deleteVehicle)).perform(click())
            onView(withText(R.string.delete)).perform(click())
            onView(withId(R.id.addVehicle)).check(matches(isDisplayed()))
        }
    }
}
