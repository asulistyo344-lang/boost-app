package com.boost.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.app.data.api.*
import com.boost.app.ui.components.*
import com.boost.app.ui.theme.BoostColors
import kotlinx.coroutines.delay

// ─── GENERATING ───────────────────────────────────────────────────

@Composable
fun GeneratingScreen() {
    var stepIndex by remember { mutableStateOf(0) }

    val steps = listOf(
        Triple("👁",  "Vision LLM analyzing your product…",     BoostColors.Orange),
        Triple("🧠", "Reasoning LLM writing your content…",     BoostColors.Violet),
        Triple("🖼", "Image Gen creating 3 variants…",          BoostColors.Green),
        Triple("🎬", "Video Gen editing 2 clips…",              BoostColors.Gold),
        Triple("✅", "Content ready!",                          BoostColors.Green),
    )
    val delays = listOf(4000L, 5000L, 8000L, 7000L)

    LaunchedEffect(stepIndex) {
        if (stepIndex < 4) {
            delay(delays[stepIndex])
            stepIndex++
        }
    }

    val current = steps[minOf(stepIndex, 4)]
    val (icon, label, color) = current

    Column(
        modifier = Modifier.fillMaxSize().background(BoostColors.Bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated orb
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(2.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 48.sp)
        }

        Spacer(Modifier.height(28.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = BoostColors.Text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(current.second.substringAfter("—").ifBlank { "Working…" },
            fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp)

        Spacer(Modifier.height(32.dp))

        // Step list
        BoostCard(modifier = Modifier.fillMaxWidth()) {
            steps.dropLast(1).forEachIndexed { i, (sIcon, sLabel, sColor) ->
                if (i > 0) Divider(color = BoostColors.Border, thickness = 0.5.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                        .alpha(if (i <= stepIndex) 1f else 0.3f),
                ) {
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                            .background(
                                if (i <= stepIndex) sColor.copy(alpha = 0.2f)
                                else BoostColors.Border
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (i < stepIndex) "✓" else sIcon, fontSize = 13.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sLabel.substringBefore("…"),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (i <= stepIndex) BoostColors.Text else BoostColors.TextDim)
                        Text(
                            when {
                                i < stepIndex  -> "Complete"
                                i == stepIndex -> "Running…"
                                else           -> "Waiting"
                            },
                            fontSize = 10.sp,
                            color = if (i < stepIndex) sColor else BoostColors.TextDim,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("~20-30 seconds total · Please wait",
            fontSize = 11.sp, color = BoostColors.TextDim)
    }
}

fun Modifier.alpha(alpha: Float) = this.then(
    Modifier.graphicsLayer(alpha = alpha)
)

// ─── RESULTS ──────────────────────────────────────────────────────

@Composable
fun ResultsScreen(
    analysis: AnalyzeResponse,
    images: List<ImageResult>,
    videos: List<VideoResult>,
    onBack: () -> Unit,
    onSaveAll: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }
    val clipboard   = LocalClipboardManager.current
    var copiedIdx   by remember { mutableStateOf<Int?>(null) }
    var hashTab     by remember { mutableStateOf("viral") }

    Column(
        modifier = Modifier.fillMaxSize().background(BoostColors.Bg),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text("‹", fontSize = 22.sp, color = BoostColors.TextDim)
            }
            Column(Modifier.weight(1f)) {
                Text("Step 3 of 3", fontSize = 9.sp, color = BoostColors.TextDim,
                    letterSpacing = 2.sp)
                Text("Your Content", fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold, color = BoostColors.Text)
            }
            GradientButton("💾 Save All", onClick = onSaveAll,
                modifier = Modifier.height(36.dp))
        }

        // Strategy card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(BoostColors.Orange.copy(0.1f), BoostColors.Violet.copy(0.1f))
                    )
                )
                .border(1.dp, BoostColors.Orange.copy(0.3f), RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            Column {
                Text("⚡ AI STRATEGY", fontSize = 10.sp, color = BoostColors.Orange,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(analysis.strategy.angle, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = BoostColors.Text)
                Spacer(Modifier.height(4.dp))
                Text(analysis.strategy.audience, fontSize = 12.sp,
                    color = BoostColors.TextMid, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    analysis.strategy.tags.take(3).forEach { tag ->
                        ChipLabel(tag, color = BoostColors.Orange)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BoostColors.Card)
                .padding(4.dp),
        ) {
            listOf(
                "🖼 Images (${images.size})",
                "🎬 Videos (${videos.size})",
                "📝 Copy (${analysis.captions.size})",
            ).forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selectedTab == i)
                                BoostColors.Orange.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (selectedTab == i) BoostColors.Orange.copy(0.5f)
                            else Color.Transparent,
                            RoundedCornerShape(9.dp),
                        )
                        .clickable { selectedTab = i }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = if (selectedTab == i) BoostColors.Text else BoostColors.TextDim)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tab content
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            when (selectedTab) {
                // IMAGES
                0 -> images.forEachIndexed { i, img ->
                    BoostCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("${listOf("🌅","⬜","💬")[i]} ${img.label} Variant",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = BoostColors.Text)
                                Text(listOf(
                                    "Aspirational scene, warm light",
                                    "Clean white, product hero",
                                    "Bold text overlay, high contrast",
                                )[i], fontSize = 11.sp, color = BoostColors.TextDim)
                            }
                            GradientButton("⬇ Save", onClick = {},
                                modifier = Modifier.height(32.dp))
                        }
                        // Image preview
                        Box(
                            Modifier.fillMaxWidth().height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BoostColors.Border),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(listOf("🌅","⬜","💬")[i], fontSize = 40.sp)
                            Text("1080×1080", fontSize = 10.sp,
                                color = BoostColors.TextMid,
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .padding(8.dp))
                        }
                    }
                }

                // VIDEOS
                1 -> videos.forEachIndexed { i, vid ->
                    BoostCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("${listOf("🎯","📣")[i]} ${vid.label}",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    color = BoostColors.Text)
                                Text(listOf(
                                    "Bold hook intro + captions",
                                    "Product focus + CTA outro",
                                )[i], fontSize = 11.sp, color = BoostColors.TextDim)
                            }
                            GradientButton("⬇ Save", onClick = {},
                                modifier = Modifier.height(32.dp))
                        }
                        Box(
                            Modifier.fillMaxWidth().height(200.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BoostColors.Border),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("▶️", fontSize = 40.sp)
                            Text("0:0${8+i}", fontSize = 10.sp,
                                color = BoostColors.TextMid,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                            Text("9:16", fontSize = 10.sp,
                                color = BoostColors.TextMid,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp))
                        }
                    }
                }

                // COPY
                2 -> {
                    Text("5 Hook + Caption Combos", fontSize = 10.sp,
                        color = BoostColors.TextDim, letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                    analysis.captions.forEachIndexed { i, cap ->
                        BoostCard(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text("Hook ${i+1}", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = BoostColors.Orange)
                            Spacer(Modifier.height(4.dp))
                            Text(cap.hook, fontSize = 13.sp,
                                color = BoostColors.Text, lineHeight = 20.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(cap.caption, fontSize = 12.sp,
                                color = BoostColors.TextMid, lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(
                                        AnnotatedString("${cap.hook}\n\n${cap.caption}")
                                    )
                                    copiedIdx = i
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (copiedIdx == i) BoostColors.Green
                                                   else BoostColors.TextDim,
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (copiedIdx == i) BoostColors.Green.copy(0.5f)
                                    else BoostColors.Border,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(if (copiedIdx == i) "✓ Copied!" else "📋 Copy",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Hashtags
                    Spacer(Modifier.height(8.dp))
                    Text("Hashtag Packs (30 total)", fontSize = 10.sp,
                        color = BoostColors.TextDim, letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BoostColors.Card)
                            .padding(3.dp),
                    ) {
                        listOf("viral","niche","brand").forEach { tab ->
                            Box(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (hashTab == tab) BoostColors.Orange.copy(0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { hashTab = tab }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(tab.replaceFirstChar { it.uppercase() } + " (10)",
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (hashTab == tab) BoostColors.Orange
                                            else BoostColors.TextDim)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    BoostCard {
                        val tags = when(hashTab) {
                            "viral" -> analysis.hashtags.viral
                            "niche" -> analysis.hashtags.niche
                            else    -> analysis.hashtags.brand
                        }
                        FlowRow(tags)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(tags.joinToString(" ")))
                                copiedIdx = 99
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (copiedIdx == 99) BoostColors.Green
                                               else BoostColors.TextDim),
                            border = BorderStroke(1.dp,
                                if (copiedIdx == 99) BoostColors.Green.copy(0.5f)
                                else BoostColors.Border),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(if (copiedIdx == 99) "✓ Copied!" else "📋 Copy All 10",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun FlowRow(tags: List<String>) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            val rows = tags.chunked(5)
            rows.forEach { row ->
                Row(Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { tag ->
                        ChipLabel(tag, color = BoostColors.Orange)
                    }
                }
            }
        }
    }
}
