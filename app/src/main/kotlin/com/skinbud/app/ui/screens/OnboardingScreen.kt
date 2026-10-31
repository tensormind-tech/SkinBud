package com.skinbud.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skinbud.app.R
import com.skinbud.app.data.GlowRepository
import com.skinbud.app.ui.theme.CreamBackground
import com.skinbud.app.ui.theme.DarkSlate
import com.skinbud.app.ui.theme.DeepRose
import com.skinbud.app.ui.theme.MutedOlive
import com.skinbud.app.ui.theme.RoseQuartz
import com.skinbud.app.ui.theme.SageEmerald
import com.skinbud.app.ui.theme.WarmIvory
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    glowRepo: GlowRepository,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("skinbud_user_profile", Context.MODE_PRIVATE) }

    var currentStep by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    // User Questionnaire Answers
    var selectedSkinType by remember { mutableStateOf(prefs.getString("skin_type", "Combination") ?: "Combination") }
    var selectedTargetConcern by remember { mutableStateOf(prefs.getString("target_concern", "Hydration & Barrier") ?: "Hydration & Barrier") }
    var selectedSunExposure by remember { mutableStateOf(prefs.getString("sun_exposure", "Moderate") ?: "Moderate") }
    var apiKeyInput by remember { mutableStateOf(glowRepo.perfectCorpClient.apiKey) }

    fun finishScanAndComplete(bitmap: Bitmap) {
        isScanning = true
        scanErrorMessage = null
        scope.launch {
            try {
                if (apiKeyInput.isNotBlank()) {
                    glowRepo.perfectCorpClient.apiKey = apiKeyInput.trim()
                }
                val result = glowRepo.analyzeAndSave(bitmap)
                isScanning = false
                if (result.isSuccess) {
                    onComplete()
                } else {
                    scanErrorMessage = result.exceptionOrNull()?.message ?: "Scan failed"
                }
            } catch (e: Exception) {
                isScanning = false
                scanErrorMessage = e.message
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { finishScanAndComplete(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    finishScanAndComplete(bitmap)
                }
            } catch (e: Exception) {
                scanErrorMessage = "Unable to load image: ${e.message}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "SkinBud",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SkinBud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                if (currentStep < 2) {
                    TextButton(onClick = { currentStep = 2 }) {
                        Text(
                            text = "Skip to Scan",
                            color = MutedOlive,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip Scan",
                            color = MutedOlive,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()) with
                                (slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()) with
                                (slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f),
                label = "onboarding_steps"
            ) { step ->
                when (step) {
                    0 -> {
                        // STEP 0: Welcome & Skin Philosophy
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.onboarding_scan),
                                    contentDescription = "Facial Diagnostic",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Surface(
                                color = RoseQuartz.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "CLINICAL BEAUTY AI",
                                    color = RoseQuartz,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Your Skin. Decoded by Science.",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "High-precision non-invasive facial biomarker analysis powered by Perfect Corp & on-device computer vision. Zero ads, zero paywalls.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedOlive,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Button(
                                onClick = { currentStep = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Start Personalized Consultation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    1 -> {
                        // STEP 1: Interactive Skincare Consultation Questions
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Personalized Skin Profile",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSlate
                                )
                                Text(
                                    text = "Answer 3 quick clinical questions to tailor your protocol",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedOlive
                                )
                            }

                            // Question 1: Skin Type
                            Text(
                                text = "1. How does your skin feel midday?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkSlate
                            )
                            val skinTypes = listOf(
                                "Dry & Tight" to "Dry",
                                "Comfortable & Balanced" to "Balanced",
                                "Oily T-Zone / Normal Cheeks" to "Combination",
                                "All-Over Oily Shine" to "Oily",
                                "Easily Flushed & Reactive" to "Sensitive"
                            )
                            skinTypes.forEach { (label, value) ->
                                val isSelected = selectedSkinType == value
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) RoseQuartz.copy(alpha = 0.12f) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) RoseQuartz else Color.LightGray.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSkinType = value
                                            prefs.edit().putString("skin_type", value).apply()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = DarkSlate, fontSize = 13.sp)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = RoseQuartz, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            // Question 2: Primary Goal
                            Text(
                                text = "2. What is your #1 skincare goal?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DarkSlate
                            )
                            val goals = listOf(
                                "Clear Acne & Clogged Pores",
                                "Deep Barrier Hydration",
                                "Reduce Redness & Rosacea",
                                "Smooth Texture & Fine Lines",
                                "Fade Dark Spots & Hyperpigmentation"
                            )
                            goals.forEach { goal ->
                                val isSelected = selectedTargetConcern == goal
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) SageEmerald.copy(alpha = 0.12f) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) SageEmerald else Color.LightGray.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTargetConcern = goal
                                            prefs.edit().putString("target_concern", goal).apply()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = goal, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = DarkSlate, fontSize = 13.sp)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = SageEmerald, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { currentStep = 2 },
                                colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Continue to Baseline Face Scan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    2 -> {
                        // STEP 2: Baseline Face Scan & Perfect Corp API Setup
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Step 3: Baseline Face Scan",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSlate,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Position your face in front of the camera for clinical biomarker diagnostics.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedOlive,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Perfect Corp API Key Input Card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 1.dp
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = SageEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Perfect Corp API Key (Optional)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = DarkSlate
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add your key from https://yce.perfectcorp.com/ to enable live cloud diagnostics. If left blank, real on-device computer vision will be used.",
                                        fontSize = 11.sp,
                                        color = MutedOlive,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        label = { Text("Paste Bearer Token") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }

                            if (scanErrorMessage != null) {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = scanErrorMessage ?: "",
                                        color = Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            if (isScanning) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(color = RoseQuartz)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Executing clinical skin diagnosis...",
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSlate
                                    )
                                }
                            } else {
                                // Camera Scan Button
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoseQuartz),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📸 Capture Face Scan with Camera", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }

                                // Gallery Pick Button
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.Collections, contentDescription = null, tint = DarkSlate)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick Photo from Gallery", fontWeight = FontWeight.SemiBold, color = DarkSlate)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..2).forEach { i ->
                    val isSelected = i == currentStep
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) RoseQuartz else Color.LightGray.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
