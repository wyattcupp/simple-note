package edu.cs371m.project.simplenote.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import edu.cs371m.project.simplenote.data.models.Folder
import edu.cs371m.project.simplenote.data.models.Note
import edu.cs371m.project.simplenote.data.models.Permission

class ViewModelDBHelper {

    private val db = FirebaseFirestore.getInstance()

    // Add a new note to Firestore and set initial permissions
    fun addNote(note: Note, userId: String, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "addNote user=$userId, note=$note")

        db.collection("notes").add(note)
            .addOnSuccessListener { documentReference ->
                val noteId = documentReference.id
                Log.d("DBHelper", "Note added with ID: ${documentReference.id}")
                setPermission(noteId, userId, Permission(canEdit = true, canView = true))
                documentReference.update("id", noteId)
                completion(true)
                Log.d("DBHelper", "addNote - Success")
            }
            .addOnFailureListener {
                completion(false)
                Log.d("DBHelper", "Failed to add note: ${it.message}", it)
            }
    }

    // Fetch all notes that a user has access to
    fun getNotes(userId: String, callback: (List<Note>?) -> Unit) {
        db.collection("notes")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { documents ->
                val notes =
                    documents.mapNotNull { it.toObject(Note::class.java).apply { id = it.id } }
                callback(notes)
                Log.d("DBHelper", "getNotes - Success")
            }
            .addOnFailureListener {
                callback(null)
                Log.d("DBHelper", "getNotes - Failure -> $it.toString()")
            }
    }

    fun getNotesForFolder(userId: String, folderId: String, callback: (List<Note>?) -> Unit) {
        db.collection("notes")
            .whereEqualTo("createdBy", userId)
            .whereEqualTo("folderId", folderId)
            .get()
            .addOnSuccessListener { documents ->
                val notes =
                    documents.mapNotNull { it.toObject(Note::class.java).apply { id = it.id } }
                callback(notes)
            }
            .addOnFailureListener { e ->
                Log.e("DBHelper", "Error fetching notes for folder $folderId", e)
                callback(null)
            }
    }

    // Update an existing note
    fun updateNote(noteId: String, note: Note, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "updateNote -> noteId=$noteId, note=$note")

        db.collection("notes").document(noteId).update(
            "title", note.title,
            "content", note.content,
            "updatedAt", FieldValue.serverTimestamp()
        )
            .addOnSuccessListener { completion(true) }
            .addOnFailureListener { completion(false) }
    }

    // Delete a specific note
    fun deleteNote(noteId: String, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "deleteNote -> noteId=$noteId")

        db.collection("notes").document(noteId).delete()
            .addOnSuccessListener { completion(true) }
            .addOnFailureListener { completion(false) }
    }

    // Set permissions for a note
    fun setPermission(noteId: String, userId: String, permission: Permission) {
        Log.d("DBHelper", "setPermission -> userId=$userId, noteId=$noteId")

        db.collection("notes").document(noteId)
            .collection("permissions").document(userId)
            .set(permission)
    }

    fun addFolder(folder: Folder, userId: String, completion: (Boolean, String) -> Unit) {
        folder.createdBy = userId  // Ensure the createdBy is properly set

        db.collection("folders").add(folder)
            .addOnSuccessListener { documentReference ->
                val folderId = documentReference.id
                setFolderPermission(folderId, userId, Permission(canEdit = true, canView = true))
                completion(true, folderId)  // Return the new folder ID for reference
            }
            .addOnFailureListener { e ->
                completion(false, "")
                Log.e("DBHelper", "Failed to add folder: ${e.message}", e)
            }
    }

    // Fetch all folders that a user has access to
    fun getFolders(userId: String, callback: (List<Folder>?) -> Unit) {
        Log.d("DBHelper", "getFolders -> userId=$userId")

        db.collection("folders")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { documents ->
                val folders =
                    documents.mapNotNull { it.toObject(Folder::class.java).apply { id = it.id } }
                callback(folders)
                Log.d("DBHelper", "getFolders - Success -> Folders=$folders")
            }
            .addOnFailureListener {
                Log.d("DBHelper", "Failure -> ${it.message}")
                callback(null)
            }
    }

    // Update an existing folder
    fun updateFolder(folderId: String, folder: Folder, completion: (Boolean) -> Unit) {
        db.collection("folders").document(folderId).update(
            "name", folder.name,
            "updatedTimestamp", FieldValue.serverTimestamp()
        )
            .addOnSuccessListener { completion(true) }
            .addOnFailureListener { completion(false) }
    }

    // Delete a specific folder and all notes within it
    fun deleteFolder(folderId: String, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "deleteFolder -> folderId=$folderId")

        // First, delete all notes in the folder
        deleteNotesInFolder(folderId) { notesDeleted ->
            if (notesDeleted) {
                // Only if all notes are successfully deleted, delete the folder
                db.collection("folders").document(folderId).delete()
                    .addOnSuccessListener {
                        Log.d("DBHelper", "Folder deleted successfully")
                        completion(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "DBHelper",
                            "Failed to delete folder after deleting notes: ${e.message}",
                            e
                        )
                        completion(false)
                    }
            } else {
                Log.e("DBHelper", "Failed to delete all notes in the folder")
                completion(false)
            }
        }
    }

    // Helper function to delete all notes in a specified folder
    private fun deleteNotesInFolder(folderId: String, completion: (Boolean) -> Unit) {
        db.collection("notes")
            .whereEqualTo("folderId", folderId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("DBHelper", "All notes in folder $folderId deleted successfully")
                        completion(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "DBHelper",
                            "Error deleting notes in folder $folderId: ${e.message}",
                            e
                        )
                        completion(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(
                    "DBHelper",
                    "Failed to fetch notes for deletion in folder $folderId: ${e.message}",
                    e
                )
                completion(false)
            }
    }

    // Set permissions for a folder
    fun setFolderPermission(folderId: String, userId: String, permission: Permission) {
        Log.d("DBHelper", "setFolderPermission -> userId=$userId, folderId=$folderId")

        db.collection("folders").document(folderId)
            .collection("permissions").document(userId)
            .set(permission)
    }

    // Get a single note by ID
    fun getNoteById(noteId: String, callback: (Note?) -> Unit) {
        db.collection("notes").document(noteId).get()
            .addOnSuccessListener { document ->
                callback(document.toObject(Note::class.java)?.apply { id = document.id })
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Delete all notes for a given user
    fun deleteAllNotes(userId: String, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "deleteAllNotes -> userId=$userId")

        db.collection("notes")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                documents.forEach { documentSnapshot ->
                    batch.delete(documentSnapshot.reference)
                }
                batch.commit()
                    .addOnSuccessListener { completion(true) }
                    .addOnFailureListener { completion(false) }
            }
            .addOnFailureListener { completion(false) }
    }

    // Rename a folder
    fun renameFolder(folderId: String, newName: String, completion: (Boolean) -> Unit) {
        Log.d("DBHelper", "renameFolder -> folderId=$folderId, newName=$newName")

        db.collection("folders").document(folderId)
            .update("name", newName)
            .addOnSuccessListener { completion(true) }
            .addOnFailureListener { completion(false) }
    }


    fun ensureDefaultFolder(userId: String, completion: (Boolean, String) -> Unit) {
        db.collection("folders")
            .whereEqualTo("createdBy", userId)
            .whereEqualTo("name", "Default")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Folder does not exist, create it
                    val defaultFolder = Folder(name = "Default", createdBy = userId)
                    addFolder(defaultFolder, userId) { success, folderId ->
                        completion(success, folderId)  // Return the success state and folderId
                    }
                } else {
                    // Folder exists, return its ID
                    val folderId = documents.documents.first().id
                    completion(true, folderId)
                }
                Log.d("DBHelper", "ensureDefaultFolder - Success")
            }
            .addOnFailureListener {
                completion(false, "")
                Log.d("DBHelper", "ensureDefaultFolder - Failure -> ${it.message}")
            }
    }

    fun deleteNotesInFolder(userId: String, folderId: String, completion: (Boolean) -> Unit) {
        db.collection("notes")
            .whereEqualTo("createdBy", userId)
            .whereEqualTo("folderId", folderId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit().addOnSuccessListener {
                    completion(true)
                }.addOnFailureListener {
                    completion(false)
                }
            }
            .addOnFailureListener {
                completion(false)
            }
    }
}
