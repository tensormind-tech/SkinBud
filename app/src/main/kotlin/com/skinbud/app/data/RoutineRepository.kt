package com.skinbud.app.data

import android.content.Context
import android.util.Log
import com.skinbud.app.model.ActiveRoutine
import com.skinbud.app.model.ProductCategory
import com.skinbud.app.model.RoutineStep
import com.skinbud.app.model.RoutineTime
import com.skinbud.app.model.ShelfProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoutineRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "active_routine.json")
    private val streakFile = File(context.filesDir, "routine_streak.json")

    private val _routine = MutableStateFlow(createDefaultRoutine())
    val routine: StateFlow<ActiveRoutine> = _routine.asStateFlow()

    private val _currentStreak = MutableStateFlow(3)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (file.exists()) {
                val content = file.readText()
                _routine.value = json.decodeFromString<ActiveRoutine>(content)
            } else {
                saveRoutine()
            }

            val prefs = context.getSharedPreferences("skinbud_streak_prefs", Context.MODE_PRIVATE)
            val savedStreak = prefs.getInt("streak_count", -1)
            if (savedStreak > 0) {
                _currentStreak.value = savedStreak
            } else if (streakFile.exists()) {
                val s = streakFile.readText().trim().toIntOrNull() ?: 3
                _currentStreak.value = s
            }
        } catch (e: Exception) {
            Log.e("RoutineRepo", "Failed to load routine data", e)
        }
    }

    private fun createDefaultRoutine(): ActiveRoutine {
        val amSteps = listOf(
            RoutineStep(
                order = 1,
                title = "Gentle Hydrating Cleanser",
                time = RoutineTime.MORNING,
                stepTip = "Wash face with lukewarm water to preserve natural barrier lipids.",
                durationSeconds = 60,
                product = ShelfProduct(
                    name = "Gentle Hydrating Foam Cleanser",
                    brand = "SkinBud Pure",
                    category = ProductCategory.CLEANSER.name
                )
            ),
            RoutineStep(
                order = 2,
                title = "Antioxidant Vitamin C Serum",
                time = RoutineTime.MORNING,
                stepTip = "Pat 3-4 drops onto face to neutralize free radicals from daytime pollution.",
                durationSeconds = 30,
                product = ShelfProduct(
                    name = "L-Ascorbic Acid 15% + Ferulic Glow",
                    brand = "SkinBud Actives",
                    category = ProductCategory.SERUM.name
                )
            ),
            RoutineStep(
                order = 3,
                title = "Ceramide Barrier Moisture Cream",
                time = RoutineTime.MORNING,
                stepTip = "Locks in hydration and cushions skin against temperature shifts.",
                durationSeconds = 45,
                product = ShelfProduct(
                    name = "Ceramide Restorative Barrier Fluid",
                    brand = "SkinBud Care",
                    category = ProductCategory.MOISTURIZER.name
                )
            ),
            RoutineStep(
                order = 4,
                title = "Invisible UV Defense SPF 50+",
                time = RoutineTime.MORNING,
                stepTip = "Essential final step. Apply 2 full finger-lengths evenly.",
                durationSeconds = 60,
                product = ShelfProduct(
                    name = "Velvet Mineral UV Shield SPF 50+",
                    brand = "SkinBud Sun",
                    category = ProductCategory.SUNSCREEN.name
                )
            )
        )

        val pmSteps = listOf(
            RoutineStep(
                order = 1,
                title = "Balm / Oil First Cleanser",
                time = RoutineTime.EVENING,
                stepTip = "Dissolves water-resistant sunscreen, makeup, and airborne grime.",
                durationSeconds = 60,
                product = ShelfProduct(
                    name = "Soothing Oat Cleansing Balm",
                    brand = "SkinBud Pure",
                    category = ProductCategory.CLEANSER.name
                )
            ),
            RoutineStep(
                order = 2,
                title = "Purifying Foam Second Cleanser",
                time = RoutineTime.EVENING,
                stepTip = "Ensures completely clear pores without stripping acid mantle.",
                durationSeconds = 60,
                product = ShelfProduct(
                    name = "Gentle Hydrating Foam Cleanser",
                    brand = "SkinBud Pure",
                    category = ProductCategory.CLEANSER.name
                )
            ),
            RoutineStep(
                order = 3,
                title = "Cellular Renewal Peptide / Retinoid",
                time = RoutineTime.EVENING,
                stepTip = "Promotes nocturnal skin regeneration and collagen maintenance.",
                durationSeconds = 45,
                product = ShelfProduct(
                    name = "Encapsulated Granactive Retinoid 2%",
                    brand = "SkinBud Actives",
                    category = ProductCategory.SERUM.name
                )
            ),
            RoutineStep(
                order = 4,
                title = "Deep Recovery Overnight Barrier Balm",
                time = RoutineTime.EVENING,
                stepTip = "Infused with Squalane and Centella for overnight cellular recovery.",
                durationSeconds = 60,
                product = ShelfProduct(
                    name = "Overnight Recovery Cica Cream",
                    brand = "SkinBud Care",
                    category = ProductCategory.MOISTURIZER.name
                )
            )
        )

        return ActiveRoutine(
            name = "Radiant Daily Protocol",
            source = "SkinBud Dermatological Team",
            morningSteps = amSteps,
            eveningSteps = pmSteps
        )
    }

    fun toggleStep(stepId: String, isMorning: Boolean) {
        val current = _routine.value
        val updatedMorning = if (isMorning) {
            current.morningSteps.map {
                if (it.id == stepId) it.copy(isCompleted = !it.isCompleted, completedAt = System.currentTimeMillis()) else it
            }
        } else current.morningSteps

        val updatedEvening = if (!isMorning) {
            current.eveningSteps.map {
                if (it.id == stepId) it.copy(isCompleted = !it.isCompleted, completedAt = System.currentTimeMillis()) else it
            }
        } else current.eveningSteps

        _routine.value = current.copy(morningSteps = updatedMorning, eveningSteps = updatedEvening)
        saveRoutine()

        val allAmDone = updatedMorning.isNotEmpty() && updatedMorning.all { it.isCompleted }
        val allPmDone = updatedEvening.isNotEmpty() && updatedEvening.all { it.isCompleted }
        if (allAmDone || allPmDone) {
            val prefs = context.getSharedPreferences("skinbud_streak_prefs", Context.MODE_PRIVATE)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastRecordedDay = prefs.getString("last_streak_day", "") ?: ""
            if (lastRecordedDay != todayStr) {
                val newStreak = _currentStreak.value + 1
                _currentStreak.value = newStreak
                prefs.edit().putString("last_streak_day", todayStr).putInt("streak_count", newStreak).apply()
                try { streakFile.writeText(newStreak.toString()) } catch (_: Exception) {}
            }
        }
    }

    fun resetDailyProgress() {
        val current = _routine.value
        val resetMorning = current.morningSteps.map { it.copy(isCompleted = false, completedAt = null) }
        val resetEvening = current.eveningSteps.map { it.copy(isCompleted = false, completedAt = null) }
        _routine.value = current.copy(morningSteps = resetMorning, eveningSteps = resetEvening)
        saveRoutine()
    }

    private fun saveRoutine() {
        try {
            file.writeText(json.encodeToString(_routine.value))
        } catch (e: Exception) {
            Log.e("RoutineRepo", "Failed to save routine", e)
        }
    }
}
