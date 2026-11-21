package com.example.data.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val text: String) : AiResponseState
    data class Success(val text: String) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

enum class ExpertMode(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val roleInstruction: String
) {
    GENERAL(
        id = "GENERAL",
        titleEn = "NEXORA Core",
        titleBn = "নেক্সোরা কোর",
        roleInstruction = "You are NEXORA AI, a warm, intelligent bilingual (Bangla & English) personal life assistant."
    ),
    DOCTOR_INFO(
        id = "DOCTOR_INFO",
        titleEn = "Health Info",
        titleBn = "স্বাস্থ্য তথ্য",
        roleInstruction = "You are NEXORA Health Information Assistant. Provide clear, evidence-based health & medicine education. Always clarify you are not a licensed doctor and urgent symptoms require emergency care."
    ),
    TEACHER(
        id = "TEACHER",
        titleEn = "AI Teacher",
        titleBn = "এআই শিক্ষক",
        roleInstruction = "You are NEXORA Personal Teacher. Explain concepts step-by-step clearly for students in Bangladesh and globally, supporting HSC/SSC, university, nursing/medical, and BCS/job preparation."
    ),
    FINANCIAL(
        id = "FINANCIAL",
        titleEn = "Finance Advisor",
        titleBn = "অর্থ উপদেষ্টা",
        roleInstruction = "You are NEXORA Financial Assistant. Help with personal budgeting, savings, and expense optimization using Bangladeshi Taka (BDT ৳) alongside practical financial literacy."
    ),
    CAREER(
        id = "CAREER",
        titleEn = "Career Expert",
        titleBn = "ক্যারিয়ার এক্সপার্ট",
        roleInstruction = "You are NEXORA Career & Communication Expert. Help craft CVs, cover letters, interview answers, and Bangladesh job preparation strategies."
    ),
    TRAVEL(
        id = "TRAVEL",
        titleEn = "Travel Planner",
        titleBn = "ভ্রমণ গাইড",
        roleInstruction = "You are NEXORA Travel Expert. Design practical itineraries, hotel & transport guides, safety tips, and realistic BDT travel budgets."
    );

    companion object {
        fun fromId(raw: String?): ExpertMode =
            entries.firstOrNull { it.id == raw } ?: GENERAL
    }
}

