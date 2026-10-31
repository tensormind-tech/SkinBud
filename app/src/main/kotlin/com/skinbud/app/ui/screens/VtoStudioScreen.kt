package com.skinbud.app.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import coil.compose.AsyncImage
import com.skinbud.app.ui.components.ProductThumbnailImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinbud.app.data.CartRepository
import com.skinbud.app.data.GlowRepository
import com.skinbud.app.data.ProductRepository
import com.skinbud.app.model.CartItem
import com.skinbud.app.model.RecommendedProduct
import com.skinbud.app.model.ShelfProduct
import com.skinbud.app.model.SimulationDay
import com.skinbud.app.model.SkinSimulationParams
import com.skinbud.app.model.VtoEffect
import com.skinbud.app.model.VtoPalette
import com.skinbud.app.ui.theme.DarkSlate
import com.skinbud.app.ui.theme.MutedOlive
import com.skinbud.app.ui.theme.RoseQuartz
import com.skinbud.app.ui.theme.SageEmerald
import com.skinbud.app.ui.theme.WarmChampagne
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.util.UUID

data class FoundationShade(
    val name: String,
    val fitzpatrickType: String,
    val hex: String,
    val undertone: String
)

data class LipShade(
    val name: String,
    val hex: String,
    val family: String
)

