package com.example.studbuddy.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.studbuddy.R
import com.example.studbuddy.StudBuddyApp
import com.example.studbuddy.core.SettingsManager
import com.example.studbuddy.core.notifications.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class NotificationSettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var rgClassReminderMode: RadioGroup
    private lateinit var rbEachLecture: RadioButton
    private lateinit var rbDailySummary: RadioButton
    private lateinit var layoutEachLecture: View
    private lateinit var layoutDailySummary: View
    private lateinit var btnLectureLeadTime: Button
    private lateinit var btnDailySummaryTime: Button
    
    private lateinit var cbAssignmentReminders: CheckBox
    private lateinit var layoutAssignmentLeadTime: View
    private lateinit var btnAssignmentLeadTime: Button
    
    private lateinit var cbExamReminders: CheckBox
    private lateinit var layoutExamLeadTime: View
    private lateinit var btnExamLeadTime: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notification_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgClassReminderMode = view.findViewById(R.id.rgClassReminderMode)
        rbEachLecture = view.findViewById(R.id.rbEachLecture)
        rbDailySummary = view.findViewById(R.id.rbDailySummary)
        layoutEachLecture = view.findViewById(R.id.layoutEachLectureSettings)
        layoutDailySummary = view.findViewById(R.id.layoutDailySummarySettings)
        btnLectureLeadTime = view.findViewById(R.id.btnLectureLeadTime)
        btnDailySummaryTime = view.findViewById(R.id.btnDailySummaryTime)
        
        cbAssignmentReminders = view.findViewById(R.id.cbAssignmentReminders)
        layoutAssignmentLeadTime = view.findViewById(R.id.layoutAssignmentLeadTime)
        btnAssignmentLeadTime = view.findViewById(R.id.btnAssignmentLeadTime)
        
        cbExamReminders = view.findViewById(R.id.cbExamReminders)
        layoutExamLeadTime = view.findViewById(R.id.layoutExamLeadTime)
        btnExamLeadTime = view.findViewById(R.id.btnExamLeadTime)

        // Initialize from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.classReminderMode.collect { classMode ->
                if (classMode == SettingsManager.MODE_EACH_LECTURE) {
                    rbEachLecture.isChecked = true
                    layoutEachLecture.visibility = View.VISIBLE
                    layoutDailySummary.visibility = View.GONE
                } else {
                    rbDailySummary.isChecked = true
                    layoutEachLecture.visibility = View.GONE
                    layoutDailySummary.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lectureLeadTime.collect { lectureLead ->
                btnLectureLeadTime.text = "$lectureLead minutes"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailySummaryTime.collect { dailyTime ->
                btnDailySummaryTime.text = "At $dailyTime"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assignmentRemindersEnabled.collect { assignEnabled ->
                cbAssignmentReminders.isChecked = assignEnabled
                layoutAssignmentLeadTime.visibility = if (assignEnabled) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.assignmentLeadTime.collect { assignLead ->
                btnAssignmentLeadTime.text = formatLeadTime(assignLead)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.examRemindersEnabled.collect { examEnabled ->
                cbExamReminders.isChecked = examEnabled
                layoutExamLeadTime.visibility = if (examEnabled) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.examLeadTime.collect { examLead ->
                btnExamLeadTime.text = formatLeadTime(examLead)
            }
        }

        rgClassReminderMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbEachLecture -> SettingsManager.MODE_EACH_LECTURE
                else -> SettingsManager.MODE_DAILY_SUMMARY
            }
            
            viewModel.setClassReminderMode(mode)
            if (mode == SettingsManager.MODE_EACH_LECTURE) {
                layoutEachLecture.visibility = View.VISIBLE
                layoutDailySummary.visibility = View.GONE
            } else {
                layoutEachLecture.visibility = View.GONE
                layoutDailySummary.visibility = View.VISIBLE
            }
            rescheduleClassReminders()
        }

        btnLectureLeadTime.setOnClickListener {
            showLeadTimeDialog()
        }

        btnDailySummaryTime.setOnClickListener {
            showSummaryTimeDialog()
        }

        cbAssignmentReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAssignmentRemindersEnabled(isChecked)
            layoutAssignmentLeadTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            rescheduleAssignmentReminders()
        }

        btnAssignmentLeadTime.setOnClickListener {
            showAssignmentLeadTimeDialog()
        }

        cbExamReminders.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setExamRemindersEnabled(isChecked)
            layoutExamLeadTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            rescheduleExamReminders()
        }

        btnExamLeadTime.setOnClickListener {
            showExamLeadTimeDialog()
        }
    }

    private fun formatLeadTime(hours: Int): String {
        return when {
            hours >= 168 -> "${hours / 168} week"
            hours >= 24 -> "${hours / 24} day"
            else -> "$hours hours"
        }
    }

    private fun showLeadTimeDialog() {
        val options = arrayOf("5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour")
        val values = intArrayOf(5, 10, 15, 30, 60)
        
        val currentLead = viewModel.lectureLeadTime.value
        var currentSelection = values.indexOf(currentLead)
        if (currentSelection == -1) currentSelection = 2 // Default to 15

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Lead Time")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                viewModel.setLectureLeadTime(values[which])
                rescheduleClassReminders()
                dialog.dismiss()
            }
            .show()
    }

    private fun showAssignmentLeadTimeDialog() {
        val options = arrayOf("12 hours", "1 day", "2 days", "3 days", "1 week")
        val values = intArrayOf(12, 24, 48, 72, 168)
        
        val currentLead = viewModel.assignmentLeadTime.value
        var currentSelection = values.indexOf(currentLead)
        if (currentSelection == -1) currentSelection = 1 // Default to 24

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Lead Time")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                viewModel.setAssignmentLeadTime(values[which])
                rescheduleAssignmentReminders()
                dialog.dismiss()
            }
            .show()
    }

    private fun showExamLeadTimeDialog() {
        val options = arrayOf("1 hour", "6 hours", "12 hours", "1 day", "2 days")
        val values = intArrayOf(1, 6, 12, 24, 48)
        
        val currentLead = viewModel.examLeadTime.value
        var currentSelection = values.indexOf(currentLead)
        if (currentSelection == -1) currentSelection = 3 // Default to 24

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Lead Time")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                viewModel.setExamLeadTime(values[which])
                rescheduleExamReminders()
                dialog.dismiss()
            }
            .show()
    }

    private fun showSummaryTimeDialog() {
        val dailyTime = viewModel.dailySummaryTime.value
        val currentTime = dailyTime.split(":")
        val h = currentTime[0].toInt()
        val m = currentTime[1].toInt()

        TimePickerDialog(requireContext(), { _, hour, minute ->
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            viewModel.setDailySummaryTime(timeStr)
            (requireActivity().application as StudBuddyApp).scheduleDailyMaintenance()
        }, h, m, true).show()
    }

    private fun rescheduleClassReminders() {
        lifecycleScope.launch {
            NotificationScheduler.rescheduleAllClassReminders(requireContext())
        }
    }

    private fun rescheduleAssignmentReminders() {
        lifecycleScope.launch {
            NotificationScheduler.rescheduleAllAssignmentReminders(requireContext())
        }
    }

    private fun rescheduleExamReminders() {
        lifecycleScope.launch {
            NotificationScheduler.rescheduleAllExamReminders(requireContext())
        }
    }
}
