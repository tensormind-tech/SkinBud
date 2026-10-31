package com.skinbud.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RoutineTime(val displayName: String) {
    MORNING("Morning"),
    EVENING("Evening"),
    WEEKLY("Weekly Treatment")
}

@Serializable
data class RoutineSchedule(
    val frequency: String = "daily",
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val timesOfDay: List<String> = listOf("morning", "evening")
)

@Serializable
data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val time: RoutineTime = RoutineTime.MORNING,
    val order: Int = 1,
    val product: ShelfProduct? = null,
    val stepTip: String = "",
    val durationSeconds: Int = 60,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Serializable
data class ActiveRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Core Daily Glow Routine",
    val source: String = "SkinBud Specialist",
    val confirmedAt: Long? = System.currentTimeMillis(),
    val morningSteps: List<RoutineStep> = emptyList(),
    val eveningSteps: List<RoutineStep> = emptyList()
)

@Serializable
data class RoutineProfile(
    val skinType: String = "Combination",
    val primaryConcerns: List<String> = listOf("Hydration", "Even Tone"),
    val sensitivities: List<String> = emptyList(),
    val experienceLevel: String = "Intermediate"
)
