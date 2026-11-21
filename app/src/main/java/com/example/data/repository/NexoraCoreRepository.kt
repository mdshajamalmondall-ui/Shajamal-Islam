package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessageRecord
import com.example.data.model.ConversationSummary
import com.example.data.model.LifeTask
import com.example.data.model.PersonalMemory
import com.example.data.model.SavedDocument
import com.example.data.model.UserProfile
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NexoraCoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId).snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    unwrapAndLog(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        defaultLanguage: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid)
            path = ref.path
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().take(100).ifBlank { "NEXORA User" }
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(120),
                    "language" to if (defaultLanguage in listOf("bn", "en")) defaultLanguage else "bn",
                    "planTier" to "FREE",
                    "monthlyBudgetBdt" to 25000.0,
                    "offlineSmartMode" to true,
                    "darkTheme" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        language: String? = null,
        planTier: String? = null,
        monthlyBudgetBdt: Double? = null,
        offlineSmartMode: Boolean? = null,
        darkTheme: Boolean? = null
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid)
            path = ref.path
            val updates = buildMap<String, Any> {
                if (language != null) put("language", language)
                if (planTier != null) put("planTier", planTier)
                if (monthlyBudgetBdt != null) put("monthlyBudgetBdt", monthlyBudgetBdt.coerceIn(0.0, 100_000_000.0))
                if (offlineSmartMode != null) put("offlineSmartMode", offlineSmartMode)
                if (darkTheme != null) put("darkTheme", darkTheme)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Conversations & Messages ---
    fun observeConversations(userId: String): Flow<List<ConversationSummary>> = flow {
        val path = "users/$userId/conversations"
        emitAll(
            userDoc(userId).collection("conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { it.toObjects(ConversationSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessageRecord>> = flow {
        val path = "users/$userId/conversations/$conversationId/messages"
        emitAll(
            userDoc(userId).collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(ChatMessageRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveChatExchange(
        conversationId: String?,
        userText: String,
        modelText: String,
        expertMode: String,
        hasImage: Boolean
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/conversations"
        return try {
            val uid = requireUserId()
            val convCol = userDoc(uid).collection("conversations")
            val convRef = if (!conversationId.isNullOrBlank()) convCol.document(conversationId) else convCol.document()
            path = convRef.path
            val isNew = conversationId.isNullOrBlank() || !convRef.get().await().exists()

            val safeTitle = userText.trim().take(120).ifBlank { "NEXORA Chat" }
            val safeLastMsg = modelText.trim().take(480)
            if (isNew) {
                convRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "expertMode" to expertMode.take(60),
                        "lastMessage" to safeLastMsg,
                        "messageCount" to 2L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                convRef.update(
                    mapOf(
                        "expertMode" to expertMode.take(60),
                        "lastMessage" to safeLastMsg,
                        "messageCount" to FieldValue.increment(2L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val msgsCol = convRef.collection("messages")
            msgsCol.document().set(
                mapOf(
                    "userId" to uid,
                    "role" to "user",
                    "text" to userText.trim().take(29000).ifBlank { "(Image query)" },
                    "expertMode" to expertMode.take(60),
                    "hasImage" to hasImage,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            msgsCol.document().set(
                mapOf(
                    "userId" to uid,
                    "role" to "model",
                    "text" to modelText.trim().take(29000).ifBlank { "..." },
                    "expertMode" to expertMode.take(60),
                    "hasImage" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Smart Life Manager Tasks ---
    fun observeTasks(userId: String): Flow<List<LifeTask>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            userDoc(userId).collection("tasks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(LifeTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addTask(
        title: String,
        category: String,
        priority: String,
        dueDateLabel: String,
        timeLabel: String,
        notes: String = ""
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("tasks").document()
            path = ref.path
            val safeTitle = title.trim().take(200)
            require(safeTitle.isNotEmpty()) { "Title cannot be empty" }
            val safeCategory = category.trim().uppercase().let {
                if (it in setOf("TASK", "ROUTINE", "EVENT", "BIRTHDAY", "MEDICINE")) it else "TASK"
            }
            val safePriority = priority.trim().uppercase().let {
                if (it in setOf("HIGH", "MEDIUM", "LOW")) it else "MEDIUM"
            }
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "category" to safeCategory,
                "priority" to safePriority,
                "dueDateLabel" to dueDateLabel.trim().take(60).ifBlank { "Today" },
                "timeLabel" to timeLabel.trim().take(40).ifBlank { "09:00 AM" },
                "completed" to false,
                "notes" to notes.trim().take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTask(taskId: String, completed: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tasks/$taskId"
        return try {
            val uid = requireUserId()
            require(taskId.isNotBlank())
            val ref = userDoc(uid).collection("tasks").document(taskId)
            path = ref.path
            ref.update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = deleteUserDoc("tasks", taskId)

    // --- Personal Memories & Digital Twin ---
    fun observeMemories(userId: String): Flow<List<PersonalMemory>> = flow {
        val path = "users/$userId/memories"
        emitAll(
            userDoc(userId).collection("memories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(PersonalMemory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun addMemory(title: String, content: String, memoryType: String, pinned: Boolean = false): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/memories"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("memories").document()
            path = ref.path
            val safeTitle = title.trim().take(160)
            val safeContent = content.trim().take(3000)
            require(safeTitle.isNotEmpty() && safeContent.isNotEmpty())
            val safeMemoryType = memoryType.trim().uppercase().let {
                if (it in setOf("PREFERENCE", "GOAL", "NOTE", "HABIT", "KNOWLEDGE")) it else "NOTE"
            }
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "content" to safeContent,
                    "memoryType" to safeMemoryType,
                    "pinned" to pinned,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemory(memoryId: String, title: String, content: String, pinned: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/memories/$memoryId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("memories").document(memoryId)
            path = ref.path
            ref.update(
                mapOf(
                    "title" to title.trim().take(160),
                    "content" to content.trim().take(3000),
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Unit> = deleteUserDoc("memories", memoryId)

    // --- Saved Documents (Document AI, CVs, Meeting Notes, Travel Plans) ---
    fun observeSavedDocuments(userId: String): Flow<List<SavedDocument>> = flow {
        val path = "users/$userId/saved_docs"
        emitAll(
            userDoc(userId).collection("saved_docs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(SavedDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLog(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveDocument(title: String, docType: String, content: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/saved_docs"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("saved_docs").document()
            path = ref.path
            val safeDocType = docType.trim().uppercase().let {
                if (it in setOf("SUMMARY", "CV", "MEETING", "STUDY_NOTE", "TRANSLATION", "TRAVEL_PLAN")) it else "SUMMARY"
            }
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().take(200).ifBlank { "Document" },
                    "docType" to safeDocType,
                    "content" to content.trim().take(29000).ifBlank { "-" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedDocument(docId: String): Result<Unit> = deleteUserDoc("saved_docs", docId)

    // --- Account Data Wipe ---
    suspend fun deleteAllUserData(): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val subCollections = listOf(
                "tasks", "transactions", "savings_goals", "health_records",
                "memories", "shopping_items", "emergency_contacts",
                "smart_devices", "saved_docs"
            )
            for (col in subCollections) {
                val docs = userDoc(uid).collection(col).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            val convs = userDoc(uid).collection("conversations").get().await()
            for (conv in convs.documents) {
                val msgs = conv.reference.collection("messages").get().await()
                for (m in msgs.documents) m.reference.delete().await()
                conv.reference.delete().await()
            }
            userDoc(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserDoc(collectionName: String, docId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$docId"
        return try {
            val uid = requireUserId()
            require(docId.isNotBlank())
            val ref = userDoc(uid).collection(collectionName).document(docId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun unwrapAndLog(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
