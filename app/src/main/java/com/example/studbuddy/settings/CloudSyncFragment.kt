package com.example.studbuddy.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.studbuddy.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@AndroidEntryPoint
class CloudSyncFragment : Fragment() {

    private val viewModel: CloudSyncViewModel by viewModels()

    private lateinit var txtSyncStatus: TextView
    private lateinit var txtLastSync: TextView
    private lateinit var btnSyncNow: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_cloud_sync, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtSyncStatus = view.findViewById(R.id.txtSyncStatus)
        txtLastSync   = view.findViewById(R.id.txtLastSyncTime)
        btnSyncNow    = view.findViewById(R.id.btnSyncNow)

        // Set initial state immediately (before first flow emission)
        if (!viewModel.isSignedIn) {
            txtSyncStatus.text = "Not signed in — go to Profile to sign in"
            btnSyncNow.isEnabled = false
        }

        // Observe last sync time
        lifecycleScope.launch {
            viewModel.lastSyncTime.collect { timestamp ->
                txtLastSync.text = if (timestamp == 0L) {
                    "Last synced: Never"
                } else {
                    val formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(timestamp))
                    "Last synced: $formatted"
                }
            }
        }

        // Observe sync status and update UI
        lifecycleScope.launch {
            viewModel.syncStatus.collect { status ->
                when (status) {
                    SyncStatus.IDLE -> {
                        txtSyncStatus.text = if (viewModel.isSignedIn) "Ready to sync" else "Not signed in — go to Profile to sign in"
                        txtSyncStatus.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
                        btnSyncNow.isEnabled = viewModel.isSignedIn
                    }
                    SyncStatus.SYNCING -> {
                        txtSyncStatus.text = "Syncing\u2026"
                        txtSyncStatus.setTextColor(getAttrColor(com.google.android.material.R.attr.itemTextColor))
                        btnSyncNow.isEnabled = false
                    }
                    SyncStatus.SUCCESS -> {
                        txtSyncStatus.text = "Sync successful \u2714"
                        txtSyncStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.sync_success))
                        btnSyncNow.isEnabled = true
                    }
                    SyncStatus.ERROR -> {
                        txtSyncStatus.text = "Sync failed — check Logcat (Firestore rules may be blocking writes)"
                        txtSyncStatus.setTextColor(getAttrColor(com.google.android.material.R.attr.errorTextColor))
                        btnSyncNow.isEnabled = true
                    }
                    SyncStatus.NOT_SIGNED_IN -> {
                        txtSyncStatus.text = "Not signed in — go to Profile to sign in"
                        txtSyncStatus.setTextColor(getAttrColor(com.google.android.material.R.attr.errorTextColor))
                        btnSyncNow.isEnabled = false
                    }
                }
            }
        }

        btnSyncNow.setOnClickListener {
            viewModel.triggerSync()
        }
    }

    private fun getAttrColor(attr: Int): Int {
        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }
}
