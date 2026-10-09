package com.boost.app.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boost.app.data.api.ProjectSummary
import com.boost.app.ui.theme.BoostColors

// ── Screen header ──────────────────────────────────────────────────
@Composable
fun ScreenHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(BoostColors.Bg)
            .padding(horizontal = if (onBack != null) 4.dp else 20.dp, vertical = 14.dp)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Text("‹", fontSize = 22.sp, color = BoostColors.TextDim)
            }
        }
        Column {
            Text(subtitle, fontSize = 9.sp, color = BoostColors.TextDim, letterSpacing = 2.sp)
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                color = BoostColors.Text)
        }
    }
    Divider(color = BoostColors.Border, thickness = 0.5.dp)
}

// ── Gradient button ────────────────────────────────────────────────
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    if (enabled) Brush.horizontalGradient(
                        listOf(BoostColors.Orange, BoostColors.Violet)
                    ) else Brush.horizontalGradient(
                        listOf(BoostColors.Border, BoostColors.Border)
                    ),
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (enabled) BoostColors.Text else BoostColors.TextDim)
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────
@Composable
fun BoostCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BoostColors.Card)
            .border(1.dp, BoostColors.Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        content = content,
    )
}

// ── Quota bar ─────────────────────────────────────────────────────
@Composable
fun QuotaBar(
    label: String, used: Int, total: Int,
    color: Color = BoostColors.Orange,
    modifier: Modifier = Modifier,
) {
    val pct = (used.toFloat() / total).coerceIn(0f, 1f)
    val over = used >= total
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, color = BoostColors.TextDim)
            Text("$used/$total", fontSize = 11.sp,
                color = if (over) BoostColors.Red else color,
                fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(3.dp)
            .clip(RoundedCornerShape(2.dp)).background(BoostColors.Border2)) {
            Box(Modifier.fillMaxHeight()
                .fillMaxWidth(pct)
                .clip(RoundedCornerShape(2.dp))
                .background(if (over) BoostColors.Red else color))
        }
    }
}

// ── Chip label ────────────────────────────────────────────────────
@Composable
fun ChipLabel(text: String, color: Color = BoostColors.TextDim) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Section divider ───────────────────────────────────────────────
@Composable
fun SectionDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Divider(Modifier.weight(1f), color = BoostColors.Border)
        Text(label, fontSize = 10.sp, color = BoostColors.TextDim, letterSpacing = 2.sp)
        Divider(Modifier.weight(1f), color = BoostColors.Border)
    }
}

// ── Section label ─────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 10.sp, color = BoostColors.TextDim,
        letterSpacing = 2.sp,
        modifier = modifier.padding(bottom = 8.dp))
}

// ── Stat card ─────────────────────────────────────────────────────
@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(BoostColors.Card)
            .border(1.dp, BoostColors.Border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 9.sp, color = BoostColors.TextDim)
    }
}

// ── Project row ───────────────────────────────────────────────────
@Composable
fun ProjectRow(
    project: ProjectSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BoostColors.Card)
            .border(1.dp, BoostColors.Border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(BoostColors.Orange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Text("📦", fontSize = 22.sp) }

        Column(Modifier.weight(1f)) {
            Text(project.productName ?: "Product", fontSize = 13.sp,
                fontWeight = FontWeight.Bold, color = BoostColors.Text)
            Text("${project.niche ?: ""} · ${project.createdAt.take(10)}",
                fontSize = 11.sp, color = BoostColors.TextDim)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "🖼" to project.imageCount,
                "🎬" to project.videoCount,
                "📝" to project.captionCount,
            ).forEach { (icon, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(icon, fontSize = 10.sp)
                    Text("$count", fontSize = 10.sp, color = BoostColors.TextDim)
                }
            }
        }
    }
}

// ── Tag grid ──────────────────────────────────────────────────────
@Composable
fun TagGrid(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    activeColor: Color = BoostColors.Orange,
) {
    val rows = items.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    val isActive = selected == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isActive) activeColor.copy(alpha = 0.15f)
                                else BoostColors.Card
                            )
                            .border(
                                1.dp,
                                if (isActive) activeColor else BoostColors.Border,
                                RoundedCornerShape(9.dp),
                            )
                            .clickable { onSelect(item) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Text(item, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isActive) BoostColors.Text else BoostColors.TextMid)
                    }
                }
            }
        }
    }
}

// ── Corner guides (camera UI) ─────────────────────────────────────
@Composable
fun CornerGuides(color: Color) {
    // Top-left
    listOf(
        Alignment.TopStart, Alignment.TopEnd,
        Alignment.BottomStart, Alignment.BottomEnd,
    ).forEach { alignment ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
            Box(Modifier.size(20.dp).padding(4.dp)
                .border(
                    width = 2.dp,
                    color = color,
                    shape = RoundedCornerShape(
                        topStart = if (alignment == Alignment.TopStart) 4.dp else 0.dp,
                        topEnd = if (alignment == Alignment.TopEnd) 4.dp else 0.dp,
                        bottomStart = if (alignment == Alignment.BottomStart) 4.dp else 0.dp,
                        bottomEnd = if (alignment == Alignment.BottomEnd) 4.dp else 0.dp,
                    )
                ))
        }
    }
}

// ── Heat bar ──────────────────────────────────────────────────────
@Composable
fun HeatBar(value: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(1f).height(4.dp)
            .clip(RoundedCornerShape(2.dp)).background(BoostColors.Border2)) {
            Box(Modifier.fillMaxHeight()
                .fillMaxWidth(value / 100f)
                .background(Brush.horizontalGradient(
                    listOf(BoostColors.Orange, BoostColors.Red)
                )))
        }
        Text("$value", fontSize = 11.sp, color = BoostColors.Orange,
            fontWeight = FontWeight.Bold)
    }
}
