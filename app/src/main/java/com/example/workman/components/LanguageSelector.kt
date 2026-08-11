package com.example.workman.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.R
import com.example.workman.utils.LocaleManager

/**
 * A globe "🌐" style icon button used in dashboard headers to open the language picker.
 * Rendered on a translucent surface so it sits nicely on the coloured gradient headers.
 *
 * @param tint icon colour (defaults to white for gradient headers).
 * @param containerColor translucent pill background behind the icon.
 */
@Composable
fun LanguageIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    containerColor: Color = Color.White.copy(alpha = 0.15f)
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        LanguageTranslateIcon(
            modifier = Modifier.size(24.dp),
            tint = tint
        )
    }
}

/**
 * A custom translate icon featuring 'A' and 'अ' in overlapping speech bubbles.
 */
@Composable
private fun LanguageTranslateIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // The container for the two overlapping bubbles
        Box(modifier = Modifier.size(22.dp)) {
            // Background/Bottom bubble (typically represents the 'secondary' language)
            // Positioned top-left
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(tint.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.Black.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 9.sp
                )
            }

            // Foreground/Top bubble (positioned bottom-right)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(tint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "अ",
                    color = Color.Black.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

/**
 * Bottom-sheet language picker. Lists every language the app supports in its own
 * script (native name) plus the English name, highlights the active one, and applies
 * the choice instantly via [LocaleManager] (which recreates the UI in the new language).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = LocaleManager.current()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.select_language_subtitle),
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )
            Spacer(Modifier.height(16.dp))

            LocaleManager.supportedLanguages.forEach { language ->
                val isSelected = language == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.10f) else Color(0xFFF6F6F8)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (!isSelected) LocaleManager.setLanguage(language)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = language.nativeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        if (language.nativeName != language.englishName) {
                            Text(
                                text = language.englishName,
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LanguageIconButtonPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        LanguageIconButton(
            onClick = {},
            containerColor = Color.LightGray
        )
    }
}


