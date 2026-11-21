package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val language: String = "bn",
    val planTier: String = "FREE",
    val monthlyBudgetBdt: Double = 25000.0,
    val offlineSmartMode: Boolean = true,
    val darkTheme: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ConversationSummary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val expertMode: String = "GENERAL",
    val lastMessage: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val expertMode: String = "GENERAL",
    val hasImage: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LifeTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "TASK",
    val priority: String = "MEDIUM",
    val dueDateLabel: String = "Today",
    val timeLabel: String = "09:00 AM",
    val completed: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FinanceTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val txType: String = "EXPENSE",
    val category: String = "General",
    val dateLabel: String = "Today",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 10000.0,
    val savedAmount: Double = 0.0,
    val deadlineLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HealthRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val recordType: String = "MEDICINE",
    val dosageOrValue: String = "",
    val scheduleTime: String = "",
    val takenToday: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PersonalMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val memoryType: String = "NOTE",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShoppingItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val estimatedPriceBdt: Double = 0.0,
    val category: String = "Groceries",
    val purchased: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EmergencyContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val relation: String = "",
    val bloodGroup: String = "O+",
    val bloodDonor: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SmartDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val room: String = "Living Room",
    val deviceType: String = "LIGHT",
    val powerOn: Boolean = false,
    val level: Long = 70L,
    val automationRule: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val docType: String = "SUMMARY",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilySpace(
    @DocumentId val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FamilyItem(
    @DocumentId val id: String = "",
    val spaceId: String = "",
    val authorId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val itemType: String = "REMINDER",
    val completed: Boolean = false,
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
