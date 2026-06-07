package com.example.studbuddy.notes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.studbuddy.R
import com.example.studbuddy.core.models.Note
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { showAddNoteDialog(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewNotes)
        
        adapter = NotesAdapter(
            onNoteClick = { openNote(it) },
            onDeleteClick = { viewModel.deleteNote(it) }
        )
        recyclerView.adapter = adapter

        getCourseId()?.let { viewModel.setManualCourseId(it) }

        setupMenu()
        observeViewModel()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, 1, Menu.NONE, "Add Note").apply {
                    setIcon(R.drawable.ic_add_24)
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == 1) {
                    filePickerLauncher.launch("*/*")
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.notes)
                    updateEmptyState(state.notes.isEmpty())
                }
            }
        }
    }

    private fun getCourseId(): String? {
        return arguments?.getString("courseId") ?: parentFragment?.parentFragment?.arguments?.getString("courseId")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            if (layoutEmpty.visibility != View.VISIBLE) {
                layoutEmpty.visibility = View.VISIBLE
                layoutEmpty.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyTitle).text = "No Notes"
                layoutEmpty.findViewById<TextView>(R.id.tvEmptyDescription).text = "Click + to add study materials"
            }
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showAddNoteDialog(uri: Uri) {
        val courseId = getCourseId()
        if (courseId == null) {
            Toast.makeText(requireContext(), "Error: Course context lost", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_note, null)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etNoteTitle)
        
        // Default title from filename
        val fileName = getFileName(uri)
        etTitle.setText(fileName)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Note")
            .setView(dialogView)
            .setMessage("Note: Files will be stored locally and not synced to cloud.")
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    saveFileAndNote(uri, title)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveFileAndNote(uri: Uri, title: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            val result = withContext(Dispatchers.IO) {
                try {
                    val dir = File(requireContext().getExternalFilesDir(null), "Notes")
                    if (!dir.exists()) dir.mkdirs()

                    val extension = requireContext().contentResolver.getType(uri)?.split("/")?.last() ?: "file"
                    val localFile = File(dir, "${System.currentTimeMillis()}.$extension")
                    
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val fileSize = localFile.length()
                    Triple(true, localFile.absolutePath, fileSize)
                } catch (e: Exception) {
                    Triple(false, "", 0L)
                }
            }

            if (result.first) {
                val type = requireContext().contentResolver.getType(uri) ?: "unknown"
                val courseId = getCourseId()
                if (courseId != null) {
                    viewModel.addNote(title, result.second, type, result.third, courseId)
                } else {
                    Toast.makeText(requireContext(), "Error: Course context lost", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Failed to save file", Toast.LENGTH_SHORT).show()
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun openNote(note: Note) {
        try {
            val file = File(note.localPath)
            if (!file.exists()) {
                Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val mimeType = resolveMimeType(note)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveMimeType(note: Note): String {
        if (note.fileType.isNotBlank() && note.fileType != "unknown") {
            return note.fileType
        }

        val extension = File(note.localPath).extension.lowercase()
        val mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mapped ?: "*/*"
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Untitled Note"
    }
}
