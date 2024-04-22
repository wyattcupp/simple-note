package edu.cs371m.project.simplenote.ui

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.project.simplenote.data.models.Folder
import edu.cs371m.project.simplenote.databinding.FolderRvBinding
import java.util.Locale

class FoldersListAdapter(private val onFolderClicked: (Folder) -> Unit) :
    ListAdapter<Folder, FoldersListAdapter.VH>(Diff()) {

    class Diff : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean =
            oldItem == newItem
    }

    inner class VH(private val binding: FolderRvBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())

        fun bind(folder: Folder) {
            Log.d("FoldersListAdapter", "VH.bind")
            binding.folderName.text = folder.name

            Log.d("FoldersListAdapter", "Folder=$folder")
            folder.updatedAt?.let {
                binding.folderTimestamp.text =
                    dateFormatter.format(it.toDate())
            } ?: run {
                binding.folderTimestamp.text = "Updating..."
            }
            binding.root.setOnClickListener {
                onFolderClicked(folder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = FolderRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}
