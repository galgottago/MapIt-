package com.example.ui.camera

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.PhotoEntity
import com.example.data.model.RoomEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraInspectionScreen(
    photos: List<PhotoEntity>,
    rooms: List<RoomEntity>,
    equipmentList: List<EquipmentEntity>,
    annotations: List<AnnotationEntity> = emptyList(),
    onSaveCapturedPhoto: (Bitmap, String, String?, String) -> Unit,
    onAddArPushpin: (String, Float, Float, Long) -> Unit = { _, _, _, _ -> },
    onDeleteAnnotation: (AnnotationEntity) -> Unit = {},
    onDeletePhoto: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedRoomName by remember(rooms) {
        mutableStateOf(rooms.firstOrNull()?.name ?: "Facility Entrance")
    }
    var selectedEquipmentTag by remember(equipmentList) {
        mutableStateOf(equipmentList.firstOrNull()?.tag)
    }
    var inspectionNote by remember { mutableStateOf("") }
    var locationExpanded by remember { mutableStateOf(false) }
    var equipmentExpanded by remember { mutableStateOf(false) }
    var selectedPhotoForView by remember { mutableStateOf<PhotoEntity?>(null) }
    var isScanningAR by remember { mutableStateOf(false) }

    // If AR Scanner mode is active, render the live camera feed with AR overlays
    if (isScanningAR) {
        val currentRoomObj = rooms.find { it.name == selectedRoomName } ?: rooms.firstOrNull()
        ArRoomScannerView(
            activeRoom = currentRoomObj,
            equipmentList = equipmentList,
            annotations = annotations,
            onCapturePhoto = { bitmap, locName, eqTag, note ->
                onSaveCapturedPhoto(bitmap, locName, eqTag, note)
                isScanningAR = false
                Toast.makeText(context, "AR Scan saved to Camera Roll: $locName", Toast.LENGTH_SHORT).show()
            },
            onAddArPushpin = onAddArPushpin,
            onDeleteAnnotation = onDeleteAnnotation,
            onClose = { isScanningAR = false },
            modifier = modifier
        )
        return
    }

    // Camera launcher contract that captures photo directly from camera hardware
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onSaveCapturedPhoto(
                bitmap,
                selectedRoomName,
                selectedEquipmentTag,
                inspectionNote.ifBlank { "Routine field inspection photo" }
            )
            Toast.makeText(
                context,
                "Photo saved to Camera Roll: Blueprint_${selectedRoomName.take(12)}",
                Toast.LENGTH_LONG
            ).show()
            inspectionNote = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Field Camera & Photo Roll",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Photos auto-save to device Camera Roll with room and equipment location in file name",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Capture Setup Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "LOCATION TAGGING SETUP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Room Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = !locationExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRoomName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Associated Room / Zone") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = { Text("${room.name} [${room.code}]") },
                                onClick = {
                                    selectedRoomName = room.name
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Equipment Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = equipmentExpanded,
                    onExpandedChange = { equipmentExpanded = !equipmentExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedEquipmentTag ?: "None (General Room Area)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Associated Equipment Tag (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = equipmentExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = equipmentExpanded,
                        onDismissRequest = { equipmentExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (General Room Area)") },
                            onClick = {
                                selectedEquipmentTag = null
                                equipmentExpanded = false
                            }
                        )
                        equipmentList.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text("${eq.tag} - ${eq.name}") },
                                onClick = {
                                    selectedEquipmentTag = eq.tag
                                    selectedRoomName = eq.roomName.ifBlank { selectedRoomName }
                                    equipmentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Note Input
                OutlinedTextField(
                    value = inspectionNote,
                    onValueChange = { inspectionNote = it },
                    label = { Text("Inspection Note / Caption") },
                    placeholder = { Text("e.g. Pump pressure gauge reading, leak check...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_note_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Launch Live AR Scanner Button
                Button(
                    onClick = { isScanningAR = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_ar_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Scan Room",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Room in Real-Time (AR View)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Snapshot to Camera Roll Button
                OutlinedButton(
                    onClick = { takePictureLauncher.launch(null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("open_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Capture Photo",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Take Photo & Save with Location Name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Saved Photos Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Captured Photos (${photos.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Stored in Pictures/Blueprint3D",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No inspection photos captured yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the camera button above to photograph equipment or room conditions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoCard(
                        photo = photo,
                        onClick = { selectedPhotoForView = photo },
                        onDelete = { onDeletePhoto(photo) }
                    )
                }
            }
        }
    }

    // Full-Size Photo Preview Dialog
    selectedPhotoForView?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPhotoForView = null },
            confirmButton = {
                TextButton(onClick = { selectedPhotoForView = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeletePhoto(photo)
                        selectedPhotoForView = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            title = {
                Column {
                    Text(text = photo.fileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        text = "Location: ${photo.roomName} • ${photo.equipmentTag ?: "General"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = photo.filePath.ifBlank { photo.uriString },
                            contentDescription = "Inspection Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (photo.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note: ${photo.note}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun PhotoCard(
    photo: PhotoEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("photo_card_${photo.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF0F172A))
            ) {
                AsyncImage(
                    model = photo.filePath.ifBlank { photo.uriString },
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Delete button in corner
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Equipment tag badge
                if (photo.equipmentTag != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = photo.equipmentTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = photo.roomName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = photo.fileName,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (photo.note.isNotBlank()) {
                    Text(
                        text = photo.note,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
