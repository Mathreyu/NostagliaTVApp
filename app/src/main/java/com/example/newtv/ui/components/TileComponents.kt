package com.example.newtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
fun TileCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    accentSeed: String
) {
    val accent = accentColor(accentSeed)
    val shape = MaterialTheme.shapes.medium
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape),
        border = CardDefaults.border(
            Border(BorderStroke(2.dp, accent), 0.dp, shape),
            Border(BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary), 4.dp, shape),
            Border(BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary), 4.dp, shape)
        ),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun TileRow(
    title: String,
    subtitle: String,
    accentSeed: String
) {
    val accent = accentColor(accentSeed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .border(2.dp, accent, MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .width(96.dp)
                .background(accent.copy(alpha = 0.35f), MaterialTheme.shapes.small)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

private fun accentColor(seed: String): Color {
    val colors = listOf(
        Color(0xFFFF4FD8),
        Color(0xFF33F6FF),
        Color(0xFFFFE66D),
        Color(0xFF8CFF5E)
    )
    val index = (seed.hashCode() and Int.MAX_VALUE) % colors.size
    return colors[index]
}
