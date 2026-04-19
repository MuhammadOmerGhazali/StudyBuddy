package com.example.studbuddy.gpa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course

class GpaAdapter(
    private val courses: MutableList<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<GpaAdapter.GpaViewHolder>() {

    class GpaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCourseName)
        val tvCredits: TextView = view.findViewById(R.id.tvCourseCredits)
        val tvGrade: TextView = view.findViewById(R.id.tvCourseGrade)
        val tvPoints: TextView = view.findViewById(R.id.tvCoursePoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GpaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_gpa, parent, false)
        return GpaViewHolder(view)
    }

    override fun onBindViewHolder(holder: GpaViewHolder, position: Int) {
        val course = courses[position]
        holder.tvName.text = course.name
        holder.tvCredits.text = "Credits: ${course.creditHours}"
        holder.tvGrade.text = course.grade ?: "N/A"
        holder.tvPoints.text = "Points: ${String.format("%.2f", course.gradePoints)}"

        holder.itemView.setOnClickListener { onCourseClick(course) }
    }

    override fun getItemCount(): Int = courses.size

    fun updateList(newList: List<Course>) {
        courses.clear()
        courses.addAll(newList)
        notifyDataSetChanged()
    }
}
