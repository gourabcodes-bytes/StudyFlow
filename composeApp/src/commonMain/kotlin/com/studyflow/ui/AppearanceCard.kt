package com.studyflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ThemeModes = listOf("System", "Light", "Dark")

@Composable
fun AppearanceCard(
    themeMode: Int,
    accentIndex: Int,
    onThemeMode: (Int) -> Unit,
    onAccent: (Int) -> Unit,
) {
    SoftCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Text("Theme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeModes.forEachIndexed { index, label ->
                    FilterChip(selected = themeMode == index, onClick = { onThemeMode(index) }, label = { Text(label) })
                }
            }

            val chosen = AccentPalettes[accentIndex.coerceIn(0, AccentPalettes.lastIndex)]
            Text(
                "Accent colour · ${chosen.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentPalettes.forEachIndexed { index, palette ->
                    val selected = index == accentIndex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.swatch)
                            .then(
                                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier,
                            )
                            .clickable { onAccent(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                        }
                    }
                }
            }
        }
    }
}
