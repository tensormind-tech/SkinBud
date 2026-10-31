package com.skinbud.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ProductCategory(val displayName: String) {
    CLEANSER("Cleanser"),
    TONER("Toner & Mist"),
    SERUM("Serum & Essence"),
    MOISTURIZER("Moisturizer & Cream"),
    SUNSCREEN("SPF / Sunscreen"),
    EXFOLIANT("Exfoliant / Peel"),
    EYE_CARE("Eye Treatment"),
    MASK("Mask & Treatment")
}

@Serializable
data class KeyIngredient(
    val name: String,
    val description: String = "",
    val benefit: String = "Hydrating & Soothing",
    val safetyLevel: String = "Safe"
)

@Serializable
data class MatchFactor(
    val label: String,
    val delta: Int, // e.g. +15 or -10
    val explanation: String = ""
)

@Serializable
data class SkinMatch(
    val score: Int = 90, // 0 to 100%
    val factors: List<MatchFactor> = emptyList(),
    val topConcern: String? = null,
    val reason: String = "Highly compatible with your balanced moisture and sensitive barrier profile.",
    val source: String = "SkinBud AI",
    val computedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ShelfProduct(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String,
    val category: String = ProductCategory.MOISTURIZER.name,
    val imageUrl: String? = null,
    val keyIngredients: List<KeyIngredient> = emptyList(),
    val ingredients: List<String> = emptyList(),
    val skinTypes: List<String> = listOf("All", "Combination", "Sensitive"),
    val targets: List<String> = listOf("Hydration", "Barrier Support"),
    val notes: String = "",
    val openDate: String? = null,
    val isFavorite: Boolean = false,
    val rating: Int? = 5,
    val barcode: String? = null,
    val match: SkinMatch? = null,
    val priceUsd: Double = 22.00,
    val buyUrl: String = "https://www.amazon.com/s?k=skincare",
    val retailer: String = "Amazon",
    val primeDelivery: Boolean = true,
    val ratingStars: Double = 4.8,
    val reviewCount: Int = 1840,
    val simulationTargetDay: Int = 14
)

@Serializable
data class RecommendedProduct(
    val product: ShelfProduct,
    val matchScore: Int,
    val priority: String = "Recommended", // Essential, Recommended, Complementary
    val clinicalRationale: String,
    val whyBuyReason: String = "Clinically recommended formulation targeting your skin scan biomarkers to achieve the 14-day simulated transformation."
)

@Serializable
data class GroqAiAnalysisResult(
    val executiveSummary: String,
    val primarySkinState: String,
    val activeIngredientStrategy: List<String>,
    val cautionActives: List<String>,
    val routineModifications: List<String>,
    val recommendations: List<RecommendedProduct>,
    val day7Forecast: String = "Stratum corneum redness calms by ~35%, surface hydration rebounds +45%, creating noticeable morning luminosity.",
    val day14Forecast: String = "Cellular epidermal renewal tightens visible pores by ~42% and softens early expression lines by ~35% with even tone.",
    val poweredBy: String = "Groq Llama-3.3-70B AI",
    val timestamp: Long = System.currentTimeMillis()
)

