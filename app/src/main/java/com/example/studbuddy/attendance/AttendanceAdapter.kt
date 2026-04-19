package com.example.studbuddy.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.AttendanceRecord
import com.example.studbuddy.core.models.Course

class AttendanceAdapter(
    private val courses: MutableList<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    private var attendanceRecords: List<AttendanceRecord> = emptyList()

    class AttendanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardAttendance)
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val tvStats: TextView = view.findViewById(R.id.tvAttendanceStats)
        val pbAttendance: ProgressBar = view.findViewById(R.id.pbAttendance)
        val tvStatus: TextView = view.findViewById(R.id.tvAttendanceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val course = courses[position]
        val context = holder.itemView.context
        
        holder.tvCourseName.text = course.name
        
        val records = attendanceRecords.filter { it.courseId == course.id }
        val presentCount = records.count { it.status == "PRESENT" }
        val totalAttended = records.size
        
        val threshold = 75.0 // Default threshold
        
        val percentage = if (totalAttended > 0) (presentCount.toDouble() / totalAttended) * 100 else 100.0
        
        holder.tvStats.text = "Present: $presentCount / $totalAttended"
        holder.pbAttendance.progress = percentage.toInt()
        holder.tvStatus.text = String.format("%.1f%%", percentage)

        if (percentage < threshold) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorStatusCritical))
            holder.tvStatus.text = "SHORT ATTENDANCE"
            holder.tvStatus.setTextColor(Color.WHITE)
            holder.tvCourseName.setTextColor(Color.WHITE)
            holder.tvStats.setTextColor(Color.WHITE)
        } else {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.colorStatusNormal))
            holder.tvCourseName.setTextColor(ContextCompat.getColor(context, R.color.colorTextPrimary))
            holder.tvStats.setTextColor(ContextCompat.getColor(context, R.color.colorTextSecondary))
        }

        holder.itemView.setOnClickListener { onCourseClick(course) }
    }

    override fun getItemCount(): Int = courses.size

    fun updateData(newCourses: List<Course>, newRecords: List<AttendanceRecord>) {
        courses.clear()
        courses.addAll(newCourses)
        attendanceRecords = newRecords
        notifyDataSetChanged()
    }
}
