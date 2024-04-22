package edu.cs371m.project.simplenote.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs371m.project.simplenote.data.models.Folder
import edu.cs371m.project.simplenote.databinding.FolderRvBinding

class FoldersListAdapter(private val onFolderClicked: (Folder) -> Unit) :
    ListAdapter<Folder, FoldersListAdapter.VH>(Diff()) {

    class Diff : DiffUtil.ItemCallback<Folder>() {
        override fun areItemsTheSame(oldItem: Folder, newItem: Folder): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Folder, newItem: Folder): Boolean =
            oldItem == newItem
    }

    inner class VH(private val binding: FolderRvBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: Folder) {
            Log.d("FoldersListAdapter", "VH.bind")
            binding.folderName.text = folder.name

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
