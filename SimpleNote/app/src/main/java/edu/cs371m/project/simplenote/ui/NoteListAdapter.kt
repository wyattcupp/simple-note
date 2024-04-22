package edu.cs371m.project.simplenote.ui

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.project.simplenote.data.models.Note
import edu.cs371m.project.simplenote.databinding.NoteRvBinding
import java.util.Locale

class NoteListAdapter(private val onNoteClicked: (Note) -> Unit) :
    ListAdapter<Note, NoteListAdapter.VH>(Diff()) {

    class Diff : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }

    inner class VH(private val binding: NoteRvBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = SimpleDateFormat("MM-dd-yyyy hh:mm a", Locale.getDefault())
        fun bind(note: Note) {
            Log.d("NoteListAdapter", "VH.bind")
            binding.noteTitle.text =
                note.title
            binding.notePreview.text = note.content
            note.updatedAt?.let {
                binding.noteDate.text =
                    dateFormatter.format(it.toDate())
            } ?: run {
                binding.noteDate.text = "No date"
            }
            binding.root.setOnClickListener { onNoteClicked(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = NoteRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
