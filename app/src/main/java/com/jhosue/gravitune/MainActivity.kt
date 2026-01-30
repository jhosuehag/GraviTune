package com.jhosue.gravitune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.jhosue.gravitune.ui.theme.GraviTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GraviTuneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onPermissionCheck = { checkOverlayPermission() },
                        context = this
                    )
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 100)
        } else {
            startVolumeService()
        }
    }

    private fun startVolumeService() {
        val intent = Intent(this, VolumeService::class.java)
        startForegroundService(intent)
    }

    @Deprecated("Deprecated in Java but needed for result callback in this simple example")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            if (Settings.canDrawOverlays(this)) {
                startVolumeService()
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onPermissionCheck: () -> Unit,
    context: android.content.Context
) {
    val prefs = context.getSharedPreferences("gravitune_prefs", android.content.Context.MODE_PRIVATE)
    // "line" or "circle"
    val (selectedStyle, onStyleSelected) = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(prefs.getString("handle_style", "line") ?: "line")
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = "GraviTune",
            style = androidx.compose.material3.MaterialTheme.typography.headlineLarge
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
        
        Text(text = "Choose Handle Style:")
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        // Option 1: Line
        androidx.compose.material3.Button(
            onClick = {
                onStyleSelected("line")
                prefs.edit().putString("handle_style", "line").apply()
                restartService(context)
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (selectedStyle == "line") androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Line (Default)")
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))

        // Option 2: Circle
        androidx.compose.material3.Button(
            onClick = {
                onStyleSelected("circle")
                prefs.edit().putString("handle_style", "circle").apply()
                restartService(context)
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (selectedStyle == "circle") androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Circle (Top-Left)")
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(48.dp))

        androidx.compose.material3.Button(onClick = onPermissionCheck) {
            Text("Start / Refresh Service")
        }
    }
}

fun restartService(context: android.content.Context) {
    val intent = Intent(context, VolumeService::class.java)
    intent.action = "STOP_SERVICE"
    context.startService(intent) // First stop it (if we added stop logic handling in onStartCommand)
    // A slight delay or just sending a "UPDATE_STYLE" intent would be better, but restarting is reliable simple approach.
    // Actually our current STOP_SERVICE kills the process or stops self.
    // Let's just start it again to trigger onStartCommand updates? No, onCreate sets up overlay.
    // We should probably just tell user to "Restart Service".
    // Or valid restart:
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        val startIntent = Intent(context, VolumeService::class.java)
        androidx.core.content.ContextCompat.startForegroundService(context, startIntent)
    }, 500)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GraviTuneTheme {
        Greeting("Android")
    }
}