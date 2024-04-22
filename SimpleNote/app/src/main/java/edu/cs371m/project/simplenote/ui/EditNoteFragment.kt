package edu.cs371m.project.simplenote.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.cs371m.project.simplenote.R
import edu.cs371m.project.simplenote.databinding.FragmentEditNoteBinding
import edu.cs371m.project.simplenote.data.models.Note

class EditNoteFragment : Fragment() {
    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedNote.observe(viewLifecycleOwner) { note ->
            note?.let {
                binding.noteTitleEditor.setText(it.title)
                binding.noteContentEditor.setText(it.content)
            } ?: Toast.makeText(context, "No note selected.", Toast.LENGTH_SHORT).show()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.saveNoteFab.setOnClickListener {
            val title = binding.noteTitleEditor.text.toString().trim()
            val content = binding.noteContentEditor.text.toString().trim()
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(context, "Title and content cannot be empty.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val noteId = viewModel.selectedNote.value?.id ?: ""
            val folderId = viewModel.selectedFolder.value?.id ?: "defaultFolderId"

            val note = Note(
                id = noteId,
                folderId = folderId,
                title = title,
                content = content,
                createdBy = viewModel.getCurrentUserId() ?: ""
            )

            viewModel.addOrUpdateNote(note)
            Toast.makeText(context, "Note saved successfully", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_note, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                viewModel.deleteSelectedNote()
                findNavController().popBackStack()  // Navigate back after deleting
                true
            }

            R.id.menu_share -> {
                shareNote()
                true
            }

            android.R.id.home -> {
                findNavController().popBackStack()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareNote() {
        val shareContent = "${binding.noteTitleEditor.text}\n${binding.noteContentEditor.text}"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareContent)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Note Via"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
