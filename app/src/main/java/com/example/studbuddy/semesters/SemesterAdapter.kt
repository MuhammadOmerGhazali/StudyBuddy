package com.example.studbuddy.semesters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Semester
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class SemesterAdapter(
    private val onSemesterClick: (Semester) -> Unit,
    private val onSetActive: (Semester) -> Unit,
    private val onEdit: (Semester) -> Unit,
    private val onDelete: (Semester) -> Unit
) : ListAdapter<Semester, SemesterAdapter.SemesterViewHolder>(SemesterDiffCallback()) {

    private val lightColors = listOf(
        "#E3F2FD", "#F1F8E9", "#FFF3E0", "#F3E5F5", "#E8EAF6", "#E0F2F1", "#F9FBE7", "#FFFDE7"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SemesterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_semester, parent, false)
        return SemesterViewHolder(view)
    }

    override fun onBindViewHolder(holder: SemesterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SemesterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.cardSemester)
        private val tvName: TextView = view.findViewById(R.id.tvSemesterName)
        private val tvDates: TextView = view.findViewById(R.id.tvDates)
        private val tvGpa: TextView = view.findViewById(R.id.tvGpa)
        private val tvActiveStatus: TextView = view.findViewById(R.id.tvActiveStatus)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnMenu)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(semester: Semester) {
            tvName.text = semester.name
            val start = dateFormat.format(Date(semester.startDate))
            val end = dateFormat.format(Date(semester.endDate))
            tvDates.text = "$start - $end"
            
            if (semester.gpa != null) {
                tvGpa.text = "GPA: ${String.format("%.2f", semester.gpa)}"
                tvGpa.visibility = View.VISIBLE
            } else {
                tvGpa.visibility = View.GONE
            }

            tvActiveStatus.visibility = if (semester.isActive) View.VISIBLE else View.GONE
            
            // Random light shade based on name hash to keep it consistent for the same semester
            val colorIndex = Math.abs(semester.name.hashCode()) % lightColors.size
            card.setCardBackgroundColor(Color.parseColor(lightColors[colorIndex]))

            itemView.setOnClickListener { onSemesterClick(semester) }

            btnMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Set as Active Semester")
                popup.menu.add("Edit Semester")
                popup.menu.add("Delete Semester")
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Set as Active Semester" -> onSetActive(semester)
                        "Edit Semester" -> onEdit(semester)
                        "Delete Semester" -> onDelete(semester)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    class SemesterDiffCallback : DiffUtil.ItemCallback<Semester>() {
        override fun areItemsTheSame(oldItem: Semester, newItem: Semester): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Semester, newItem: Semester): Boolean = oldItem == newItem
    }
}
