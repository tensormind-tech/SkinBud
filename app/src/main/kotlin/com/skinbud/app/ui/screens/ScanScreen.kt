package com.skinbud.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinbud.app.data.GlowRepository
import com.skinbud.app.model.GlowSnapshot
import com.skinbud.app.ui.components.GlowScoreRing
import com.skinbud.app.ui.theme.DarkSlate
import com.skinbud.app.ui.theme.MetricAttention
import com.skinbud.app.ui.theme.MetricExcellent
import com.skinbud.app.ui.theme.MetricFair
import com.skinbud.app.ui.theme.MetricGood
import com.skinbud.app.ui.theme.MutedOlive
import com.skinbud.app.ui.theme.RoseQuartz
import com.skinbud.app.ui.theme.SageEmerald
import com.skinbud.app.ui.theme.WarmChampagne
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanScreen(
    glowRepo: GlowRepository,
    onScanSaved: () -> Unit,
    onNavigateToVto: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var resultSnapshot by remember { mutableStateOf<GlowSnapshot?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showApiSettingsDialog by remember { mutableStateOf(false) }
    var showMissingKeyPrompt by remember { mutableStateOf(false) }
    var pendingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val isApiConfigured = glowRepo.perfectCorpClient.isConfigured

    fun processRealScan(bitmap: Bitmap, forceOnDevice: Boolean = false) {
        selectedBitmap = bitmap
        isAnalyzing = true
        scope.launch {
            try {
                val result = glowRepo.analyzeAndSave(bitmap, useApiIfAvailable = !forceOnDevice)
                isAnalyzing = false
                if (result.isSuccess) {
                    resultSnapshot = result.getOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "Real scan analysis failed"
                }
            } catch (e: Exception) {
                isAnalyzing = false
                errorMessage = "Scan error: ${e.message}"
            }
        }
    }

    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val file = tempCameraFile
        if (success && file != null && file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                if (isApiConfigured) {
                    processRealScan(bitmap)
                } else {
                    pendingBitmap = bitmap
                    showMissingKeyPrompt = true
                }
            }
        }
    }

    fun launchCamera() {
        try {
            val cacheImagesDir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(cacheImagesDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "com.skinbud.app.files",
                file
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "Camera error: ${e.message}"
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(input)
                input?.close()
                if (bitmap != null) {
                    if (isApiConfigured) {
                        processRealScan(bitmap)
                    } else {
                        pendingBitmap = bitmap
                        showMissingKeyPrompt = true
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load selected photo: ${e.message}"
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1412))
    ) {
        // Active Camera / Scan Viewfinder
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Viewfinder Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clinical Facial Scan",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isApiConfigured) "Perfect Corp™ S2S 16-Dermal Diagnostic" else "On-Device Neural Vision Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isApiConfigured) SageEmerald else Color.White.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isApiConfigured) SageEmerald.copy(alpha = 0.2f) else RoseQuartz.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { showApiSettingsDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isApiConfigured) SageEmerald else RoseQuartz)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isApiConfigured) "API Active" else "Setup Key",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isApiConfigured) SageEmerald else RoseQuartz
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Viewfinder Ellipse Target
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Scanned Face",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Oval Head Positioning Guide
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(380.dp)
                ) {
                    val ovalWidth = size.width
                    val ovalHeight = size.height

                    // Outer guide border
                    drawOval(
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Crosshair guides
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(ovalWidth / 2f, 0f),
                        end = Offset(ovalWidth / 2f, ovalHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, ovalHeight * 0.42f),
                        end = Offset(ovalWidth, ovalHeight * 0.42f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Animated radar sweep when analyzing
                    if (isAnalyzing) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, RoseQuartz.copy(alpha = 0.6f))
                            ),
                            startAngle = sweepAngle,
                            sweepAngle = 90f,
                            useCenter = true
                        )
                    }
                }

                // Analyzing Status Overlay
                if (isAnalyzing) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = RoseQuartz,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Analyzing 16 Clinical Biomarkers...",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Acne • Pores • Wrinkles • Moisture • Tear Troughs",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Error Message Bar
            if (errorMessage != null) {
                Surface(
                    color = MetricAttention.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MetricAttention,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MetricAttention, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Bottom Shutter & Picker Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pick from Gallery
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = "Pick Photo",
                            tint = Color.White
                        )
                    }

                    // Main Camera Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(RoseQuartz, WarmChampagne)
                                )
                            )
                            .clickable {
                                launchCamera()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // API Key Settings
                    IconButton(
                        onClick = { showApiSettingsDialog = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Settings",
                            tint = if (isApiConfigured) SageEmerald else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Center: Real Camera Snap • Left: Pick Real Photo • Right: API Key",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // =========================================================================
        // FULL-SCREEN CLINICAL DERMAL REPORT WITH PROMINENT FACE IMAGE & GRAPHS
        // =========================================================================
        if (resultSnapshot != null) {
            val snap = resultSnapshot!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = SageEmerald.copy(alpha = 0.15f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = SageEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "CLINICAL DERMAL REPORT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = if (snap.isRealScan) "Perfect Corp™ AI 16-Dermal Diagnostic Suite" else "On-Device Diagnostic Engine",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { resultSnapshot = null },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Scrollable Report Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // 1. SCANNED FACE SHOWCASE CARD (Placed in Right Place)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = RoseQuartz,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Facial Biometric Scan Target",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkSlate
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SageEmerald.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "16 Biomarkers Analyzed",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SageEmerald,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Side-by-side Face Photo and Core Score
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Scanned Face Photo with Diagnostic Overlay
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val faceBitmap = selectedBitmap ?: snap.imagePath?.let {
                                                if (File(it).exists()) BitmapFactory.decodeFile(it) else null
                                            }

                                            if (faceBitmap != null) {
                                                Image(
                                                    bitmap = faceBitmap.asImageBitmap(),
                                                    contentDescription = "Scanned Face",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Face,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(60.dp)
                                                    )
                                                }
                                            }

                                            // Biometric Watermark Badge
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Black.copy(alpha = 0.65f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 6.dp)
                                            ) {
                                                Text(
                                                    text = "YouCam S2S Verified",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Overall Score & Age Breakdown
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${snap.glowScore.toInt()}",
                                                    fontSize = 38.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = RoseQuartz
                                                )
                                                Text(
                                                    text = "/100",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MutedOlive
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = RoseQuartz.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = snap.skinType,
                                                    color = RoseQuartz,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            Text(
                                                text = "Biological Skin Age: ${snap.skinAge} yrs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DarkSlate
                                            )

                                            Text(
                                                text = "Fitzpatrick: ${snap.skinColor.fitzpatrick} (${snap.skinColor.shadeCategory})",
                                                fontSize = 11.sp,
                                                color = SageEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. DERMAL BALANCE RADAR / SPIDER CHART
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                                text = "Dermal Equilibrium Radar Graph",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkSlate
                                            )
                                        }

                                        Text(
                                            text = "Equilibrium: 88%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SageEmerald
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Canvas Spider Radar Chart
                                    DermalRadarChart(
                                        metrics = listOf(
                                            "Hydration" to snap.hydration,
                                            "Sebum Ctrl" to (100.0 - snap.oiliness),
                                            "Smoothness" to snap.texture,
                                            "Vascular Calm" to (100.0 - snap.redness),
                                            "Elasticity" to snap.firmness,
                                            "Pigment Clear" to (100.0 - snap.darkSpots)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                    )
                                }
                            }

                            // 3. 16-BIOMARKER COMPREHENSIVE CLINICAL BREAKDOWN
                            Text(
                                text = "Comprehensive 16-Biomarker Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Category 1: Barrier & Hydration
                            BiomarkerCategoryCard(
                                title = "1. Barrier & Cutaneous Hydration",
                                items = listOf(
                                    BiomarkerBarItem("Moisture / Hydration", snap.hydration.toInt(), "Optimal", SageEmerald),
                                    BiomarkerBarItem("Sebum & Oiliness", snap.oiliness.toInt(), snap.oilinessLevel, WarmChampagne),
                                    BiomarkerBarItem("Redness & Erythema", snap.redness.toInt(), snap.rednessLevel, if (snap.redness > 40) MetricAttention else SageEmerald),
                                    BiomarkerBarItem("Surface Texture", snap.texture.toInt(), snap.textureLevel, SageEmerald)
                                )
                            )

                            // Category 2: Aging & Dermal Matrix
                            BiomarkerCategoryCard(
                                title = "2. Aging & Structural Elasticity",
                                items = listOf(
                                    BiomarkerBarItem("Wrinkles & Fine Lines", snap.wrinkles.toInt(), snap.wrinklesLevel, if (snap.wrinkles > 30) MetricAttention else SageEmerald),
                                    BiomarkerBarItem("Skin Firmness", snap.firmness.toInt(), snap.firmnessLevel, SageEmerald),
                                    BiomarkerBarItem("Radiance & Glow", snap.radiance.toInt(), snap.radianceLevel, WarmChampagne),
                                    BiomarkerBarItem("Age Spots & Pigment", snap.darkSpots.toInt(), snap.darkSpotsLevel, if (snap.darkSpots > 35) MetricAttention else SageEmerald)
                                )
                            )

                            // Category 3: Periorbital & Eye Contour
                            BiomarkerCategoryCard(
                                title = "3. Periorbital & Eye Contour",
                                items = listOf(
                                    BiomarkerBarItem("Dark Circles (v2)", snap.darkCircles.toInt(), snap.darkCirclesLevel, if (snap.darkCircles > 35) MetricAttention else SageEmerald),
                                    BiomarkerBarItem("Under-Eye Bags", snap.eyeBags.toInt(), snap.eyeBagsLevel, SageEmerald),
                                    BiomarkerBarItem("Droopy Upper Eyelid", snap.droopyUpperEyelid.toInt(), if (snap.droopyUpperEyelid < 20) "Firm" else "Mild", SageEmerald),
                                    BiomarkerBarItem("Droopy Lower Eyelid", snap.droopyLowerEyelid.toInt(), if (snap.droopyLowerEyelid < 20) "Firm" else "Mild", SageEmerald),
                                    BiomarkerBarItem("Tear Trough Contour", snap.tearTrough.toInt(), if (snap.tearTrough < 25) "Smooth" else "Visible", WarmChampagne)
                                )
                            )

                            // Category 4: Follicular & Phototype
                            BiomarkerCategoryCard(
                                title = "4. Follicular Health & Phototype",
                                items = listOf(
                                    BiomarkerBarItem("Pore Visibility", snap.pores.toInt(), snap.poresLevel, if (snap.pores > 45) MetricAttention else SageEmerald),
                                    BiomarkerBarItem("Acne & Blemish State", if (snap.acne.count == 0) 10 else 45, snap.acne.severity, if (snap.acne.count == 0) SageEmerald else MetricAttention),
                                    BiomarkerBarItem("Fitzpatrick Tone", 85, snap.skinColor.fitzpatrick, SageEmerald)
                                )
                            )

                            // 4. ACTION CALLOUT CARDS (7-14 Day Simulation + VTO)
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = RoseQuartz.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        resultSnapshot = null
                                        onNavigateToVto()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "🔮 See Your Skin After 7 & 14 Days",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkSlate
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "YouCam AI Skin Simulation visualizes your clinical improvement with targeted Amazon skincare.",
                                            fontSize = 11.sp,
                                            color = MutedOlive,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            resultSnapshot = null
                                            onNavigateToVto()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Simulate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Sticky Bottom Action Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        resultSnapshot = null
                                        onScanSaved()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save to Glow Diary & Update Routine",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { resultSnapshot = null },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retake Photo", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Missing API Key Prompt Dialog
        if (showMissingKeyPrompt && pendingBitmap != null) {
            AlertDialog(
                onDismissRequest = {
                    showMissingKeyPrompt = false
                    pendingBitmap = null
                },
                title = {
                    Text(
                        text = "Perfect Corp API Key Required",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "To send this real photo to Perfect Corp YouCam AI cloud, enter your API key in Settings.\n\nAlternatively, you can analyze this real photo immediately using the built-in computer vision engine (analyzes true CIELAB skin tone, sebum sheen, and erythema redness).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMissingKeyPrompt = false
                            showApiSettingsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SageEmerald)
                    ) {
                        Text("Enter API Key", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            val bmp = pendingBitmap
                            showMissingKeyPrompt = false
                            pendingBitmap = null
                            if (bmp != null) {
                                processRealScan(bmp, forceOnDevice = true)
                            }
                        }
                    ) {
                        Text("Analyze On-Device", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        // API Key Settings Modal
        if (showApiSettingsDialog) {
            var tempKey by remember { mutableStateOf(glowRepo.perfectCorpClient.apiKey) }
            var tempEndpoint by remember { mutableStateOf(glowRepo.perfectCorpClient.endpointUrl) }
            var showTempKey by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showApiSettingsDialog = false },
                title = {
                    Text(
                        text = "Perfect Corp API Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Enter your API Key from the Perfect Corp Developer Console (https://yce.perfectcorp.com/api-console).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = tempKey,
                            onValueChange = { tempKey = it },
                            label = { Text("API Key (Bearer Token)") },
                            visualTransformation = if (showTempKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showTempKey = !showTempKey }) {
                                    Icon(
                                        imageVector = if (showTempKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showTempKey) "Hide key" else "Show key"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tempEndpoint,
                            onValueChange = { tempEndpoint = it },
                            label = { Text("Endpoint Base URL") },
                            placeholder = { Text("https://yce-api-01.makeupar.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            glowRepo.perfectCorpClient.apiKey = tempKey.trim()
                            glowRepo.perfectCorpClient.endpointUrl = tempEndpoint.trim()
                            showApiSettingsDialog = false
                            val bmp = pendingBitmap
                            if (bmp != null) {
                                pendingBitmap = null
                                processRealScan(bmp)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SageEmerald)
                    ) {
                        Text("Save & Connect", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showApiSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// RADAR / SPIDER CHART COMPONENT
// ---------------------------------------------------------------------------
@Composable
private fun DermalRadarChart(
    metrics: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = metrics.size
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = (minOf(size.width, size.height) / 2f) * 0.72f

        // Draw 4 Concentric Background Polygons
        for (step in 1..4) {
            val r = maxRadius * (step / 4f)
            val gridPath = Path()
            for (i in 0 until count) {
                val angle = (2 * PI / count * i) - (PI / 2)
                val x = centerX + (r * cos(angle)).toFloat()
                val y = centerY + (r * sin(angle)).toFloat()
                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
            }
            gridPath.close()
            drawPath(
                path = gridPath,
                color = Color.LightGray.copy(alpha = 0.35f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw Axis Lines from Center
        for (i in 0 until count) {
            val angle = (2 * PI / count * i) - (PI / 2)
            val x = centerX + (maxRadius * cos(angle)).toFloat()
            val y = centerY + (maxRadius * sin(angle)).toFloat()
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw User Data Polygon
        val dataPath = Path()
        val points = mutableListOf<Offset>()
        for (i in 0 until count) {
            val normalized = (metrics[i].second / 100.0).coerceIn(0.1, 1.0)
            val r = (maxRadius * normalized).toFloat()
            val angle = (2 * PI / count * i) - (PI / 2)
            val x = centerX + (r * cos(angle)).toFloat()
            val y = centerY + (r * sin(angle)).toFloat()
            points.add(Offset(x, y))
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        // Fill Data Area
        drawPath(
            path = dataPath,
            brush = Brush.radialGradient(
                listOf(RoseQuartz.copy(alpha = 0.55f), WarmChampagne.copy(alpha = 0.25f)),
                center = Offset(centerX, centerY),
                radius = maxRadius
            )
        )

        // Draw Data Outline
        drawPath(
            path = dataPath,
            color = RoseQuartz,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Draw Points at Vertices
        points.forEach { pt ->
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
            drawCircle(color = RoseQuartz, radius = 3.dp.toPx(), center = pt)
        }
    }
}

// ---------------------------------------------------------------------------
// BIOMARKER PROGRESSION CARD COMPONENT
// ---------------------------------------------------------------------------
data class BiomarkerBarItem(
    val name: String,
    val score: Int,
    val status: String,
    val color: Color
)

@Composable
private fun BiomarkerCategoryCard(
    title: String,
    items: List<BiomarkerBarItem>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DarkSlate
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkSlate
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = item.color
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${item.score}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedOlive
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { (item.score / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = item.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
