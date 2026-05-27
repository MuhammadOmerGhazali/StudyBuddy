package com.example.studbuddy.timetable

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Course
import com.example.studbuddy.core.models.TimetableEntry

class DayTimetableAdapter(
    private val dayMap: Map<Int, List<TimetableEntry>>,
    private val courseList: List<Course>,
    private val onEntryClick: (TimetableEntry) -> Unit
) : RecyclerView.Adapter<DayTimetableAdapter.DayViewHolder>() {

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayName: TextView = view.findViewById(R.id.tvDayName)
        val rvInner: RecyclerView = view.findViewById(R.id.rvInnerTimetable)
        val tvEmpty: TextView = view.findViewById(R.id.tvEmptyDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_timetable, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayIndex = position + 1
        holder.tvDayName.text = days[position]
        
        val entries = dayMap[dayIndex]?.sortedBy { it.startTime } ?: emptyList()
        
        if (entries.isEmpty()) {
            holder.tvEmpty.visibility = View.VISIBLE
            holder.rvInner.visibility = View.GONE
        } else {
            holder.tvEmpty.visibility = View.GONE
            holder.rvInner.visibility = View.VISIBLE
            holder.rvInner.layoutManager = LinearLayoutManager(holder.itemView.context)
            
            val adapter = TimetableAdapter(entries, onEntryClick)
            adapter.updateData(entries, courseList)
            holder.rvInner.adapter = adapter
        }
    }

    override fun getItemCount(): Int = days.size
}
