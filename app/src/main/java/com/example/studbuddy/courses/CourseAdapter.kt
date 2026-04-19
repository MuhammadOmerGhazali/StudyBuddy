package com.example.studbuddy.courses

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.Semester
import com.google.android.material.card.MaterialCardView

class CourseAdapter(
    private val courses: MutableList<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private var currentSemester: Semester? = null
    private val lightColors = listOf(
        "#E3F2FD", "#F1F8E9", "#FFF3E0", "#F3E5F5", "#E8EAF6", "#E0F2F1", "#F9FBE7", "#FFFDE7"
    )

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardCourse)
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val tvCourseInstructor: TextView = view.findViewById(R.id.tvCourseInstructor)
        val tvSemester: TextView = view.findViewById(R.id.tvSemester)
        val tvCreditHours: TextView = view.findViewById(R.id.tvCreditHours)
        val tvMarks: TextView = view.findViewById(R.id.tvMarks)
        val tvGrade: TextView = view.findViewById(R.id.tvGrade)
        val tvGradePoints: TextView = view.findViewById(R.id.tvGradePoints)
        val stripe: View = view.findViewById(R.id.stripe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        
        holder.tvCourseName.text = course.name
        holder.tvCourseInstructor.text = course.instructor ?: "No Instructor"
        holder.tvSemester.text = if (course.semesterId == currentSemester?.id) "Semester: ${currentSemester?.name}" else "Semester: Unknown"
        holder.tvCreditHours.text = "Credits: ${course.creditHours}"
        
        if (course.grade != null) {
            holder.tvGrade.text = course.grade
            holder.tvGrade.visibility = View.VISIBLE
            holder.tvMarks.text = "GP: ${String.format("%.2f", course.gradePoints)}"
            holder.tvMarks.visibility = View.VISIBLE
        } else {
            holder.tvGrade.visibility = View.GONE
            holder.tvMarks.visibility = View.GONE
        }

        // Random light shade based on name hash to keep it consistent
        val colorIndex = Math.abs(course.name.hashCode()) % lightColors.size
        holder.card.setCardBackgroundColor(Color.parseColor(lightColors[colorIndex]))
        holder.stripe.visibility = View.GONE

        holder.itemView.setOnClickListener { onCourseClick(course) }
    }

    override fun getItemCount(): Int = courses.size

    fun updateData(newCourses: List<Course>, semester: Semester?) {
        courses.clear()
        courses.addAll(newCourses)
        currentSemester = semester
        notifyDataSetChanged()
    }
}
