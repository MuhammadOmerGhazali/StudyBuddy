package com.example.studbuddy.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Semester
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var btnSetupSemester: Button
    private lateinit var cardSetupSemester: MaterialCardView
    private lateinit var cardHero: MaterialCardView
    private lateinit var tvHeroEyebrow: TextView
    private lateinit var tvHeroTitle: TextView
    private lateinit var tvHeroSubtitle: TextView
    private lateinit var tvHeroBadge: TextView
    private lateinit var dashboardItemsContainer: LinearLayout

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSetupSemester = view.findViewById(R.id.btnSetupSemester)
        cardSetupSemester = view.findViewById(R.id.cardSetupSemester)
        cardHero = view.findViewById(R.id.cardHero)
        tvHeroEyebrow = view.findViewById(R.id.tvHeroEyebrow)
        tvHeroTitle = view.findViewById(R.id.tvHeroTitle)
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle)
        tvHeroBadge = view.findViewById(R.id.tvHeroBadge)
        dashboardItemsContainer = view.findViewById(R.id.dashboardItemsContainer)

        btnSetupSemester.setOnClickListener { showSemesterDialog() }
        
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: DashboardUiState) {
        val semester = state.semester
        if (semester != null) {
            cardSetupSemester.visibility = View.GONE
            cardHero.visibility = View.VISIBLE
            updateDashboard(state)
        } else {
            cardSetupSemester.visibility = View.VISIBLE
            cardHero.visibility = View.GONE
            dashboardItemsContainer.removeAllViews()
        }
    }

    private fun updateDashboard(state: DashboardUiState) {
        dashboardItemsContainer.removeAllViews()
        val semester = state.semester ?: return
        val courses = state.courses
        val timetable = state.timetable
        val attendance = state.attendance
        val assignments = state.assignments
        val exams = state.exams
        val pendingAssignments = assignments.filter { !it.isCompleted }.sortedBy { it.dueDate }
        val upcomingExams = exams.filter { !it.isCompleted && it.date > System.currentTimeMillis() }
        val attendanceAlerts = getAttendanceAlerts(courses, attendance)

        tvHeroEyebrow.text = if (semester.isActive) "Active semester" else "Semester overview"
        tvHeroTitle.text = semester.name
        tvHeroSubtitle.text = buildString {
            append(formatDateRange(semester.startDate, semester.endDate))
            append("\n")
            append(courses.size)
            append(" courses")
            append(" • ")
            append(pendingAssignments.size)
            append(" pending tasks")
        }
        tvHeroBadge.text = if (semester.isActive) "ACTIVE" else "INACTIVE"

        // 1. Next Lecture Card
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val dayIndex = if (currentDay == 1) 7 else currentDay - 1 // Adjust to 1=Mon...7=Sun
        val currentTime = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

        val nextLecture = timetable
            .filter { it.dayOfWeek == dayIndex && it.startTime > currentTime }
            .minByOrNull { it.startTime }
            ?: timetable
                .filter { it.dayOfWeek > dayIndex || (it.dayOfWeek < dayIndex) }
                .sortedWith(compareBy({ (it.dayOfWeek - dayIndex + 7) % 7 }, { it.startTime }))
                .firstOrNull()

        if (nextLecture != null) {
            val course = courses.find { it.id == nextLecture.courseId }
            val dayName = when(nextLecture.dayOfWeek) {
                1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
                5 -> "Friday"; 6 -> "Saturday"; 7 -> "Sunday"; else -> ""
            }
            addDashboardCard(
                "Next Lecture",
                "${course?.name ?: "Unknown"}: ${nextLecture.startTime}",
                "Day: $dayName | Room: ${nextLecture.room}"
            )
        } else {
            addDashboardCard("Next Lecture", "No lectures scheduled", "Check timetable for full schedule")
        }

        // 2. Short Attendance Card
        val threshold = 75.0
        if (attendanceAlerts.isNotEmpty()) {
            addDashboardCard(
                "Attendance Alert",
                "Short attendance in: ${attendanceAlerts.joinToString(", ")}",
                "Required Threshold: ${String.format(Locale.getDefault(), "%.0f%%", threshold)}"
            )
        }

        // 3. Pending Assignment Card
        if (pendingAssignments.isNotEmpty()) {
            val topAssignment = pendingAssignments.first()
            val course = courses.find { it.id == topAssignment.courseId }
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            addDashboardCard(
                "Pending Assignment",
                "${topAssignment.name} (${course?.name ?: "Unknown"})",
                "Due Date: ${sdf.format(Date(topAssignment.dueDate))}"
            )
        }

        // 4. Next Exam Card
        val nextExam = exams
            .filter { !it.isCompleted && it.date > System.currentTimeMillis() }
            .minByOrNull { it.date }
        
        if (nextExam != null) {
            val course = courses.find { it.id == nextExam.courseId }
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            addDashboardCard(
                "Next Exam",
                "${course?.name ?: "Unknown"}: ${nextExam.type}",
                "Date: ${sdf.format(Date(nextExam.date))} | Venue: ${nextExam.venue ?: "TBD"}"
            )
        }

        // 5. Semester Status / GPA Card
        val totalPoints = courses.sumOf { it.gradePoints }
        val totalCredits = courses.filter { it.grade != null }.sumOf { it.creditHours }.toDouble()
        val calculatedGpa = if (totalCredits > 0) totalPoints / totalCredits else 0.0

        addDashboardCard(
            "Semester GPA", 
            "Current GPA: ${String.format(Locale.getDefault(), "%.2f", calculatedGpa)}", 
            if (semester.isActive) "Active Semester" else "Inactive Semester"
        )
    }

    private fun addDashboardCard(title: String, main: String, sub: String) {
        val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_dashboard_card, dashboardItemsContainer, false)
        cardView.findViewById<TextView>(R.id.tvCardTitle).text = title
        cardView.findViewById<TextView>(R.id.tvCardMainText).text = main
        cardView.findViewById<TextView>(R.id.tvCardSubText).text = sub
        dashboardItemsContainer.addView(cardView)
    }

    private fun getAttendanceAlerts(
        courses: List<com.example.studbuddy.core.models.Course>,
        attendance: List<com.example.studbuddy.core.models.AttendanceRecord>
    ): List<String> {
        val threshold = 75.0
        val alerts = mutableListOf<String>()
        courses.forEach { course ->
            val records = attendance.filter { it.courseId == course.id }
            val percentage = if (records.isNotEmpty()) {
                (records.count { it.status == "PRESENT" }.toDouble() / records.size) * 100
            } else {
                100.0
            }
            if (percentage < threshold) {
                alerts.add("${course.name} (${String.format(Locale.getDefault(), "%.1f%%", percentage)})")
            }
        }
        return alerts
    }

    private fun formatDateRange(startDate: Long, endDate: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
    }

    private fun showSemesterDialog(existingSemester: Semester? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_semester, null)
        val etName = dialogView.findViewById<EditText>(R.id.etSemesterName)
        val btnStart = dialogView.findViewById<Button>(R.id.btnStartDate)
        val btnEnd = dialogView.findViewById<Button>(R.id.btnEndDate)

        var startMs = existingSemester?.startDate ?: 0L
        var endMs = existingSemester?.endDate ?: 0L

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        existingSemester?.let {
            etName.setText(it.name)
            if (startMs > 0) btnStart.text = sdf.format(Date(startMs))
            if (endMs > 0) btnEnd.text = sdf.format(Date(endMs))
        }

        btnStart.setOnClickListener {
            showDatePicker { ms -> 
                startMs = ms
                btnStart.text = sdf.format(Date(ms))
            }
        }

        btnEnd.setOnClickListener {
            showDatePicker { ms -> 
                endMs = ms
                btnEnd.text = sdf.format(Date(ms))
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingSemester == null) "Setup Semester" else "Edit Semester")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter semester name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (startMs > 0 && endMs > startMs) {
                    val semester = Semester(
                        id = existingSemester?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        startDate = startMs,
                        endDate = endMs,
                        isActive = existingSemester?.isActive ?: true,
                        gpa = existingSemester?.gpa
                    )
                    viewModel.saveSemester(semester)
                } else {
                    Toast.makeText(requireContext(), "Please select valid dates", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val selected = Calendar.getInstance()
            selected.set(y, m, d)
            onDateSelected(selected.timeInMillis)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}
