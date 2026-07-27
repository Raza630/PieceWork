package com.example.workman.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Visual style of a feedback message. */
enum class FeedbackType { SUCCESS, ERROR, INFO }

/**
 * Immutable payload describing a feedback message to show the user.
 *
 * @param confirmLabel  Text for the primary button (defaults to "Great!").
 */
data class FeedbackData(
    val type: FeedbackType,
    val title: String,
    val message: String,
    val confirmLabel: String = "Great!"
)

/**
 * A polished, animated feedback dialog used to give the worker (or boss)
 * professional confirmation for important actions such as accepting a job,
 * starting work, or completing a job.
 *
 * Shows an animated icon that pops in with a spring, a bold title and a
 * supporting message, plus an optional secondary action button.
 */
@Composable
fun FeedbackDialog(
    data: FeedbackData?,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    if (data == null) return

    val (accent, icon) = when (data.type) {
        FeedbackType.SUCCESS -> Color(0xFF22C55E) to Icons.Default.CheckCircle
        FeedbackType.ERROR -> Color(0xFFEF4444) to Icons.Default.Warning
        FeedbackType.INFO -> Color(0xFF5B61F4) to Icons.Default.Info
    }

    // Spring "pop" animation for the icon.
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(data) {
        scale.snapTo(0.3f)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C3D),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = data.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7089),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onConfirm?.invoke(); onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        text = data.confirmLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                if (secondaryLabel != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { onSecondary?.invoke(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = secondaryLabel,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

