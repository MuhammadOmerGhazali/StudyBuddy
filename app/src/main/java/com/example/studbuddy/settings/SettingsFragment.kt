package com.example.studbuddy.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.studbuddy.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private lateinit var cardProfile: MaterialCardView
    private lateinit var cardCloudSync: MaterialCardView
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var cardAppearance: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardProfile       = view.findViewById(R.id.cardProfile)
        cardCloudSync     = view.findViewById(R.id.cardCloudSync)
        cardNotifications = view.findViewById(R.id.cardNotifications)
        cardAppearance    = view.findViewById(R.id.cardAppearance)

        // Navigate to Profile page
        cardProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        // Navigate to Cloud Sync page
        cardCloudSync.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_cloudSyncFragment)
        }

        // Navigate to Notifications Sub-page
        cardNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_notificationSettingsFragment)
        }

        // Navigate to Appearance Sub-page
        cardAppearance.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_appearanceSettingsFragment)
        }
    }
}
