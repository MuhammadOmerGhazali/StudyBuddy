package com.example.studbuddy.timetable

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.TimetableEntry

class TimetableAdapter(
    private var entries: List<TimetableEntry>,
    private val onItemClick: (TimetableEntry) -> Unit
) : RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder>() {

    private var courseList: List<Course> = emptyList()

    class TimetableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewColorStripe: View = view.findViewById(R.id.viewColorStripe)
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val tvTimeSlot: TextView = view.findViewById(R.id.tvTimeSlot)
        val tvRoom: TextView = view.findViewById(R.id.tvRoom)
        val tvDay: TextView = view.findViewById(R.id.tvDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimetableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timetable, parent, false)
        return TimetableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimetableViewHolder, position: Int) {
        val entry = entries[position]
        
        val course = courseList.find { it.id == entry.courseId }
        
        holder.tvCourseName.text = course?.name ?: "Unknown Course"
        holder.tvTimeSlot.text = "${entry.startTime} - ${entry.endTime}"
        holder.tvRoom.text = "Place: ${entry.room}"
        holder.tvDay.visibility = View.GONE
        
        try {
            holder.viewColorStripe.setBackgroundColor(Color.parseColor(entry.color))
        } catch (e: Exception) {
            holder.viewColorStripe.setBackgroundColor(Color.BLUE)
        }

        holder.itemView.setOnClickListener { onItemClick(entry) }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<TimetableEntry>, newCourses: List<Course>) {
        entries = newEntries
        courseList = newCourses
        notifyDataSetChanged()
    }
}
