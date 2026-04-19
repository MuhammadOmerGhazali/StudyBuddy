package com.example.studbuddy.exams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Exam
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ExamAdapter(
    private val exams: MutableList<Exam>,
    private val onItemClicked: (Exam) -> Unit
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    private var courseList: List<Course> = emptyList()

    class ExamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExamType: TextView = view.findViewById(R.id.tvExamType)
        val tvExamDate: TextView = view.findViewById(R.id.tvExamDate)
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val tvVenue: TextView = view.findViewById(R.id.tvVenue)
        val tvMarks: TextView = view.findViewById(R.id.tvMarks)
        val tvWeightage: TextView = view.findViewById(R.id.tvWeightage)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        val exam = exams[position]
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        holder.tvExamType.text = exam.type.name
        holder.tvExamDate.text = sdf.format(Date(exam.date))
        
        val course = courseList.find { it.id == exam.courseId }
        holder.tvCourseName.text = course?.name ?: "Unknown Course"
        holder.tvVenue.text = "Venue: ${exam.venue ?: "Not set"}"

        if (exam.isCompleted) {
            holder.tvStatus.text = "Completed"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusNormal))
        } else {
            holder.tvStatus.text = "Pending"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusCritical))
        }

        val obtained = exam.obtainedMarks?.let { String.format("%.1f", it) } ?: "-"
        holder.tvMarks.text = "Marks: $obtained / ${exam.totalMarks}"
        holder.tvWeightage.text = "Weight: ${exam.weightage}%"

        // Highlight exam date in red if 1 day or less is left (and not completed)
        if (!exam.isCompleted) {
            val diff = exam.date - System.currentTimeMillis()
            if (diff > 0 && diff <= TimeUnit.DAYS.toMillis(1)) {
                holder.tvExamDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusCritical))
            } else if (diff < 0) {
                holder.tvExamDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorStatusOverdue))
            } else {
                holder.tvExamDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTextSecondary))
            }
        } else {
            holder.tvExamDate.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorTextSecondary))
        }

        holder.itemView.setOnClickListener { onItemClicked(exam) }
    }

    override fun getItemCount(): Int = exams.size

    fun updateData(newExams: List<Exam>, newCourses: List<Course>) {
        exams.clear()
        exams.addAll(newExams)
        courseList = newCourses
        notifyDataSetChanged()
    }
}