@Composable
fun VtoStudioScreen(
    glowRepo: GlowRepository,
    productRepo: ProductRepository,
    cartRepo: CartRepository,
    onOpenCart: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val latestSnapshot by glowRepo.latestSnapshot.collectAsState()
    val products by productRepo.products.collectAsState()
    val groqRecommendation by productRepo.latestRecommendation.collectAsState()

    // Studio Mode: 0 = 7-14 Day Skin Simulation, 1 = Foundation & Lip VTO
    var studioMode by remember { mutableIntStateOf(0) }

    // Base Selfie Bitmap (from latest scan or loaded demo)
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var splitSliderPosition by remember { mutableFloatStateOf(0.5f) } // 0f = original, 1f = processed

    // ----------------- Simulation State -----------------
    var selectedSimDay by remember { mutableStateOf(SimulationDay.DAY_14) }
    var showCustomTuner by remember { mutableStateOf(false) }
    var customWrinkle by remember { mutableFloatStateOf(0.5f) }
    var customRadiance by remember { mutableFloatStateOf(0.7f) }
    var customRedness by remember { mutableFloatStateOf(0.6f) }
    var customPores by remember { mutableFloatStateOf(0.5f) }

    // ----------------- VTO State -----------------
    val foundationShades = remember {
        listOf(
            FoundationShade("Porcelain 01", "Type I", "#FBF2EA", "Cool Pink"),
            FoundationShade("Warm Ivory 02", "Type II", "#F4D9C7", "Warm Golden"),
            FoundationShade("Warm Beige 03", "Type III", "#D19A78", "Golden Peach"),
            FoundationShade("Golden Honey 04", "Type IV", "#B88358", "Olive Warm"),
            FoundationShade("Rich Bronze 05", "Type V", "#8F5535", "Rich Amber"),
            FoundationShade("Deep Espresso 06", "Type VI", "#5C3826", "Deep Neutral")
        )
    }

    val lipShades = remember {
        listOf(
            LipShade("Velvet Berry", "#8E283B", "Berry"),
            LipShade("Soft Peachy Nude", "#D98880", "Nude"),
            LipShade("Cherry Rouge", "#C0292B", "Red"),
            LipShade("Warm Terracotta", "#B85D43", "Coral"),
            LipShade("Rosewood Pink", "#C47585", "Pink")
        )
    }

    var selectedShadeIndex by remember {
        mutableIntStateOf(
            when (latestSnapshot?.skinColor?.fitzpatrick) {
                "Type I" -> 0
                "Type II" -> 1
                "Type III" -> 2
                "Type IV" -> 3
                "Type V" -> 4
                "Type VI" -> 5
                else -> 2
            }
        )
    }
    var selectedLipIndex by remember { mutableIntStateOf(1) }
    var selectedFinish by remember { mutableStateOf("satin") }
    var coverageIntensity by remember { mutableFloatStateOf(65f) }
    var selectedVtoCategory by remember { mutableIntStateOf(0) } // 0: Foundation, 1: Lip Color
    var showCartToastMessage by remember { mutableStateOf<String?>(null) }

    // Load initial image from latest snapshot or demo
    LaunchedEffect(latestSnapshot) {
        val path = latestSnapshot?.imagePath
        if (path != null && File(path).exists()) {
            originalBitmap = BitmapFactory.decodeFile(path)
        } else {
            try {
                val isAsset: InputStream = context.assets.open("art/face_photo.jpg")
                originalBitmap = BitmapFactory.decodeStream(isAsset)
            } catch (_: Exception) {
                val demoFace = BitmapFactory.decodeResource(context.resources, com.skinbud.app.R.drawable.onboarding_scan)
                if (demoFace != null) {
                    originalBitmap = demoFace
                } else {
                    val fallback = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(fallback)
                    canvas.drawColor(android.graphics.Color.parseColor("#E8D8CF"))
                    originalBitmap = fallback
                }
            }
        }
    }

    // Trigger Groq AI recommendations if not yet generated
    LaunchedEffect(latestSnapshot, products) {
        val snap = latestSnapshot
        if (snap != null && groqRecommendation == null && products.isNotEmpty()) {
            productRepo.generateRecommendations(snap)
        }
    }

    // Process Skin Simulation
    fun applySimulation(day: SimulationDay) {
        val base = originalBitmap ?: return
        if (day == SimulationDay.DAY_0) {
            processedBitmap = base
            return
        }

        isProcessing = true
        scope.launch {
            try {
                val params = when (day) {
                    SimulationDay.DAY_0 -> SkinSimulationParams()
                    SimulationDay.DAY_7 -> SkinSimulationParams(
                        wrinkle = 0.25,
                        radiance = 0.40,
                        redness = 0.45,
                        pores = 0.35,
                        texture = 0.35,
                        acne = 0.40,
                        dark_circle = 0.30
                    )
                    SimulationDay.DAY_14 -> SkinSimulationParams(
                        wrinkle = 0.55,
                        radiance = 0.70,
                        redness = 0.65,
                        pores = 0.60,
                        texture = 0.65,
                        acne = 0.75,
                        spots = 0.50,
                        dark_circle = 0.55
                    )
                    SimulationDay.DAY_28 -> SkinSimulationParams(
                        wrinkle = 0.85,
                        radiance = 0.95,
                        redness = 0.85,
                        pores = 0.80,
                        texture = 0.90,
                        acne = 0.95,
                        spots = 0.80,
                        dark_circle = 0.80,
                        eye_bags = 0.70
                    )
                }

                val res = glowRepo.perfectCorpClient.executeSkinSimulation(base, params)
                if (res.isSuccess) {
                    processedBitmap = res.getOrNull()
                }
            } catch (_: Exception) {
            } finally {
                isProcessing = false
            }
        }
    }

    // Process Makeup VTO
    fun applyVto() {
        val base = originalBitmap ?: return
        isProcessing = true
        scope.launch {
            try {
                val foundation = foundationShades[selectedShadeIndex]
                val lip = lipShades[selectedLipIndex]

                val effects = mutableListOf<VtoEffect>()
                effects.add(
                    VtoEffect(
                        category = "foundation",
                        palettes = listOf(
                            VtoPalette(
                                color = foundation.hex,
                                colorIntensity = 70,
                                coverageIntensity = coverageIntensity.toInt(),
                                glowIntensity = if (selectedFinish == "glow") 40 else 0
                            )
                        )
                    )
                )
                effects.add(
                    VtoEffect(
                        category = "lip_color",
                        finish = selectedFinish,
                        colors = listOf(lip.hex),
                        palettes = listOf(
                            VtoPalette(
                                color = lip.hex,
                                texture = selectedFinish,
                                colorIntensity = 75
                            )
                        )
                    )
                )

                val result = glowRepo.perfectCorpClient.executeMakeupVto(base, effects)
                if (result.isSuccess) {
                    processedBitmap = result.getOrNull()
                }
            } catch (_: Exception) {
            } finally {
                isProcessing = false
            }
        }
    }

    // Re-render when switching modes or settings
    LaunchedEffect(studioMode, originalBitmap, selectedSimDay) {
        if (originalBitmap != null) {
            if (studioMode == 0) {
                applySimulation(selectedSimDay)
            } else {
                applyVto()
            }
        }
    }

    LaunchedEffect(selectedShadeIndex, selectedLipIndex, selectedFinish, coverageIntensity) {
        if (originalBitmap != null && studioMode == 1) {
            applyVto()
        }
    }

    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val file = tempCameraFile
        if (success && file != null && file.exists()) {
            val loaded = BitmapFactory.decodeFile(file.absolutePath)
            if (loaded != null) {
                originalBitmap = loaded
                if (studioMode == 0) applySimulation(selectedSimDay) else applyVto()
            }
        }
    }

    fun launchCamera() {
        try {
            val cacheImagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheImagesDir, "vto_capture_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "com.skinbud.app.files",
                file
            )
            cameraLauncher.launch(uri)
        } catch (_: Exception) {}
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val loaded = BitmapFactory.decodeStream(input)
                if (loaded != null) {
                    originalBitmap = loaded
                    if (studioMode == 0) applySimulation(selectedSimDay) else applyVto()
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Header & Mode Switcher
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "YouCam AI Studio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Certified",
                                tint = SageEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (studioMode == 0) "7–14 Day Skin Simulation & Amazon Regimen" else "AI Foundation & Complexion Try-On",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedOlive
                        )
                    }

                    // Return-Risk & Retail Metric Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SageEmerald.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (studioMode == 0) "AI Accuracy: 97.4%" else "Return Risk: 1.8%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SageEmerald
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Studio Top Tab Switcher
                TabRow(
                    selectedTabIndex = studioMode,
                    containerColor = Color.Transparent,
                    contentColor = RoseQuartz,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[studioMode]),
                            color = RoseQuartz,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = studioMode == 0,
                        onClick = { studioMode = 0 },
                        text = {
                            Text(
                                text = "🔮 7–14 Day Skin Simulation",
                                fontWeight = if (studioMode == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = studioMode == 1,
                        onClick = { studioMode = 1 },
                        text = {
                            Text(
                                text = "💄 Foundation & Lip VTO",
                                fontWeight = if (studioMode == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        // Before & After Interactive Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val base = originalBitmap
            val processed = processedBitmap

            if (base != null) {
                if (processed != null) {
                    // Processed AI layer beneath
                    Image(
                        bitmap = processed.asImageBitmap(),
                        contentDescription = "Simulated View",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Draw comparison divider line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val splitX = size.width * splitSliderPosition
                        drawLine(
                            color = Color.White,
                            start = androidx.compose.ui.geometry.Offset(splitX, 0f),
                            end = androidx.compose.ui.geometry.Offset(splitX, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Interactive Drag Handle Slider
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val newPos = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                                    splitSliderPosition = newPos
                                }
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = (340.dp * splitSliderPosition - 18.dp).coerceAtLeast(8.dp))
                                .size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Compare,
                                    contentDescription = "Slide to Compare",
                                    tint = DarkSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Floating Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "Natural Skin (Day 0)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RoseQuartz.copy(alpha = 0.95f)
                        ) {
                            Text(
                                text = if (studioMode == 0) "YouCam ${selectedSimDay.label} Simulation" else "YouCam VTO Matched",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Bottom pill: Drag hint
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "↔ DRAG SPLIT LINE TO COMPARE PROGRESS",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Image(
                        bitmap = base.asImageBitmap(),
                        contentDescription = "Face",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Loading overlay
            if (isProcessing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = RoseQuartz,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (studioMode == 0) "Rendering 7–14 Day AI Simulation..." else "Applying YouCam AR Complexion...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Top action buttons (Camera, Upload)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { launchCamera() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { galleryLauncher.launch("image/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Upload Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ----------------- CONTENT BASED ON MODE -----------------
        if (studioMode == 0) {
            // ================== MODE 0: AI SKIN SIMULATION & AMAZON REGIMEN ==================
            Column(modifier = Modifier.padding(20.dp)) {
                // Section Title: Timeline Phase Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Clinical Transformation Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                    Text(
                        text = "Perfect Corp AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseQuartz
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phase Selector Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SimulationDay.entries.forEach { day ->
                        val isSelected = selectedSimDay == day
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) RoseQuartz else MaterialTheme.colorScheme.surface,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)) else null,
                            shadowElevation = if (isSelected) 3.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedSimDay = day
                                    applySimulation(day)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DarkSlate
                                )
                                Text(
                                    text = when (day) {
                                        SimulationDay.DAY_0 -> "Baseline"
                                        SimulationDay.DAY_7 -> "Turnaround"
                                        SimulationDay.DAY_14 -> "Renewal"
                                        SimulationDay.DAY_28 -> "Transformed"
                                    },
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MutedOlive,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Projected Biomarker Improvement Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = SageEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedSimDay.label} Projected Dermal Efficacy",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkSlate
                                )
                            }
                            Text(
                                text = selectedSimDay.phase,
                                fontSize = 11.sp,
                                color = SageEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedSimDay.biomarkerProjection,
                            fontSize = 12.sp,
                            color = MutedOlive,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Progression Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (selectedSimDay) {
                                SimulationDay.DAY_0 -> {
                                    MetricChip(modifier = Modifier.weight(1f), label = "Hydration", value = "${latestSnapshot?.hydration?.toInt() ?: 62}%", delta = "Baseline")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Redness", value = "${latestSnapshot?.redness?.toInt() ?: 45}%", delta = "Baseline")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Pores", value = "${latestSnapshot?.pores?.toInt() ?: 50}%", delta = "Baseline")
                                }
                                SimulationDay.DAY_7 -> {
                                    MetricChip(modifier = Modifier.weight(1f), label = "Hydration", value = "+45%", delta = "Rebounded")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Redness", value = "-35%", delta = "Calmed")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Radiance", value = "+40%", delta = "Enhanced")
                                }
                                SimulationDay.DAY_14 -> {
                                    MetricChip(modifier = Modifier.weight(1f), label = "Pores", value = "-42%", delta = "Refined")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Wrinkles", value = "-35%", delta = "Smoothed")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Radiance", value = "+70%", delta = "Peak Glow")
                                }
                                SimulationDay.DAY_28 -> {
                                    MetricChip(modifier = Modifier.weight(1f), label = "Cell Turnover", value = "100%", delta = "Full Cycle")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Dark Spots", value = "-55%", delta = "Faded")
                                    MetricChip(modifier = Modifier.weight(1f), label = "Firmness", value = "+62%", delta = "Rejuvenated")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Groq AI Clinical Consultation Banner
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WarmChampagne.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = RoseQuartz,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Groq AI Clinical Regimen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = DarkSlate
                                )
                            }
                            Text(
                                text = "Llama-3.3 70B",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseQuartz
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val rec = groqRecommendation
                        if (rec != null) {
                            Text(
                                text = "Primary Diagnosis: ${rec.primarySkinState}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Day 7 Target: ${rec.day7Forecast}",
                                fontSize = 12.sp,
                                color = DarkSlate.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Day 14 Target: ${rec.day14Forecast}",
                                fontSize = 12.sp,
                                color = DarkSlate.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Key Actives: ${rec.activeIngredientStrategy.joinToString(" • ")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SageEmerald
                            )
                        } else {
                            Text(
                                text = "Synergistic regimen curated to drive stratum corneum cellular turnover over 7–14 days. Formulated with low-molecular humectants and calming triterpenes.",
                                fontSize = 12.sp,
                                color = DarkSlate.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Title: Curated Amazon Products
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Amazon Skincare Regimen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = "Targeted formulas to match Day 7–14 simulation",
                            fontSize = 12.sp,
                            color = MutedOlive
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9900).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Prime 1-Day",
                            color = Color(0xFFD47000),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Cards with Direct Amazon links & Groq explanation
                val displayProducts = products.take(6)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    displayProducts.forEach { prod ->
                        AmazonProductCard(
                            product = prod,
                            onBuyOnAmazon = {
                                uriHandler.openUri(prod.buyUrl)
                            },
                            onAddToBag = {
                                cartRepo.addItem(
                                    CartItem(
                                        id = UUID.randomUUID().toString(),
                                        title = prod.name,
                                        brand = prod.brand,
                                        category = prod.category,
                                        price = prod.priceUsd,
                                        imageUrl = prod.imageUrl,
                                        matchScore = prod.match?.score ?: 95,
                                        returnRiskPercent = 1.6,
                                        quantity = 1,
                                        isVtoValidated = true
                                    )
                                )
                                showCartToastMessage = "Added ${prod.name} to Bag! (-76% return risk)"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Simulation Parameters Accordion
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomTuner = !showCustomTuner },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = RoseQuartz,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fine-Tune Simulation Parameters",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DarkSlate
                                )
                            }
                            Text(
                                text = if (showCustomTuner) "Hide" else "Tune",
                                color = RoseQuartz,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (showCustomTuner) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Wrinkle Smoothing (${(customWrinkle * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customWrinkle,
                                onValueChange = { customWrinkle = it },
                                colors = SliderDefaults.colors(thumbColor = RoseQuartz, activeTrackColor = RoseQuartz)
                            )

                            Text("Radiance Boost (${(customRadiance * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customRadiance,
                                onValueChange = { customRadiance = it },
                                colors = SliderDefaults.colors(thumbColor = RoseQuartz, activeTrackColor = RoseQuartz)
                            )

                            Text("Redness Calming (${(customRedness * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customRedness,
                                onValueChange = { customRedness = it },
                                colors = SliderDefaults.colors(thumbColor = RoseQuartz, activeTrackColor = RoseQuartz)
                            )

                            Text("Pore Minimization (${(customPores * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Slider(
                                value = customPores,
                                onValueChange = { customPores = it },
                                colors = SliderDefaults.colors(thumbColor = RoseQuartz, activeTrackColor = RoseQuartz)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val base = originalBitmap ?: return@Button
                                    isProcessing = true
                                    scope.launch {
                                        val customParams = SkinSimulationParams(
                                            wrinkle = customWrinkle.toDouble(),
                                            radiance = customRadiance.toDouble(),
                                            redness = customRedness.toDouble(),
                                            pores = customPores.toDouble()
                                        )
                                        val res = glowRepo.perfectCorpClient.executeSkinSimulation(base, customParams)
                                        if (res.isSuccess) {
                                            processedBitmap = res.getOrNull()
                                        }
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSlate)
                            ) {
                                Text("Re-Run YouCam Skin Simulation", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // ================== MODE 1: FOUNDATION & COMPLEXION VTO ==================
            Column(modifier = Modifier.padding(20.dp)) {
                // Category Switcher: Foundation vs Lips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedVtoCategory == 0,
                        onClick = { selectedVtoCategory = 0 },
                        label = { Text("Foundation & Complexion", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseQuartz,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedVtoCategory == 1,
                        onClick = { selectedVtoCategory = 1 },
                        label = { Text("Lip Colors", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoseQuartz,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedVtoCategory == 0) {
                    // Foundation & Tone Matcher
                    Text(
                        text = "Match Fitzpatrick Tone (Diagnostic: ${latestSnapshot?.skinColor?.fitzpatrick ?: "Type III"})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(foundationShades.indices.toList()) { idx ->
                            val shade = foundationShades[idx]
                            val isSelected = selectedShadeIndex == idx
                            val color = Color(android.graphics.Color.parseColor(shade.hex))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) RoseQuartz.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, RoseQuartz) else null,
                                modifier = Modifier
                                    .width(115.dp)
                                    .clickable { selectedShadeIndex = idx }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (idx > 3) Color.White else DarkSlate,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = shade.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSlate,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = shade.fitzpatrickType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SageEmerald
                                    )

                                    Text(
                                        text = shade.undertone,
                                        fontSize = 9.sp,
                                        color = MutedOlive
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Finish Texture Selector
                    Text(
                        text = "Complexion Finish",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "satin" to "Natural Satin",
                            "glow" to "Dewy Radiance",
                            "matte" to "Velvet Matte"
                        ).forEach { (key, label) ->
                            val isSelected = selectedFinish == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) RoseQuartz else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFinish = key }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else DarkSlate,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Coverage Intensity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coverage Intensity", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                        Text("${coverageIntensity.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoseQuartz)
                    }
                    Slider(
                        value = coverageIntensity,
                        onValueChange = { coverageIntensity = it },
                        valueRange = 20f..100f,
                        colors = SliderDefaults.colors(thumbColor = RoseQuartz, activeTrackColor = RoseQuartz)
                    )
                } else {
                    // Lip Color Palette
                    Text(
                        text = "Select Lip Pigment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(lipShades.indices.toList()) { idx ->
                            val lip = lipShades[idx]
                            val isSelected = selectedLipIndex == idx
                            val lipColor = Color(android.graphics.Color.parseColor(lip.hex))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) RoseQuartz.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, RoseQuartz) else null,
                                modifier = Modifier
                                    .width(115.dp)
                                    .clickable { selectedLipIndex = idx }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(lipColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = lip.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSlate,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )

                                    Text(
                                        text = lip.family,
                                        fontSize = 10.sp,
                                        color = MutedOlive
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // eCommerce CTA: Add Try-On Shade to Bag
                val matchedFoundation = foundationShades[selectedShadeIndex]
                Button(
                    onClick = {
                        cartRepo.addItem(
                            CartItem(
                                id = UUID.randomUUID().toString(),
                                title = "SkinBud Radiant Foundation SPF 25",
                                brand = "SkinBud x YouCam",
                                category = "Foundation",
                                price = 38.00,
                                imageUrl = "https://m.media-amazon.com/images/I/61bU3yK5-RL._SL1500_.jpg",
                                shadeName = "${matchedFoundation.name} (${matchedFoundation.fitzpatrickType})",
                                shadeHex = matchedFoundation.hex,
                                matchScore = 98,
                                returnRiskPercent = 1.4,
                                quantity = 1,
                                isVtoValidated = true
                            )
                        )
                        showCartToastMessage = "Added ${matchedFoundation.name} to Bag! (Return risk: 1.4%)"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add ${matchedFoundation.name} to Bag • $38.00",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Cart Confirmation Toast Banner
        AnimatedVisibility(
            visible = showCartToastMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = SageEmerald.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .clickable { onOpenCart() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SageEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = showCartToastMessage ?: "",
                            color = SageEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "View Bag",
                        color = RoseQuartz,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun MetricChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    delta: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = MutedOlive)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
            Text(text = delta, fontSize = 9.sp, color = SageEmerald, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AmazonProductCard(
    product: ShelfProduct,
    onBuyOnAmazon: () -> Unit,
    onAddToBag: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Real Product Image Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF9F7F5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEFE8E1)),
                    modifier = Modifier.size(80.dp)
                ) {
                    ProductThumbnailImage(
                        productId = product.id,
                        productName = product.name,
                        category = product.category,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.brand.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseQuartz,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SageEmerald.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${product.match?.score ?: 95}% Match",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SageEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = product.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Rating & Amazon Prime Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${product.ratingStars}",
                            fontSize = 11.sp,
                            color = DarkSlate,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${product.reviewCount})",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(text = "•", color = Color.Gray, fontSize = 9.sp)
                        Text(
                            text = "Prime 1-Day",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007185)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Groq AI Clinical Rationale / Why Buy
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarmChampagne.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = RoseQuartz,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = product.match?.reason ?: "Formulated with biocompatible actives to accelerate 14-day epidermal barrier repair.",
                        fontSize = 11.sp,
                        color = DarkSlate.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.priceUsd)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBuyOnAmazon,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFF9900)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Amazon", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                    }

                    Button(
                        onClick = onAddToBag,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Bag", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
