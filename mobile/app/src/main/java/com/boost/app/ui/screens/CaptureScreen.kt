package com.boost.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.boost.app.ui.theme.BoostColors
import com.boost.app.ui.components.*
import com.google.accompanist.permissions.*

// ─── CAPTURE ──────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onCapture: (Uri, Boolean) -> Unit,  // (uri, isVideo)
    onBack: () -> Unit,
) {
    var isVideoMode by remember { mutableStateOf(false) }

    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val galleryLauncher  = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onCapture(it, isVideoMode) } }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BoostColors.Bg),
    ) {
        // Header
        ScreenHeader(title = "Capture Product", subtitle = "Step 1 of 3", onBack = onBack)

        if (!cameraPermission.status.isGranted) {
            // Permission prompt
            Column(
                modifier = Modifier.weight(1f).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("Camera access needed", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = BoostColors.Text)
                Spacer(Modifier.height(8.dp))
                Text("Required to capture product photos and videos",
                    fontSize = 13.sp, color = BoostColors.TextMid)
                Spacer(Modifier.height(20.dp))
                GradientButton("Allow Camera") {
                    cameraPermission.launchPermissionRequest()
                }
            }
        } else {
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .aspectRatio(9f / 16f)
                    .background(BoostColors.Card),
            ) {
                // CameraX preview
                val previewView = remember { PreviewView(context) }
                AndroidView(factory = { previewView },
                    modifier = Modifier.fillMaxSize())

                LaunchedEffect(Unit) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                        )
                    }, ContextCompat.getMainExecutor(context))
                }

                // Corner guides
                CornerGuides(color = BoostColors.Orange)
            }

            // Mode switcher + buttons
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Mode tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BoostColors.Card)
                        .padding(4.dp),
                ) {
                    listOf(false to "📸 Photo", true to "🎥 Video").forEach { (isVid, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (isVideoMode == isVid)
                                        BoostColors.Orange.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isVideoMode == isVid) BoostColors.Orange.copy(0.5f)
                                    else Color.Transparent,
                                    RoundedCornerShape(9.dp),
                                )
                                .clickable { isVideoMode = isVid }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVideoMode == isVid) BoostColors.Text
                                        else BoostColors.TextDim)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Capture button
                GradientButton(
                    text = if (isVideoMode) "🎥 Start Recording" else "📸 Take Photo",
                    onClick = {
                        // In real app: trigger CameraX capture
                        // For demo: open gallery picker
                        val mimeType = if (isVideoMode) "video/*" else "image/*"
                        galleryLauncher.launch(mimeType)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                )

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val mimeType = if (isVideoMode) "video/*" else "image/*"
                        galleryLauncher.launch(mimeType)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BoostColors.TextMid),
                    border = BorderStroke(1.dp, BoostColors.Border),
                ) {
                    Text("🖼 Upload from Gallery", fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─── CONTEXT PICKER ───────────────────────────────────────────────

private val NICHES    = listOf("Beauty","Fashion","Tech","Food","Home","Fitness","Pets","Finance","Other")
private val VIBES     = listOf("Aesthetic","Funny","Genuine","Aggressive CTA")
private val PLATFORMS = listOf("TikTok","Reels","Shorts")

@Composable
fun ContextScreen(
    captureUri: Uri,
    isVideo: Boolean,
    onAnalyze: (niche: String, vibe: String, platform: String, name: String?) -> Unit,
    onBack: () -> Unit,
) {
    var productName by remember { mutableStateOf("") }
    var selectedNiche    by remember { mutableStateOf("") }
    var selectedVibe     by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("TikTok") }

    val canProceed = selectedNiche.isNotEmpty() && selectedVibe.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxSize().background(BoostColors.Bg),
    ) {
        ScreenHeader(title = "Add Context", subtitle = "Step 2 of 3", onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // Capture preview row
            BoostCard {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                            .background(BoostColors.Orange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (isVideo) "🎥" else "📸", fontSize = 22.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (isVideo) "Product video captured"
                             else "Product photo captured",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = BoostColors.Text)
                        Text("Ready to analyze",
                            fontSize = 11.sp, color = BoostColors.TextDim)
                    }
                    ChipLabel("✓ Ready", color = BoostColors.Green)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Product name
            SectionLabel("Product Name (optional)")
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                placeholder = { Text("e.g. Black Insulated Tumbler",
                    color = BoostColors.TextDim) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = BoostColors.Orange,
                    unfocusedBorderColor = BoostColors.Border2,
                    focusedTextColor     = BoostColors.Text,
                    unfocusedTextColor   = BoostColors.Text,
                    cursorColor          = BoostColors.Orange,
                ),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(16.dp))

            // Niche
            SectionLabel("Product Niche *")
            TagGrid(
                items = NICHES,
                selected = selectedNiche,
                onSelect = { selectedNiche = it },
                activeColor = BoostColors.Orange,
            )
            Spacer(Modifier.height(16.dp))

            // Vibe
            SectionLabel("Content Vibe *")
            TagGrid(
                items = VIBES,
                selected = selectedVibe,
                onSelect = { selectedVibe = it },
                activeColor = BoostColors.Violet,
            )
            Spacer(Modifier.height(16.dp))

            // Platform
            SectionLabel("Target Platform")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                PLATFORMS.forEach { platform ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedPlatform == platform)
                                    BoostColors.Orange.copy(alpha = 0.13f)
                                else BoostColors.Card
                            )
                            .border(
                                1.dp,
                                if (selectedPlatform == platform) BoostColors.Orange
                                else BoostColors.Border,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedPlatform = platform }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (selectedPlatform == platform) BoostColors.Orange
                                    else BoostColors.TextDim)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // AI pipeline preview
            BoostCard {
                Text("AI PIPELINE", fontSize = 10.sp, color = BoostColors.TextDim,
                    letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 10.dp))
                listOf(
                    Triple("👁", "Vision LLM — GLM-4V sees your product",   BoostColors.Orange),
                    Triple("🧠", "Reasoning — DeepSeek writes your content", BoostColors.Violet),
                    Triple("🖼", "Image Gen — 3 polished image variants",    BoostColors.Green),
                    Triple("🎬", "Video Gen — 2 scroll-stopping edits",      BoostColors.Gold),
                ).forEachIndexed { i, (icon, label, color) ->
                    if (i > 0) Divider(color = BoostColors.Border, thickness = 0.5.dp)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 14.sp)
                        }
                        Text(label, fontSize = 11.sp, color = BoostColors.TextMid,
                            modifier = Modifier.weight(1f))
                        Text("READY", fontSize = 10.sp, color = color,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Generate button
            GradientButton(
                text = if (canProceed) "⚡ Analyze & Generate →"
                       else "Select niche + vibe to continue",
                onClick = {
                    if (canProceed) onAnalyze(
                        selectedNiche, selectedVibe, selectedPlatform,
                        productName.takeIf { it.isNotBlank() },
                    )
                },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )

            Text(
                "~20-30 seconds · 4 AI models working in parallel",
                fontSize = 11.sp, color = BoostColors.TextDim,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
