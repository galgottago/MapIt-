package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity

@Composable
fun EditRoomDialog(
    room: RoomEntity,
    onDismiss: () -> Unit,
    onSave: (newName: String, newCode: String, newNotes: String) -> Unit,
    onNavigateToRoom: () -> Unit,
    onAddRoomNote: (roomName: String) -> Unit,
    onImmerseInRoom: () -> Unit
) {
    var name by remember { mutableStateOf(room.name) }
    var code by remember { mutableStateOf(room.code) }
    var notes by remember { mutableStateOf(room.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Room Label & Notes", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, code, notes)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_room_label_button")
            ) {
                Text("Save Changes")
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Room / Zone Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("room_name_input")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Room Code / Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("room_code_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("In-Room Technical Notes") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("room_notes_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddRoomNote(room.name)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Note", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToRoom()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("navigate_to_room_btn")
                    ) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate HUD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onImmerseInRoom()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color(0xFF261900)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("immerse_room_btn")
                ) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Step Inside / Immerse into Room (360° Walkthrough)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEquipmentDialog(
    equipment: EquipmentEntity,
    onDismiss: () -> Unit,
    onSave: (newTag: String, newName: String, newStatus: String, newSpecs: String, newNotes: String) -> Unit,
    onNavigateToEquipment: () -> Unit
) {
    var tag by remember { mutableStateOf(equipment.tag) }
    var name by remember { mutableStateOf(equipment.name) }
    var status by remember { mutableStateOf(equipment.status) }
    var specs by remember { mutableStateOf(equipment.specifications) }
    var notes by remember { mutableStateOf(equipment.notes) }
    var statusExpanded by remember { mutableStateOf(false) }

    val statusOptions = listOf("Operational", "Maintenance", "Offline")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Equipment: ${equipment.tag}", fontWeight = FontWeight.Bold)
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(tag, name, status, specs, notes)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_equipment_label_button")
            ) {
                Text("Save Changes")
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
                    label = { Text("Equipment Tag ID (e.g. CHILLER-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_equipment_tag_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Equipment Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Operational Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    status = opt
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = specs,
                    onValueChange = { specs = it },
                    label = { Text("Specifications & Parameters") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Maintenance Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToEquipment()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Highlight 3D Path in HUD", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
