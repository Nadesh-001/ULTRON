package com.ultron.ai.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.ai.core.context.ContextItem
import com.ultron.ai.core.context.MentionCategory
import com.ultron.ai.ui.theme.*

/**
 * Modern Add Context Popup matching the ULTRON AI HTML HUD Specification.
 * Presents a 2-column grid of context sources: Files, Folder, @ Mentions, Actions, Browser, Drive.
 */
@Composable
fun AddContextPopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onOptionSelected: (ContextOption) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineDim),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ADD CONTEXT & TOOLS",
                        color = OnSurfaceMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close context menu",
                            tint = OnSurfaceMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 2-Column Grid of 6 Context Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContextCard(
                        title = "Files",
                        subtitle = "PDF, DOCX, CSV",
                        icon = Icons.Default.AttachFile,
                        onClick = { onOptionSelected(ContextOption.FILES) },
                        modifier = Modifier.weight(1f)
                    )
                    ContextCard(
                        title = "Folder",
                        subtitle = "Kotlin & Gradle",
                        icon = Icons.Default.FolderOpen,
                        onClick = { onOptionSelected(ContextOption.FOLDER) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContextCard(
                        title = "@ Mentions",
                        subtitle = "Smart Sources",
                        icon = Icons.Default.AlternateEmail,
                        onClick = { onOptionSelected(ContextOption.MENTIONS) },
                        modifier = Modifier.weight(1f)
                    )
                    ContextCard(
                        title = "Actions",
                        subtitle = "Tools & Routines",
                        icon = Icons.Default.Bolt,
                        onClick = { onOptionSelected(ContextOption.ACTIONS) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContextCard(
                        title = "Browser",
                        subtitle = "Web & Extract",
                        icon = Icons.Default.Language,
                        onClick = { onOptionSelected(ContextOption.BROWSER) },
                        modifier = Modifier.weight(1f)
                    )
                    ContextCard(
                        title = "Drive",
                        subtitle = "Google OAuth",
                        icon = Icons.Default.CloudSync,
                        onClick = { onOptionSelected(ContextOption.DRIVE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

enum class ContextOption {
    FILES, FOLDER, MENTIONS, ACTIONS, BROWSER, DRIVE
}

@Composable
private fun ContextCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .border(1.dp, OutlineDim, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = UltronRed,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    subtitle,
                    color = OnSurfaceMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Autocomplete bar for intelligent @ mentions.
 */
@Composable
fun MentionsAutocompleteBar(
    isVisible: Boolean,
    onMentionSelected: (MentionCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .border(1.dp, UltronRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MentionCategory.values().forEach { category ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, OutlineDim, RoundedCornerShape(4.dp))
                        .clickable { onMentionSelected(category) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        category.tag,
                        color = UltronRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Horizontal row of active attachment and context chips with remove buttons.
 */
@Composable
fun ContextChipsRow(
    items: List<ContextItem>,
    onRemoveItem: (ContextItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceDark)
                    .border(
                        1.dp,
                        if (item is ContextItem.MentionReference) UltronRed else OutlineDim,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val icon = when (item) {
                        is ContextItem.FileAttachment -> Icons.Default.Description
                        is ContextItem.FolderContext -> Icons.Default.FolderOpen
                        is ContextItem.MentionReference -> Icons.Default.AlternateEmail
                        is ContextItem.WebContext -> Icons.Default.Language
                        is ContextItem.DriveContext -> Icons.Default.CloudSync
                        is ContextItem.ActionRequestItem -> Icons.Default.Bolt
                    }
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = UltronRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        item.displayName.take(24),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "×",
                        color = OnSurfaceMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onRemoveItem(item) }
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authorized Tool Execution Confirmation Card.
 * Displays when the AI requests an action (e.g. sending email, modifying settings)
 * with [CONFIRM] and [CANCEL] triggers.
 */
@Composable
fun ActionConfirmationCard(
    action: ContextItem.ActionRequestItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, UltronRed),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = UltronRed, modifier = Modifier.size(18.dp))
                    Text(
                        "ACTION: ${action.displayName.uppercase()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    action.status.name,
                    color = if (action.status == com.ultron.ai.core.context.ActionStatus.PENDING) UltronRed else SuccessGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                "TARGET: ${action.target}",
                color = OnSurfaceMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            if (action.status == com.ultron.ai.core.context.ActionStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CONFIRM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineDim),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
