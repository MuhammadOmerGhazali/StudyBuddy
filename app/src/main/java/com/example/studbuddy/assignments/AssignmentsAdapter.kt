package com.example.studbuddy.assignments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Assignment
import com.example.studbuddy.core.models.Course
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AssignmentsAdapter(
    private val assignments: MutableList<Assignment>,
    private val onStatusChanged: (Assignment, Boolean) -> Unit,
    private val onItemClicked: (Assignment) -> Unit
) : RecyclerView.Adapter<AssignmentsAdapter.AssignmentViewHolder>() {

    private var courseList: List<Course> = emptyList()

    class AssignmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvAssignmentTitle)
        val tvCourse: TextView = view.findViewById(R.id.tvCourseName)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvWeightage: TextView = view.findViewById(R.id.tvWeightage)
        val tvMarks: TextView = view.findViewById(R.id.tvMarks)
        val cbCompleted: CheckBox = view.findViewById(R.id.cbCompleted)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment, parent, false)
        return AssignmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssignmentViewHolder, position: Int) {
        val assignment = assignments[position]
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        holder.tvTitle.text = assignment.name
        holder.tvDueDate.text = "Due: ${sdf.format(Date(assignment.dueDate))}"
        holder.tvWeightage.text = "Weight: ${assignment.weightage}%"
        
        val obtained = assignment.obtainedMarks?.let { String.format("%.1f", it) } ?: "-"
        holder.tvMarks.text = "Marks: $obtained / ${assignment.totalMarks}"
        
        val course = courseList.find { it.id == assignment.courseId }
        holder.tvCourse.text = course?.name ?: "Unknown Course"

        if (assignment.isCompleted) {
            holder.tvStatus.text = "Completed"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusNormal))
        } else {
            holder.tvStatus.text = "Pending"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusCritical))
        }

        // Highlight due date in red if 1 day or less is left (and not completed)
        if (!assignment.isCompleted) {
            val diff = assignment.dueDate - System.currentTimeMillis()
            if (diff > 0 && diff <= TimeUnit.DAYS.toMillis(1)) {
                holder.tvDueDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusCritical))
            } else if (diff < 0) {
                holder.tvDueDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusOverdue))
            } else {
                holder.tvDueDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTextSecondary))
            }
        } else {
            holder.tvDueDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTextSecondary))
        }

        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = assignment.isCompleted
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            onStatusChanged(assignment, isChecked)
        }

        holder.itemView.setOnClickListener { onItemClicked(assignment) }
    }

    override fun getItemCount(): Int = assignments.size

    fun updateData(newAssignments: List<Assignment>, newCourses: List<Course>) {
        assignments.clear()
        assignments.addAll(newAssignments)
        courseList = newCourses
        notifyDataSetChanged()
    }
}
