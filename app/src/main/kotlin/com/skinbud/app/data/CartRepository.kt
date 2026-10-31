package com.skinbud.app.data

import android.content.Context
import com.skinbud.app.model.CartItem
import com.skinbud.app.model.CartSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class CartRepository(private val context: Context) {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _summary = MutableStateFlow(calculateSummary(emptyList()))
    val summary: StateFlow<CartSummary> = _summary.asStateFlow()

    init {
        seedInitialCart()
    }

    private fun seedInitialCart() {
        val initial = listOf(
            CartItem(
                id = UUID.randomUUID().toString(),
                title = "SkinBud Precision Radiant Foundation",
                brand = "SkinBud Lab x YouCam",
                category = "Foundation",
                price = 38.00,
                imageUrl = "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg",
                shadeName = "Warm Beige (Type III)",
                shadeHex = "#D19A78",
                matchScore = 98,
                returnRiskPercent = 1.4,
                quantity = 1,
                isVtoValidated = true
            ),
            CartItem(
                id = UUID.randomUUID().toString(),
                title = "Barrier Ceramide Hydrating Fluid",
                brand = "SkinBud Lab",
                category = "Moisturizer",
                price = 32.00,
                imageUrl = "https://m.media-amazon.com/images/I/61S7BrLnZmL._SL1500_.jpg",
                size = "50 ml / 1.7 fl oz",
                matchScore = 96,
                returnRiskPercent = 2.1,
                quantity = 1,
                isVtoValidated = false
            )
        )
        _items.value = initial
        _summary.value = calculateSummary(initial)
    }

    fun addItem(item: CartItem) {
        val current = _items.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.title == item.title && it.shadeName == item.shadeName }
        if (existingIndex >= 0) {
            val existing = current[existingIndex]
            current[existingIndex] = existing.copy(quantity = existing.quantity + item.quantity)
        } else {
            current.add(0, item)
        }
        _items.value = current
        _summary.value = calculateSummary(current)
    }

    fun removeItem(itemId: String) {
        val updated = _items.value.filterNot { it.id == itemId }
        _items.value = updated
        _summary.value = calculateSummary(updated)
    }

    fun updateQuantity(itemId: String, delta: Int) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _items.value = current
            _summary.value = calculateSummary(current)
        }
    }

    fun clearCart() {
        _items.value = emptyList()
        _summary.value = calculateSummary(emptyList())
    }

    private fun calculateSummary(items: List<CartItem>): CartSummary {
        val subtotal = items.sumOf { it.price * it.quantity }
        val discount = if (subtotal > 50.0) subtotal * 0.15 else 0.0 // 15% discount for complete routines
        val shipping = if (subtotal >= 45.0 || subtotal == 0.0) 0.0 else 5.99
        val total = (subtotal - discount + shipping).coerceAtLeast(0.0)
        
        // Return-risk calculation
        // Industry baseline return rate for online cosmetics is ~32%.
        // With AI Skin Analysis & VTO precision match, return rate drops to ~2.2%
        val reduction = if (items.isNotEmpty()) 78.4 else 0.0
        val savedLogistics = if (items.isNotEmpty()) items.size * 14.25 else 0.0

        return CartSummary(
            subtotal = subtotal,
            discount = discount,
            shipping = shipping,
            total = total,
            returnRiskReductionPercent = reduction,
            estimatedLogisticsSaved = savedLogistics
        )
    }
}
