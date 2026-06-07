package com.example.studbuddy.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgIcon: ImageView = view.findViewById(R.id.imgFileType)
        private val tvTitle: TextView = view.findViewById(R.id.tvNoteTitle)
        private val tvInfo: TextView = view.findViewById(R.id.tvNoteInfo)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnNoteMenu)
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(note: Note) {
            tvTitle.text = note.title
            val sizeKb = note.fileSize / 1024
            val dateStr = dateFormat.format(Date(note.createdAt))
            tvInfo.text = "${note.fileType.uppercase()} • $sizeKb KB • $dateStr"

            itemView.setOnClickListener { onNoteClick(note) }

            btnMenu.setOnClickListener { v ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add("Delete")
                popup.setOnMenuItemClickListener {
                    onDeleteClick(note)
                    true
                }
                popup.show()
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }
}
