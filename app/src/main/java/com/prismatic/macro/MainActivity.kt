package com.prismatic.macro

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

// --- THEME COLORS ---
val DarkBg = Color(0xFF0A0F0D)
val PanelBg = Color(0xFF111A16)
val NeonGreen = Color(0xFF39FF14)
val NeonRed = Color(0xFFFF0000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Landscape Mode strictly
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        setContent {
            var hasPermission by remember { mutableStateOf(checkAccessibilityPermission()) }
            val lifecycleOwner = LocalLifecycleOwner.current

            // Safely check for permission when returning from Settings
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermission = checkAccessibilityPermission()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MaterialTheme {
                Surface(color = DarkBg, modifier = Modifier.fillMaxSize()) {
                    if (!hasPermission) {
                        PermissionScreen { 
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) 
                        }
                    } else {
                        MainAppScreen()
                    }
                }
            }
        }
    }

    private fun checkAccessibilityPermission(): Boolean {
        val service = "$packageName/${MacroAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains(service) == true
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PRISMATIC", color = NeonGreen, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Enable Accessibility to use the macro.", color = Color.White, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
            border = BorderStroke(2.dp, NeonGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("GRANT PERMISSION", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun MainAppScreen() {
    var isMacroEnabled by remember { mutableStateOf(MacroState.isMacroEnabled) }
    var showCpsScreen by remember { mutableStateOf(false) }

    if (showCpsScreen) {
        CpsScreen { showCpsScreen = false }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "PRISMATIC", 
            color = NeonGreen, 
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT: Keyboard
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight().border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SELECT KEYS", color = NeonGreen.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                KeyRow(listOf("Q", "W", "E", "R", "T"))
                KeyRow(listOf("A", "S", "D", "F", "G"))
                KeyRow(listOf("Z", "X", "C", "V", "B"))
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT: Mouse, Toggle, CPS
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("MOUSE", color = NeonGreen.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        KeyButton("LMB")
                        KeyButton("RMB")
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).background(PanelBg).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ENABLE ALL-KEY MACRO", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Switch(
                        checked = isMacroEnabled,
                        onCheckedChange = { 
                            isMacroEnabled = it
                            MacroState.isMacroEnabled = it 
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedBorderColor = NeonGreen
                        )
                    )
                }

                Button(
                    onClick = { showCpsScreen = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
                    border = BorderStroke(2.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("TEST MACRO (CPS CHECKER)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun KeyRow(keys: List<String>) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        keys.forEach { KeyButton(it) }
    }
}

@Composable
fun KeyButton(label: String) {
    var isSelected by remember { mutableStateOf(false) }
    
    // Turns RED when selected
    val borderColor = if (isSelected) NeonRed else NeonGreen.copy(alpha = 0.3f)
    val textColor = if (isSelected) NeonRed else Color.Gray

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(width = if (label.length > 1) 80.dp else 50.dp, height = 50.dp)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonRed.copy(alpha = 0.1f) else PanelBg)
            .clickable { isSelected = !isSelected },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun CpsScreen(onBack: () -> Unit) {
    var taps by remember { mutableStateOf(0) }
    var cps by remember { mutableStateOf(0f) }
    var startTime by remember { mutableStateOf(0L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .clickable {
                if (startTime == 0L) startTime = System.currentTimeMillis()
                taps++
                val timeElapsed = (System.currentTimeMillis() - startTime) / 1000f
                if (timeElapsed > 0) cps = taps / timeElapsed
            }
    ) {
        Text(
            text = "CPS - ${String.format("%.1f", cps)}",
            color = NeonGreen,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(32.dp)
        )
        
        Text(
            text = "TAPS: $taps",
            color = Color.White,
            fontSize = 32.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
            border = BorderStroke(2.dp, NeonGreen)
        ) {
            Text("BACK", color = NeonGreen, fontWeight = FontWeight.Bold)
        }
    }
}
