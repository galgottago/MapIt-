package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.BlueprintDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.PhotoEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectNoteEntity
import com.example.data.model.RoomEntity
import com.example.data.repository.BlueprintRepository
import com.example.engine3d.Camera3D
import com.example.engine3d.Pathfinding3D
import com.example.engine3d.Point3D
import com.example.ui.blueprint.DrawingEffect
import com.example.ui.blueprint.DrawingHistory
import com.example.ui.blueprint.DrawingOp
import com.example.ui.blueprint.effects
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AppTab(val title: String) {
    BLUEPRINT_3D("3D Map"),
    EQUIPMENT("Equipment"),
    CAMERA("Camera"),
    NOTES("Notes")
}

enum class ToolMode {
    NAVIGATE,
    MARKER,
    HIGHLIGHTER,
    TEXT_3D,
    ERASER
}

data class NavTarget(
    val id: String,
    val name: String,
    val tag: String? = null,
    val targetOffset: Offset,
    val roomName: String,
    val status: String = "Operational"
)

class BlueprintViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BlueprintRepository

    init {
        val database = BlueprintDatabase.getDatabase(application)
        repository = BlueprintRepository(application, database.blueprintDao())
        viewModelScope.launch {
            repository.initializeSampleDataIfNeeded()
        }
    }

    // Theme Switcher: Dark vs Light Mode
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.BLUEPRINT_3D)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Projects
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow("proj-hvac-plant")
    val selectedProjectId: StateFlow<String> = _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProject: StateFlow<ProjectEntity?> = _selectedProjectId
        .flatMapLatest { id ->
            allProjects.combine(_selectedProjectId) { list, curId ->
                list.find { it.id == curId } ?: list.firstOrNull()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val rooms: StateFlow<List<RoomEntity>> = _selectedProjectId
        .flatMapLatest { repository.getRooms(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val equipmentList: StateFlow<List<EquipmentEntity>> = _selectedProjectId
        .flatMapLatest { repository.getEquipment(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val annotations: StateFlow<List<AnnotationEntity>> = _selectedProjectId
        .flatMapLatest { repository.getAnnotations(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val photos: StateFlow<List<PhotoEntity>> = _selectedProjectId
        .flatMapLatest { repository.getPhotos(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<ProjectNoteEntity>> = _selectedProjectId
        .flatMapLatest { repository.getNotes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3D Camera State
    val camera = MutableStateFlow(
        Camera3D(
            yawDeg = 45f,
            pitchDeg = 55f,
            zoom = 1.0f,
            panOffsetX = 0f,
            panOffsetY = 0f,
            is3DMode = true
        )
    )

    fun toggle2D3D() {
        val current = camera.value
        camera.value = current.copy(is3DMode = !current.is3DMode)
    }

    fun updateCameraPitchYaw(deltaPitch: Float, deltaYaw: Float) {
        val current = camera.value
        val newPitch = (current.pitchDeg + deltaPitch).coerceIn(20f, 90f)
        val newYaw = (current.yawDeg + deltaYaw) % 360f
        camera.value = current.copy(pitchDeg = newPitch, yawDeg = if (newYaw < 0) newYaw + 360f else newYaw)
    }

    fun updateCameraZoomPan(zoomFactor: Float, panX: Float, panY: Float) {
        val current = camera.value
        val newZoom = (current.zoom * zoomFactor).coerceIn(0.4f, 4.0f)
        camera.value = current.copy(
            zoom = newZoom,
            panOffsetX = current.panOffsetX + panX,
            panOffsetY = current.panOffsetY + panY
        )
    }

    fun resetCamera() {
        camera.value = Camera3D(
            yawDeg = 45f,
            pitchDeg = 55f,
            zoom = 1.0f,
            panOffsetX = 0f,
            panOffsetY = 0f,
            focalPointX = 0.5f,
            focalPointY = 0.5f,
            is3DMode = camera.value.is3DMode,
            isImmersiveMode = false,
            immersedRoomName = null
        )
    }

    fun enterImmersiveRoom(room: RoomEntity) {
        val centerX = (room.minX + room.maxX) / 2f
        val centerY = (room.minY + room.maxY) / 2f
        _virtualUserLocation.value = Offset(centerX, centerY)

        camera.value = Camera3D(
            yawDeg = 45f,
            pitchDeg = 26f, // Eye-level perspective inside room
            zoom = 2.8f,    // Focused interior view
            panOffsetX = 0f,
            panOffsetY = 0f,
            focalPointX = centerX,
            focalPointY = centerY,
            is3DMode = true,
            isImmersiveMode = true,
            immersedRoomName = room.name
        )
        _currentTab.value = AppTab.BLUEPRINT_3D
    }

    fun exitImmersiveMode() {
        val cur = camera.value
        camera.value = cur.copy(
            isImmersiveMode = false,
            immersedRoomName = null,
            focalPointX = 0.5f,
            focalPointY = 0.5f,
            zoom = 1.0f,
            pitchDeg = 55f
        )
    }

    fun turnHead(deltaDeg: Float) {
        val cur = camera.value
        val newYaw = (cur.yawDeg + deltaDeg) % 360f
        camera.value = cur.copy(yawDeg = if (newYaw < 0) newYaw + 360f else newYaw)
    }

    fun walkInsideRoom(stepMeters: Float) {
        val cur = camera.value
        val rad = Math.toRadians((cur.yawDeg).toDouble())
        // Move focal point and user in direction of gaze
        val stepNorm = stepMeters * 0.008f
        val dx = (sin(rad) * stepNorm).toFloat()
        val dy = (-cos(rad) * stepNorm).toFloat()

        val newFx = (cur.focalPointX + dx).coerceIn(0.06f, 0.94f)
        val newFy = (cur.focalPointY + dy).coerceIn(0.06f, 0.94f)

        camera.value = cur.copy(focalPointX = newFx, focalPointY = newFy)
        _virtualUserLocation.value = Offset(newFx, newFy)
    }

    // Virtual User Location (for Navigation & Wayfinding)
    private val _virtualUserLocation = MutableStateFlow(Offset(0.50f, 0.94f))
    val virtualUserLocation: StateFlow<Offset> = _virtualUserLocation.asStateFlow()

    fun setVirtualUserLocation(newOffset: Offset) {
        _virtualUserLocation.value = Offset(
            newOffset.x.coerceIn(0.05f, 0.95f),
            newOffset.y.coerceIn(0.05f, 0.95f)
        )
        // Recalculate path if active target exists
        updatePath()
    }

    // Navigation & Wayfinding Target
    private val _navigationTarget = MutableStateFlow<NavTarget?>(null)
    val navigationTarget: StateFlow<NavTarget?> = _navigationTarget.asStateFlow()

    private val _calculatedPath = MutableStateFlow<List<Point3D>>(emptyList())
    val calculatedPath: StateFlow<List<Point3D>> = _calculatedPath.asStateFlow()

    fun navigateToRoom(room: RoomEntity) {
        val center = Offset((room.minX + room.maxX) / 2f, (room.minY + room.maxY) / 2f)
        _navigationTarget.value = NavTarget(
            id = room.id,
            name = room.name,
            tag = room.code,
            targetOffset = center,
            roomName = room.name
        )
        updatePath()
        _currentTab.value = AppTab.BLUEPRINT_3D
    }

    fun navigateToEquipment(equipment: EquipmentEntity) {
        _navigationTarget.value = NavTarget(
            id = equipment.id,
            name = equipment.name,
            tag = equipment.tag,
            targetOffset = Offset(equipment.posX, equipment.posY),
            roomName = equipment.roomName,
            status = equipment.status
        )
        updatePath()
        _currentTab.value = AppTab.BLUEPRINT_3D
    }

    fun searchAndNavigate(query: String): Boolean {
        if (query.isBlank()) return false
        val clean = query.trim().lowercase()

        // 1. Try finding matching equipment by tag or name
        val eq = equipmentList.value.find {
            it.tag.lowercase().contains(clean) || it.name.lowercase().contains(clean)
        }
        if (eq != null) {
            navigateToEquipment(eq)
            return true
        }

        // 2. Try finding room by name or code
        val r = rooms.value.find {
            it.name.lowercase().contains(clean) || it.code.lowercase().contains(clean)
        }
        if (r != null) {
            navigateToRoom(r)
            return true
        }
        return false
    }

    fun clearNavigation() {
        _navigationTarget.value = null
        _calculatedPath.value = emptyList()
    }

    private fun updatePath() {
        val target = _navigationTarget.value ?: return
        val path = Pathfinding3D.calculateCorridorPath(
            start = _virtualUserLocation.value,
            destination = target.targetOffset
        )
        _calculatedPath.value = path
    }

    // Editing Tools (Marker, Highlighter, 3D Text, Eraser)
    private val _activeTool = MutableStateFlow(ToolMode.NAVIGATE)
    val activeTool: StateFlow<ToolMode> = _activeTool.asStateFlow()

    fun setTool(tool: ToolMode) {
        _activeTool.value = tool
    }

    private val _activeColor = MutableStateFlow(0xFF00E5FF) // Holographic Cyan
    val activeColor: StateFlow<Long> = _activeColor.asStateFlow()

    fun setColor(color: Long) {
        _activeColor.value = color
    }

    // Active in-progress freehand stroke points
    val currentStrokePoints = mutableStateListOf<Offset>()

    fun startStroke(point: Offset) {
        currentStrokePoints.clear()
        currentStrokePoints.add(point)
    }

    fun addStrokePoint(point: Offset) {
        currentStrokePoints.add(point)
    }

    fun finishStroke() {
        if (currentStrokePoints.size > 1) {
            val toolType = when (_activeTool.value) {
                ToolMode.HIGHLIGHTER -> "HIGHLIGHTER"
                else -> "MARKER"
            }
            val strokeWidth = if (toolType == "HIGHLIGHTER") 24f else 6f

            // Convert points to JSON
            val jsonArray = JSONArray()
            for (pt in currentStrokePoints) {
                val obj = JSONObject().apply {
                    put("x", pt.x.toDouble())
                    put("y", pt.y.toDouble())
                }
                jsonArray.put(obj)
            }

            val projectId = _selectedProjectId.value
            val annotation = AnnotationEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                type = toolType,
                color = _activeColor.value,
                strokeWidth = strokeWidth,
                pointsJson = jsonArray.toString()
            )
            commitDrawing(projectId, DrawingOp.AddAnnotation(annotation))
        }
        currentStrokePoints.clear()
    }

    // 3D Text Annotation
    fun add3DTextAnnotation(text: String, location: Offset) {
        if (text.isBlank()) return
        val projectId = _selectedProjectId.value
        val annotation = AnnotationEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            type = "TEXT_3D",
            color = _activeColor.value,
            text = text,
            posX = location.x,
            posY = location.y,
            posZ = 15f // Elevation
        )
        commitDrawing(projectId, DrawingOp.AddAnnotation(annotation))
    }

    // AR Live View Pushpin / Marker Annotation
    fun addArPushpin(text: String, posX: Float, posY: Float, color: Long) {
        if (text.isBlank()) return
        val projectId = _selectedProjectId.value
        val annotation = AnnotationEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            type = "AR_PUSHPIN",
            color = color,
            text = text,
            posX = posX,
            posY = posY,
            posZ = 0f
        )
        commitDrawing(projectId, DrawingOp.AddAnnotation(annotation))
    }

    fun deleteAnnotation(annotation: AnnotationEntity) {
        commitDrawing(annotation.projectId, DrawingOp.DeleteAnnotation(annotation))
    }

    fun clearAllAnnotations() {
        val projectId = _selectedProjectId.value
        viewModelScope.launch {
            drawingMutex.withLock {
                val existing = repository.getAnnotationsNow(projectId)
                if (existing.isEmpty()) return@withLock
                val op = DrawingOp.ClearAnnotations(projectId, existing)
                historyFor(projectId).record(op)
                applyEffects(op.effects(inverse = false))
                publishHistoryFlags(projectId)
            }
        }
    }

    fun undoDrawing() {
        replayDrawing(inverse = true)
    }

    fun redoDrawing() {
        replayDrawing(inverse = false)
    }

    private val drawingMutex = Mutex()
    private val drawingHistories = mutableMapOf<String, DrawingHistory>()

    private val _canUndoDrawing = MutableStateFlow(false)
    val canUndoDrawing: StateFlow<Boolean> = _canUndoDrawing.asStateFlow()

    private val _canRedoDrawing = MutableStateFlow(false)
    val canRedoDrawing: StateFlow<Boolean> = _canRedoDrawing.asStateFlow()

    private fun historyFor(projectId: String): DrawingHistory =
        drawingHistories.getOrPut(projectId) { DrawingHistory() }

    private fun publishHistoryFlags(projectId: String) {
        if (_selectedProjectId.value != projectId) return
        val history = drawingHistories[projectId]
        _canUndoDrawing.value = history?.canUndo == true
        _canRedoDrawing.value = history?.canRedo == true
    }

    private fun commitDrawing(projectId: String, op: DrawingOp) {
        viewModelScope.launch {
            drawingMutex.withLock {
                historyFor(projectId).record(op)
                applyEffects(op.effects(inverse = false))
                publishHistoryFlags(projectId)
            }
        }
    }

    private fun replayDrawing(inverse: Boolean) {
        val projectId = _selectedProjectId.value
        viewModelScope.launch {
            drawingMutex.withLock {
                val history = drawingHistories[projectId] ?: return@withLock
                val op = if (inverse) history.popUndo() else history.popRedo()
                if (op == null) return@withLock
                applyEffects(op.effects(inverse))
                publishHistoryFlags(projectId)
            }
        }
    }

    private suspend fun applyEffects(effects: List<DrawingEffect>) {
        for (effect in effects) {
            when (effect) {
                is DrawingEffect.InsertAnnotation -> repository.addAnnotation(effect.annotation)
                is DrawingEffect.RemoveAnnotation -> repository.deleteAnnotation(effect.annotation)
                is DrawingEffect.ClearProjectAnnotations -> repository.clearAnnotations(effect.projectId)
                is DrawingEffect.UpsertRoom -> {
                    repository.updateRoom(effect.room)
                    if (_selectedRoom.value?.id == effect.room.id) {
                        _selectedRoom.value = effect.room
                    }
                }
                is DrawingEffect.UpsertEquipment -> {
                    repository.updateEquipment(effect.equipment)
                    if (_selectedEquipment.value?.id == effect.equipment.id) {
                        _selectedEquipment.value = effect.equipment
                    }
                }
            }
        }
    }

    // Selected Items for Inspection / Label Editing
    private val _selectedRoom = MutableStateFlow<RoomEntity?>(null)
    val selectedRoom: StateFlow<RoomEntity?> = _selectedRoom.asStateFlow()

    fun selectRoom(room: RoomEntity?) {
        _selectedRoom.value = room
    }

    private val _selectedEquipment = MutableStateFlow<EquipmentEntity?>(null)
    val selectedEquipment: StateFlow<EquipmentEntity?> = _selectedEquipment.asStateFlow()

    fun selectEquipment(equipment: EquipmentEntity?) {
        _selectedEquipment.value = equipment
    }

    fun updateRoomLabel(room: RoomEntity, newName: String, newCode: String, newNotes: String) {
        val updated = room.copy(name = newName, code = newCode, notes = newNotes)
        if (updated == room) return
        commitDrawing(room.projectId, DrawingOp.EditRoom(before = room, after = updated))
    }

    fun updateEquipmentDetails(
        equipment: EquipmentEntity,
        newTag: String,
        newName: String,
        newStatus: String,
        newSpecs: String,
        newNotes: String
    ) {
        val updated = equipment.copy(
            tag = newTag,
            name = newName,
            status = newStatus,
            specifications = newSpecs,
            notes = newNotes,
            updatedAt = System.currentTimeMillis()
        )
        if (updated.copy(updatedAt = equipment.updatedAt) == equipment) return
        commitDrawing(equipment.projectId, DrawingOp.EditEquipment(before = equipment, after = updated))
    }

    fun addEquipment(
        name: String,
        tag: String,
        category: String,
        status: String,
        specs: String,
        roomId: String?,
        roomName: String,
        location: Offset
    ) {
        viewModelScope.launch {
            val item = EquipmentEntity(
                id = UUID.randomUUID().toString(),
                projectId = _selectedProjectId.value,
                roomId = roomId,
                roomName = roomName,
                tag = tag,
                name = name,
                category = category,
                posX = location.x,
                posY = location.y,
                status = status,
                specifications = specs
            )
            repository.addEquipment(item)
        }
    }

    // Camera Photo Saving
    fun saveCapturedPhoto(
        bitmap: Bitmap,
        locationName: String,
        equipmentTag: String?,
        note: String
    ) {
        viewModelScope.launch {
            val proj = currentProject.value
            repository.saveInspectionPhoto(
                projectId = _selectedProjectId.value,
                projectName = proj?.name ?: "Facility",
                locationName = locationName,
                equipmentTag = equipmentTag,
                bitmap = bitmap,
                note = note
            )
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    // Notes
    fun addNote(title: String, content: String, category: String, roomName: String?, tag: String?) {
        viewModelScope.launch {
            val note = ProjectNoteEntity(
                id = UUID.randomUUID().toString(),
                projectId = _selectedProjectId.value,
                title = title,
                content = content,
                category = category,
                linkedRoomName = roomName,
                linkedEquipmentTag = tag
            )
            repository.addNote(note)
        }
    }

    fun deleteNote(note: ProjectNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Project Switching & Import
    fun selectProject(projectId: String) {
        _selectedProjectId.value = projectId
        clearNavigation()
        currentStrokePoints.clear()
        _selectedRoom.value = null
        _selectedEquipment.value = null
        publishHistoryFlags(projectId)
    }

    fun importPdfBlueprint(pdfUri: Uri, fileName: String) {
        viewModelScope.launch {
            val newProject = repository.importPdfBlueprint(pdfUri, fileName)
            selectProject(newProject.id)
        }
    }

    // GPS & Compass Georeferencing
    private val _gpsActive = MutableStateFlow(true)
    val gpsActive: StateFlow<Boolean> = _gpsActive.asStateFlow()

    fun toggleGps() {
        _gpsActive.value = !_gpsActive.value
    }
}
