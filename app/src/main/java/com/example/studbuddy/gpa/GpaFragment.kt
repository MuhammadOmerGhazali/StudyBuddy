package com.example.studbuddy.gpa

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class GpaFragment : Fragment() {

    private lateinit var tvSemesterGpa: TextView
    private lateinit var tvCgpa: TextView
    private lateinit var recyclerViewActive: RecyclerView
    private lateinit var recyclerViewPrevious: RecyclerView
    private lateinit var activeAdapter: GpaAdapter
    private lateinit var previousAdapter: GpaAdapter
    private lateinit var tvActiveLabel: View
    private lateinit var tvPreviousLabel: View

    private val viewModel: GpaViewModel by viewModels()

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
        return inflater.inflate(R.layout.fragment_gpa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        tvSemesterGpa = view.findViewById(R.id.tvSemesterGpa)
        tvCgpa = view.findViewById(R.id.tvCgpa)
        recyclerViewActive = view.findViewById(R.id.recyclerViewActiveCourses)
        recyclerViewPrevious = view.findViewById(R.id.recyclerViewPreviousCourses)
        tvActiveLabel = view.findViewById(R.id.tvActiveCoursesLabel)
        tvPreviousLabel = view.findViewById(R.id.tvPreviousCoursesLabel)
    }

    private fun setupRecyclerViews() {
        activeAdapter = GpaAdapter(mutableListOf()) { course -> showGradeDialog(course) }
        previousAdapter = GpaAdapter(mutableListOf()) { course -> showGradeDialog(course) }

        recyclerViewActive.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewActive.adapter = activeAdapter

        recyclerViewPrevious.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPrevious.adapter = previousAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    activeAdapter.updateList(state.activeCourses)
                    previousAdapter.updateList(state.previousCourses)
                    
                    tvActiveLabel.visibility = if (state.activeCourses.isEmpty()) View.GONE else View.VISIBLE
                    tvPreviousLabel.visibility = if (state.previousCourses.isEmpty()) View.GONE else View.VISIBLE

                    tvSemesterGpa.text = String.format(Locale.getDefault(), "%.2f", state.semesterGpa)
                    tvCgpa.text = String.format(Locale.getDefault(), "%.2f", state.cgpa)
                }
            }
        }
    }

    private fun showGradeDialog(course: Course) {
        val grades = gradeMap.keys.toList()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_course, grades)

        val spinner = Spinner(requireContext()).apply {
            this.adapter = adapter
            val index = grades.indexOf(course.grade ?: "Select Grade")
            if (index != -1) setSelection(index)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Grade: ${course.name}")
            .setMessage("Select the grade for this course to update GPA.")
            .setView(spinner)
            .setPositiveButton("Update") { _, _ ->
                val selectedGrade = spinner.selectedItem.toString()
                val basePoints = gradeMap[selectedGrade] ?: -1.0
                if (basePoints >= 0) {
                    viewModel.updateCourseGrade(course, selectedGrade, basePoints * course.creditHours)
                } else {
                    // Logic to clear grade if "Select Grade" is chosen
                    viewModel.updateCourseGrade(course, "N/A", 0.0)
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }
}
