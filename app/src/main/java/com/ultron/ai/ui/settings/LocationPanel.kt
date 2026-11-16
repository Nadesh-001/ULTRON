package com.ultron.ai.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.ArcCyan
import com.ultron.ai.ui.theme.HudButton
import com.ultron.ai.ui.theme.HudNote
import com.ultron.ai.ui.theme.HudOutlinedButton
import com.ultron.ai.ui.theme.HudPanel
import com.ultron.ai.ui.theme.HudReadout
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.hudFieldColors
import org.koin.androidx.compose.koinViewModel

private fun locationGranted(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

@Composable
fun LocationPanel(viewModel: LocationViewModel = koinViewModel()) {
    val context = LocalContext.current
    val saved by viewModel.city.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    var typed by remember { mutableStateOf("") }
    LaunchedEffect(saved) { typed = saved }
    var granted by remember { mutableStateOf(locationGranted(context)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    HudPanel("Location", index = 12) {
        HudNote(
            "For weather queries without a specified place. The city set here (or by saying \"my city is Boston\") " +
                "takes priority; otherwise device location if granted; fallback to timezone city."
        )
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text("Default City") },
            placeholder = { Text("Empty — use device location") },
            singleLine = true,
            colors = hudFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                HudButton(onClick = { viewModel.save(typed) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
            }
            Box(Modifier.weight(1f)) {
                HudOutlinedButton(onClick = { viewModel.save("") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear")
                }
            }
        }
        result?.let { HudNote(it) }
        HudReadout(
            "Location Access",
            if (granted) "GRANTED" else "NOT GRANTED",
            valueColor = if (granted) ArcCyan else OnBackground
        )
        if (!granted) {
            HudOutlinedButton(
                onClick = { ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Allow Approximate Location") }
        }
    }
}
