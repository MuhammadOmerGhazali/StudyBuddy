package com.example.studbuddy.assignments

import android.app.DatePickerDialog
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
import com.example.studbuddy.core.models.Assignment
import com.example.studbuddy.core.notifications.NotificationScheduler
import com.example.studbuddy.core.notifications.NotificationType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AssignmentsFragment : Fragment() {

    private lateinit var rvPending: RecyclerView
    private lateinit var rvCompleted: RecyclerView
    private lateinit var pendingAdapter: AssignmentsAdapter
    private lateinit var completedAdapter: AssignmentsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var scrollView: View
    private lateinit var tvPendingHeader: View
    private lateinit var tvCompletedHeader: View
    
    private val pendingList = mutableListOf<Assignment>()
    private val completedList = mutableListOf<Assignment>()

    private val viewModel: AssignmentsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_assignments, container, false)
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
                menu.add(Menu.NONE, 1, Menu.NONE, "Add Assignment").apply {
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
                        showAssignmentDialog(null, course)
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
        rvPending = view.findViewById(R.id.rvPending)
        rvCompleted = view.findViewById(R.id.rvCompleted)
        tvPendingHeader = view.findViewById(R.id.tvPendingHeader)
        tvCompletedHeader = view.findViewById(R.id.tvCompletedHeader)

        pendingAdapter = AssignmentsAdapter(pendingList, { assignment, isChecked ->
            updateAssignmentStatus(assignment, isChecked)
        }, { assignment ->
            showAssignmentDialog(assignment)
        })

        completedAdapter = AssignmentsAdapter(completedList, { assignment, isChecked ->
            updateAssignmentStatus(assignment, isChecked)
        }, { assignment ->
            showAssignmentDialog(assignment)
        })

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
                        val filteredAssignments = if (courseId != null) {
                            state.assignments.filter { it.courseId == courseId }
                        } else if (state.activeSemester != null) {
                            val activeCourseIds = state.courses
                                .filter { it.semesterId == state.activeSemester.id }
                                .map { it.id }
                                .toSet()
                            state.assignments.filter { it.courseId in activeCourseIds }
                        } else {
                            state.assignments
                        }
                        updateAdapters(filteredAssignments, state.courses)
                        updateEmptyState(filteredAssignments.isEmpty())
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
                
                layoutEmpty.findViewById<ImageView>(R.id.imgEmptyState).setImageResource(R.drawable.ic_assignment_24)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_assignments_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_assignments_desc)
            }
            scrollView.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            scrollView.visibility = View.VISIBLE
        }
    }

    private fun updateAdapters(allAssignments: List<Assignment>, allCourses: List<com.example.studbuddy.core.models.Course>) {
        val pending = allAssignments.filter { !it.isCompleted }.sortedBy { it.dueDate }
        val completed = allAssignments.filter { it.isCompleted }.sortedByDescending { it.dueDate }
        
        tvPendingHeader.visibility = if (pending.isEmpty()) View.GONE else View.VISIBLE
        tvCompletedHeader.visibility = if (completed.isEmpty()) View.GONE else View.VISIBLE

        pendingAdapter.updateData(pending, allCourses)
        completedAdapter.updateData(completed, allCourses)
    }

    private fun updateAssignmentStatus(assignment: Assignment, isCompleted: Boolean) {
        val updated = assignment.copy(isCompleted = isCompleted)
        viewModel.updateAssignment(updated)
        
        if (isCompleted) {
            cancelReminder(updated)
        } else {
            scheduleReminder(updated)
        }
    }

    private fun showAssignmentDialog(existing: Assignment?, preselectedCourse: com.example.studbuddy.core.models.Course? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_assignment, null)
        val spinnerCourses = dialogView.findViewById<Spinner>(R.id.spinnerCourses)
        val etName = dialogView.findViewById<EditText>(R.id.etAssignmentTitle)
        val etTotal = dialogView.findViewById<EditText>(R.id.etTotalMarks)
        val etWeight = dialogView.findViewById<EditText>(R.id.etWeightage)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDueDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnDueTime)
        val layoutObtained = dialogView.findViewById<View>(R.id.layoutObtainedMarks)
        val etObtained = dialogView.findViewById<EditText>(R.id.etObtainedMarks)

        val state = viewModel.uiState.value
        val courses = if (state.activeSemester != null) {
            state.courses.filter { it.semesterId == state.activeSemester.id }
        } else {
            state.courses
        }
        val courseNames = courses.map { it.name }
        spinnerCourses.adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, courseNames)

        val selectedCalendar = Calendar.getInstance().apply {
            timeInMillis = existing?.dueDate ?: System.currentTimeMillis()
        }
        val dateSdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        btnDate.text = dateSdf.format(selectedCalendar.time)
        btnTime.text = timeSdf.format(selectedCalendar.time)

        if (existing != null) {
            etName.setText(existing.name)
            etTotal.setText(existing.totalMarks.toString())
            etWeight.setText(existing.weightage.toString())
            val courseIdx = courses.indexOfFirst { it.id == existing.courseId }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
                spinnerCourses.isEnabled = false
            }
            
            if (existing.isCompleted) {
                layoutObtained.visibility = View.VISIBLE
                etObtained.setText(existing.obtainedMarks?.toString() ?: "")
            }
        } else if (preselectedCourse != null) {
            val courseIdx = courses.indexOfFirst { it.id == preselectedCourse.id }
            if (courseIdx != -1) {
                spinnerCourses.setSelection(courseIdx)
            }
        }

        btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedCalendar.set(Calendar.YEAR, y)
                selectedCalendar.set(Calendar.MONTH, m)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
                btnDate.text = dateSdf.format(selectedCalendar.time)
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnTime.setOnClickListener {
            android.app.TimePickerDialog(requireContext(), { _, h, min ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                selectedCalendar.set(Calendar.MINUTE, min)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                btnTime.text = timeSdf.format(selectedCalendar.time)
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), false).show()
        }

        val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existing == null) "Add Assignment" else "Edit Assignment")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button, null)
            .setNegativeButton(R.string.cancel_button, null)
        
        if (existing != null) {
            dialogBuilder.setNeutralButton(R.string.delete_button) { _, _ ->
                confirmDelete(existing)
            }
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedIdx = spinnerCourses.selectedItemPosition
            if (selectedIdx == -1) return@setOnClickListener
            
            val courseId = courses[selectedIdx].id
            val name = etName.text.toString().trim()
            val total = etTotal.text.toString().toDoubleOrNull() ?: 0.0

            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val obtained = etObtained.text.toString().toDoubleOrNull()

            if (name.isNotEmpty()) {
                val assignment = Assignment(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name,
                    courseId = courseId,
                    dueDate = selectedCalendar.timeInMillis,
                    totalMarks = total,
                    obtainedMarks = obtained,
                    weightage = weight,
                    isCompleted = existing?.isCompleted ?: false
                )
                viewModel.updateAssignment(assignment)
                
                if (!assignment.isCompleted) {
                    scheduleReminder(assignment)
                }
                alertDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter assignment name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(assignment: Assignment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Assignment")
            .setMessage("Are you sure you want to delete this assignment?")
            .setPositiveButton(R.string.delete_button) { _, _ ->
                cancelReminder(assignment)
                viewModel.deleteAssignment(assignment)
                Toast.makeText(requireContext(), "Assignment deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun scheduleReminder(assignment: Assignment) {
        val course = viewModel.uiState.value.courses.find { it.id == assignment.courseId }
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationScheduler.scheduleAssignmentReminder(requireContext(), assignment, course?.name ?: "Unknown")
        }
    }

    private fun cancelReminder(assignment: Assignment) {
        NotificationScheduler.cancelReminder(requireContext(), NotificationType.ASSIGNMENT_DUE, assignment.id)
    }
}
