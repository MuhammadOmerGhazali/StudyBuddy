package com.example.studbuddy.courses

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.semesters.SortOrder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class CoursesFragment : Fragment() {

    private lateinit var recyclerViewCourses: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private val courseList = mutableListOf<Course>()

    private val viewModel: CourseViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_courses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        
        setupViews(view)
        setupRecyclerView(view)
        setupMenu()
        observeViewModel()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.course_sort_menu, menu)
                
                val state = viewModel.uiState.value
                val sortId = when (state.sortBy) {
                    CourseSortBy.NAME -> R.id.sort_courses_by_name
                    CourseSortBy.CREATION -> R.id.sort_courses_by_creation
                    CourseSortBy.CREDITS -> R.id.sort_courses_by_credits
                }
                menu.findItem(sortId)?.isChecked = true
                menu.findItem(if (state.sortOrder == SortOrder.ASC) R.id.order_courses_asc else R.id.order_courses_desc)?.isChecked = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sort_courses_by_name -> {
                        menuItem.isChecked = true
                        viewModel.setSortBy(CourseSortBy.NAME)
                        true
                    }
                    R.id.sort_courses_by_creation -> {
                        menuItem.isChecked = true
                        viewModel.setSortBy(CourseSortBy.CREATION)
                        true
                    }
                    R.id.sort_courses_by_credits -> {
                        menuItem.isChecked = true
                        viewModel.setSortBy(CourseSortBy.CREDITS)
                        true
                    }
                    R.id.order_courses_asc -> {
                        menuItem.isChecked = true
                        viewModel.setSortOrder(SortOrder.ASC)
                        true
                    }
                    R.id.order_courses_desc -> {
                        menuItem.isChecked = true
                        viewModel.setSortOrder(SortOrder.DESC)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupViews(view: View) {
        view.findViewById<View>(R.id.btnAddCourse).setOnClickListener {
            val semester = viewModel.uiState.value.semester
            if (semester == null) {
                Toast.makeText(requireContext(), "Please setup or select a semester first", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.semesterFragment)
            } else {
                showCourseDialog(null)
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerViewCourses = view.findViewById(R.id.recyclerViewCourses)
        courseAdapter = CourseAdapter(courseList) { course ->
            val bundle = Bundle().apply {
                putString("courseId", course.id)
            }
            findNavController().navigate(R.id.action_coursesFragment_to_courseDetailFragment, bundle)
        }
        recyclerViewCourses.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewCourses.adapter = courseAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    if (!state.isLoading) {
                        courseAdapter.updateData(state.courses, state.semester)
                        updateEmptyState(state.courses.isEmpty())
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
                
                // Customize empty state for courses
                layoutEmpty.findViewById<ImageView>(R.id.imgEmptyState).setImageResource(R.drawable.ic_book_24)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_courses_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_courses_desc)
            }
            recyclerViewCourses.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerViewCourses.visibility = View.VISIBLE
        }
    }

    private fun showCourseDialog(existingCourse: Course?) {
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

        // Hide semester spinner when adding from specific semester view
        spinnerSemester.visibility = View.GONE
        dialogView.findViewById<View>(R.id.cardSemester).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvSemesterLabel).visibility = View.GONE

        val grades = gradeMap.keys.toList()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, grades)
        spinnerGrade.adapter = adapter

        existingCourse?.let {
            etName.setText(it.name)
            etDescription.setText(it.description)
            etInstructor.setText(it.instructor)
            etCredits.setText(it.creditHours.toString())
            val gradeIndex = grades.indexOf(it.grade ?: "Select Grade")
            if (gradeIndex != -1) spinnerGrade.setSelection(gradeIndex)
            tvGpDisplay.text = "Grade Points: ${String.format(Locale.getDefault(), "%.2f", it.gradePoints)}"
        }

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
            .setTitle(if (existingCourse == null) "Add Course" else "Edit Course")
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
                val semester = viewModel.uiState.value.semester
                if (semester != null) {
                    val gradePoints = if (basePoints >= 0) basePoints * credits else 0.0
                    
                    val course = Course(
                        id = existingCourse?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        description = if (description.isEmpty()) null else description,
                        instructor = if (instructor.isEmpty()) null else instructor,
                        creditHours = credits,
                        semesterId = semester.id,
                        marks = existingCourse?.marks ?: 0.0,
                        grade = if (basePoints >= 0) selectedGrade else null,
                        gradePoints = gradePoints
                    )
                    
                    if (existingCourse == null) {
                        viewModel.addCourse(course)
                    } else {
                        viewModel.updateCourse(course)
                    }
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Semester not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
