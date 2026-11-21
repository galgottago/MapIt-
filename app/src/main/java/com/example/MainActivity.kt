package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity
import com.example.ui.AppTab
import com.example.ui.BlueprintViewModel
import com.example.ui.ToolMode
import com.example.ui.blueprint.BlueprintCanvasView
import com.example.ui.blueprint.BlueprintToolbar
import com.example.ui.blueprint.DrawingShortcut
import com.example.ui.blueprint.drawingShortcut
import com.example.ui.camera.CameraInspectionScreen
import com.example.ui.dialogs.Add3DTextDialog
import com.example.ui.dialogs.AddEquipmentDialog
import com.example.ui.dialogs.EditEquipmentDialog
import com.example.ui.dialogs.EditRoomDialog
import com.example.ui.dialogs.ProjectSelectorDialog
import com.example.ui.equipment.EquipmentCatalogScreen
import com.example.ui.hud.HeadsUpDisplay
import com.example.ui.notes.NotesHubScreen
import com.example.ui.theme.BlueprintTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BlueprintViewModel by viewModels()

    /** True while a text field or dialog should keep its own undo. */
    @Volatile
    private var drawingShortcutsBlocked = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0 && handleDrawingShortcut(keyCode, event)) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun handleDrawingShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if (drawingShortcutsBlocked) return false
        val action = drawingShortcut(
            ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed,
            shift = event.isShiftPressed,
            isZ = keyCode == KeyEvent.KEYCODE_Z,
            isY = keyCode == KeyEvent.KEYCODE_Y
        ) ?: return false
        when (action) {
            DrawingShortcut.Undo -> viewModel.undoDrawing()
            DrawingShortcut.Redo -> viewModel.redoDrawing()
        }
        return true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val currentProject by viewModel.currentProject.collectAsState()
            val allProjects by viewModel.allProjects.collectAsState()
            val selectedProjectId by viewModel.selectedProjectId.collectAsState()
            val rooms by viewModel.rooms.collectAsState()
            val equipmentList by viewModel.equipmentList.collectAsState()
            val annotations by viewModel.annotations.collectAsState()
            val photos by viewModel.photos.collectAsState()
            val notes by viewModel.notes.collectAsState()
            val camera by viewModel.camera.collectAsState()
            val userLocation by viewModel.virtualUserLocation.collectAsState()
            val navTarget by viewModel.navigationTarget.collectAsState()
            val calculatedPath by viewModel.calculatedPath.collectAsState()
            val activeTool by viewModel.activeTool.collectAsState()
            val activeColor by viewModel.activeColor.collectAsState()
            val selectedRoom by viewModel.selectedRoom.collectAsState()
            val selectedEquipment by viewModel.selectedEquipment.collectAsState()
            val gpsActive by viewModel.gpsActive.collectAsState()
            val canUndoDrawing by viewModel.canUndoDrawing.collectAsState()
            val canRedoDrawing by viewModel.canRedoDrawing.collectAsState()

            var showProjectDialog by remember { mutableStateOf(false) }
            var showAdd3DTextDialog by remember { mutableStateOf(false) }
            var textPlacementOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
            var showAddEquipmentDialog by remember { mutableStateOf(false) }
            var searchFocused by remember { mutableStateOf(false) }
            var clearConfirmVisible by remember { mutableStateOf(false) }
            val shortcutFocus = remember { FocusRequester() }
            val blockDrawingShortcuts = searchFocused || clearConfirmVisible || showProjectDialog ||
                showAdd3DTextDialog || showAddEquipmentDialog || selectedRoom != null ||
                selectedEquipment != null || currentTab != AppTab.BLUEPRINT_3D

            SideEffect { drawingShortcutsBlocked = blockDrawingShortcuts }

            LaunchedEffect(currentTab, blockDrawingShortcuts) {
                if (!blockDrawingShortcuts) {
                    runCatching { shortcutFocus.requestFocus() }
                }
            }

            // Back handler: if immersed in room, exit immersion; if navigating, clear navigation; if on other tab, return to 3D map
            BackHandler(enabled = camera.isImmersiveMode || navTarget != null || currentTab != AppTab.BLUEPRINT_3D) {
                if (camera.isImmersiveMode) {
                    viewModel.exitImmersiveMode()
                } else if (navTarget != null) {
                    viewModel.clearNavigation()
                } else if (currentTab != AppTab.BLUEPRINT_3D) {
                    viewModel.setTab(AppTab.BLUEPRINT_3D)
                }
            }

            BlueprintTheme(darkTheme = isDarkTheme) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(shortcutFocus)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || blockDrawingShortcuts) {
                                return@onPreviewKeyEvent false
                            }
                            when (
                                drawingShortcut(
                                    ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed,
                                    shift = event.isShiftPressed,
                                    isZ = event.key == Key.Z,
                                    isY = event.key == Key.Y
                                )
                            ) {
                                DrawingShortcut.Undo -> {
                                    viewModel.undoDrawing()
                                    true
                                }
                                DrawingShortcut.Redo -> {
                                    viewModel.redoDrawing()
                                    true
                                }
                                null -> false
                            }
                        }
                ) {
                    val isFoldableExpanded = maxWidth >= 600.dp

                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                title = {
                                    // Project Switcher Chip
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showProjectDialog = true }
                                            .testTag("project_switcher_chip")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = currentProject?.name ?: "Facility Blueprint",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                if (currentProject?.gpsLat != null) {
                                                    Text(
                                                        text = String.format("GPS: %.3f°N, %.3f°W", currentProject?.gpsLat, kotlin.math.abs(currentProject?.gpsLng ?: 0.0)),
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    // GPS Toggle Badge
                                    IconButton(
                                        onClick = { viewModel.toggleGps() },
                                        modifier = Modifier.testTag("gps_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GpsFixed,
                                            contentDescription = "GPS Georeference",
                                            tint = if (gpsActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Dark / Light Theme Switcher
                                    IconButton(
                                        onClick = { viewModel.toggleTheme() },
                                        modifier = Modifier.testTag("theme_switcher_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Dark/Light Mode",
                                            tint = if (isDarkTheme) Color(0xFFFFD166) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!isFoldableExpanded) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("main_bottom_nav")
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == AppTab.BLUEPRINT_3D,
                                        onClick = { viewModel.setTab(AppTab.BLUEPRINT_3D) },
                                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D Map") },
                                        label = { Text("3D Map", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_3d")
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == AppTab.EQUIPMENT,
                                        onClick = { viewModel.setTab(AppTab.EQUIPMENT) },
                                        icon = { Icon(Icons.Default.DeviceHub, contentDescription = "Equipment") },
                                        label = { Text("Equipment", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_equipment")
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == AppTab.CAMERA,
                                        onClick = { viewModel.setTab(AppTab.CAMERA) },
                                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera") },
                                        label = { Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_camera")
                                    )

                                    NavigationBarItem(
                                        selected = currentTab == AppTab.NOTES,
                                        onClick = { viewModel.setTab(AppTab.NOTES) },
                                        icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                                        label = { Text("Notes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_notes")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isFoldableExpanded) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("main_nav_rail")
                                ) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NavigationRailItem(
                                        selected = currentTab == AppTab.BLUEPRINT_3D,
                                        onClick = { viewModel.setTab(AppTab.BLUEPRINT_3D) },
                                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = "3D Map") },
                                        label = { Text("3D Map", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_rail_3d")
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == AppTab.EQUIPMENT,
                                        onClick = { viewModel.setTab(AppTab.EQUIPMENT) },
                                        icon = { Icon(Icons.Default.DeviceHub, contentDescription = "Equipment") },
                                        label = { Text("Equipment", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_rail_equipment")
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == AppTab.CAMERA,
                                        onClick = { viewModel.setTab(AppTab.CAMERA) },
                                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Camera") },
                                        label = { Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_rail_camera")
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == AppTab.NOTES,
                                        onClick = { viewModel.setTab(AppTab.NOTES) },
                                        icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                                        label = { Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_rail_notes")
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            ) {
                        when (currentTab) {
                            AppTab.BLUEPRINT_3D -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Interactive 2D/3D Blueprint Canvas
                                    BlueprintCanvasView(
                                        camera = camera,
                                        rooms = rooms,
                                        equipmentList = equipmentList,
                                        annotations = annotations,
                                        currentStrokePoints = viewModel.currentStrokePoints,
                                        virtualUserLocation = userLocation,
                                        navigationTarget = navTarget,
                                        calculatedPath = calculatedPath,
                                        activeTool = activeTool,
                                        isDarkTheme = isDarkTheme,
                                        onTapFloor = { floorPt ->
                                            if (activeTool == ToolMode.TEXT_3D) {
                                                textPlacementOffset = floorPt
                                                showAdd3DTextDialog = true
                                            }
                                        },
                                        onTapRoom = { room ->
                                            viewModel.selectRoom(room)
                                        },
                                        onTapEquipment = { eq ->
                                            viewModel.selectEquipment(eq)
                                        },
                                        onStartStroke = { pt ->
                                            viewModel.startStroke(pt)
                                        },
                                        onAddStrokePoint = { pt ->
                                            viewModel.addStrokePoint(pt)
                                        },
                                        onFinishStroke = {
                                            viewModel.finishStroke()
                                        },
                                        onUpdateCameraPitchYaw = { dPitch, dYaw ->
                                            viewModel.updateCameraPitchYaw(dPitch, dYaw)
                                        },
                                        onUpdateCameraZoomPan = { z, px, py ->
                                            viewModel.updateCameraZoomPan(z, px, py)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Heads-Up Display (Radar, Waypoint Guidance & Room Immersion)
                                    HeadsUpDisplay(
                                        navTarget = navTarget,
                                        userLocation = userLocation,
                                        cameraYaw = camera.yawDeg,
                                        is3DMode = camera.is3DMode,
                                        isDarkTheme = isDarkTheme,
                                        isImmersiveMode = camera.isImmersiveMode,
                                        immersedRoomName = camera.immersedRoomName,
                                        onClearNavigation = { viewModel.clearNavigation() },
                                        onExitImmersive = { viewModel.exitImmersiveMode() },
                                        onWalk = { step -> viewModel.walkInsideRoom(step) },
                                        onTurn = { delta -> viewModel.turnHead(delta) },
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    )

                                    // Floating Toolbar at Bottom
                                    BlueprintToolbar(
                                        is3DMode = camera.is3DMode,
                                        activeTool = activeTool,
                                        activeColor = activeColor,
                                        isDarkTheme = isDarkTheme,
                                        onToggle2D3D = { viewModel.toggle2D3D() },
                                        onSetTool = { tool -> viewModel.setTool(tool) },
                                        onSetColor = { color -> viewModel.setColor(color) },
                                        onResetCamera = { viewModel.resetCamera() },
                                        canUndo = canUndoDrawing,
                                        canRedo = canRedoDrawing,
                                        canClearDrawing = annotations.isNotEmpty(),
                                        onUndo = { viewModel.undoDrawing() },
                                        onRedo = { viewModel.redoDrawing() },
                                        onClearAnnotations = { viewModel.clearAllAnnotations() },
                                        onSearchFocusChanged = { searchFocused = it },
                                        onClearConfirmVisible = { clearConfirmVisible = it },
                                        onSearchDestination = { query ->
                                            viewModel.searchAndNavigate(query)
                                        },
                                        onRelocateUser = {
                                            // Move user closer to room center or entrance
                                            val newPt = if (userLocation.y > 0.6f) Offset(0.5f, 0.51f) else Offset(0.5f, 0.94f)
                                            viewModel.setVirtualUserLocation(newPt)
                                        },
                                        onOpenArScanner = {
                                            viewModel.setTab(AppTab.CAMERA)
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }

                            AppTab.EQUIPMENT -> {
                                EquipmentCatalogScreen(
                                    equipmentList = equipmentList,
                                    onNavigateToEquipment = { eq ->
                                        viewModel.navigateToEquipment(eq)
                                    },
                                    onEditEquipment = { eq ->
                                        viewModel.selectEquipment(eq)
                                    },
                                    onAddNewEquipment = {
                                        showAddEquipmentDialog = true
                                    }
                                )
                            }

                            AppTab.CAMERA -> {
                                CameraInspectionScreen(
                                    photos = photos,
                                    rooms = rooms,
                                    equipmentList = equipmentList,
                                    annotations = annotations,
                                    onSaveCapturedPhoto = { bitmap, locName, eqTag, note ->
                                        viewModel.saveCapturedPhoto(bitmap, locName, eqTag, note)
                                    },
                                    onAddArPushpin = { text, posX, posY, color ->
                                        viewModel.addArPushpin(text, posX, posY, color)
                                    },
                                    onDeleteAnnotation = { ann ->
                                        viewModel.deleteAnnotation(ann)
                                    },
                                    onDeletePhoto = { photo ->
                                        viewModel.deletePhoto(photo)
                                    }
                                )
                            }

                            AppTab.NOTES -> {
                                NotesHubScreen(
                                    notes = notes,
                                    rooms = rooms,
                                    equipmentList = equipmentList,
                                    onAddNote = { title, content, category, roomName, eqTag ->
                                        viewModel.addNote(title, content, category, roomName, eqTag)
                                    },
                                    onDeleteNote = { note ->
                                        viewModel.deleteNote(note)
                                    }
                                )
                            }
                        }

                        // Edit Room Label & In-Room Notes Dialog
                        selectedRoom?.let { room ->
                            EditRoomDialog(
                                room = room,
                                onDismiss = { viewModel.selectRoom(null) },
                                onSave = { newName, newCode, newNotes ->
                                    viewModel.updateRoomLabel(room, newName, newCode, newNotes)
                                },
                                onNavigateToRoom = {
                                    viewModel.navigateToRoom(room)
                                },
                                onAddRoomNote = { roomName ->
                                    viewModel.addNote(
                                        title = "Note for $roomName",
                                        content = room.notes.ifBlank { "Routine inspection note" },
                                        category = "Inspection",
                                        roomName = roomName,
                                        tag = null
                                    )
                                },
                                onImmerseInRoom = {
                                    viewModel.enterImmersiveRoom(room)
                                }
                            )
                        }

                        // Edit Equipment Details Dialog
                        selectedEquipment?.let { eq ->
                            EditEquipmentDialog(
                                equipment = eq,
                                onDismiss = { viewModel.selectEquipment(null) },
                                onSave = { newTag, newName, newStatus, newSpecs, newNotes ->
                                    viewModel.updateEquipmentDetails(eq, newTag, newName, newStatus, newSpecs, newNotes)
                                },
                                onNavigateToEquipment = {
                                    viewModel.navigateToEquipment(eq)
                                }
                            )
                        }

                        // 3D Text Input Dialog
                        if (showAdd3DTextDialog) {
                            Add3DTextDialog(
                                onDismiss = { showAdd3DTextDialog = false },
                                onConfirm = { text ->
                                    viewModel.add3DTextAnnotation(text, textPlacementOffset)
                                }
                            )
                        }

                        // Register New Equipment Dialog
                        if (showAddEquipmentDialog) {
                            AddEquipmentDialog(
                                rooms = rooms,
                                onDismiss = { showAddEquipmentDialog = false },
                                onConfirm = { name, tag, category, status, specs, roomId, roomName, loc ->
                                    viewModel.addEquipment(name, tag, category, status, specs, roomId, roomName, loc)
                                }
                            )
                        }

                        // Project Selector & PDF Blueprint Importer Dialog
                        if (showProjectDialog) {
                            ProjectSelectorDialog(
                                projects = allProjects,
                                selectedProjectId = selectedProjectId,
                                onSelectProject = { id -> viewModel.selectProject(id) },
                                onImportPdf = { uri, fileName ->
                                    viewModel.importPdfBlueprint(uri, fileName)
                                },
                                onDismiss = { showProjectDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
}
