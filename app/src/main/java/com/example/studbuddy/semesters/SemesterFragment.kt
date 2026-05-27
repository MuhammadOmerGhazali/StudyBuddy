package com.example.studbuddy.semesters

import android.app.DatePickerDialog
import android.os.Bundle
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
import com.example.studbuddy.core.models.Semester
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SemesterFragment : Fragment() {

    private lateinit var recyclerViewSemesters: RecyclerView
    private lateinit var semesterAdapter: SemesterAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private val viewModel: SemesterViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_semester, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        recyclerViewSemesters = view.findViewById(R.id.recyclerViewSemesters)

        setupRecyclerView()
        setupViews(view)
        setupMenu()
        observeViewModel()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.semester_sort_menu, menu)
                
                // Update menu state based on current ViewModel state
                val state = viewModel.uiState.value
                menu.findItem(if (state.sortBy == SemesterSortBy.CREATION) R.id.sort_by_creation else R.id.sort_by_date)?.isChecked = true
                menu.findItem(if (state.sortOrder == SortOrder.ASC) R.id.order_asc else R.id.order_desc)?.isChecked = true
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sort_by_creation -> {
                        menuItem.isChecked = true
                        viewModel.setSortBy(SemesterSortBy.CREATION)
                        true
                    }
                    R.id.sort_by_date -> {
                        menuItem.isChecked = true
                        viewModel.setSortBy(SemesterSortBy.DATE)
                        true
                    }
                    R.id.order_asc -> {
                        menuItem.isChecked = true
                        viewModel.setSortOrder(SortOrder.ASC)
                        true
                    }
                    R.id.order_desc -> {
                        menuItem.isChecked = true
                        viewModel.setSortOrder(SortOrder.DESC)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        semesterAdapter = SemesterAdapter(
            onSemesterClick = { semester ->
                val bundle = Bundle().apply {
                    putString("semesterId", semester.id)
                }
                findNavController().navigate(R.id.coursesFragment, bundle)
            },
            onSetActive = { semester ->
                viewModel.setActiveSemester(semester.id)
            },
            onEdit = { semester ->
                showSemesterDialog(semester)
            },
            onDelete = { semester ->
                showDeleteConfirmation(semester)
            }
        )
        recyclerViewSemesters.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewSemesters.adapter = semesterAdapter
    }

    private fun setupViews(view: View) {
        view.findViewById<View>(R.id.btnAddSemester).setOnClickListener {
            showSemesterDialog(null)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    
                    if (!state.isLoading) {
                        semesterAdapter.submitList(state.semesters)
                        updateEmptyState(state.semesters.isEmpty())
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
                
                layoutEmpty.findViewById<ImageView>(R.id.imgEmptyState).setImageResource(R.drawable.ic_calendar_month_24)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).setText(R.string.empty_semesters_title)
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).setText(R.string.empty_semesters_desc)
            }
            recyclerViewSemesters.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            recyclerViewSemesters.visibility = View.VISIBLE
        }
    }

    private fun showSemesterDialog(existingSemester: Semester?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_semester, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etSemesterName)
        val btnStart = dialogView.findViewById<MaterialButton>(R.id.btnStartDate)
        val btnEnd = dialogView.findViewById<MaterialButton>(R.id.btnEndDate)
        val tvRange = dialogView.findViewById<TextView>(R.id.tvDateRange)

        var startDate = existingSemester?.startDate ?: System.currentTimeMillis()
        var endDate = existingSemester?.endDate ?: (System.currentTimeMillis() + 120L * 24 * 60 * 60 * 1000) // ~4 months

        fun updateDateDisplay() {
            tvRange.text = "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
        }

        existingSemester?.let {
            etName.setText(it.name)
        }
        updateDateDisplay()

        btnStart.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = startDate }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                startDate = cal.timeInMillis
                updateDateDisplay()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEnd.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = endDate }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                endDate = cal.timeInMillis
                updateDateDisplay()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingSemester == null) "Add Semester" else "Edit Semester")
            .setView(dialogView)
            .setPositiveButton(R.string.save_button) { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (existingSemester == null) {
                        viewModel.addSemester(name, startDate, endDate)
                    } else {
                        viewModel.updateSemester(existingSemester.copy(name = name, startDate = startDate, endDate = endDate))
                    }
                } else {
                    Toast.makeText(requireContext(), "Please enter semester name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showDeleteConfirmation(semester: Semester) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Semester")
            .setMessage("Are you sure you want to delete ${semester.name}?")
            .setPositiveButton(R.string.delete_button) { _, _ ->
                viewModel.deleteSemester(semester) { success, message ->
                    if (!success) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }
}
