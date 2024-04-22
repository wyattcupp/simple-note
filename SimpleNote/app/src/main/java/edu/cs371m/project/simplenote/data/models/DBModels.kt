package edu.cs371m.project.simplenote.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// TODO: Maybe not use this...?
data class User(
    val username: String = "",
    val email: String = ""
)

data class Note(
    var id: String = "",
    var folderId: String = "",
    val title: String = "",
    val content: String = "",
    var createdBy: String = "",
    @ServerTimestamp val createdTimestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @DocumentId var firestoreId: String = ""
)

data class Folder(
    @DocumentId var id: String = "",  // Firestore will auto-populate this when reading documents
    var name: String = "",
    var createdBy: String = "",
    @ServerTimestamp val createdTimestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class Permission(
    val canEdit: Boolean = false,
    val canView: Boolean = false
)
