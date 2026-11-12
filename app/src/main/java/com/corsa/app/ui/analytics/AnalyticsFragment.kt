package com.corsa.app.ui.analytics

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.corsa.app.R

class AnalyticsFragment : Fragment(R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.placeholderTitle).setText(R.string.dashboard_tab_analytics)
        view.findViewById<TextView>(R.id.placeholderMessage).setText(R.string.analytics_placeholder_message)
    }
}
