package com.example.studbuddy.exams

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Exam
import com.example.studbuddy.core.models.ExamType
import com.example.studbuddy.core.notifications.NotificationScheduler
import com.example.studbuddy.core.notifications.NotificationType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ExamsFragment : Fragment() {

    private lateinit var rvPending: RecyclerView
    private lateinit var rvCompleted: RecyclerView
    private lateinit var pendingAdapter: ExamAdapter
    private lateinit var completedAdapter: ExamAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var scrollView: View
    private lateinit var tvPendingHeader: View
    private lateinit var tvCompletedHeader: View
    
    private val pendingList = mutableListOf<Exam>()
    private val completedList = mutableListOf<Exam>()

    private val viewModel: ExamsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_exams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val courseId = getCourseId()
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        scrollView = view.findViewById(R.id.scrollView)
        
        setupViews(view)
        setupRecyclerViews(view)
        setupMenu(courseId)
        observeViewModel(courseId)
    }

    private fun getCourseId(): String? {
        return arguments?.getString("courseId") ?: parentFragment?.parentFragment?.arguments?.getString("courseId")
    }

    private fun setupMenu(courseId: String?) {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, 1, Menu.NONE, "Add Exam").apply {
                    setIcon(R.drawable.ic_add_24)
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == 1) {
                    val state = viewModel.uiState.value
                    if (state.courses.isEmpty()) {
                        Toast.makeText(requireContext(), "Please add courses first", Toast.LENGTH_SHORT).show()
                    } else {
                        val course = if (courseId != null) state.courses.find { it.id == courseId } else null
                        showExamDialog(null, course)
                    }
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupViews(view: View) {
        // No button to setup anymore
    }

    private fun setupRecyclerViews(view: View) {
        rvPending = view.findViewById(R.id.rvPendingExams)
        rvCompleted = view.findViewById(R.id.rvCompletedExams)
        tvPendingHeader = view.findViewById(R.id.tvPendingHeader)
        tvCompletedHeader = view.findViewById(R.id.tvCompletedHeader)

        pendingAdapter = ExamAdapter(pendingList) { exam ->
            showExamDialog(exam)
        }

        completedAdapter = ExamAdapter(completedList) { exam ->
            showExamDialog(exam)
        }

        rvPending.layoutManager = LinearLayoutManager(requireContext())
        rvPending.adapter = pendingAdapter

        rvCompleted.layoutManager = LinearLayoutManager(requireContext())
        rvCompleted.adapter = completedAdapter
    }

    private fun observeViewModel(courseId: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    if (!state.isLoading) {
                        val filteredExams = if (courseId != null) {
                            state.exams.filter { it.courseId == courseId }
                        } else if (state.activeSemester != null) {
                            val activeCourseIds = state.courses
                                .filter { it.semesterId == state.activeSemester.id }
                                .map { it.id }
                                .toSet()
                            state.exams.filter { it.courseId in activeCourseIds }
                        } else {
                            state.exams
                        }
                        updateAdapters(filteredExams, state.courses)
                        updateEmptyState(filteredExams.isEmpty())
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
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_exams_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_exams_desc)
            }
            scrollView.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
        }
    }

    private fun updateAdapters(allExams: List<Exam>, allCourses: List<Course>) {
        val now = System.currentTimeMillis()
        val pending = mutableListOf<Exam>()
        val completed = mutableListOf<Exam>()

        allExams.forEach { exam ->
            if (!exam.isCompleted && exam.date < now) {
                // Auto-complete past exams
                viewModel.updateExam(exam.copy(isCompleted = true))
            } else if (exam.isCompleted) {
                completed.add(exam)
            } else {
                pending.add(exam)
            }
        }

        pending.sortBy { it.date }
        completed.sortByDescending { it.date }
        
        tvPendingHeader.visibility = if (pending.isEmpty()) View.GONE else View.VISIBLE
        tvCompletedHeader.visibility = if (completed.isEmpty()) View.GONE else View.VISIBLE

        pendingAdapter.updateData(pending, allCourses)
        completedAdapter.updateData(completed, allCourses)
    }

    private fun showExamDialog(existing: Exam?, preselectedCourse: Course? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_exam, null)
        val spinnerCourses = dialogView.findViewById<Spinner>(R.id.spinnerCourses)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerExamType)
        val btnDate = dialogView.findViewById<Button>(R.id.btnExamDate)
        val etVenue = dialogView.findViewById<EditText>(R.id.etVenue)
        val etTotalMarks = dialogView.findViewById<EditText>(R.id.etTotalMarks)
        val etWeightage = dialogView.findViewById<EditText>(R.id.etWeightage)
        val cbCompleted = dialogView.findViewById<CheckBox>(R.id.cbExamCompleted)
        val layoutObtained = dialogView.findViewById<View>(R.id.layoutObtainedMarks)
        val etObtained = dialogView.findViewById<EditText>(R.id.etObtainedMarks)

        val state = viewModel.uiState.value
        val courses = if (state.activeSemester != null) {
            state.courses.filter { it.semesterId == state.activeSemester.id }
        } else {
            state.courses
        }
        if (courses.isEmpty()) return
        
        spinnerCourses.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, courses.map { it.name })
        spinnerType.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, ExamType.entries.map { it.name })

        val calendar = Calendar.getInstance()
        if (existing != null) { 
            calendar.timeInMillis = existing.date
            etVenue.setText(existing.venue)
            etTotalMarks.setText(existing.totalMarks.toString())
            etWeightage.setText(existing.weightage.toString())
            cbCompleted.isChecked = existing.isCompleted
            etObtained.setText(existing.obtainedMarks?.toString() ?: "")
            if (existing.isCompleted) layoutObtained.visibility = View.VISIBLE
            
            val courseIdx = courses.indexOfFirst { c -> c.id == existing.courseId }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
                spinnerCourses.isEnabled = false
            }
            spinnerType.setSelection(existing.type.ordinal)
        } else if (preselectedCourse != null) {
            val courseIdx = courses.indexOfFirst { it.id == preselectedCourse.id }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
                // spinnerCourses.isEnabled = false
            }
        }

        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        btnDate.text = sdf.format(calendar.time)

        btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(requireContext(), { _, hh, mm ->
                    calendar.set(Calendar.HOUR_OF_DAY, hh)
                    calendar.set(Calendar.MINUTE, mm)
                    btnDate.text = sdf.format(calendar.time)
                    cbCompleted.isChecked = calendar.timeInMillis < System.currentTimeMillis()
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            layoutObtained.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add Exam" else "Edit Exam")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button, null)
            .setNegativeButton(R.string.cancel_button, null)

        if (existing != null) {
            dialogBuilder.setNeutralButton(R.string.delete_button) { _, _ -> confirmDelete(existing) }
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedIdx = spinnerCourses.selectedItemPosition
            if (selectedIdx == -1) return@setOnClickListener
            
            val courseId = courses[selectedIdx].id
            val type = ExamType.entries[spinnerType.selectedItemPosition]
            val total = etTotalMarks.text.toString().toDoubleOrNull() ?: 0.0
            val weight = etWeightage.text.toString().toDoubleOrNull() ?: 0.0
            val isComp = cbCompleted.isChecked
            val obtained = if (isComp) etObtained.text.toString().toDoubleOrNull() else null

            val exam = Exam(
                id = existing?.id ?: UUID.randomUUID().toString(),
                courseId = courseId,
                type = type,
                date = calendar.timeInMillis,
                venue = etVenue.text.toString(),
                totalMarks = total,
                obtainedMarks = obtained,
                weightage = weight,
                isCompleted = isComp
            )
            viewModel.updateExam(exam)
            
            if (!exam.isCompleted) {
                scheduleExamReminder(exam)
            } else {
                cancelExamReminder(exam)
            }
            
            alertDialog.dismiss()
        }
    }

    private fun confirmDelete(exam: Exam) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Exam")
            .setMessage("Are you sure you want to delete this exam?")
            .setPositiveButton(R.string.delete_button) { _, _ ->
                cancelExamReminder(exam)
                viewModel.deleteExam(exam)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun scheduleExamReminder(exam: Exam) {
        val course = viewModel.uiState.value.courses.find { it.id == exam.courseId }
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationScheduler.scheduleExamReminders(requireContext(), exam, course?.name ?: "Unknown")
        }
    }

    private fun cancelExamReminder(exam: Exam) {
        NotificationScheduler.cancelReminder(requireContext(), NotificationType.EXAM_REMINDER, "${exam.id}_24h")
        NotificationScheduler.cancelReminder(requireContext(), NotificationType.EXAM_REMINDER, "${exam.id}_1h")
    }
}
