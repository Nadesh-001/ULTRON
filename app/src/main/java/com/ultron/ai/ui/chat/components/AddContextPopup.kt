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
 * Modern Add Context Popup matching the ULTRON AI HUD Specification (Exact Visual Match).
 * Presents a sleek vertical list of context sources: Files, Folder, Mentions, Actions, Browser, Drive.
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
                .width(220.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16181D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2F3A)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Context",
                        color = Color(0xFF8E95A5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = Color(0xFF222630), thickness = 1.dp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))

                // Vertical List of Context Items (Exact Match)
                ContextMenuItem(
                    title = "Files",
                    icon = Icons.Default.AttachFile,
                    onClick = { onOptionSelected(ContextOption.FILES) }
                )
                ContextMenuItem(
                    title = "Folder",
                    icon = Icons.Default.FolderOpen,
                    onClick = { onOptionSelected(ContextOption.FOLDER) }
                )
                ContextMenuItem(
                    title = "Mentions",
                    icon = Icons.Default.AlternateEmail,
                    onClick = { onOptionSelected(ContextOption.MENTIONS) }
                )
                ContextMenuItem(
                    title = "Actions",
                    icon = Icons.Default.EditNote,
                    onClick = { onOptionSelected(ContextOption.ACTIONS) }
                )
                ContextMenuItem(
                    title = "Browser",
                    icon = Icons.Default.Language,
                    onClick = { onOptionSelected(ContextOption.BROWSER) }
                )
                ContextMenuItem(
                    title = "Drive",
                    icon = Icons.Default.CloudSync,
                    trailingText = "Set up",
                    trailingTextColor = Color(0xFF38BDF8),
                    onClick = { onOptionSelected(ContextOption.DRIVE) }
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    title: String,
    icon: ImageVector,
    trailingText: String? = null,
    trailingTextColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
            Text(
                title,
                color = Color(0xFFE0E0E0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
        if (trailingText != null) {
            Text(
                trailingText,
                color = trailingTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
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
