package com.skinbud.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinbud.app.data.CartRepository
import com.skinbud.app.ui.theme.DarkSlate
import com.skinbud.app.ui.theme.MutedOlive
import com.skinbud.app.ui.theme.RoseQuartz
import com.skinbud.app.ui.theme.SageEmerald
import com.skinbud.app.ui.theme.WarmChampagne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartRepo: CartRepository,
    onDismiss: () -> Unit
) {
    val items by cartRepo.items.collectAsState()
    val summary by cartRepo.summary.collectAsState()
    var isCheckingOut by remember { mutableStateOf(false) }
    var orderPlaced by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (orderPlaced) {
            // Order Success State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(SageEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SageEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Order Confirmed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )

                Text(
                    text = "Order #SB-94821 • Estimated Delivery in 2 Days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedOlive
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sustainability & Return Impact Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SageEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SageEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero-Return Guarantee Verified",
                                fontWeight = FontWeight.Bold,
                                color = SageEmerald,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Thanks to your YouCam AI skin diagnostic & VTO shade confirmation, return probability is under 1.8%. You prevented approximately $28.50 in reverse-logistics carbon emissions!",
                            fontSize = 11.sp,
                            color = DarkSlate,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        cartRepo.clearCart()
                        orderPlaced = false
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz)
                ) {
                    Text("Return to Boutique", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = RoseQuartz,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Your Skincare Bag",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = RoseQuartz.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${items.sumOf { it.quantity }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseQuartz,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DarkSlate)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Return Risk Optimization Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SageEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SageEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "YouCam VTO Return-Risk Protection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = DarkSlate
                            )
                            Text(
                                text = "Return likelihood reduced by ${summary.returnRiskReductionPercent}% via Fitzpatrick & VTO match",
                                fontSize = 11.sp,
                                color = SageEmerald,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your bag is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Discover personalized formulations in the Boutique or try shades in VTO Studio.",
                            fontSize = 12.sp,
                            color = MutedOlive,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .height((items.size * 105).coerceAtMost(320).dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        // Product Thumbnail Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFF9F7F5),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEFE8E1)),
                                            modifier = Modifier.size(52.dp)
                                        ) {
                                            if (item.shadeHex != null) {
                                                val swatchColor = try {
                                                    Color(android.graphics.Color.parseColor(item.shadeHex))
                                                } catch (_: Exception) {
                                                    Color(0xFFD19A78)
                                                }
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(swatchColor)
                                                    )
                                                }
                                            } else {
                                                ProductThumbnailImage(
                                                    productId = item.id,
                                                    productName = item.title,
                                                    category = item.category,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = DarkSlate,
                                                maxLines = 1
                                            )
                                            if (item.shadeName != null) {
                                                Text(
                                                    text = item.shadeName,
                                                    fontSize = 11.sp,
                                                    color = RoseQuartz,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "$${String.format("%.2f", item.price)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DarkSlate
                                            )
                                        }
                                    }

                                    // Quantity Controls
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clickable { cartRepo.updateQuantity(item.id, -1) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp), tint = DarkSlate)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "${item.quantity}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkSlate
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clickable { cartRepo.updateQuantity(item.id, 1) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp), tint = DarkSlate)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subtotal", fontSize = 13.sp, color = MutedOlive)
                        Text(text = "$${String.format("%.2f", summary.subtotal)}", fontSize = 13.sp, color = DarkSlate)
                    }

                    if (summary.discount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Routine Bundle Discount (15%)", fontSize = 13.sp, color = SageEmerald, fontWeight = FontWeight.SemiBold)
                            Text(text = "-$${String.format("%.2f", summary.discount)}", fontSize = 13.sp, color = SageEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Estimated Shipping", fontSize = 13.sp, color = MutedOlive)
                        Text(text = if (summary.shipping == 0.0) "FREE" else "$${String.format("%.2f", summary.shipping)}", fontSize = 13.sp, color = DarkSlate)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                        Text(text = "$${String.format("%.2f", summary.total)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = RoseQuartz)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { orderPlaced = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proceed to 1-Click Checkout • $${String.format("%.2f", summary.total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
