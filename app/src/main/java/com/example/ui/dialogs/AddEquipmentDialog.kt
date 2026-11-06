package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.RoomEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEquipmentDialog(
    rooms: List<RoomEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, tag: String, category: String, status: String, specs: String, roomId: String?, roomName: String, location: Offset) -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HVAC") }
    var status by remember { mutableStateOf("Operational") }
    var specs by remember { mutableStateOf("") }
    var selectedRoom by remember(rooms) { mutableStateOf(rooms.firstOrNull()) }

    var catExpanded by remember { mutableStateOf(false) }
    var roomExpanded by remember { mutableStateOf(false) }

    val categories = listOf("HVAC", "ELECTRICAL", "PLUMBING", "NETWORK", "SENSOR", "SAFETY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Equipment", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(
                onClick = {
                    if (tag.isNotBlank() && name.isNotBlank()) {
                        val roomCenter = selectedRoom?.let {
                            Offset((it.minX + it.maxX) / 2f, (it.minY + it.maxY) / 2f)
                        } ?: Offset(0.5f, 0.5f)

                        onConfirm(
                            name.trim(),
                            tag.trim().uppercase(),
                            category,
                            status,
                            specs.trim(),
                            selectedRoom?.id,
                            selectedRoom?.name ?: "Plant Floor",
                            roomCenter
                        )
                        onDismiss()
                    }
                },
                enabled = tag.isNotBlank() && name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_equipment_btn")
            ) {
                Text("Register Equipment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Equipment Tag ID (e.g. PUMP-03)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_eq_tag_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Equipment / Machine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_eq_name_input")
                )

                // Category
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { category = c; catExpanded = false })
                        }
                    }
                }

                // Room placement
                ExposedDropdownMenuBox(
                    expanded = roomExpanded,
                    onExpandedChange = { roomExpanded = !roomExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRoom?.name ?: "General Floor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign to Room") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roomExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roomExpanded,
                        onDismissRequest = { roomExpanded = false }
                    ) {
                        rooms.forEach { r ->
                            DropdownMenuItem(text = { Text(r.name) }, onClick = { selectedRoom = r; roomExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = specs,
                    onValueChange = { specs = it },
                    label = { Text("Specifications / Model") },
                    placeholder = { Text("e.g. 480V 30kW, R-410A, 1200 RPM") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
