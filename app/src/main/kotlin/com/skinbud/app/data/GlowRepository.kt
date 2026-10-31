package com.skinbud.app.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.skinbud.app.api.PerfectCorpSkinApiClient
import com.skinbud.app.engine.SkinAnalysisEngine
import com.skinbud.app.model.GlowHabit
import com.skinbud.app.model.GlowSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

class GlowRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "glow_snapshots.json")
    private val habitsFile = File(context.filesDir, "glow_habits.json")

    private val _snapshots = MutableStateFlow<List<GlowSnapshot>>(emptyList())
    val snapshots: StateFlow<List<GlowSnapshot>> = _snapshots.asStateFlow()

    private val _latestSnapshot = MutableStateFlow<GlowSnapshot?>(null)
    val latestSnapshot: StateFlow<GlowSnapshot?> = _latestSnapshot.asStateFlow()

    private val _habits = MutableStateFlow<List<GlowHabit>>(emptyList())
    val habits: StateFlow<List<GlowHabit>> = _habits.asStateFlow()

    val perfectCorpClient = PerfectCorpSkinApiClient(context)

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (file.exists()) {
                val content = file.readText()
                val list = json.decodeFromString<List<GlowSnapshot>>(content)
                // Filter out any legacy dummy snapshots
                val realList = list.filter { it.isRealScan && it.imagePath != null }
                _snapshots.value = realList
                _latestSnapshot.value = realList.firstOrNull()
            } else {
                _snapshots.value = emptyList()
                _latestSnapshot.value = null
            }

            if (habitsFile.exists()) {
                val hContent = habitsFile.readText()
                _habits.value = json.decodeFromString<List<GlowHabit>>(hContent)
            } else {
                seedInitialHabits()
            }
        } catch (e: Exception) {
            Log.e("GlowRepo", "Failed to load glow data", e)
            _snapshots.value = emptyList()
            _latestSnapshot.value = null
            seedInitialHabits()
        }
    }

    private fun seedInitialHabits() {
        val defaults = listOf(
            GlowHabit(title = "Morning SPF 50+ Sun Protection", kind = "sun"),
            GlowHabit(title = "Drink 2L Hydrating Water", kind = "water"),
            GlowHabit(title = "Gentle PM Double Cleanse", kind = "cleanse"),
            GlowHabit(title = "Night Barrier Ceramide Repair", kind = "moisture")
        )
        _habits.value = defaults
        saveHabits()
    }

    suspend fun analyzeAndSave(bitmap: Bitmap, useApiIfAvailable: Boolean = true): Result<GlowSnapshot> = withContext(Dispatchers.IO) {
        try {
            // Save image locally inside "scans" subfolder configured in file_paths.xml
            val scansDir = File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }
            val imageFileName = "scan_${System.currentTimeMillis()}.jpg"
            val imageFile = File(scansDir, imageFileName)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Execute skin diagnosis
            // If Perfect Corp API is configured, call it directly and DO NOT swallow errors
            val snapshot = if (useApiIfAvailable && perfectCorpClient.isConfigured) {
                perfectCorpClient.analyzeSkin(bitmap).copy(
                    imagePath = imageFile.absolutePath,
                    isRealScan = true
                )
            } else {
                // Real on-device computer vision analyzing true pixels
                SkinAnalysisEngine.analyzeBitmap(bitmap).copy(
                    imagePath = imageFile.absolutePath,
                    isRealScan = true
                )
            }

            val currentList = _snapshots.value
            // Keep up to 20 most recent scans, delete pruned photo files from disk
            val maxRetention = 20
            if (currentList.size >= maxRetention) {
                val toPrune = currentList.drop(maxRetention - 1)
                for (oldSnap in toPrune) {
                    oldSnap.imagePath?.let { oldPath ->
                        try { File(oldPath).delete() } catch (_: Exception) {}
                    }
                }
            }
            val updated = (listOf(snapshot) + currentList).take(maxRetention)
            _snapshots.value = updated
            _latestSnapshot.value = snapshot
            saveSnapshots()

            Result.success(snapshot)
        } catch (e: Exception) {
            Log.e("GlowRepo", "Real scan analysis error", e)
            Result.failure(e)
        }
    }

    fun toggleHabit(habitId: String) {
        val updated = _habits.value.map {
            if (it.id == habitId) it.copy(isCompletedToday = !it.isCompletedToday) else it
        }
        _habits.value = updated
        saveHabits()
    }

    private fun saveSnapshots() {
        try {
            file.writeText(json.encodeToString(_snapshots.value))
        } catch (e: Exception) {
            Log.e("GlowRepo", "Failed to save snapshots", e)
        }
    }

    private fun saveHabits() {
        try {
            habitsFile.writeText(json.encodeToString(_habits.value))
        } catch (e: Exception) {
            Log.e("GlowRepo", "Failed to save habits", e)
        }
    }
}
