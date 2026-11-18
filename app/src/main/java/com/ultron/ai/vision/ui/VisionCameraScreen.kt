package com.ultron.ai.vision.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ultron.ai.ui.theme.BackgroundDark
import com.ultron.ai.ui.theme.CrimsonRed
import com.ultron.ai.ui.theme.OnBackground
import com.ultron.ai.ui.theme.OnPrimary
import com.ultron.ai.ui.theme.OnSurfaceMuted
import com.ultron.ai.ui.theme.OutlineDim
import com.ultron.ai.ui.theme.OutlineVariant
import com.ultron.ai.ui.theme.SurfaceContainerLow
import com.ultron.ai.ui.theme.SurfaceDark
import com.ultron.ai.ui.theme.SurfaceVariant
import com.ultron.ai.ui.theme.UltronRed
import com.ultron.ai.vision.ai.VisionMode
import org.koin.androidx.compose.koinViewModel

/**
 * CameraX live preview and visual intelligence screen.
 * Implements Google Lens-inspired interface consistent with ULTRON dark metallic HUD design.
 */
@Composable
fun VisionCameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    viewModel: VisionViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFlashOn by viewModel.cameraController.isFlashOn.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onGalleryImageSelected(uri) {
                onNavigateToChat()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            viewModel.cameraController.startCamera(lifecycleOwner, previewView)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cameraController.stopCamera()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 1. Top CameraX Control Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "ULTRON // VISION",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.cameraController.toggleFlash() },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, if (isFlashOn) UltronRed else OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isFlashOn) UltronRed else OnSurfaceMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.cameraController.switchCamera(lifecycleOwner, previewView) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- 2. Live CameraX Viewfinder Section ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, OutlineDim, RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Laser Reticle Corner Brackets
                ReticleCorners(modifier = Modifier.fillMaxSize().padding(24.dp))

                // Animated Scanning Laser Line
                ScanLineSweep(modifier = Modifier.fillMaxSize())

                // Top-Left Live ML Kit Status Chip
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceDark)
                        .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(UltronRed)
                        )
                        Text(
                            text = "MODE: ${state.mode.tag} // CAMERAX",
                            color = OnBackground,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                // Bottom-Center Live Detection Banner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark.copy(alpha = 0.95f))
                        .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ML KIT STATUS",
                                color = UltronRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                            Text(
                                text = state.detectedSummary,
                                color = OnBackground,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(UltronRed)
                                .clickable {
                                    viewModel.captureAndAnalyze {
                                        onNavigateToChat()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ANALYZE",
                                color = OnPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = UltronRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "CAMERA PERMISSION REQUIRED",
                        color = OnBackground,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ULTRON needs camera access to perform live visual intelligence, OCR, and object identification.",
                        color = OnSurfaceMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(UltronRed)
                            .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "GRANT PERMISSION",
                            color = OnPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 3. Vision Mode Selector Strip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisionMode.entries.forEach { mode ->
                val isSelected = state.mode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) UltronRed else SurfaceDark)
                        .border(1.dp, if (isSelected) UltronRed else OutlineDim, RoundedCornerShape(4.dp))
                        .clickable { viewModel.selectMode(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) OnPrimary else OnSurfaceMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.6.sp
                    )
                }
            }
        }

        // --- 4. Camera Shutter & Action Dock ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLow)
                .border(width = 1.dp, color = OutlineVariant)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = UltronRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "GALLERY",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }

            // Central Shutter Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant)
                    .border(3.dp, UltronRed, CircleShape)
                    .clickable {
                        viewModel.captureAndAnalyze {
                            onNavigateToChat()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(UltronRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Capture Image",
                        tint = OnPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Ask AI Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onNavigateToChat() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = "Ask AI",
                    tint = UltronRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "ASK AI",
                    color = OnBackground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

@Composable
private fun ReticleCorners(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val cornerLen = 24.dp.toPx()
        val color = UltronRed

        // Top-Left
        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth)

        // Top-Right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLen, 0f), strokeWidth)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLen), strokeWidth)

        // Bottom-Left
        drawLine(color, Offset(0f, size.height), Offset(cornerLen, size.height), strokeWidth)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLen), strokeWidth)

        // Bottom-Right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLen, size.height), strokeWidth)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLen), strokeWidth)
    }
}

@Composable
private fun ScanLineSweep(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanSweep")
    val progress by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    Canvas(modifier = modifier) {
        val y = size.height * progress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    UltronRed.copy(alpha = 0.9f),
                    CrimsonRed.copy(alpha = 0.9f),
                    Color.Transparent
                )
            ),
            start = Offset(24.dp.toPx(), y),
            end = Offset(size.width - 24.dp.toPx(), y),
            strokeWidth = 3.dp.toPx()
        )
    }
}
