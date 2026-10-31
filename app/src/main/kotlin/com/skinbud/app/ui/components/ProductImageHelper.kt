package com.skinbud.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.skinbud.app.R

object ProductImageHelper {

    @DrawableRes
    fun getProductDrawableResId(
        productId: String?,
        category: String? = null,
        productName: String? = null
    ): Int {
        val id = productId?.lowercase() ?: ""
        val name = productName?.lowercase() ?: ""

        return when {
            // ID matches
            id.contains("cleanse_bha") || id.contains("salicylic") || name.contains("salicylic") -> {
                R.drawable.prod_cleanse_bha
            }
            id.contains("cleanse") || id.contains("oat") || name.contains("oat") || name.contains("cleanser") -> {
                R.drawable.prod_cleanse_oat
            }
            id.contains("toner") || id.contains("heartleaf") || name.contains("heartleaf") || name.contains("toner") -> {
                R.drawable.prod_toner_heartleaf
            }
            id.contains("cica") || id.contains("centella") || name.contains("cica") || name.contains("centella") -> {
                R.drawable.prod_serum_cica
            }
            id.contains("hyaluronic") || id.contains("serum") || name.contains("hyaluronic") || name.contains("serum") || name.contains("elixir") -> {
                R.drawable.prod_serum_hyaluronic
            }
            // Category fallbacks
            category?.uppercase()?.contains("CLEANSER") == true -> R.drawable.prod_cleanse_oat
            category?.uppercase()?.contains("TONER") == true -> R.drawable.prod_toner_heartleaf
            category?.uppercase()?.contains("SERUM") == true -> R.drawable.prod_serum_hyaluronic
            category?.uppercase()?.contains("MOISTURIZER") == true -> R.drawable.prod_cleanse_oat
            category?.uppercase()?.contains("SUNSCREEN") == true -> R.drawable.prod_cleanse_oat
            category?.uppercase()?.contains("EXFOLIANT") == true -> R.drawable.prod_cleanse_bha
            else -> R.drawable.prod_cleanse_oat
        }
    }
}

@Composable
fun ProductThumbnailImage(
    productId: String?,
    productName: String? = null,
    category: String? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Crop
) {
    val resId = ProductImageHelper.getProductDrawableResId(
        productId = productId,
        category = category,
        productName = productName
    )
    Image(
        painter = painterResource(id = resId),
        contentDescription = productName ?: "Product Image",
        modifier = modifier,
        contentScale = contentScale
    )
}
