package com.example.studbuddy.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
    private val records: List<AttendanceRecord>,
    private val onRecordClick: (AttendanceRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvRecordDate)
        val tvStatus: TextView = view.findViewById(R.id.tvRecordStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        val context = holder.itemView.context
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        
        holder.tvDate.text = sdf.format(Date(record.dateTime))
        holder.tvStatus.text = record.status
        
        val color = when (record.status) {
            "PRESENT" -> ContextCompat.getColor(context, R.color.colorStatusNormal)
            "ABSENT" -> ContextCompat.getColor(context, R.color.colorStatusCritical)
            "LATE" -> ContextCompat.getColor(context, R.color.colorStatusWarning)
            else -> Color.GRAY
        }
        holder.tvStatus.setTextColor(color)
        
        holder.itemView.setOnClickListener { onRecordClick(record) }
    }

    override fun getItemCount(): Int = records.size
}
