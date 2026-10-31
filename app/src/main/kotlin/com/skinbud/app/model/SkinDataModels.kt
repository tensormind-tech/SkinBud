package com.skinbud.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GlowFix(
    val tag: String,
    val text: String
)

@Serializable
data class GlowHabit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val kind: String = "general",
    val sourceLever: String? = null,
    val exerciseID: String? = null,
    val isCompletedToday: Boolean = false
)

@Serializable
data class GlowLever(
    val id: String,
    val group: String,
    val name: String,
    val valueText: String,
    val value: Double,
    val low: Double = 0.0,
    val high: Double = 100.0,
    val verdict: String,
    val meaning: String,
    val science: String,
    val cite: String = "",
    val fixes: List<GlowFix> = emptyList(),
    val severity: Double = 0.0
)

@Serializable
data class GlowMarker(
    val id: String,
    val group: String,
    val name: String,
    val valueText: String,
    val value: Double,
    val low: Double = 0.0,
    val high: Double = 100.0,
    val inRange: Boolean = true,
    val verdict: String,
    val meaning: String,
    val science: String = "",
    val cite: String = ""
)

@Serializable
data class SkinColorRecord(
    val hex: String = "#F2D0B8",
    val rgb: List<Int> = listOf(242, 208, 184),
    val fitzpatrick: String = "Type II",
    val undertone: String = "Warm",
    val shadeCategory: String = "Fair-Light",
    val itaAngle: Double = 42.5,
    val itaCategory: String = "Fair",
    val luminance: Double = 81.2,
    val undertoneConfidence: Double = 0.92,
    val sunReaction: String = "Usually burns, tans minimally",
    val spfRequirement: String = "SPF 50+ Broad Spectrum Essential"
)

@Serializable
data class FoundationRecord(
    val primaryShade: String = "Warm Ivory (02)",
    val shadeHex: String = "#F5D6BE",
    val undertone: String = "Warm Golden",
    val undertoneConfidence: Double = 0.94,
    val undertoneGuidance: String = "Look for yellow or peachy undertones; avoid cool pink bases which can look ashy.",
    val lightestZone: String? = "Cheeks",
    val darkestZone: String? = "Forehead",
    val variationRange: Double = 1.8,
    val summer: String = "Honey Beige (04)",
    val winter: String = "Warm Ivory (02)",
    val coverage: String = "Light to Medium dewy finish",
    val coverageTip: String = "Pair with a hydrating primer on high points for natural radiance."
)

@Serializable
data class AcneAssessment(
    val count: Int = 0,
    val severity: String = "Mild",
    val inflamedCount: Int = 0,
    val nonInflamedCount: Int = 0,
    val distribution: Map<String, Int> = emptyMap(),
    val notes: String = "No critical cystic inflammation detected."
)

@Serializable
data class MetricReading(
    val score: Double,
    val level: String,
    val status: String = "Good",
    val details: String = "",
    val zoneScores: Map<String, Double> = emptyMap()
)

@Serializable
data class GlowSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val glowScore: Double = 85.0,
    val skinAge: Int = 24,
    val overallHealth: Double = 86.5,
    
    // Core Skin Biomarkers
    val oiliness: Double = 42.0,
    val oilinessLevel: String = "Balanced",
    
    val hydration: Double = 78.0,
    val hydrationLevel: String = "Optimal",
    
    val redness: Double = 22.0,
    val rednessLevel: String = "Low",
    
    val darkSpots: Double = 18.0,
    val darkSpotsLevel: String = "Minimal",
    
    val texture: Double = 82.0,
    val textureLevel: String = "Smooth",

    val pores: Double = 25.0,
    val poresLevel: String = "Refined",

    val wrinkles: Double = 15.0,
    val wrinklesLevel: String = "Minimal",

    val darkCircles: Double = 20.0,
    val darkCirclesLevel: String = "Low",

    val eyeBags: Double = 12.0,
    val eyeBagsLevel: String = "None",

    val radiance: Double = 84.0,
    val radianceLevel: String = "Luminous",

    val firmness: Double = 86.0,
    val firmnessLevel: String = "Elastic",
    
    val acne: AcneAssessment = AcneAssessment(),
    
    val droopyUpperEyelid: Double = 12.0,
    val droopyLowerEyelid: Double = 10.0,
    val tearTrough: Double = 15.0,

    val skinType: String = "Combination / Balanced",
    val skinColor: SkinColorRecord = SkinColorRecord(),
    val foundation: FoundationRecord = FoundationRecord(),
    
    val levers: List<GlowLever> = emptyList(),
    val markers: List<GlowMarker> = emptyList(),
    val imagePath: String? = null,
    val faceImageUrl: String? = null,
    val isRealScan: Boolean = true
)

@Serializable
data class GlowDiskData(
    val snapshots: List<GlowSnapshot> = emptyList(),
    val habits: List<GlowHabit> = emptyList(),
    val logs: Map<String, Boolean> = emptyMap(),
    val gender: String = "other",
    val firstName: String = "Beauty"
)

// YouCam Makeup VTO Models
@Serializable
data class VtoPalette(
    val color: String,
    val texture: String? = null,
    val colorIntensity: Int? = null,
    val coverageIntensity: Int? = null,
    val glowIntensity: Int? = null
)

@Serializable
data class VtoPattern(
    val name: String
)

@Serializable
data class VtoEffect(
    val category: String,
    val palettes: List<VtoPalette> = emptyList(),
    val pattern: VtoPattern? = null,
    val intensity: Int? = null,
    val finish: String? = null,
    val colors: List<String>? = null
)

// eCommerce & Return-Risk Models
@Serializable
data class CartItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val brand: String = "SkinBud Lab",
    val category: String,
    val price: Double,
    val imageUrl: String? = null,
    val shadeName: String? = null,
    val shadeHex: String? = null,
    val size: String = "30 ml / 1.0 fl oz",
    val matchScore: Int = 98,
    val returnRiskPercent: Double = 1.8,
    val quantity: Int = 1,
    val isVtoValidated: Boolean = true
)

@Serializable
data class CartSummary(
    val subtotal: Double,
    val discount: Double,
    val shipping: Double,
    val total: Double,
    val returnRiskReductionPercent: Double = 76.5,
    val estimatedLogisticsSaved: Double = 28.50
)

// AI Skin Simulation Models (https://docs.perfectcorp.com/reference/ai_skin_simulation)
@Serializable
data class SkinSimulationParams(
    val wrinkle: Double = 0.0,
    val radiance: Double = 0.0,
    val oiliness: Double = 0.0,
    val acne: Double = 0.0,
    val eye_bags: Double = 0.0,
    val dark_circle: Double = 0.0,
    val spots: Double = 0.0,
    val pores: Double = 0.0,
    val texture: Double = 0.0,
    val redness: Double = 0.0
)

enum class SimulationDay(
    val dayNumber: Int,
    val label: String,
    val phase: String,
    val biomarkerProjection: String
) {
    DAY_0(0, "Today", "Baseline Dermal State", "Original scanned skin metrics"),
    DAY_7(7, "Day 7", "Cellular Turnaround", "Redness -35% · Radiance +45% · Hydration Rebound"),
    DAY_14(14, "Day 14", "Stratum Corneum Renewal", "Wrinkles -35% · Pores -42% · Tone Uniformity"),
    DAY_28(28, "Day 28", "Full Cellular Turnover", "Wrinkles -62% · Spots -55% · Peak Dermal Glow")
}

