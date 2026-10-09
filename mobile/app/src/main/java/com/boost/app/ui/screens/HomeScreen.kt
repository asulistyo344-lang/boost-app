package com.boost.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.app.data.api.QuotaResponse
import com.boost.app.data.api.ProjectSummary
import com.boost.app.ui.theme.BoostColors
import com.boost.app.ui.components.*

// ─── ONBOARDING ───────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var slide by remember { mutableStateOf(0) }

    val slides = listOf(
        Triple("🎬", "Film It",    "Shoot your product or upload from gallery. Photo or video — both work."),
        Triple("🤖", "AI Analyzes","4 AI models analyze your product and generate scroll-stopping content in 30 seconds."),
        Triple("📲", "Post It",    "Download polished images, videos, and captions. Post manually to TikTok or Reels."),
    )
    val colors = listOf(BoostColors.Orange, BoostColors.Violet, BoostColors.Green)
    val (icon, title, desc) = slides[slide]
    val color = colors[slide]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoostColors.Bg)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Logo
        Text(
            text = "BOOST.",
            fontSize = 20.sp, fontWeight = FontWeight.Black,
            color = BoostColors.Text,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 40.dp),
        )

        // Icon box
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(color.copy(alpha = 0.13f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 52.sp)
        }

        Spacer(Modifier.height(32.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = BoostColors.Text)
        Spacer(Modifier.height(14.dp))
        Text(
            desc, fontSize = 14.sp, color = BoostColors.TextMid,
            textAlign = TextAlign.Center, lineHeight = 22.sp,
        )

        // Progress dots
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            slides.indices.forEach { i ->
                Box(modifier = Modifier
                    .height(8.dp)
                    .width(if (i == slide) 24.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == slide) BoostColors.Orange else BoostColors.Border2)
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        // CTA
        Button(
            onClick = { if (slide < 2) slide++ else onDone() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(BoostColors.Orange, BoostColors.Violet)),
                        RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (slide < 2) "Next →" else "Start Creating",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = BoostColors.Text,
                )
            }
        }

        if (slide < 2) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDone) {
                Text("Skip", color = BoostColors.TextDim, fontSize = 13.sp)
            }
        }
    }
}

// ─── HOME ─────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    isPro: Boolean,
    quota: QuotaResponse?,
    projects: List<ProjectSummary>,
    onNewContent: () -> Unit,
    onUpgrade: () -> Unit,
    onProjectClick: (ProjectSummary) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BoostColors.Bg)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Creator Studio", fontSize = 10.sp, color = BoostColors.TextDim,
                    letterSpacing = 2.sp)
                Text(
                    text = "BOOST.", fontSize = 20.sp,
                    fontWeight = FontWeight.Black, color = BoostColors.Text,
                )
            }
            if (!isPro) {
                OutlinedButton(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BoostColors.Gold,
                    ),
                    border = BorderStroke(1.dp, BoostColors.Gold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("✦ Go Pro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quota card (free only)
        if (!isPro && quota != null) {
            BoostCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Today's Quota", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = BoostColors.Text)
                    GradientButton(text = "Upgrade", onClick = onUpgrade,
                        modifier = Modifier.height(30.dp))
                }
                quota.quotas.forEach { q ->
                    QuotaBar(
                        label = q.genType.replaceFirstChar { it.uppercase() },
                        used = q.used, total = q.limit,
                        color = when(q.genType) {
                            "image" -> BoostColors.Violet
                            "video" -> BoostColors.Green
                            else    -> BoostColors.Orange
                        },
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text("Resets midnight UTC", fontSize = 11.sp, color = BoostColors.TextDim,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        // Main CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            BoostColors.Orange.copy(alpha = 0.13f),
                            BoostColors.Violet.copy(alpha = 0.13f),
                        )
                    )
                )
                .border(
                    1.dp,
                    BoostColors.Orange.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp),
                )
                .clickable { onNewContent() }
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎬", fontSize = 36.sp)
                Spacer(Modifier.height(10.dp))
                Text("Create New Content", fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold, color = BoostColors.Text)
                Text("Film or upload product → AI does the rest",
                    fontSize = 12.sp, color = BoostColors.TextDim)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("📸 Photo", "🎥 Video", "🖼 Gallery").forEach {
                        ChipLabel(it)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                Triple("Projects", projects.size.toString(), BoostColors.Orange),
                Triple("Images",   (projects.sumOf { it.imageCount }).toString(), BoostColors.Violet),
                Triple("Videos",   (projects.sumOf { it.videoCount }).toString(), BoostColors.Green),
                Triple("Captions", (projects.sumOf { it.captionCount }).toString(), BoostColors.Gold),
            ).forEach { (label, value, color) ->
                StatCard(label, value, color, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(20.dp))

        // Recent projects
        if (projects.isNotEmpty()) {
            SectionDivider("Recent Projects")
            projects.take(5).forEach { project ->
                ProjectRow(project = project, onClick = { onProjectClick(project) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