enum class ToolModule(
    val route: String,
    val titleEn: String,
    val titleBn: String,
    val subtitleEn: String,
    val subtitleBn: String,
    val categoryEn: String,
    val categoryBn: String
) {
    LIFE_MANAGER("tool_life", "Smart Life Manager", "স্মার্ট লাইফ ম্যানেজার", "Tasks, Routine, Calendar & AI Plan", "কাজ, রুটিন, ক্যালেন্ডার ও প্ল্যান", "Productivity", "দৈনন্দিন"),
    HEALTH_ASSISTANT("tool_health", "AI Health Assistant", "স্বাস্থ্য সহায়ক", "Reports, Medicines & First Aid", "রিপোর্ট, ওষুধ রিমাইন্ডার ও ফার্স্ট এইড", "Wellness", "স্বাস্থ্য"),
    EDUCATION_CENTER("tool_education", "AI Education Center", "শিক্ষা কেন্দ্র", "Teacher, Photo Solve, MCQ & English", "শিক্ষক, ছবি সমাধান ও MCQ টেস্ট", "Learning", "শিক্ষা"),
    FINANCE_MANAGER("tool_finance", "Smart Finance (BDT)", "স্মার্ট ফাইন্যান্স (৳)", "Income, Expenses, Goals & AI Audit", "আয়-ব্যয়, বাজেট ও সঞ্চয় লক্ষ্য", "Finance", "অর্থ"),
    DOCUMENT_AI("tool_document", "Document AI", "ডকুমেন্ট AI", "PDF Reader, OCR, CV & Summary", "PDF রিডার, স্ক্যান ও সিভি তৈরি", "Productivity", "দৈনন্দিন"),
    TRANSLATOR("tool_translator", "AI Translator", "এআই অনুবাদক", "Bangla↔English, Voice & Grammar", "বাংলা↔ইংরেজি, ভয়েস ও গ্রামার", "Language", "ভাষা"),
    EMERGENCY_SAFETY("tool_emergency", "Emergency & SOS", "জরুরি ও নিরাপত্তা", "SOS, Location, Hospitals & Blood", "SOS, লোকেশন, হাসপাতাল ও রক্তদাতা", "Safety", "নিরাপত্তা"),
    AI_VISION("tool_vision", "AI Vision", "এআই ভিশন", "Identify Objects, Text & Food", "ছবি চেনা, লেখা পড়া ও খাবার বিশ্লেষণ", "Intelligence", "বুদ্ধিমত্তা"),
    SHOPPING_ASSISTANT("tool_shopping", "AI Shopping", "শপিং অ্যাসিস্ট্যান্ট", "Compare, Budget Picks & List", "তুলনা, বাজেট পরামর্শ ও শপিং লিস্ট", "Lifestyle", "জীবনযাত্রা"),
    TRAVEL_PLANNER("tool_travel", "AI Travel Planner", "ভ্রমণ পরিকল্পনাকারী", "Tour Plans, Budget & Maps", "ট্যুর প্ল্যান, বাজেট ও ম্যাপ", "Lifestyle", "জীবনযাত্রা"),
    SMART_COMMUNICATION("tool_comms", "Smart Writing", "স্মার্ট যোগাযোগ", "Emails, Captions, Grammar & Tone", "ইমেইল, ক্যাপশন ও টোন পরিবর্তন", "Productivity", "দৈনন্দিন"),
    SMART_HOME("tool_smarthome", "Smart Home", "স্মার্ট হোম", "Lights, AC, Devices & Routines", "লাইট, এসি, ডিভাইস ও অটোমেশন", "Automation", "অটোমেশন"),
    MEETING_INTELLIGENCE("tool_meeting", "Meeting & Lecture AI", "মিটিং ও লেকচার AI", "Transcript, Summary & Tasks", "ট্রান্সক্রিপ্ট, সারাংশ ও কাজ", "Intelligence", "বুদ্ধিমত্তা"),
    FAMILY_SPACE("tool_family", "Family AI Space", "ফ্যামিলি স্পেস", "Shared Calendar, List & Contacts", "পরিবারের শেয়ার্ড ক্যালেন্ডার ও লিস্ট", "Social", "পরিবার"),
    MEMORY_TWIN("tool_memory", "Memory & Digital Twin", "পার্সোনাল মেমোরি", "Preferences, Goals & Predictive AI", "পছন্দ, লক্ষ্য ও প্রেডিক্টিভ এআই", "Intelligence", "বুদ্ধিমত্তা");

    companion object {
        fun fromRoute(route: String?): ToolModule? = entries.firstOrNull { it.route == route }
    }
}

data class ProposedTask(
    val title: String,
    val category: String,
    val priority: String,
    val dueDateLabel: String,
    val timeLabel: String
)

data class ProposedExpense(
    val title: String,
    val amount: Double,
    val txType: String,
    val category: String
)

data class ProposedMemory(
    val title: String,
    val content: String,
    val memoryType: String
)

data class AgentActionProposal(
    val replyText: String,
    val summaryDescription: String,
    val tasks: List<ProposedTask> = emptyList(),
    val expenses: List<ProposedExpense> = emptyList(),
    val memories: List<ProposedMemory> = emptyList()
) {
    val hasActions: Boolean
        get() = tasks.isNotEmpty() || expenses.isNotEmpty() || memories.isNotEmpty()
}

data class McqQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class DailyPlanItem(
    val timeSlot: String,
    val activity: String,
    val priority: String,
    val tip: String
)

data class TravelDayPlan(
    val dayTitle: String,
    val activities: String,
    val estimatedCostBdt: Double
)

data class TravelItinerary(
    val destination: String,
    val totalEstimatedBdt: Double,
    val hotelSuggestions: String,
    val checklist: List<String>,
    val days: List<TravelDayPlan>
)
