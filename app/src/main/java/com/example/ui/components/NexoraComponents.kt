package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiResponseState
import com.example.data.model.ToolModule
import com.example.localization.AppLanguage
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CrimsonSos
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldLife
import com.example.ui.theme.IndigoNeural
import com.example.ui.theme.VioletExpert

@Composable
fun NexoraGlowOrb(
    size: Dp = 76.dp,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.12f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(pulse)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CyanPulse.copy(alpha = 0.5f),
                        IndigoNeural.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(CyanPulse, IndigoNeural)),
                shape = CircleShape
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_nexora_avatar),
            contentDescription = "NEXORA AI Orb",
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraModuleTopBar(
    title: String,
    language: AppLanguage,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            FilterChip(
                selected = language == AppLanguage.BN,
                onClick = onToggleLanguage,
                label = {
                    Text(
                        text = if (language == AppLanguage.BN) "বাং" else "EN",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("language_toggle_chip")
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        modifier = modifier
    )
}

@Composable
fun AiResponseCard(
    state: AiResponseState,
    language: AppLanguage,
    onSpeak: ((String) -> Unit)? = null,
    onSaveDoc: ((String) -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (state is AiResponseState.Idle) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (state is AiResponseState.Error) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (state is AiResponseState.Error) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (state is AiResponseState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (state) {
                            is AiResponseState.Loading -> if (language == AppLanguage.BN) "NEXORA চিন্তা করছে..." else "NEXORA is thinking..."
                            is AiResponseState.Streaming -> "NEXORA AI • Live"
                            is AiResponseState.Success -> "NEXORA AI"
                            is AiResponseState.Error -> if (language == AppLanguage.BN) "সমস্যা হয়েছে" else "Error"
                            AiResponseState.Idle -> ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state is AiResponseState.Error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary
                    )
                }

                val textToAct = when (state) {
                    is AiResponseState.Success -> state.text
                    is AiResponseState.Streaming -> state.text
                    else -> null
                }
                if (textToAct != null) {
                    Row {
                        if (onSpeak != null) {
                            IconButton(
                                onClick = { onSpeak(textToAct) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("NEXORA AI", textToAct))
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (onSaveDoc != null) {
                            IconButton(
                                onClick = { onSaveDoc(textToAct) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Save to Documents",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                is AiResponseState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "তথ্য বিশ্লেষণ চলছে..." else "Analyzing with Gemini...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is AiResponseState.Streaming -> {
                    Text(text = state.text, style = MaterialTheme.typography.bodyLarge)
                }
                is AiResponseState.Success -> {
                    Text(text = state.text, style = MaterialTheme.typography.bodyLarge)
                }
                is AiResponseState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onRetry) {
                            Text(if (language == AppLanguage.BN) "আবার চেষ্টা করুন" else "Retry")
                        }
                    }
                }
                AiResponseState.Idle -> Unit
            }
        }
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun moduleVisualSpec(module: ToolModule): Pair<ImageVector, Color> = when (module) {
    ToolModule.LIFE_MANAGER -> Icons.Default.EventAvailable to CyanPulse
    ToolModule.HEALTH_ASSISTANT -> Icons.Default.HealthAndSafety to EmeraldLife
    ToolModule.EDUCATION_CENTER -> Icons.Default.School to IndigoNeural
    ToolModule.FINANCE_MANAGER -> Icons.Default.AccountBalanceWallet to EmeraldLife
    ToolModule.DOCUMENT_AI -> Icons.Default.Description to CyanPulse
    ToolModule.TRANSLATOR -> Icons.Default.Translate to VioletExpert
    ToolModule.EMERGENCY_SAFETY -> Icons.Default.Sos to CrimsonSos
    ToolModule.AI_VISION -> Icons.Default.Visibility to CyanPulse
    ToolModule.SHOPPING_ASSISTANT -> Icons.Default.ShoppingBag to AmberAlert
    ToolModule.TRAVEL_PLANNER -> Icons.Default.FlightTakeoff to IndigoNeural
    ToolModule.SMART_COMMUNICATION -> Icons.Default.EditNote to VioletExpert
    ToolModule.SMART_HOME -> Icons.Default.Home to EmeraldLife
    ToolModule.MEETING_INTELLIGENCE -> Icons.Default.RecordVoiceOver to AmberAlert
    ToolModule.FAMILY_SPACE -> Icons.Default.Groups to CyanPulse
    ToolModule.MEMORY_TWIN -> Icons.Default.Psychology to VioletExpert
}

@Composable
fun expertIconFor(modeId: String): ImageVector = when (modeId) {
    "DOCTOR_INFO" -> Icons.Default.HealthAndSafety
    "TEACHER" -> Icons.Default.School
    "FINANCIAL" -> Icons.Default.AccountBalanceWallet
    "CAREER" -> Icons.Default.EditNote
    "TRAVEL" -> Icons.Default.FlightTakeoff
    else -> Icons.AutoMirrored.Filled.Chat
}
