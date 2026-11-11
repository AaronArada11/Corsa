package com.corsa.app.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.corsa.app.R

class SettingsFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.placeholderTitle).setText(R.string.dashboard_settings)
        view.findViewById<TextView>(R.id.placeholderMessage).setText(R.string.settings_placeholder_message)
    }
}
