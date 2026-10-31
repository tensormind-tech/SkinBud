package com.skinbud.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.skinbud.app.reminders.ReminderScheduler
import com.skinbud.app.ui.components.CartBottomSheet
import com.skinbud.app.ui.components.NavigationTab
import com.skinbud.app.ui.components.SkinBudBottomBar
import com.skinbud.app.ui.components.SkinBudTopBar
import com.skinbud.app.ui.screens.HomeScreen
import com.skinbud.app.ui.screens.OnboardingScreen
import com.skinbud.app.ui.screens.RoutineScreen
import com.skinbud.app.ui.screens.ScanScreen
import com.skinbud.app.ui.screens.ShelfScreen
import com.skinbud.app.ui.screens.VtoStudioScreen
import com.skinbud.app.ui.theme.DarkSlate
import com.skinbud.app.ui.theme.RoseQuartz
import com.skinbud.app.ui.theme.SageEmerald
import com.skinbud.app.ui.theme.SkinBudTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestAppPermissions()
        ReminderScheduler.scheduleAllReminders(this)

        val initialTab = when (intent?.data?.host) {
            "scan" -> NavigationTab.SCAN
            "tryon" -> NavigationTab.TRY_ON
            "routine" -> NavigationTab.ROUTINE
            "shelf" -> NavigationTab.SHELF
            else -> NavigationTab.HOME
        }

        val app = application as SkinBudApp

        setContent {
            SkinBudTheme {
                val prefs = remember { getSharedPreferences("skinbud_prefs", Context.MODE_PRIVATE) }
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean("has_completed_onboarding", false))
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        glowRepo = app.glowRepository,
                        onComplete = {
                            prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                            showOnboarding = false
                        }
                    )
                } else {
                    MainAppScaffold(
                        app = app,
                        initialTab = initialTab,
                        onReplayOnboarding = { showOnboarding = true }
                    )
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}

@Composable
fun MainAppScaffold(
    app: SkinBudApp,
    initialTab: NavigationTab = NavigationTab.HOME,
    onReplayOnboarding: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val cartItems by app.cartRepository.items.collectAsState()
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        topBar = {
            if (currentTab != NavigationTab.SCAN) {
                SkinBudTopBar(
                    title = "SkinBud",
                    subtitle = when (currentTab) {
                        NavigationTab.HOME -> "Today's Glow Protocol"
                        NavigationTab.TRY_ON -> "AI Simulation & Complexion VTO"
                        NavigationTab.ROUTINE -> "Daily Routine Steps"
                        NavigationTab.SHELF -> "Curated Amazon Boutique"
                        else -> null
                    },
                    isApiConnected = app.glowRepository.perfectCorpClient.isConfigured,
                    cartItemCount = cartCount,
                    onCartClick = { showCartSheet = true },
                    onSettingsClick = { showSettingsDialog = true }
                )
            }
        },
        bottomBar = {
            SkinBudBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> HomeScreen(
                    glowRepo = app.glowRepository,
                    routineRepo = app.routineRepository,
                    onNavigateToScan = { currentTab = NavigationTab.SCAN },
                    onNavigateToRoutine = { currentTab = NavigationTab.ROUTINE },
                    onNavigateToVto = { currentTab = NavigationTab.TRY_ON }
                )
                NavigationTab.SCAN -> ScanScreen(
                    glowRepo = app.glowRepository,
                    onScanSaved = { currentTab = NavigationTab.HOME },
                    onNavigateToVto = { currentTab = NavigationTab.TRY_ON }
                )
                NavigationTab.TRY_ON -> VtoStudioScreen(
                    glowRepo = app.glowRepository,
                    productRepo = app.productRepository,
                    cartRepo = app.cartRepository,
                    onOpenCart = { showCartSheet = true }
                )
                NavigationTab.ROUTINE -> RoutineScreen(
                    routineRepo = app.routineRepository
                )
                NavigationTab.SHELF -> ShelfScreen(
                    productRepo = app.productRepository,
                    glowRepo = app.glowRepository
                )
            }

            if (showCartSheet) {
                CartBottomSheet(
                    cartRepo = app.cartRepository,
                    onDismiss = { showCartSheet = false }
                )
            }

            if (showSettingsDialog) {
                AppSettingsDialog(
                    app = app,
                    onDismiss = { showSettingsDialog = false },
                    onReplayOnboarding = {
                        showSettingsDialog = false
                        onReplayOnboarding()
                    }
                )
            }
        }
    }
}

@Composable
fun AppSettingsDialog(
    app: SkinBudApp,
    onDismiss: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    var perfectCorpKey by remember { mutableStateOf(app.glowRepository.perfectCorpClient.apiKey) }
    var perfectCorpEndpoint by remember { mutableStateOf(app.glowRepository.perfectCorpClient.endpointUrl) }
    var groqKey by remember { mutableStateOf(app.productRepository.groqClient.apiKey) }
    var groqModel by remember { mutableStateOf(app.productRepository.groqClient.selectedModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SkinBud AI Engine Settings",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SageEmerald.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Perfect Corp YouCam S2S v2.0 AI Skin Analysis (16 actions), Fitzpatrick Skin Type, AI Simulation & Makeup VTO.",
                        fontSize = 11.sp,
                        color = DarkSlate,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 15.sp
                    )
                }

                OutlinedTextField(
                    value = perfectCorpEndpoint,
                    onValueChange = { perfectCorpEndpoint = it },
                    label = { Text("Perfect Corp Endpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = perfectCorpKey,
                    onValueChange = { perfectCorpKey = it },
                    label = { Text("Perfect Corp API Key") },
                    placeholder = { Text("Optional - fallback to on-device AI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = groqKey,
                    onValueChange = { groqKey = it },
                    label = { Text("Groq AI Key (Amazon Recommendation)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = groqModel,
                    onValueChange = { groqModel = it },
                    label = { Text("Groq Model (e.g. openai/gpt-oss-120b)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onReplayOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Replay Facial Quiz & Onboarding", color = RoseQuartz)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    app.glowRepository.perfectCorpClient.apiKey = perfectCorpKey.trim()
                    app.glowRepository.perfectCorpClient.endpointUrl = perfectCorpEndpoint.trim()
                    app.productRepository.groqClient.apiKey = groqKey.trim()
                    app.productRepository.groqClient.selectedModel = groqModel.trim()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SageEmerald)
            ) {
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = DarkSlate)
            ) {
                Text("Cancel")
            }
        }
    )
}
