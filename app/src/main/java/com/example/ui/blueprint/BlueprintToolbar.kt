package com.example.ui.blueprint

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ToolMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintToolbar(
    is3DMode: Boolean,
    activeTool: ToolMode,
    activeColor: Long,
    isDarkTheme: Boolean,
    onToggle2D3D: () -> Unit,
    onSetTool: (ToolMode) -> Unit,
    onSetColor: (Long) -> Unit,
    onResetCamera: () -> Unit,
    onClearAnnotations: () -> Unit,
    onSearchDestination: (String) -> Boolean,
    onRelocateUser: () -> Unit,
    onOpenArScanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchError by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val colors = listOf(
        0xFF00E5FF, // Cyan
        0xFFFFD166, // Amber
        0xFF10B981, // Emerald Green
        0xFFEF4444, // Red
        0xFFA855F7, // Purple
        0xFFFFFFFF  // White
    )

    Column(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Quick Destination Search Bar with Instant Path Calculation
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDarkTheme) Color(0xDD141F36) else Color(0xEEFFFFFF),
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchError = false
                    },
                    placeholder = {
                        Text(
                            text = "Navigate: enter room name or ID (e.g. CHILLER-01)...",
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val success = onSearchDestination(searchQuery)
                            if (!success) searchError = true
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("blueprint_search_input")
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            searchError = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val success = onSearchDestination(searchQuery)
                        if (!success) searchError = true
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .testTag("blueprint_search_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Route Path",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (searchError) {
            Text(
                text = "Target not found. Try 'Chiller', 'Boiler', 'CH-100', 'SCADA', or 'Cleanroom'.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tool Palette & Mode Toggles
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDarkTheme) Color(0xDD141F36) else Color(0xEEFFFFFF),
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 2D / 3D Mode Switcher
                FilterChip(
                    selected = is3DMode,
                    onClick = onToggle2D3D,
                    label = {
                        Text(
                            text = if (is3DMode) "3D Extrusion" else "2D Floorplan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Layers,
                            contentDescription = "Toggle 2D/3D",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("toggle_2d_3d_chip")
                )

                // Tool Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Navigate / Pan
                    ToolIconButton(
                        icon = Icons.Default.Navigation,
                        contentDesc = "Navigate",
                        isSelected = activeTool == ToolMode.NAVIGATE,
                        onClick = { onSetTool(ToolMode.NAVIGATE) },
                        testTag = "tool_navigate"
                    )

                    // Marker Pen
                    ToolIconButton(
                        icon = Icons.Default.Edit,
                        contentDesc = "Marker Pen",
                        isSelected = activeTool == ToolMode.MARKER,
                        onClick = { onSetTool(ToolMode.MARKER) },
                        testTag = "tool_marker"
                    )

                    // Highlighter
                    ToolIconButton(
                        icon = Icons.Default.Highlight,
                        contentDesc = "Highlighter",
                        isSelected = activeTool == ToolMode.HIGHLIGHTER,
                        onClick = { onSetTool(ToolMode.HIGHLIGHTER) },
                        testTag = "tool_highlighter"
                    )

                    // 3D Text Annotation
                    ToolIconButton(
                        icon = Icons.Default.TextFields,
                        contentDesc = "3D Text",
                        isSelected = activeTool == ToolMode.TEXT_3D,
                        onClick = { onSetTool(ToolMode.TEXT_3D) },
                        testTag = "tool_text_3d"
                    )

                    // Color Palette Toggle
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(activeColor.toInt()))
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { showColorPicker = !showColorPicker }
                            .testTag("tool_color_picker_toggle")
                    )

                    // Reset Camera
                    IconButton(
                        onClick = onResetCamera,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tool_reset_camera")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Camera View",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Relocate User
                    IconButton(
                        onClick = onRelocateUser,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tool_relocate_user")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = "Relocate User Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // AR Room Scanner
                    IconButton(
                        onClick = onOpenArScanner,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tool_open_ar_scanner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusWeak,
                            contentDescription = "Scan Room in AR",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Color Palette Selector Row
        AnimatedVisibility(visible = showColorPicker) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0xFF1E2D4A) else Color(0xFFE2E8F0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Tool Color:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(c.toInt()))
                                .border(
                                    width = if (activeColor == c) 2.5.dp else 1.dp,
                                    color = if (activeColor == c) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onSetColor(c)
                                    showColorPicker = false
                                }
                        )
                    }

                    IconButton(
                        onClick = {
                            onClearAnnotations()
                            showColorPicker = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Markups",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(34.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
