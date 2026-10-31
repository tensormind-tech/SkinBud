package com.skinbud.app.data

import android.content.Context
import android.util.Log
import com.skinbud.app.api.GroqApiClient
import com.skinbud.app.model.GlowSnapshot
import com.skinbud.app.model.GroqAiAnalysisResult
import com.skinbud.app.model.KeyIngredient
import com.skinbud.app.model.MatchFactor
import com.skinbud.app.model.ProductCategory
import com.skinbud.app.model.ShelfProduct
import com.skinbud.app.model.SkinMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ProductRepository(private val context: Context) {

    val groqClient = GroqApiClient(context)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val file = File(context.filesDir, "shelf_products.json")

    private val _products = MutableStateFlow<List<ShelfProduct>>(emptyList())
    val products: StateFlow<List<ShelfProduct>> = _products.asStateFlow()

    private val _latestRecommendation = MutableStateFlow<GroqAiAnalysisResult?>(null)
    val latestRecommendation: StateFlow<GroqAiAnalysisResult?> = _latestRecommendation.asStateFlow()

    init {
        loadData()
    }

    companion object {
        val DEFAULT_IMAGE_MAP = mapOf(
            "prod_cleanse_oat" to "https://m.media-amazon.com/images/I/71VnC87n9BL._SL1500_.jpg",
            "prod_cleanse_bha" to "https://m.media-amazon.com/images/I/71Y+w+x3NLL._SL1500_.jpg",
            "prod_toner_heartleaf" to "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg",
            "prod_toner_bha_liquid" to "https://m.media-amazon.com/images/I/61B6n4rP5ML._SL1500_.jpg",
            "prod_serum_hyaluronic" to "https://m.media-amazon.com/images/I/61wL3p8vQ+L._SL1500_.jpg",
            "prod_serum_cica" to "https://m.media-amazon.com/images/I/61b-9o4HaoL._SL1500_.jpg",
            "prod_serum_niacinamide" to "https://m.media-amazon.com/images/I/61m-wM9J2uL._SL1500_.jpg",
            "prod_serum_vitc" to "https://m.media-amazon.com/images/I/61m-wM9J2uL._SL1500_.jpg",
            "prod_moisturizer_ceramide" to "https://m.media-amazon.com/images/I/61S7BrLnZmL._SL1500_.jpg",
            "prod_moisturizer_watergel" to "https://m.media-amazon.com/images/I/61S7BrLnZmL._SL1500_.jpg",
            "prod_sun_mineral" to "https://m.media-amazon.com/images/I/51bU28V+QNL._SL1500_.jpg",
            "prod_exfoliant_mandelic" to "https://m.media-amazon.com/images/I/61B6n4rP5ML._SL1500_.jpg",
            "prod_mask_mugwort" to "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg"
        )
    }

    private fun loadData() {
        try {
            if (file.exists()) {
                val content = file.readText()
                val loaded = json.decodeFromString<List<ShelfProduct>>(content)
                if (loaded.isNotEmpty()) {
                    _products.value = loaded.map { prod ->
                        if (prod.imageUrl.isNullOrBlank()) {
                            prod.copy(imageUrl = DEFAULT_IMAGE_MAP[prod.id] ?: "https://m.media-amazon.com/images/I/71VnC87n9BL._SL1500_.jpg")
                        } else {
                            prod
                        }
                    }
                } else {
                    seedInitialProducts()
                }
            } else {
                seedInitialProducts()
            }
        } catch (e: Exception) {
            Log.e("ProductRepo", "Failed to load products", e)
            seedInitialProducts()
        }
    }

    private fun seedInitialProducts() {
        val catalog = listOf(
            // CLEANSERS
            ShelfProduct(
                id = "prod_cleanse_oat",
                name = "Colloidal Oat Gentle Hydrating Cleanser",
                brand = "SkinBud Lab",
                category = ProductCategory.CLEANSER.name,
                imageUrl = "https://m.media-amazon.com/images/I/71VnC87n9BL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=colloidal+oat+cleanser",
                priceUsd = 22.00,
                ratingStars = 4.8,
                reviewCount = 1840,
                keyIngredients = listOf(
                    KeyIngredient("Colloidal Oatmeal", "Micro-pulverized beta-glucans soothe irritated stratum corneum", "Soothes & Calms"),
                    KeyIngredient("Sodium Cocoyl Amino Acids", "Ultra-mild surfactant preserving natural lipid mantle", "Barrier Protection")
                ),
                skinTypes = listOf("All", "Sensitive", "Dry", "Reactive"),
                targets = listOf("Barrier Support", "Redness", "Gentle Cleansing"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 97, reason = "Zero sulfates; preserves the lipid barrier while gently removing impurities.")
            ),
            ShelfProduct(
                id = "prod_cleanse_bha",
                name = "Clarifying Salicylic 1.5% Gel Cleanser",
                brand = "SkinBud Actives",
                category = ProductCategory.CLEANSER.name,
                imageUrl = "https://m.media-amazon.com/images/I/71Y+w+x3NLL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=salicylic+acid+cleanser",
                priceUsd = 24.00,
                ratingStars = 4.7,
                reviewCount = 2410,
                keyIngredients = listOf(
                    KeyIngredient("Salicylic Acid (BHA 1.5%)", "Lipophilic beta hydroxy acid clears follicular sebum plugs", "Decongests Pores"),
                    KeyIngredient("Tea Tree Terpinen-4-ol", "Purified botanical active targeting acne pathogens", "Anti-Blemish")
                ),
                skinTypes = listOf("Oily", "Combination", "Acne-Prone"),
                targets = listOf("Excess Sebum", "Blackheads", "Acne"),
                isFavorite = false,
                rating = 4,
                match = SkinMatch(score = 91, reason = "Effectively targets follicular sebum hyperactivity and micro-comedones.")
            ),

            // TONERS
            ShelfProduct(
                id = "prod_toner_heartleaf",
                name = "Heartleaf 77% Calming Moisture Toner",
                brand = "SkinBud Pure",
                category = ProductCategory.TONER.name,
                imageUrl = "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=anua+heartleaf+77+soothing+toner",
                priceUsd = 21.00,
                ratingStars = 4.9,
                reviewCount = 3120,
                keyIngredients = listOf(
                    KeyIngredient("Houttuynia Cordata (Heartleaf) 77%", "Rich in quercetin to calm cutaneous erythema and heat", "Redness Reduction"),
                    KeyIngredient("Centella Asiatica Extract", "Promotes micro-circulation and dermal hydration", "Soothing")
                ),
                skinTypes = listOf("All", "Sensitive", "Combination"),
                targets = listOf("Redness", "Hydration", "Skin Soothing"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 96, reason = "Immediate relief for sensitized skin and persistent surface redness.")
            ),
            ShelfProduct(
                id = "prod_toner_bha_liquid",
                name = "Pore-Refining 2% BHA Liquid Exfoliant",
                brand = "SkinBud Actives",
                category = ProductCategory.TONER.name,
                imageUrl = "https://m.media-amazon.com/images/I/61B6n4rP5ML._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=bha+liquid+exfoliant",
                priceUsd = 35.00,
                ratingStars = 4.8,
                reviewCount = 14200,
                keyIngredients = listOf(
                    KeyIngredient("Salicylic Acid 2%", "Deeply exfoliates inside pore linings to dissolve sebum", "Pore Refiner"),
                    KeyIngredient("Green Tea EGCG", "Potent polyphenol antioxidant reducing lipid peroxidation", "Antioxidant Defense")
                ),
                skinTypes = listOf("Oily", "Combination"),
                targets = listOf("Large Pores", "Rough Texture", "T-Zone Shine"),
                isFavorite = false,
                rating = 5,
                match = SkinMatch(score = 89, reason = "High-efficacy exfoliant for stubborn pores and irregular skin texture.")
            ),

            // SERUMS
            ShelfProduct(
                id = "prod_serum_hyaluronic",
                name = "Multi-Molecular Hyaluronic Surge Serum",
                brand = "SkinBud Lab",
                category = ProductCategory.SERUM.name,
                imageUrl = "https://m.media-amazon.com/images/I/61wL3p8vQ+L._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=hyaluronic+acid+serum",
                priceUsd = 28.00,
                ratingStars = 4.8,
                reviewCount = 4920,
                keyIngredients = listOf(
                    KeyIngredient("Multi-Weight Hyaluronic Acid", "High, medium, and low Daltons for multi-layer hydration", "Deep Moisture"),
                    KeyIngredient("Panthenol (Pro-Vit B5)", "Retains stratum corneum moisture and speeds repair", "Barrier Soothing")
                ),
                skinTypes = listOf("All", "Dry", "Dehydrated", "Sensitive"),
                targets = listOf("Dehydration", "Plumping", "Fine Lines"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 98, reason = "Replenishes transepidermal water loss and restores instant dermal rebound.")
            ),
            ShelfProduct(
                id = "prod_serum_cica",
                name = "Centella & Madecassoside Relief Ampoule",
                brand = "SkinBud Lab",
                category = ProductCategory.SERUM.name,
                imageUrl = "https://m.media-amazon.com/images/I/61b-9o4HaoL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=centella+ampoule",
                priceUsd = 26.00,
                ratingStars = 4.9,
                reviewCount = 2870,
                keyIngredients = listOf(
                    KeyIngredient("Madecassoside", "Isolated pure triterpene from Centella Asiatica", "Calming"),
                    KeyIngredient("Asiatic Acid", "Stimulates collagen I synthesis and reinforces vessel walls", "Anti-Redness")
                ),
                skinTypes = listOf("Sensitive", "Acne-Prone", "Reactive"),
                targets = listOf("Erythema", "Barrier Repair", "Inflammation"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 95, reason = "Rapidly down-regulates inflammatory cytokines and surface flush.")
            ),
            ShelfProduct(
                id = "prod_serum_niacinamide",
                name = "Niacinamide 10% + Zinc PCA 1% Elixir",
                brand = "SkinBud Actives",
                category = ProductCategory.SERUM.name,
                imageUrl = "https://m.media-amazon.com/images/I/61m-wM9J2uL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=niacinamide+10+zinc+1",
                priceUsd = 12.00,
                ratingStars = 4.7,
                reviewCount = 9840,
                keyIngredients = listOf(
                    KeyIngredient("Niacinamide (Vitamin B3) 10%", "Reduces sebaceous excretion rate and refines pores", "Sebum Normalizer"),
                    KeyIngredient("Zinc PCA 1%", "Inhibits 5-alpha reductase to control androgenic shine", "Anti-Shine")
                ),
                skinTypes = listOf("Oily", "Combination", "Congested"),
                targets = listOf("Excess Oil", "Enlarged Pores", "Blemishes"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 92, reason = "Clinically balances elevated follicular sebum and minimizes shine.")
            ),
            ShelfProduct(
                id = "prod_serum_vitc",
                name = "Pure Vitamin C 15% + Ferulic Glow Booster",
                brand = "SkinBud Glow",
                category = ProductCategory.SERUM.name,
                imageUrl = "https://m.media-amazon.com/images/I/61m-wM9J2uL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=vitamin+c+ferulic+serum",
                priceUsd = 34.00,
                ratingStars = 4.6,
                reviewCount = 1920,
                keyIngredients = listOf(
                    KeyIngredient("L-Ascorbic Acid 15%", "Pure Vitamin C neutralizes free radicals and boosts collagen", "Radiance & Tone"),
                    KeyIngredient("Ferulic Acid 0.5%", "Doubles photoprotective antioxidant synergy", "Photoprotection")
                ),
                skinTypes = listOf("Normal", "Combination", "Dull"),
                targets = listOf("Dullness", "Dark Spots", "Tone Uniformity"),
                isFavorite = false,
                rating = 4,
                match = SkinMatch(score = 88, reason = "Significantly elevates skin radiance index and reduces hyperpigmentation.")
            ),

            // MOISTURIZERS
            ShelfProduct(
                id = "prod_moisturizer_ceramide",
                name = "Barrier Shield Multi-Ceramide Liposomal Cream",
                brand = "SkinBud Care",
                category = ProductCategory.MOISTURIZER.name,
                imageUrl = "https://m.media-amazon.com/images/I/61S7BrLnZmL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=ceramide+barrier+cream",
                priceUsd = 29.00,
                ratingStars = 4.9,
                reviewCount = 5420,
                keyIngredients = listOf(
                    KeyIngredient("Ceramides NP, AP, EOP", "Recreates 3:1:1 physiological lipid membrane structure", "Barrier Repair"),
                    KeyIngredient("Phytosphingosine", "Natural antimicrobial barrier defense lipid", "Blemish Defense"),
                    KeyIngredient("Squalane", "Biomimetic plant squalane locks in deep hydration without clogging", "Weightless Occlusion")
                ),
                skinTypes = listOf("All", "Dry", "Combination", "Sensitive"),
                targets = listOf("Barrier Fortification", "Dry Flakes", "Roughness"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 99, reason = "Gold standard lipid barrier restoration for optimal dermal protection.")
            ),
            ShelfProduct(
                id = "prod_moisturizer_watergel",
                name = "Weightless Aqua Surge Hydro-Gel Cream",
                brand = "SkinBud Care",
                category = ProductCategory.MOISTURIZER.name,
                imageUrl = "https://m.media-amazon.com/images/I/61S7BrLnZmL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=aqua+gel+cream",
                priceUsd = 25.00,
                ratingStars = 4.7,
                reviewCount = 3120,
                keyIngredients = listOf(
                    KeyIngredient("Birch Juice 70%", "Rich in amino acids and minerals to replenish moisture", "Cooling Hydration"),
                    KeyIngredient("Trehalose", "Sugar humectant protecting cells against osmotic dehydration", "Moisture Lock")
                ),
                skinTypes = listOf("Oily", "Combination", "Acne-Prone"),
                targets = listOf("Hydration", "Zero Shine", "Pore-Friendly"),
                isFavorite = false,
                rating = 4,
                match = SkinMatch(score = 93, reason = "Ultra-refreshing, zero-oil hydrator perfect under sunscreen and makeup.")
            ),

            // SUNSCREENS
            ShelfProduct(
                id = "prod_sun_mineral",
                name = "Velvet Mineral UV Shield SPF 50+ PA++++",
                brand = "SkinBud Sun",
                category = ProductCategory.SUNSCREEN.name,
                imageUrl = "https://m.media-amazon.com/images/I/51bU28V+QNL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=mineral+sunscreen+spf+50",
                priceUsd = 27.00,
                ratingStars = 4.9,
                reviewCount = 8740,
                keyIngredients = listOf(
                    KeyIngredient("Zinc Oxide 12%", "Non-nano physical filter reflecting full UVA/UVB spectrum", "Broad-Spectrum SPF"),
                    KeyIngredient("Ectoin 1%", "Extremolyte molecule protecting skin cells from blue light and UV", "Cellular Defense")
                ),
                skinTypes = listOf("All", "Sensitive", "Post-Procedure"),
                targets = listOf("UV Defense", "Anti-Photoaging", "Anti-Pollution"),
                isFavorite = true,
                rating = 5,
                match = SkinMatch(score = 97, reason = "Essential daily defense against UV radiation, photoaging, and hyperpigmentation.")
            ),

            // EXFOLIANTS & MASKS
            ShelfProduct(
                id = "prod_exfoliant_mandelic",
                name = "Mandelic Acid 5% + PHA Ultra-Gentle Micro-Peel",
                brand = "SkinBud Actives",
                category = ProductCategory.EXFOLIANT.name,
                imageUrl = "https://m.media-amazon.com/images/I/61B6n4rP5ML._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=mandelic+acid+5",
                priceUsd = 22.00,
                ratingStars = 4.6,
                reviewCount = 1120,
                keyIngredients = listOf(
                    KeyIngredient("Mandelic Acid 5%", "Large molecular weight AHA for smooth resurfacing without irritation", "Gentle Turnover"),
                    KeyIngredient("Gluconolactone (PHA) 3%", "Humectant polyhydroxy acid gently removes surface keratin", "Micro-Polishing")
                ),
                skinTypes = listOf("All", "Sensitive", "Textured"),
                targets = listOf("Texture Roughness", "Flaky Patches", "Dull Tone"),
                isFavorite = false,
                rating = 4,
                match = SkinMatch(score = 90, reason = "Buffs rough surface texture while preserving moisture barrier integrity.")
            ),
            ShelfProduct(
                id = "prod_mask_mugwort",
                name = "Mugwort Calming Pore Detox Clay Mask",
                brand = "SkinBud Pure",
                category = ProductCategory.MASK.name,
                imageUrl = "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg",
                buyUrl = "https://www.amazon.com/s?k=mugwort+calming+clay+mask",
                priceUsd = 23.00,
                ratingStars = 4.8,
                reviewCount = 2050,
                keyIngredients = listOf(
                    KeyIngredient("Artemisia Princeps (Mugwort)", "Potent Korean herbal medicine calming inflammatory flare-ups", "Pore Clarifying"),
                    KeyIngredient("Kaolin Clay", "Gently absorbs excess sebum without stripping moisture", "Mattifying")
                ),
                skinTypes = listOf("Oily", "Combination", "Acne-Prone"),
                targets = listOf("Sebum Absorption", "Pore Purifying", "Soothing"),
                isFavorite = false,
                rating = 4,
                match = SkinMatch(score = 92, reason = "Weekly detox treatment to extract impurities and refine visible pores.")
            )
        )

        _products.value = catalog
        saveProducts()
    }

    suspend fun generateRecommendations(snapshot: GlowSnapshot): GroqAiAnalysisResult {
        val result = groqClient.analyzeAndRecommend(snapshot, _products.value)
        _latestRecommendation.value = result

        // Update matches in current products list
        val updatedList = _products.value.map { prod ->
            val matchItem = result.recommendations.find { it.product.id == prod.id }
            if (matchItem != null) {
                prod.copy(
                    match = SkinMatch(
                        score = matchItem.matchScore,
                        reason = matchItem.clinicalRationale,
                        topConcern = matchItem.priority,
                        source = result.poweredBy
                    )
                )
            } else {
                prod
            }
        }
        _products.value = updatedList
        saveProducts()
        return result
    }

    fun addProduct(product: ShelfProduct) {
        _products.value = listOf(product) + _products.value
        saveProducts()
    }

    fun toggleFavorite(productId: String) {
        _products.value = _products.value.map {
            if (it.id == productId) it.copy(isFavorite = !it.isFavorite) else it
        }
        saveProducts()
    }

    fun deleteProduct(productId: String) {
        _products.value = _products.value.filter { it.id != productId }
        saveProducts()
    }

    private fun saveProducts() {
        try {
            file.writeText(json.encodeToString(_products.value))
        } catch (e: Exception) {
            Log.e("ProductRepo", "Failed to save products", e)
        }
    }
}
