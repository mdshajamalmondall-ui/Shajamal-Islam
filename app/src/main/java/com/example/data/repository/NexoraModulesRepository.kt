package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.EmergencyContact
import com.example.data.model.FamilyItem
import com.example.data.model.FamilySpace
import com.example.data.model.FinanceTransaction
import com.example.data.model.HealthRecord
import com.example.data.model.SavingsGoal
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartDevice
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

class NexoraModulesRepository(
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

    // --- Finance Transactions & Savings Goals ---
    fun observeTransactions(userId: String): Flow<List<FinanceTransaction>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(FinanceTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addTransaction(
        title: String,
        amount: Double,
        txType: String,
        category: String,
        dateLabel: String,
        notes: String = ""
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().take(160)
            require(safeTitle.isNotEmpty()) { "Title cannot be empty" }
            require(amount > 0.0) { "Amount must be greater than zero" }
            val safeTxType = txType.trim().uppercase().let {
                if (it in setOf("INCOME", "EXPENSE", "BILL")) it else "EXPENSE"
            }
            val ref = userDoc(uid).collection("transactions").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "amount" to amount.coerceAtMost(100_000_000.0),
                    "txType" to safeTxType,
                    "category" to category.trim().take(60).ifBlank { "General" },
                    "dateLabel" to dateLabel.trim().take(40).ifBlank { "Today" },
                    "notes" to notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> = flow {
        val path = "users/$userId/savings_goals"
        emitAll(
            userDoc(userId).collection("savings_goals")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(SavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addSavingsGoal(title: String, targetAmount: Double, savedAmount: Double, deadlineLabel: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savings_goals"
        return try {
            val uid = requireUserId()
            require(targetAmount > 0.0)
            val ref = userDoc(uid).collection("savings_goals").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().take(160),
                    "targetAmount" to targetAmount,
                    "savedAmount" to savedAmount.coerceAtLeast(0.0),
                    "deadlineLabel" to deadlineLabel.trim().take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun contributeToGoal(goalId: String, newSavedAmount: Double): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/savings_goals/$goalId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("savings_goals").document(goalId)
            path = ref.path
            ref.update(
                mapOf(
                    "savedAmount" to newSavedAmount.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
    }

    // --- Health & Medicine Tracker ---
    fun observeHealthRecords(userId: String): Flow<List<HealthRecord>> = flow {
        val path = "users/$userId/health_records"
        emitAll(
            userDoc(userId).collection("health_records")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(HealthRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addHealthRecord(
        title: String,
        recordType: String,
        dosageOrValue: String,
        scheduleTime: String,
        notes: String = ""
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/health_records"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().take(200)
            require(safeTitle.isNotEmpty()) { "Title cannot be empty" }
            val safeRecordType = recordType.trim().uppercase().let {
                if (it in setOf("MEDICINE", "REPORT", "CHECKUP", "FIRST_AID")) it else "MEDICINE"
            }
            val ref = userDoc(uid).collection("health_records").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "recordType" to safeRecordType,
                    "dosageOrValue" to dosageOrValue.trim().take(120),
                    "scheduleTime" to scheduleTime.trim().take(60),
                    "takenToday" to false,
                    "notes" to notes.trim().take(5000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun toggleMedicineTaken(recordId: String, takenToday: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/health_records/$recordId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("health_records").document(recordId)
            path = ref.path
            ref.update(mapOf("takenToday" to takenToday, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
    }

    // --- Shopping Items ---
    fun observeShoppingItems(userId: String): Flow<List<ShoppingItem>> = flow {
        val path = "users/$userId/shopping_items"
        emitAll(
            userDoc(userId).collection("shopping_items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(ShoppingItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addShoppingItem(name: String, estimatedPriceBdt: Double, category: String, notes: String = ""): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/shopping_items"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("shopping_items").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "name" to name.trim().take(160),
                    "estimatedPriceBdt" to estimatedPriceBdt.coerceAtLeast(0.0),
                    "category" to category.trim().take(60).ifBlank { "General" },
                    "purchased" to false,
                    "notes" to notes.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun toggleShoppingItem(itemId: String, purchased: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/shopping_items/$itemId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("shopping_items").document(itemId)
            path = ref.path
            ref.update(mapOf("purchased" to purchased, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
    }

    // --- Emergency Contacts & Blood Donors ---
    fun observeEmergencyContacts(userId: String): Flow<List<EmergencyContact>> = flow {
        val path = "users/$userId/emergency_contacts"
        emitAll(
            userDoc(userId).collection("emergency_contacts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(EmergencyContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addEmergencyContact(name: String, phone: String, relation: String, bloodGroup: String, bloodDonor: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/emergency_contacts"
        return try {
            val uid = requireUserId()
            val safeName = name.trim().take(100)
            val safePhone = phone.trim().take(30)
            require(safeName.isNotEmpty() && safePhone.length >= 3) { "Valid name and phone number required" }
            val ref = userDoc(uid).collection("emergency_contacts").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "phone" to safePhone,
                    "relation" to relation.trim().take(60),
                    "bloodGroup" to bloodGroup.trim().take(10),
                    "bloodDonor" to bloodDonor,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    // --- Smart Home Devices ---
    fun observeSmartDevices(userId: String): Flow<List<SmartDevice>> = flow {
        val path = "users/$userId/smart_devices"
        emitAll(
            userDoc(userId).collection("smart_devices")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(SmartDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun addSmartDevice(name: String, room: String, deviceType: String, level: Long, automationRule: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/smart_devices"
        return try {
            val uid = requireUserId()
            val safeName = name.trim().take(100)
            require(safeName.isNotEmpty()) { "Device name cannot be empty" }
            val safeType = deviceType.trim().uppercase().let {
                if (it in setOf("LIGHT", "AC", "FAN", "LOCK", "PLUG")) it else "LIGHT"
            }
            val ref = userDoc(uid).collection("smart_devices").document()
            path = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "room" to room.trim().take(60).ifBlank { "Living Room" },
                    "deviceType" to safeType,
                    "powerOn" to true,
                    "level" to level.coerceIn(0L, 100L),
                    "automationRule" to automationRule.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun updateSmartDevice(deviceId: String, powerOn: Boolean, level: Long): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/smart_devices/$deviceId"
        return try {
            val uid = requireUserId()
            val ref = userDoc(uid).collection("smart_devices").document(deviceId)
            path = ref.path
            ref.update(
                mapOf(
                    "powerOn" to powerOn,
                    "level" to level.coerceIn(0L, 100L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
    }

    // --- Family AI Space (Shared Collaboration) ---
    fun observeFamilySpaces(userId: String): Flow<List<FamilySpace>> = flow {
        val path = "family_spaces"
        emitAll(
            db.collection("family_spaces")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { it.toObjects(FamilySpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    fun observeFamilyItems(spaceId: String, userId: String): Flow<List<FamilyItem>> = flow {
        val path = "family_spaces/$spaceId/items"
        emitAll(
            db.collection("family_spaces").document(spaceId).collection("items")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { it.toObjects(FamilyItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> unwrapAndLog(error, OperationType.LIST, path, userId); throw error }
        )
    }

    suspend fun createFamilySpace(name: String, inviteCode: String): Result<Unit> {
        val cleanCode = inviteCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        var path = "family_spaces/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.length in 4..20) { "Invite code must be 4-20 alphanumeric characters" }
            val ref = db.collection("family_spaces").document(cleanCode)
            path = ref.path
            ref.set(
                mapOf(
                    "name" to name.trim().take(100),
                    "ownerId" to uid,
                    "inviteCode" to cleanCode,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun joinFamilySpace(inviteCode: String): Result<Unit> {
        val cleanCode = inviteCode.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
        var path = "family_spaces/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.length in 4..20) { "Invalid invite code" }
            val ref = db.collection("family_spaces").document(cleanCode)
            path = ref.path
            ref.update(
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
    }

    suspend fun addFamilyItem(space: FamilySpace, title: String, itemType: String, details: String): Result<Unit> {
        var path = "family_spaces/${space.id}/items"
        return try {
            val uid = requireUserId()
            val ref = db.collection("family_spaces").document(space.id).collection("items").document()
            path = ref.path
            ref.set(
                mapOf(
                    "spaceId" to space.id,
                    "authorId" to uid,
                    "memberUids" to space.memberUids,
                    "title" to title.trim().take(200),
                    "itemType" to itemType,
                    "completed" to false,
                    "details" to details.trim().take(1000),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.CREATE, path); Result.failure(e) }
    }

    suspend fun toggleFamilyItem(spaceId: String, itemId: String, completed: Boolean): Result<Unit> {
        var path = "family_spaces/$spaceId/items/$itemId"
        return try {
            requireUserId()
            val ref = db.collection("family_spaces").document(spaceId).collection("items").document(itemId)
            path = ref.path
            ref.update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { handleFirestoreError(e, OperationType.UPDATE, path); Result.failure(e) }
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
