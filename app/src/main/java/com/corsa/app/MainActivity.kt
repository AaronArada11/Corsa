package com.corsa.app

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.enableEdgeToEdge(window)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.main)

        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                safeInsets.left,
                safeInsets.top,
                safeInsets.right,
                safeInsets.bottom
            )

            // The root handles these insets for the whole screen.
            // Consuming them prevents the navigation bar adding them again.
            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.requestApplyInsets(root)

        ViewCompat.setAccessibilityHeading(
            findViewById(R.id.emptyTitle),
            true
        )

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.nav_dashboard

        // This draft contains only the Dashboard.
        // Keep its selected state until real destinations are connected.
        bottomNavigation.setOnItemSelectedListener { item ->
            item.itemId == R.id.nav_dashboard
        }
    }
}