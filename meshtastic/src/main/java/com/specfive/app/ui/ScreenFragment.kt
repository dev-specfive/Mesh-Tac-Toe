package com.specfive.app.ui

import androidx.fragment.app.Fragment
import com.specfive.app.android.GeeksvilleApplication

/**
 * A fragment that represents a current 'screen' in our app.
 *
 * Useful for tracking analytics
 */
open class ScreenFragment(private val screenName: String) : Fragment() {

    override fun onResume() {
        super.onResume()
        GeeksvilleApplication.analytics.sendScreenView(screenName)
    }

    override fun onPause() {
        GeeksvilleApplication.analytics.endScreenView()
        super.onPause()
    }
}
