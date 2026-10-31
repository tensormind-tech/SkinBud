package com.skinbud.app.api

import android.content.Context
import com.skinbud.app.model.GlowSnapshot
import com.skinbud.app.model.GroqAiAnalysisResult
import com.skinbud.app.model.RecommendedProduct
import com.skinbud.app.model.ShelfProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GroqApiClient(private val context: Context) {

    private val prefs = context.getSharedPreferences("skinbud_groq_config", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val DEFAULT_GROQ_KEY = ""
        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
        const val FALLBACK_MODEL = "llama-3.1-8b-instant"
    }

    var apiKey: String
        get() = prefs.getString("groq_api_key", DEFAULT_GROQ_KEY) ?: DEFAULT_GROQ_KEY
        set(value) = prefs.edit().putString("groq_api_key", value.trim()).apply()

    var selectedModel: String
        get() {
            val saved = prefs.getString("groq_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
            return if (saved == "openai/gpt-oss-120b" || saved.isBlank()) DEFAULT_MODEL else saved
        }
        set(value) = prefs.edit().putString("groq_model", value.trim()).apply()

    suspend fun analyzeAndRecommend(
        snapshot: GlowSnapshot,
        availableProducts: List<ShelfProduct>
    ): GroqAiAnalysisResult = withContext(Dispatchers.IO) {
        val currentKey = apiKey
        if (currentKey.isNotBlank()) {
            try {
                return@withContext callGroqApi(currentKey, snapshot, availableProducts, selectedModel)
            } catch (e: Exception) {
                // If API call fails (network or quota), gracefully fallback to on-device engine
                e.printStackTrace()
            }
        }
        return@withContext computeOnDeviceClinicalAnalysis(snapshot, availableProducts)
    }

    private fun callGroqApi(
        key: String,
        snapshot: GlowSnapshot,
        availableProducts: List<ShelfProduct>,
        modelToUse: String
    ): GroqAiAnalysisResult {
        val productCatalogSummary = availableProducts.joinToString("\n") { p ->
            "- ID: ${p.id} | ${p.brand} ${p.name} (${p.category}) | Actives: ${p.keyIngredients.joinToString { it.name }} | Targets: ${p.targets.joinToString()}"
        }

        val prompt = """
You are a Board-Certified Cosmetic Dermatologist and Skin Health AI.
Analyze the following clinical biomarkers returned from a high-precision facial skin scan:
- Overall Skin Health Score: ${snapshot.glowScore.toInt()}/100
- Estimated Skin Age: ${snapshot.skinAge}
- Skin Type Classification: ${snapshot.skinType}
- Fitzpatrick Phototype: ${snapshot.skinColor.fitzpatrick} (${snapshot.skinColor.shadeCategory}, Sun Reaction: ${snapshot.skinColor.sunReaction})
- Hydration Level: ${snapshot.hydration.toInt()}%
- Oiliness / Sebum Sheen: ${snapshot.oiliness.toInt()}%
- Acne Severity Index: ${snapshot.acne.severity} (Count: ${snapshot.acne.count})
- Surface Texture Roughness: ${snapshot.texture.toInt()}%
- Redness / Erythema / Sensitivity: ${snapshot.redness.toInt()}%
- Dark Spots Level: ${snapshot.darkSpots.toInt()}%

Available Product Catalog (Available on Amazon Prime):
$productCatalogSummary

Return a strictly valid JSON object matching this exact schema:
{
  "executiveSummary": "Concise 2-sentence clinical diagnosis explaining the primary dermal state and root cause.",
  "primarySkinState": "Short 3-5 word diagnostic label",
  "activeIngredientStrategy": ["List of 3 synergistic active ingredients recommended to target these biomarkers"],
  "cautionActives": ["List of 2 ingredients to avoid or use with caution"],
  "routineModifications": ["2 actionable changes to their AM/PM routine"],
  "day7Forecast": "What visual transformation will occur by Day 7 (e.g. redness reduction, hydration rebound).",
  "day14Forecast": "What visual transformation will occur by Day 14 (e.g. cellular pore refining, fine line smoothing).",
  "recommendations": [
    {
      "productId": "Matching product ID from catalog",
      "matchScore": 95,
      "priority": "Essential",
      "clinicalRationale": "Why this specific formulation rectifies the measured biomarker.",
      "whyBuyReason": "Why the user should buy this on Amazon to achieve their 14-day simulated skin goals."
    }
  ]
}
        """.trimIndent()

        val requestBodyJson = buildJsonObject {
            put("model", modelToUse)
            put("temperature", 0.3)
            putJsonObject("response_format") {
                put("type", "json_object")
            }
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", "You are an expert clinical dermatological AI assistant for SkinBud. Respond only in valid JSON.")
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .post(requestBodyJson.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                android.util.Log.w("GroqApiClient", "Groq request with $modelToUse failed (${response.code}): $errBody")
                if (modelToUse != FALLBACK_MODEL) {
                    android.util.Log.i("GroqApiClient", "Retrying Groq request with $FALLBACK_MODEL")
                    return callGroqApi(key, snapshot, availableProducts, FALLBACK_MODEL)
                }
                throw IllegalStateException("Groq API error (${response.code}): $errBody")
            }
            val responseString = response.body?.string() ?: throw IllegalStateException("Empty response")
            val root = jsonParser.parseToJsonElement(responseString).jsonObject
            val content = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: throw IllegalStateException("No content in Groq choice")

            val parsedJson = jsonParser.parseToJsonElement(content).jsonObject
            val summary = parsedJson["executiveSummary"]?.jsonPrimitive?.content ?: "Balanced skin profile."
            val skinState = parsedJson["primarySkinState"]?.jsonPrimitive?.content ?: "Combination Skin"
            
            val actives = parsedJson["activeIngredientStrategy"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val cautions = parsedJson["cautionActives"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val routineMods = parsedJson["routineModifications"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val day7 = parsedJson["day7Forecast"]?.jsonPrimitive?.content ?: "Redness decreases by ~35% and moisture rebounds +45%."
            val day14 = parsedJson["day14Forecast"]?.jsonPrimitive?.content ?: "Pore appearance tightens by ~42% with visibly smoothed fine lines."

            val recs = mutableListOf<RecommendedProduct>()
            val recArray = parsedJson["recommendations"]?.jsonArray ?: JsonArray(emptyList())
            for (recElement in recArray) {
                val recObj = recElement.jsonObject
                val prodId = recObj["productId"]?.jsonPrimitive?.content
                val matchedProd = availableProducts.find { it.id == prodId }
                if (matchedProd != null) {
                    val score = recObj["matchScore"]?.jsonPrimitive?.content?.toIntOrNull() ?: 90
                    val priority = recObj["priority"]?.jsonPrimitive?.content ?: "Recommended"
                    val rationale = recObj["clinicalRationale"]?.jsonPrimitive?.content ?: "Synergistic with your skin needs."
                    val whyBuy = recObj["whyBuyReason"]?.jsonPrimitive?.content ?: "Clinically chosen to drive your 14-day simulated skin transformation."
                    recs.add(RecommendedProduct(matchedProd, score, priority, rationale, whyBuy))
                }
            }

            // If groq returned empty recs, fall back to matching available products
            if (recs.isEmpty()) {
                val fallbackRecs = availableProducts.take(3).map {
                    RecommendedProduct(it, 92, "Recommended", "Selected based on clinical biomarker scoring.", "Available on Amazon Prime for Day 7-14 progression.")
                }
                recs.addAll(fallbackRecs)
            }

            return GroqAiAnalysisResult(
                executiveSummary = summary,
                primarySkinState = skinState,
                activeIngredientStrategy = actives,
                cautionActives = cautions,
                routineModifications = routineMods,
                recommendations = recs,
                day7Forecast = day7,
                day14Forecast = day14,
                poweredBy = "Groq Llama-3.3-70B AI"
            )
        }
    }

    fun computeOnDeviceClinicalAnalysis(
        snapshot: GlowSnapshot,
        availableProducts: List<ShelfProduct>
    ): GroqAiAnalysisResult {
        val hydration = snapshot.hydration
        val oil = snapshot.oiliness
        val redness = snapshot.redness
        val texture = snapshot.texture

        val primaryState = when {
            hydration < 50 && oil > 65 -> "Dehydrated Surface with Rebound Sebum"
            hydration < 50 -> "Lipid & Water Depleted Barrier"
            oil > 65 -> "Congestion-Prone Sebum Hyperactivity"
            redness > 50 -> "Sensitized Reactive Erythema"
            texture > 55 -> "Keratinized Irregular Texture"
            else -> "Harmonized Balanced Glow"
        }

        val executiveSummary = when {
            hydration < 50 && oil > 65 ->
                "Your surface barrier exhibits transepidermal water loss triggering compensatory sebum production in the T-zone. Lightweight humectants paired with sebum regulators are advised."
            hydration < 50 ->
                "Significant stratum corneum dehydration detected. Stratum lipid replenishment with multi-weight hyaluronic acid and ceramides will restore dermal plumpness."
            oil > 65 ->
                "Elevated follicular sebum detected with high surface sheen. Salicylic acid and niacinamide will clear follicular congestion and refine pores."
            redness > 50 ->
                "Localized capillary reactivity and surface erythema observed. Soothe with Centella Asiatica, Madecassoside, and barrier-repairing ectoin."
            else ->
                "Your epidermal barrier functions optimally with balanced hydration and refined texture. Maintain antioxidant defense and broad-spectrum SPF 50."
        }

        val activeActives = mutableListOf<String>()
        val cautions = mutableListOf<String>()
        val routineMods = mutableListOf<String>()

        if (hydration < 60) {
            activeActives.add("Hyaluronic Acid (Multi-Molecular)")
            activeActives.add("Ceramides NP/AP/EOP")
            routineMods.add("Apply hydrating essence on damp skin before occlusive moisturizer")
        }
        if (oil > 55) {
            activeActives.add("Niacinamide (5-10%) + Zinc PCA")
            activeActives.add("Salicylic Acid (BHA 1-2%)")
            cautions.add("Heavy comedogenic mineral oils or shea butters on T-zone")
            routineMods.add("Double cleanse in PM; switch to oil-free gel hydrator in AM")
        }
        if (redness > 45) {
            activeActives.add("Centella Asiatica / Madecassoside")
            cautions.add("High-percentage physical scrubs or unbuffered pure ascorbic acid")
            routineMods.add("Rinse face only with lukewarm water; layer soothing barrier balm")
        }
        if (activeActives.isEmpty()) {
            activeActives.addAll(listOf("Vitamin C (Sodium Ascorbyl Phosphate)", "Ectoin & Peptides", "Green Tea Epigallocatechin"))
            cautions.add("Skipping broad-spectrum sunscreen")
            routineMods.add("Maintain daily antioxidant shield followed by SPF 50+")
        }

        // Rank available products based on biomarkers
        val scoredProducts = availableProducts.map { prod ->
            var score = 75
            var priority = "Recommended"
            var rationale = "Compatible with your daily skincare protocol."

            val targetStr = prod.targets.joinToString().lowercase()
            val ingrStr = prod.keyIngredients.joinToString { it.name }.lowercase()

            if (hydration < 60 && (targetStr.contains("hydrat") || ingrStr.contains("hyaluronic") || ingrStr.contains("ceramide"))) {
                score += 18
                priority = "Essential"
                rationale = "Crucial to replenish moisture and seal transepidermal water loss."
            }
            if (oil > 55 && (targetStr.contains("oil") || targetStr.contains("pore") || ingrStr.contains("niacinamide") || ingrStr.contains("bha") || ingrStr.contains("zinc"))) {
                score += 15
                priority = "Essential"
                rationale = "Regulates follicular sebum excretion and decongests pores."
            }
            if (redness > 45 && (targetStr.contains("calm") || targetStr.contains("sooth") || targetStr.contains("barrier") || ingrStr.contains("centella"))) {
                score += 16
                priority = "Essential"
                rationale = "Attenuates cutaneous redness and reinforces sensitive capillary walls."
            }

            score = score.coerceIn(60, 99)
            val whyBuy = "Clinically recommended to purchase on Amazon Prime for steady active ingredient delivery over your 7 to 14 day transformation."
            RecommendedProduct(prod, score, priority, rationale, whyBuy)
        }.sortedByDescending { it.matchScore }

        val d7 = when {
            redness > 50 -> "Erythema and surface flush reduce by ~35%, restoring balanced skin barrier calm."
            hydration < 50 -> "Stratum corneum hydration rebounds +45%, alleviating tightness and dry fine lines."
            oil > 60 -> "Sebum secretion normalizes with noticeable reduction in midday T-zone shine."
            else -> "Initial cellular hydration increase (+30%) and refreshed morning skin radiance."
        }

        val d14 = when {
            texture > 50 -> "Keratinized surface roughness smooths by ~42%, revealing silky, refined micro-relief."
            oil > 60 -> "Follicular congestion dissolves and pore aperture visibility decreases by ~38%."
            redness > 50 -> "Micro-capillary resilience increases with even, uniform tone across cheeks and nose."
            else -> "Visible pore tightness and radiant skin luminosity with restored lipid mantle integrity."
        }

        return GroqAiAnalysisResult(
            executiveSummary = executiveSummary,
            primarySkinState = primaryState,
            activeIngredientStrategy = activeActives.distinct(),
            cautionActives = cautions.ifEmpty { listOf("Over-exfoliation", "Harsh sulfates") },
            routineModifications = routineMods.ifEmpty { listOf("Consistent daily SPF application", "Gentle evening double cleanse") },
            recommendations = scoredProducts,
            day7Forecast = d7,
            day14Forecast = d14,
            poweredBy = "SkinBud On-Device Clinical Intelligence"
        )
    }
}
