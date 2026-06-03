package com.example.studbuddy.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.studbuddy.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppearanceSettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var rgThemeMode: RadioGroup
    private lateinit var rbSystem: RadioButton
    private lateinit var rbLight: RadioButton
    private lateinit var rbDark: RadioButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_appearance_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgThemeMode = view.findViewById(R.id.rgThemeMode)
        rbSystem = view.findViewById(R.id.rbSystem)
        rbLight = view.findViewById(R.id.rbLight)
        rbDark = view.findViewById(R.id.rbDark)

        // Initialize from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.themeMode.collect { mode ->
                when (mode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> rbLight.isChecked = true
                    AppCompatDelegate.MODE_NIGHT_YES -> rbDark.isChecked = true
                    else -> rbSystem.isChecked = true
                }
            }
        }

        rgThemeMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rbDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            viewModel.setThemeMode(mode)
        }
    }
}
