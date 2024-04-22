package edu.cs371m.project.simplenote.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.cs371m.project.simplenote.R
import edu.cs371m.project.simplenote.databinding.FragmentNotesBinding

class NotesFragment : Fragment() {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNotes()
        setupAddNoteActionButton()

        val folderId = arguments?.getString("folderId") ?: "defaultFolderId"
        viewModel.fetchNotesForFolder(folderId)
    }

    private fun setupRecyclerView() {
        val notesAdapter = NoteListAdapter { note ->
            viewModel.selectedNote.value = note
            val action = NotesFragmentDirections.actionNotesFragmentToEditNoteFragment(
                note.id,
                note.folderId
            )
            findNavController().navigate(action)
        }

        binding.notesRV.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notesAdapter
        }
    }

    private fun observeNotes() {
        viewModel.notesLiveData.observe(viewLifecycleOwner) { notes ->
            (binding.notesRV.adapter as NoteListAdapter).submitList(notes)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notes_in_folder, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete_all_notes -> {
                viewModel.deleteAllNotes()
                true
            }

            R.id.menu_logout -> {
                viewModel.handleUserLogout()
                true
            }

            R.id.menu_delete_folder -> {
                viewModel.selectedFolder.value?.let {
                    Log.d("NF", "menu_delete_folder - ${it.id}")
                    viewModel.deleteFolder(it.id)
                }
                true
            }

            R.id.menu_rename_folder -> {
                viewModel.selectedFolder.value?.let {
                    showRenameDialog(it.id)
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Allows user to enter a new folder name
    private fun showRenameDialog(folderId: String) {
        val input = EditText(requireContext()).apply {
            hint = "Enter new folder name"
        }
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Rename Folder")
            setView(input)
            setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameFolder(folderId, newName)
                }
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun setupAddNoteActionButton() {
        binding.root.findViewById<FloatingActionButton>(R.id.actionButtonAddNote)?.apply {
            setOnClickListener {
                navigateToCreateNote()
            }
        }
    }

    private fun navigateToCreateNote() {
        viewModel.resetSelectedNote()
        val action = NotesFragmentDirections.actionNotesFragmentToEditNoteFragment("", "")
        findNavController().navigate(action)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
