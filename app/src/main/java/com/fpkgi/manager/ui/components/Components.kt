package com.fpkgi.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpkgi.manager.data.model.AvailStatus
import com.fpkgi.manager.i18n.AppStrings
import com.fpkgi.manager.i18n.LocalAppStrings
import com.fpkgi.manager.ui.theme.*

@Composable
fun StatusBadge(status: AvailStatus, strings: AppStrings = LocalAppStrings.current) {
    val (text, color) = when (status) {
        AvailStatus.AVAILABLE   -> strings.statusAvailable   to NeonGreen
        AvailStatus.UNAVAILABLE -> strings.statusUnavailable to ErrorRed
        AvailStatus.CHECKING    -> strings.statusChecking    to GoldYellow
        AvailStatus.UNCHECKED   -> strings.statusUnchecked   to TextMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MonoLabel(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(100.dp))
        Text(value, color = TextPrimary, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title, color = CyberBlue, fontSize = 11.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun FpkgiButton(
    text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = CyberBlue, enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.fillMaxWidth(),
        colors   = ButtonDefaults.buttonColors(
            containerColor          = color.copy(alpha = 0.15f),
            contentColor            = color,
            disabledContainerColor  = TextMuted.copy(alpha = 0.1f),
            disabledContentColor    = TextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        shape  = RoundedCornerShape(6.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun InfoChip(text: String, color: Color = TextSecondary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(TextMuted.copy(alpha = 0.3f))
    )
}
