package edu.cs371m.project.simplenote.ui

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.cs371m.project.simplenote.auth.AuthUser
import edu.cs371m.project.simplenote.data.ViewModelDBHelper
import edu.cs371m.project.simplenote.data.models.Folder
import edu.cs371m.project.simplenote.data.models.Note
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val dbHelper = ViewModelDBHelper()
    private var currentUserId: String? = null

    val notesLiveData = MutableLiveData<List<Note>?>()
    val foldersLiveData = MutableLiveData<List<Folder>?>()
    val selectedFolder = MutableLiveData<Folder?>()
    val selectedNote = MutableLiveData<Note?>()
    val operationStatus = MutableLiveData<String>()
    val loading = MutableLiveData<Boolean>()
    val messageLiveData = MutableLiveData<String>()
    var defaultFolderId: String? = null
    val defaultFolder = MutableLiveData<Folder?>()

    private lateinit var authUser: AuthUser

    init {
        selectedFolder.observeForever { folder ->
            folder?.let {
                fetchNotesForFolder(it.id)
            }
        }
    }

    fun initializeAuthUser(authUser: AuthUser) {
        this.authUser = authUser
        listenToAuthState()
    }

    private fun listenToAuthState() {
        authUser.observeUser().observeForever { user ->
            Log.d("MVM", "listenToAuthState, user=$user, currentUserId=$currentUserId")
            if (user.uid != "-1" && currentUserId != user.uid) {
                // Set current user ID and perform initialization only if the user has changed
                currentUserId = user.uid
                initializeUserDependentFeatures(user.uid)
            } else if (user == null) {
                handleUserLogout()
            }
        }
    }

    private fun initializeUserDependentFeatures(userId: String) {
        // Ensure all user-dependent initializations are handled here
        Log.d("MVM", "Initializing or re-initializing user dependent features for userId=$userId")
        ensureDefaultFolder(userId)
        fetchFolders()
        //fetchNotes()
        selectedFolder.value?.let { fetchNotesForFolder(it.id) }
    }

    fun handleUserLogout() {
        // Handle user logout scenario, clear data, etc.
        currentUserId = null
        notesLiveData.postValue(emptyList())
        foldersLiveData.postValue(emptyList())
        authUser.logout()
        Log.d("MVM", "User logged out, cleared user-dependent data")
    }


    private fun ensureDefaultFolder(userId: String) {
        viewModelScope.launch {
            dbHelper.ensureDefaultFolder(userId) { exists, folderId ->
                if (exists) {
                    val folder = Folder(id = folderId, name = "Default")
                    selectedFolder.postValue(folder)
                    defaultFolder.postValue(folder)
                    defaultFolderId = folderId
                    Log.d("MVM", "Default folder exists & set successfully, ID: $folderId")
                } else {
                    Log.d("MVM", "Failed to ensure default folder was created/exists")
                }
            }
        }
    }

    // Ensure this function is modified to handle the addition of a folder properly
    fun addFolder(folder: Folder) {
        getCurrentUserId()?.let { userId ->
            loading.value = true
            viewModelScope.launch {
                dbHelper.addFolder(folder, userId) { success, folderId ->
                    if (success) {
                        fetchFolders()
                        selectedFolder.postValue(folder)
                        operationStatus.postValue("Folder added successfully")
                    } else {
                        operationStatus.postValue("Failed to add folder")
                    }
                    loading.postValue(false)
                }
            }
        }
    }

    fun deleteAllNotes() {
        selectedFolder.value?.id?.let { folderId ->
            getCurrentUserId()?.let { userId ->
                loading.value = true
                dbHelper.deleteNotesInFolder(userId, folderId) { success ->
                    if (success) {
                        fetchNotesForFolder(selectedFolder.value!!.id)  // Refresh notes after deletion
                        operationStatus.postValue("All notes in the folder deleted successfully")
                    } else {
                        operationStatus.postValue("Failed to delete notes in folder")
                    }
                    loading.postValue(false)
                }
            }
        }
    }

    fun getCurrentUserId(): String? = currentUserId

    // Fetches ALL notes. TODO: include this in an all notes tab (might not get to it)
    fun fetchNotes() {
        currentUserId?.let { userId ->
            loading.value = true
            viewModelScope.launch {
                dbHelper.getNotes(userId) { notes ->
                    notesLiveData.postValue(notes)
                    loading.postValue(false)
                }
            }
        }
    }

    fun fetchNotesForFolder(folderId: String) {
        currentUserId?.let { userId ->
            loading.value = true
            viewModelScope.launch {
                dbHelper.getNotesForFolder(userId, folderId) { notes ->
                    notesLiveData.postValue(notes)
                    loading.postValue(false)
                }
            }
        }
    }

    // Fetches folders and filters default out so that RV is displayed properly.
    private fun fetchFolders() {
        currentUserId?.let { userId ->
            loading.value = true
            viewModelScope.launch {
                dbHelper.getFolders(userId) { folders ->
                    val filteredFolders = folders?.filter { it.id != defaultFolderId && it.name != "Default" }
                    // foldersListAdapter.submitList(filteredFolders)
                    foldersLiveData.postValue(filteredFolders)
                    loading.postValue(false)
                }
            }
        }
    }

    fun addOrUpdateNote(note: Note) {
        currentUserId?.let { userId ->
            loading.value = true
            viewModelScope.launch {
                if (note.id.isEmpty()) {
                    dbHelper.addNote(note, userId) { success ->
                        if (success) {
                            selectedFolder.value?.let { fetchNotesForFolder(it.id) }  // Refresh list after adding
                            operationStatus.postValue("Note added successfully")
                        } else {
                            operationStatus.postValue("Failed to add note")
                        }
                    }
                } else {
                    dbHelper.updateNote(note.id, note) { success ->
                        if (success) {
                            selectedFolder.value?.let { fetchNotesForFolder(it.id) }
                            operationStatus.postValue("Note updated successfully")
                        } else {
                            operationStatus.postValue("Failed to update note")
                        }
                    }
                }
                loading.postValue(false)
            }
        }
    }

    fun deleteSelectedNote() {
        selectedNote.value?.id?.let { noteId ->
            deleteNote(noteId)
        }
    }

    fun deleteNote(noteId: String) {
        loading.value = true
        viewModelScope.launch {
            dbHelper.deleteNote(noteId) { success ->
                if (success) {
                    notesLiveData.value = notesLiveData.value?.filterNot { it.id == noteId }
                    selectedNote.postValue(null)  // Clear selection after deletion
                    operationStatus.postValue("Note deleted successfully")
                } else {
                    operationStatus.postValue("Failed to delete note")
                }
                loading.postValue(false)
            }
        }
    }

    fun renameSelectedFolder(newName: String) {
        selectedFolder.value?.id?.let { folderId ->
            renameFolder(folderId, newName)
        }
    }


    fun renameFolder(folderId: String, newName: String) {

        if (folderId == defaultFolderId) {
            operationStatus.postValue("Cannot Rename Default Folder.")
            messageLiveData.postValue("Cannot Rename Default Folder.")
            return
        }

        loading.value = true
        viewModelScope.launch {
            dbHelper.renameFolder(folderId, newName) { success ->
                if (success) {
                    // Check if the currently selected folder is the one being renamed
                    selectedFolder.value?.let {
                        if (it.id == folderId) {
                            // Update the folder name
                            val updatedFolder = it.copy(name = newName)
                            selectedFolder.postValue(updatedFolder)
                        }
                    }
                    fetchFolders()  // Refresh list after renaming
                    operationStatus.postValue("Folder renamed successfully")
                } else {
                    operationStatus.postValue("Failed to rename folder")
                }
                loading.postValue(false)
            }
        }
    }

    fun deleteFolder(folderId: String) {
        if (folderId == defaultFolderId) {
            operationStatus.postValue("Cannot delete the default folder.")
            messageLiveData.postValue("Cannot delete the default folder.")
            return
        }

        loading.value = true
        viewModelScope.launch {
            dbHelper.deleteFolder(folderId) { success ->
                if (success) {
                    foldersLiveData.value = foldersLiveData.value?.filterNot { it.id == folderId }
                    // Navigate to default folder after deletion
                    selectedFolder.postValue(defaultFolder.value)
                    defaultFolder.value?.id?.let {
                        fetchNotesForFolder(it)
                    }
                    operationStatus.postValue("Folder deleted successfully")
                } else {
                    operationStatus.postValue("Failed to delete folder")
                }
                loading.postValue(false)
            }
        }
    }

}