package com.example.studbuddy.courses.details

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputLayout

@AndroidEntryPoint
class CourseSummaryFragment : Fragment() {

    private val viewModel: CourseSummaryViewModel by viewModels()

    private val gradeMap = mapOf(
        "Select Grade" to -1.0,
        "A" to 4.0,
        "A-" to 3.7,
        "B+" to 3.3,
        "B" to 3.0,
        "B-" to 2.7,
        "C+" to 2.3,
        "C" to 2.0,
        "C-" to 1.7,
        "D+" to 1.3,
        "D" to 1.0,
        "F" to 0.0
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_summary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        observeViewModel(view)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, 1, Menu.NONE, "Edit Course").apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
                menu.add(Menu.NONE, 2, Menu.NONE, "Delete Course").apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    1 -> {
                        val course = viewModel.uiState.value.course
                        if (course != null) showEditCourseDialog(course)
                        true
                    }
                    2 -> {
                        showDeleteConfirmation()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel(view: View) {
        val tvName = view.findViewById<TextView>(R.id.tvCourseName)
        val tvInstructor = view.findViewById<TextView>(R.id.tvInstructor)
        val tvCredits = view.findViewById<TextView>(R.id.tvCredits)
        val tvSemester = view.findViewById<TextView>(R.id.tvSemester)
        val tvGrade = view.findViewById<TextView>(R.id.tvGrade)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val tvAttendance = view.findViewById<TextView>(R.id.tvAttendanceStat)
        val tvMarks = view.findViewById<TextView>(R.id.tvMarksStat)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.course?.let { course ->
                        tvName.text = course.name
                        tvInstructor.text = course.instructor ?: "No Instructor"
                        tvCredits.text = course.creditHours.toString()
                        tvSemester.text = state.semester?.name ?: "Unknown"
                        
                        if (course.grade != null) {
                            tvGrade.text = "${course.grade} (${String.format("%.2f", course.gradePoints)})"
                        } else {
                            tvGrade.text = "Not Added"
                        }

                        if (!course.description.isNullOrEmpty()) {
                            tvDescription.text = course.description
                            tvDescription.visibility = View.VISIBLE
                        } else {
                            tvDescription.visibility = View.GONE
                        }

                        // Stats
                        val total = state.attendance.size
                        val present = state.attendance.count { it.status == "PRESENT" }
                        val percentage = if (total > 0) (present * 100 / total) else 0
                        tvAttendance.text = "$percentage%"
                        
                        tvMarks.text = String.format("%.1f", course.marks)
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete this course? All associated assignments, exams, notes, and attendance will be permanently removed.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCourse {
                    Toast.makeText(requireContext(), "Course deleted", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.coursesFragment, false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditCourseDialog(course: Course) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_course, null)
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilCourseName)
        val etName = dialogView.findViewById<EditText>(R.id.etCourseName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etInstructor = dialogView.findViewById<EditText>(R.id.etInstructor)
        val tilCredits = dialogView.findViewById<TextInputLayout>(R.id.tilCredits)
        val etCredits = dialogView.findViewById<EditText>(R.id.etCredits)
        val spinnerGrade = dialogView.findViewById<Spinner>(R.id.spinnerGrade)
        val spinnerSemester = dialogView.findViewById<Spinner>(R.id.spinnerSemester)
        val tvGpDisplay = dialogView.findViewById<TextView>(R.id.tvGradePointsDisplay)

        // Hide semester spinner during edit as requested
        spinnerSemester.visibility = View.GONE
        dialogView.findViewById<View>(R.id.cardSemester).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvSemesterLabel).visibility = View.GONE

        etName.setText(course.name)
        etDescription.setText(course.description)
        etInstructor.setText(course.instructor)
        etCredits.setText(course.creditHours.toString())

        // Setup Grade Spinner
        val grades = gradeMap.keys.toList()
        val gradeAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, grades)
        spinnerGrade.adapter = gradeAdapter
        val gradeIndex = grades.indexOf(course.grade ?: "Select Grade")
        if (gradeIndex != -1) spinnerGrade.setSelection(gradeIndex)
        tvGpDisplay.text = "Grade Points: ${String.format(Locale.getDefault(), "%.2f", course.gradePoints)}"

        // Setup Semester Spinner
        val semesters = viewModel.allSemesters.value
        val semesterNames = semesters.map { it.name }
        val semesterAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, semesterNames)
        spinnerSemester.adapter = semesterAdapter
        val semesterIndex = semesters.indexOfFirst { it.id == course.semesterId }
        if (semesterIndex != -1) spinnerSemester.setSelection(semesterIndex)

        fun updateGpDisplay() {
            val selectedGrade = spinnerGrade.selectedItem?.toString() ?: "Select Grade"
            val basePoints = gradeMap[selectedGrade] ?: -1.0
            val credits = etCredits.text.toString().toIntOrNull() ?: 0
            
            if (basePoints >= 0 && credits > 0) {
                tvGpDisplay.text = "Grade Points: ${String.format(Locale.getDefault(), "%.2f", basePoints * credits)}"
            } else {
                tvGpDisplay.text = "Grade Points: 0.00"
            }
        }

        etCredits.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateGpDisplay()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        spinnerGrade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGpDisplay()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Course")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button, null)
            .setNegativeButton(R.string.cancel_button, null)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val instructor = etInstructor.text.toString().trim()
            val credits = etCredits.text.toString().toIntOrNull() ?: 0
            val selectedGrade = spinnerGrade.selectedItem.toString()
            val basePoints = gradeMap[selectedGrade] ?: -1.0
            
            var isValid = true
            if (name.isEmpty()) {
                tilName.error = "Course name is required"
                isValid = false
            } else {
                tilName.error = null
            }

            if (credits <= 0) {
                tilCredits.error = "Enter valid credit hours"
                isValid = false
            } else {
                tilCredits.error = null
            }

            if (isValid) {
                val gradePoints = if (basePoints >= 0) basePoints * credits else 0.0
                viewModel.updateCourse(course.copy(
                    name = name,
                    description = if (description.isEmpty()) null else description,
                    instructor = if (instructor.isEmpty()) null else instructor,
                    creditHours = credits,
                    grade = if (basePoints >= 0) selectedGrade else null,
                    gradePoints = gradePoints
                ))
                dialog.dismiss()
            }
        }
    }
}
