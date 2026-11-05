package com.example.todolist

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class Todo(val id: String, val title: String, val done: Boolean = false)

/**
 * Stochează task-urile în Firestore: users/{uid}/todos/{id}.
 * Autentificare anonimă; Firestore ține cache offline și sincronizează automat.
 */
class TodoViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var collection: CollectionReference? = null
    private var listener: ListenerRegistration? = null

    val todos = mutableStateListOf<Todo>()
    val error = mutableStateOf<String?>(null)

    init {
        val user = auth.currentUser
        if (user != null) {
            listen(user.uid)
        } else {
            auth.signInAnonymously()
                .addOnSuccessListener { listen(it.user!!.uid) }
                .addOnFailureListener { error.value = "Autentificare eșuată: ${it.message}" }
        }
    }

    private fun listen(uid: String) {
        val col = db.collection("users").document(uid).collection("todos")
        collection = col
        listener = col.orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    error.value = "Eroare Firestore: ${e.message}"
                    return@addSnapshotListener
                }
                error.value = null
                todos.clear()
                snap?.documents?.forEach {
                    todos.add(
                        Todo(
                            id = it.id,
                            title = it.getString("title") ?: "",
                            done = it.getBoolean("done") ?: false
                        )
                    )
                }
            }
    }

    fun add(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        collection?.add(
            mapOf(
                "title" to clean,
                "done" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun toggle(id: String) {
        val todo = todos.firstOrNull { it.id == id } ?: return
        collection?.document(id)?.update("done", !todo.done)
    }

    fun delete(id: String) {
        collection?.document(id)?.delete()
    }

    fun clearDone() {
        val col = collection ?: return
        val batch = db.batch()
        todos.filter { it.done }.forEach { batch.delete(col.document(it.id)) }
        batch.commit()
    }

    override fun onCleared() {
        listener?.remove()
    }
}
