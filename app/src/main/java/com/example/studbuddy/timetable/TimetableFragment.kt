package com.example.studbuddy.timetable

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.TimetableEntry
import com.example.studbuddy.core.notifications.NotificationScheduler
import com.example.studbuddy.core.notifications.NotificationType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class TimetableFragment : Fragment() {

    private lateinit var recyclerViewTimetable: RecyclerView
    private lateinit var dayTimetableAdapter: DayTimetableAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View

    private val viewModel: TimetableViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        
        setupViews(view)
        observeViewModel()
    }

    private fun setupViews(view: View) {
        view.findViewById<Button>(R.id.btnAddTimetableEntry).setOnClickListener {
            if (viewModel.uiState.value.courses.isEmpty()) {
                Toast.makeText(requireContext(), "Please add courses first", Toast.LENGTH_SHORT).show()
            } else {
                showTimetableDialog(null)
            }
        }
        
        recyclerViewTimetable = view.findViewById(R.id.recyclerViewTimetable)
        recyclerViewTimetable.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    if (!state.isLoading) {
                        updateAdapter(state.timetable, state.courses)
                        updateEmptyState(state.timetable.isEmpty())
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            if (layoutEmpty.visibility != View.VISIBLE) {
                layoutEmpty.visibility = View.VISIBLE
                layoutEmpty.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                
                layoutEmpty.findViewById<ImageView>(R.id.imgEmptyState).setImageResource(R.drawable.ic_event_24)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_timetable_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_timetable_desc)
            }
            recyclerViewTimetable.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerViewTimetable.visibility = View.VISIBLE
        }
    }

    private fun updateAdapter(entries: List<TimetableEntry>, courses: List<com.example.studbuddy.core.models.Course>) {
        val dayMap = entries.groupBy { it.dayOfWeek }
        dayTimetableAdapter = DayTimetableAdapter(dayMap, courses) { entry ->
            showTimetableDialog(entry)
        }
        recyclerViewTimetable.adapter = dayTimetableAdapter
    }

    private fun showTimetableDialog(existing: TimetableEntry?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_timetable, null)
        val spinnerCourses = dialogView.findViewById<Spinner>(R.id.spinnerCourses)
        val spinnerDay = dialogView.findViewById<Spinner>(R.id.spinnerDay)
        val btnStart = dialogView.findViewById<Button>(R.id.btnStartTime)
        val btnEnd = dialogView.findViewById<Button>(R.id.btnEndTime)
        val etRoom = dialogView.findViewById<EditText>(R.id.etRoom)

        val courses = viewModel.uiState.value.courses
        spinnerCourses.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, courses.map { it.name })

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        spinnerDay.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, days)

        var startTime = existing?.startTime ?: "09:00"
        var endTime = existing?.endTime ?: "10:00"

        btnStart.text = startTime
        btnEnd.text = endTime

        if (existing != null) {
            val courseIdx = courses.indexOfFirst { it.id == existing.courseId }
            if (courseIdx != -1) spinnerCourses.setSelection(courseIdx)
            spinnerDay.setSelection(existing.dayOfWeek - 1)
            etRoom.setText(existing.room)
        }

        btnStart.setOnClickListener {
            val parts = startTime.split(":")
            TimePickerDialog(requireContext(), { _, h, m ->
                startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                btnStart.text = startTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        btnEnd.setOnClickListener {
            val parts = endTime.split(":")
            TimePickerDialog(requireContext(), { _, h, m ->
                endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                btnEnd.text = endTime
            }, parts[0].toInt(), parts[1].toInt(), true).show()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add Class" else "Edit Class")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button, null)
            .setNegativeButton(R.string.cancel_button, null)

        if (existing != null) {
            dialog.setNeutralButton(R.string.delete_button) { _, _ ->
                cancelClassReminder(existing.id)
                viewModel.deleteTimetableEntry(existing.id)
                Toast.makeText(requireContext(), "Class deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val alertDialog = dialog.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedCourseIndex = spinnerCourses.selectedItemPosition
            if (selectedCourseIndex == -1) return@setOnClickListener
            
            val courseId = courses[selectedCourseIndex].id
            val day = spinnerDay.selectedItemPosition + 1
            val room = etRoom.text.toString().trim()

            if (room.isNotEmpty()) {
                val entry = TimetableEntry(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    courseId = courseId,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = endTime,
                    room = room,
                    color = existing?.color ?: "#1565C0"
                )
                
                if (existing == null) {
                    viewModel.addTimetableEntry(entry)
                } else {
                    viewModel.updateTimetableEntry(entry)
                }
                scheduleClassReminder(entry)
                alertDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a room/place", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleClassReminder(entry: TimetableEntry) {
        val course = viewModel.uiState.value.courses.find { it.id == entry.courseId }
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationScheduler.scheduleTimetableReminder(requireContext(), entry, course?.name ?: "Unknown")
        }
    }

    private fun cancelClassReminder(entryId: String) {
        NotificationScheduler.cancelReminder(requireContext(), NotificationType.TIMETABLE_CLASS, entryId)
    }
}
