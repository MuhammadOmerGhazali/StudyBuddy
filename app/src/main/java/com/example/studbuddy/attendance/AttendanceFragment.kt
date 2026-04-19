package com.example.studbuddy.attendance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.AttendanceRecord
import com.example.studbuddy.core.models.Course
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class AttendanceFragment : Fragment() {

    private lateinit var recyclerViewAttendance: RecyclerView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private val courseList = mutableListOf<Course>()

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val courseId = getCourseId()
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        
        setupViews(view, courseId)
        setupRecyclerView(view)
        observeViewModel(courseId)
    }

    private fun getCourseId(): String? {
        return arguments?.getString("courseId") ?: parentFragment?.parentFragment?.arguments?.getString("courseId")
    }

    private fun setupViews(view: View, courseId: String?) {
        view.findViewById<Button>(R.id.btnMarkAttendance).setOnClickListener {
            val state = viewModel.uiState.value
            if (state.courses.isEmpty()) {
                Toast.makeText(requireContext(), "Please add courses first", Toast.LENGTH_SHORT).show()
            } else {
                val course = if (courseId != null) state.courses.find { it.id == courseId } else null
                showMarkAttendanceDialog(null, course)
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerViewAttendance = view.findViewById(R.id.recyclerViewAttendance)
        attendanceAdapter = AttendanceAdapter(courseList) { course ->
            showAttendanceHistoryDialog(course)
        }
        recyclerViewAttendance.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewAttendance.adapter = attendanceAdapter
    }

    private fun observeViewModel(courseId: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    if (!state.isLoading) {
                        val filteredCourses = if (courseId != null) {
                            state.courses.filter { it.id == courseId }
                        } else {
                            state.courses
                        }
                        attendanceAdapter.updateData(filteredCourses, state.attendance)
                        updateEmptyState(filteredCourses.isEmpty())
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
                
                layoutEmpty.findViewById<ImageView>(R.id.imgEmptyState).setImageResource(R.drawable.ic_check_circle_24)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_courses_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_courses_desc)
            }
            recyclerViewAttendance.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerViewAttendance.visibility = View.VISIBLE
        }
    }

    private fun showMarkAttendanceDialog(existingRecord: AttendanceRecord?, preselectedCourse: Course? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mark_attendance, null)
        val spinnerCourses = dialogView.findViewById<Spinner>(R.id.spinnerCourses)
        val rgStatus = dialogView.findViewById<RadioGroup>(R.id.rgStatus)

        val courses = viewModel.uiState.value.courses
        spinnerCourses.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, courses.map { it.name })

        if (existingRecord != null) {
            val record = existingRecord
            val courseIdx = courses.indexOfFirst { it.id == record.courseId }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
                spinnerCourses.isEnabled = false
            }
            when (record.status) {
                "PRESENT" -> rgStatus.check(R.id.rbPresent)
                "ABSENT" -> rgStatus.check(R.id.rbAbsent)
                "LATE" -> rgStatus.check(R.id.rbLate)
            }
        } else if (preselectedCourse != null) {
            val courseIdx = courses.indexOfFirst { it.id == preselectedCourse.id }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
                // We might want to keep it enabled if user wants to change, or disabled for strict context
                // Let's keep it enabled but pre-selected.
            }
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingRecord == null) "Mark Attendance" else "Update Attendance")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button) { _, _ ->
                val selectedIdx = spinnerCourses.selectedItemPosition
                if (selectedIdx == -1) return@setPositiveButton
                
                val courseId = courses[selectedIdx].id
                val status = when (rgStatus.checkedRadioButtonId) {
                    R.id.rbPresent -> "PRESENT"
                    R.id.rbAbsent -> "ABSENT"
                    R.id.rbLate -> "LATE"
                    else -> "PRESENT"
                }

                val record = AttendanceRecord(
                    id = existingRecord?.id ?: UUID.randomUUID().toString(),
                    courseId = courseId,
                    dateTime = existingRecord?.dateTime ?: System.currentTimeMillis(),
                    status = status
                )
                
                if (existingRecord == null) {
                    viewModel.addAttendanceRecord(record)
                } else {
                    viewModel.updateAttendanceRecord(record)
                }
            }
            .setNegativeButton(R.string.cancel_button, null)

        if (existingRecord != null) {
            dialogBuilder.setNeutralButton(R.string.delete_button) { _, _ ->
                viewModel.deleteAttendanceRecord(existingRecord.id)
                Toast.makeText(requireContext(), "Record deleted", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.show()
    }

    private fun showAttendanceHistoryDialog(course: Course) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_attendance_history, null)
        val rvRecords = dialogView.findViewById<RecyclerView>(R.id.rvAttendanceRecords)
        val tvCourseName = dialogView.findViewById<TextView>(R.id.tvHistoryCourseName)

        tvCourseName.text = String.format(Locale.getDefault(), "History for %s", course.name)
        
        val records = viewModel.uiState.value.attendance
            .filter { it.courseId == course.id }
            .sortedByDescending { it.dateTime }
        
        val historyDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton(R.string.cancel_button, null)
            .create()

        val adapter = RecordAdapter(records) { record ->
            historyDialog.dismiss()
            showMarkAttendanceDialog(record)
        }
        
        rvRecords.layoutManager = LinearLayoutManager(requireContext())
        rvRecords.adapter = adapter
        
        historyDialog.show()
    }
}
