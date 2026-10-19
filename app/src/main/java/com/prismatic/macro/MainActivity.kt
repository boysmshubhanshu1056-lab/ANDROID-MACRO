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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp // <-- CHANGED IMPORT
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
        
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        setContent {
            var hasPermission by remember { mutableStateOf(checkAccessibilityPermission()) }
            val lifecycleOwner = LocalLifecycleOwner.current

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
                modifier = Modifier.weight(1.5f).fillMaxHeight().border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("SELECT KEYS", color = NeonGreen.copy(alpha = 0.7f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KeyButton("1")
                    KeyButton("2")
                    KeyButton("3")
                    KeyButton("4")
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KeyButton("Z")
                    KeyButton("X")
                    KeyButton("C")
                    KeyButton("V")
                    KeyButton("F")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT: Mouse & Toggles
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mouse Panel
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("MOUSE MACRO", color = NeonGreen.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // The Icon
                    Icon(
                        imageVector = Icons.Filled.TouchApp, // <-- CHANGED ICON
                        contentDescription = "Touch",
                        tint = NeonGreen,
                        modifier = Modifier.size(50.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Big LMB and RMB buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        KeyButtonWide("LMB")
                        KeyButtonWide("RMB")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle
                Column(
                    modifier = Modifier.fillMaxWidth().border(2.dp, NeonGreen, RoundedCornerShape(8.dp)).background(PanelBg).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ENABLE ALL-KEY MACRO", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
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
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showCpsScreen = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanelBg),
                    border = BorderStroke(2.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("TEST MACRO (CPS CHECKER)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun KeyButton(label: String) {
    var isSelected by remember { mutableStateOf(false) }
    val borderColor = if (isSelected) NeonRed else NeonGreen.copy(alpha = 0.3f)
    val textColor = if (isSelected) NeonRed else Color.Gray

    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 60.dp)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonRed.copy(alpha = 0.1f) else PanelBg)
            .clickable { isSelected = !isSelected },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
fun KeyButtonWide(label: String) {
    var isSelected by remember { mutableStateOf(false) }
    val borderColor = if (isSelected) NeonRed else NeonGreen.copy(alpha = 0.3f)
    val textColor = if (isSelected) NeonRed else Color.Gray

    Box(
        modifier = Modifier
            .size(width = 70.dp, height = 45.dp)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonRed.copy(alpha = 0.1f) else PanelBg)
            .clickable { isSelected = !isSelected },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
