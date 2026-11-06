package com.example.ui.blueprint

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity
import com.example.engine3d.Camera3D
import com.example.engine3d.Point3D
import com.example.ui.NavTarget
import com.example.ui.ToolMode
import org.json.JSONArray
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BlueprintCanvasView(
    camera: Camera3D,
    rooms: List<RoomEntity>,
    equipmentList: List<EquipmentEntity>,
    annotations: List<AnnotationEntity>,
    currentStrokePoints: List<Offset>,
    virtualUserLocation: Offset,
    navigationTarget: NavTarget?,
    calculatedPath: List<Point3D>,
    activeTool: ToolMode,
    isDarkTheme: Boolean,
    onTapFloor: (Offset) -> Unit,
    onTapRoom: (RoomEntity) -> Unit,
    onTapEquipment: (EquipmentEntity) -> Unit,
    onStartStroke: (Offset) -> Unit,
    onAddStrokePoint: (Offset) -> Unit,
    onFinishStroke: () -> Unit,
    onUpdateCameraPitchYaw: (Float, Float) -> Unit,
    onUpdateCameraZoomPan: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for HUD beacon and active path
    val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_val"
    )

    val gridColor = if (isDarkTheme) Color(0x1F00E5FF) else Color(0x140284C7)
    val floorBg = if (isDarkTheme) Color(0xFF0B132B) else Color(0xFFF8FAFC)
    val textPaint = remember(isDarkTheme) {
        Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
        }
    }
    val tagPaint = remember(isDarkTheme) {
        Paint().apply {
            color = if (isDarkTheme) 0xFF00E5FF.toInt() else 0xFF0369A1.toInt()
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("blueprint_3d_canvas")
                // Transform gestures for 3D Camera navigation
                .pointerInput(activeTool) {
                    if (activeTool == ToolMode.NAVIGATE) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onUpdateCameraZoomPan(zoom, pan.x, pan.y)
                            if (camera.is3DMode && abs(rotation) > 0.4f) {
                                onUpdateCameraPitchYaw(0f, -rotation * 0.8f)
                            }
                        }
                    }
                }
                // Drag gestures for Freehand Drawing tools (Marker / Highlighter) or Pitch/Yaw
                .pointerInput(activeTool) {
                    if (activeTool == ToolMode.MARKER || activeTool == ToolMode.HIGHLIGHTER) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val floorPt = camera.unprojectToFloor(offset, size.width.toFloat(), size.height.toFloat())
                                onStartStroke(floorPt)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val floorPt = camera.unprojectToFloor(change.position, size.width.toFloat(), size.height.toFloat())
                                onAddStrokePoint(floorPt)
                            },
                            onDragEnd = { onFinishStroke() },
                            onDragCancel = { onFinishStroke() }
                        )
                    } else if (activeTool == ToolMode.NAVIGATE && camera.is3DMode) {
                        // 1-finger vertical drag near edges or secondary drag can adjust pitch
                        detectDragGestures { change, dragAmount ->
                            // If shift-like tilt: drag vertically with significant Y
                            if (abs(dragAmount.y) > abs(dragAmount.x) * 1.5f && abs(dragAmount.y) > 4f) {
                                change.consume()
                                onUpdateCameraPitchYaw(-dragAmount.y * 0.25f, dragAmount.x * 0.25f)
                            }
                        }
                    }
                }
                // Tap gesture for Room/Equipment selection or 3D text placement
                .pointerInput(activeTool, rooms, equipmentList) {
                    detectTapGestures { tapOffset ->
                        val floorPt = camera.unprojectToFloor(tapOffset, size.width.toFloat(), size.height.toFloat())

                        if (activeTool == ToolMode.TEXT_3D) {
                            onTapFloor(floorPt)
                            return@detectTapGestures
                        }

                        // Check if tapped on an equipment piece first
                        var tappedEq: EquipmentEntity? = null
                        for (eq in equipmentList) {
                            val eqOffset = camera.project(
                                Point3D(eq.posX, eq.posY, eq.posZ + 5f),
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                            val dist = (tapOffset - eqOffset).getDistance()
                            if (dist < 40f) {
                                tappedEq = eq
                                break
                            }
                        }

                        if (tappedEq != null) {
                            onTapEquipment(tappedEq)
                            return@detectTapGestures
                        }

                        // Check if tapped inside a room
                        val tappedRoom = rooms.find { r ->
                            floorPt.x in r.minX..r.maxX && floorPt.y in r.minY..r.maxY
                        }

                        if (tappedRoom != null) {
                            onTapRoom(tappedRoom)
                        } else {
                            onTapFloor(floorPt)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Blueprint Background Grid
            drawRect(color = floorBg)

            // Draw architectural floor plane boundary (0..1)
            val floorCorners = listOf(
                Point3D(0.04f, 0.04f, 0f).toOffset(camera, canvasW, canvasH),
                Point3D(0.96f, 0.04f, 0f).toOffset(camera, canvasW, canvasH),
                Point3D(0.96f, 0.96f, 0f).toOffset(camera, canvasW, canvasH),
                Point3D(0.04f, 0.96f, 0f).toOffset(camera, canvasW, canvasH)
            )

            val floorPath = Path().apply {
                moveTo(floorCorners[0].x, floorCorners[0].y)
                lineTo(floorCorners[1].x, floorCorners[1].y)
                lineTo(floorCorners[2].x, floorCorners[2].y)
                lineTo(floorCorners[3].x, floorCorners[3].y)
                close()
            }
            drawPath(
                path = floorPath,
                color = if (isDarkTheme) Color(0xFF141F36) else Color(0xFFFFFFFF)
            )
            drawPath(
                path = floorPath,
                color = if (isDarkTheme) Color(0xFF00E5FF).copy(alpha = 0.4f) else Color(0xFF0284C7).copy(alpha = 0.5f),
                style = Stroke(width = 2.5f)
            )

            // Draw Blueprint Grid Lines across floor
            val gridDivisions = 10
            for (i in 1 until gridDivisions) {
                val frac = i.toFloat() / gridDivisions
                val pStart = Point3D(0.04f + frac * 0.92f, 0.04f, 0f).toOffset(camera, canvasW, canvasH)
                val pEnd = Point3D(0.04f + frac * 0.92f, 0.96f, 0f).toOffset(camera, canvasW, canvasH)
                drawLine(color = gridColor, start = pStart, end = pEnd, strokeWidth = 1f)

                val hStart = Point3D(0.04f, 0.04f + frac * 0.92f, 0f).toOffset(camera, canvasW, canvasH)
                val hEnd = Point3D(0.96f, 0.04f + frac * 0.92f, 0f).toOffset(camera, canvasW, canvasH)
                drawLine(color = gridColor, start = hStart, end = hEnd, strokeWidth = 1f)
            }

            // 2. Draw Rooms with 3D Wall Extrusions
            rooms.forEach { room ->
                drawRoom3D(
                    room = room,
                    camera = camera,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    isDarkTheme = isDarkTheme,
                    textPaint = textPaint,
                    tagPaint = tagPaint
                )
            }

            // 3. Draw Saved Freehand Annotations (Marker & Highlighter) & 3D Pins
            annotations.forEach { ann ->
                if (ann.type == "MARKER" || ann.type == "HIGHLIGHTER") {
                    drawAnnotationStroke(ann, camera, canvasW, canvasH)
                } else if (ann.type == "TEXT_3D") {
                    draw3DTextAnnotation(ann, camera, canvasW, canvasH, isDarkTheme)
                } else if (ann.type == "AR_PUSHPIN") {
                    drawArPushpinOnMap(ann, camera, canvasW, canvasH, isDarkTheme, pulseAnim)
                }
            }

            // Draw active in-progress drawing stroke
            if (currentStrokePoints.size > 1) {
                val inProgressPath = Path()
                val first = Point3D(currentStrokePoints[0].x, currentStrokePoints[0].y, 1f).toOffset(camera, canvasW, canvasH)
                inProgressPath.moveTo(first.x, first.y)
                for (i in 1 until currentStrokePoints.size) {
                    val pt = Point3D(currentStrokePoints[i].x, currentStrokePoints[i].y, 1f).toOffset(camera, canvasW, canvasH)
                    inProgressPath.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = inProgressPath,
                    color = if (activeTool == ToolMode.HIGHLIGHTER) Color(0x66FFD166) else Color(0xFF00E5FF),
                    style = Stroke(width = if (activeTool == ToolMode.HIGHLIGHTER) 20f else 6f)
                )
            }

            // 4. Draw Highlighted 3D Navigation Path
            if (calculatedPath.isNotEmpty()) {
                drawHighlighted3DPath(
                    pathPoints = calculatedPath,
                    camera = camera,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    pulse = pulseAnim
                )
            }

            // 5. Draw 3D Waypoint Beacon at Destination
            if (navigationTarget != null) {
                drawWaypointBeacon(
                    target = navigationTarget,
                    camera = camera,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    pulse = pulseAnim
                )
            }

            // 6. Draw Equipment & Device Markers in 3D
            equipmentList.forEach { eq ->
                drawEquipmentMarker(
                    equipment = eq,
                    camera = camera,
                    canvasW = canvasW,
                    canvasH = canvasH,
                    isDarkTheme = isDarkTheme,
                    isTargeted = navigationTarget?.id == eq.id
                )
            }

            // 7. Draw Virtual User Location Avatar
            drawUserAvatar(
                location = virtualUserLocation,
                camera = camera,
                canvasW = canvasW,
                canvasH = canvasH,
                pulse = pulseAnim
            )
        }
    }
}

/**
 * Draws an extruded 3D room with floor fill, illuminated walls, and floating room labels.
 */
private fun DrawScope.drawRoom3D(
    room: RoomEntity,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    isDarkTheme: Boolean,
    textPaint: Paint,
    tagPaint: Paint
) {
    val h = if (camera.is3DMode) room.wallHeight else 0f

    // 4 Base corners (z = 0)
    val bNW = Point3D(room.minX, room.minY, 0f).toOffset(camera, canvasW, canvasH)
    val bNE = Point3D(room.maxX, room.minY, 0f).toOffset(camera, canvasW, canvasH)
    val bSE = Point3D(room.maxX, room.maxY, 0f).toOffset(camera, canvasW, canvasH)
    val bSW = Point3D(room.minX, room.maxY, 0f).toOffset(camera, canvasW, canvasH)

    // Floor Quad
    val roomFloor = Path().apply {
        moveTo(bNW.x, bNW.y)
        lineTo(bNE.x, bNE.y)
        lineTo(bSE.x, bSE.y)
        lineTo(bSW.x, bSW.y)
        close()
    }

    val baseColor = try {
        Color(android.graphics.Color.parseColor(room.colorHex))
    } catch (_: Exception) {
        Color(0xFF00E5FF)
    }

    drawPath(
        path = roomFloor,
        color = baseColor.copy(alpha = if (isDarkTheme) 0.14f else 0.10f)
    )
    drawPath(
        path = roomFloor,
        color = baseColor.copy(alpha = 0.4f),
        style = Stroke(width = 1.5f)
    )

    // In 3D Mode, extrude walls
    if (camera.is3DMode && h > 0f) {
        // 4 Top corners (z = wallHeight)
        val tNW = Point3D(room.minX, room.minY, h).toOffset(camera, canvasW, canvasH)
        val tNE = Point3D(room.maxX, room.minY, h).toOffset(camera, canvasW, canvasH)
        val tSE = Point3D(room.maxX, room.maxY, h).toOffset(camera, canvasW, canvasH)
        val tSW = Point3D(room.minX, room.maxY, h).toOffset(camera, canvasW, canvasH)

        // Draw side walls with lighting
        // Wall 1: South (bSW -> bSE -> tSE -> tSW)
        val southWall = Path().apply {
            moveTo(bSW.x, bSW.y)
            lineTo(bSE.x, bSE.y)
            lineTo(tSE.x, tSE.y)
            lineTo(tSW.x, tSW.y)
            close()
        }
        drawPath(path = southWall, color = baseColor.copy(alpha = if (isDarkTheme) 0.22f else 0.18f))
        drawPath(path = southWall, color = baseColor.copy(alpha = 0.5f), style = Stroke(width = 1f))

        // Wall 2: East (bNE -> bSE -> tSE -> tNE)
        val eastWall = Path().apply {
            moveTo(bNE.x, bNE.y)
            lineTo(bSE.x, bSE.y)
            lineTo(tSE.x, tSE.y)
            lineTo(tNE.x, tNE.y)
            close()
        }
        drawPath(path = eastWall, color = baseColor.copy(alpha = if (isDarkTheme) 0.30f else 0.24f))
        drawPath(path = eastWall, color = baseColor.copy(alpha = 0.6f), style = Stroke(width = 1f))

        // Top Wall Rim
        val topRim = Path().apply {
            moveTo(tNW.x, tNW.y)
            lineTo(tNE.x, tNE.y)
            lineTo(tSE.x, tSE.y)
            lineTo(tSW.x, tSW.y)
            close()
        }
        drawPath(path = topRim, color = baseColor.copy(alpha = 0.85f), style = Stroke(width = 2f))
    }

    // Draw Room Name & Code Tag Label in center
    val centerX = (room.minX + room.maxX) / 2f
    val centerY = (room.minY + room.maxY) / 2f
    val centerScreen = Point3D(centerX, centerY, if (camera.is3DMode) h + 6f else 0f)
        .toOffset(camera, canvasW, canvasH)

    drawContext.canvas.nativeCanvas.drawText(
        room.name,
        centerScreen.x,
        centerScreen.y - 12f,
        textPaint
    )
    drawContext.canvas.nativeCanvas.drawText(
        "[${room.code}]",
        centerScreen.x,
        centerScreen.y + 16f,
        tagPaint
    )
}

/**
 * Draws highlighted illuminated 3D path on the floor.
 */
private fun DrawScope.drawHighlighted3DPath(
    pathPoints: List<Point3D>,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    pulse: Float
) {
    if (pathPoints.size < 2) return

    val screenPoints = pathPoints.map { it.toOffset(camera, canvasW, canvasH) }
    val path = Path()
    path.moveTo(screenPoints[0].x, screenPoints[0].y)
    for (i in 1 until screenPoints.size) {
        path.lineTo(screenPoints[i].x, screenPoints[i].y)
    }

    // Outer glow
    drawPath(
        path = path,
        color = Color(0xFF00E5FF).copy(alpha = 0.28f + pulse * 0.22f),
        style = Stroke(width = 16f)
    )

    // Sharp glowing core
    drawPath(
        path = path,
        color = Color(0xFF00F5D4),
        style = Stroke(width = 5.5f)
    )

    // Intermediate pulsating beacon dots along the path
    for (pt in screenPoints) {
        drawCircle(
            color = Color(0xFF00F5D4),
            radius = 6f,
            center = pt
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = pt
        )
    }
}

/**
 * Draws a 3D Waypoint Beacon with vertical light pillar and concentric pulsing rings.
 */
private fun DrawScope.drawWaypointBeacon(
    target: NavTarget,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    pulse: Float
) {
    val floorPt = Point3D(target.targetOffset.x, target.targetOffset.y, 0f)
        .toOffset(camera, canvasW, canvasH)
    val pillarTop = Point3D(target.targetOffset.x, target.targetOffset.y, 60f)
        .toOffset(camera, canvasW, canvasH)

    // Vertical holographic light pillar
    drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.35f),
        start = floorPt,
        end = pillarTop,
        strokeWidth = 6f
    )
    drawLine(
        color = Color.White,
        start = floorPt,
        end = pillarTop,
        strokeWidth = 2f
    )

    // Concentric pulsing rings on floor
    val baseRadius = 24f
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 1f - pulse),
        radius = baseRadius + pulse * 32f,
        center = floorPt,
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
        radius = baseRadius * 0.6f,
        center = floorPt,
        style = Stroke(width = 2f)
    )

    // Floating Target Beacon Diamond Icon at pillar top
    drawCircle(
        color = Color(0xFF00E5FF),
        radius = 10f,
        center = pillarTop
    )
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = pillarTop
    )
}

/**
 * Draws 3D equipment marker with status diamond and floating tag.
 */
private fun DrawScope.drawEquipmentMarker(
    equipment: EquipmentEntity,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    isDarkTheme: Boolean,
    isTargeted: Boolean
) {
    val elevZ = if (camera.is3DMode) equipment.posZ + 12f else 0f
    val screenPos = Point3D(equipment.posX, equipment.posY, elevZ).toOffset(camera, canvasW, canvasH)
    val groundPos = Point3D(equipment.posX, equipment.posY, 0f).toOffset(camera, canvasW, canvasH)

    val statusColor = when (equipment.status) {
        "Operational" -> Color(0xFF10B981) // Emerald Green
        "Maintenance" -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Coral Red
    }

    if (camera.is3DMode && elevZ > 0f) {
        // Drop line to floor
        drawLine(
            color = statusColor.copy(alpha = 0.4f),
            start = groundPos,
            end = screenPos,
            strokeWidth = 1.5f
        )
    }

    // Equipment Diamond Marker
    val size = if (isTargeted) 16f else 12f
    val diamondPath = Path().apply {
        moveTo(screenPos.x, screenPos.y - size)
        lineTo(screenPos.x + size, screenPos.y)
        lineTo(screenPos.x, screenPos.y + size)
        lineTo(screenPos.x - size, screenPos.y)
        close()
    }

    drawPath(path = diamondPath, color = statusColor)
    drawPath(
        path = diamondPath,
        color = if (isTargeted) Color.White else Color.Black.copy(alpha = 0.6f),
        style = Stroke(width = if (isTargeted) 2.5f else 1.2f)
    )

    // Tag text above marker
    val paint = Paint().apply {
        color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 20f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }
    drawContext.canvas.nativeCanvas.drawText(
        equipment.tag,
        screenPos.x,
        screenPos.y - size - 6f,
        paint
    )
}

/**
 * Draws user avatar / pinpoint on floor with heading indicator.
 */
private fun DrawScope.drawUserAvatar(
    location: Offset,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    pulse: Float
) {
    val screenPos = Point3D(location.x, location.y, 4f).toOffset(camera, canvasW, canvasH)

    // Pulsing outer radar circle
    drawCircle(
        color = Color(0xFF3B82F6).copy(alpha = (1f - pulse) * 0.7f),
        radius = 16f + pulse * 18f,
        center = screenPos
    )

    // User pin core
    drawCircle(
        color = Color(0xFF2563EB),
        radius = 9f,
        center = screenPos
    )
    drawCircle(
        color = Color.White,
        radius = 4.5f,
        center = screenPos
    )
}

/**
 * Draws vector strokes for Marker and Highlighter tools.
 */
private fun DrawScope.drawAnnotationStroke(
    annotation: AnnotationEntity,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float
) {
    try {
        val jsonArray = JSONArray(annotation.pointsJson)
        if (jsonArray.length() < 2) return

        val path = Path()
        val firstObj = jsonArray.getJSONObject(0)
        val first = Point3D(firstObj.getDouble("x").toFloat(), firstObj.getDouble("y").toFloat(), 2f)
            .toOffset(camera, canvasW, canvasH)
        path.moveTo(first.x, first.y)

        for (i in 1 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val pt = Point3D(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat(), 2f)
                .toOffset(camera, canvasW, canvasH)
            path.lineTo(pt.x, pt.y)
        }

        val strokeColor = Color(annotation.color.toInt())
        val alpha = if (annotation.type == "HIGHLIGHTER") 0.45f else 1.0f

        drawPath(
            path = path,
            color = strokeColor.copy(alpha = alpha),
            style = Stroke(width = annotation.strokeWidth)
        )
    } catch (_: Exception) {
    }
}

/**
 * Draws 3D floating text annotations anchored to coordinates.
 */
private fun DrawScope.draw3DTextAnnotation(
    annotation: AnnotationEntity,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    isDarkTheme: Boolean
) {
    val screenPos = Point3D(annotation.posX, annotation.posY, annotation.posZ)
        .toOffset(camera, canvasW, canvasH)

    val paint = Paint().apply {
        color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 22f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    val textWidth = paint.measureText(annotation.text)
    val padH = 14f
    val padV = 10f

    // Background sticky pill
    val bgRect = Rect(
        left = screenPos.x - textWidth / 2f - padH,
        top = screenPos.y - 24f - padV,
        right = screenPos.x + textWidth / 2f + padH,
        bottom = screenPos.y + padV
    )

    drawRoundRect(
        color = if (isDarkTheme) Color(0xDD1E2D4A) else Color(0xEEFEF08A),
        topLeft = Offset(bgRect.left, bgRect.top),
        size = androidx.compose.ui.geometry.Size(bgRect.width, bgRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(annotation.color.toInt()),
        topLeft = Offset(bgRect.left, bgRect.top),
        size = androidx.compose.ui.geometry.Size(bgRect.width, bgRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
        style = Stroke(width = 2f)
    )

    drawContext.canvas.nativeCanvas.drawText(
        annotation.text,
        screenPos.x,
        screenPos.y - 6f,
        paint
    )
}

/**
 * Draws 3D digital pushpin markers placed during live camera scanning.
 */
private fun DrawScope.drawArPushpinOnMap(
    annotation: AnnotationEntity,
    camera: Camera3D,
    canvasW: Float,
    canvasH: Float,
    isDarkTheme: Boolean,
    pulse: Float
) {
    val screenPos = Point3D(annotation.posX, annotation.posY, 14f).toOffset(camera, canvasW, canvasH)
    val groundPos = Point3D(annotation.posX, annotation.posY, 0f).toOffset(camera, canvasW, canvasH)
    val pinColor = Color(annotation.color.toInt())

    if (camera.is3DMode) {
        drawLine(
            color = pinColor.copy(alpha = 0.5f),
            start = groundPos,
            end = screenPos,
            strokeWidth = 2f
        )
    }

    // Concentric base ring on floor
    drawCircle(
        color = pinColor.copy(alpha = (1f - pulse) * 0.7f),
        radius = 12f + pulse * 14f,
        center = groundPos,
        style = Stroke(width = 1.5f)
    )

    // Spherical Pin Head
    drawCircle(color = pinColor, radius = 9f, center = screenPos)
    drawCircle(color = Color.White, radius = 4f, center = screenPos)

    // Floating text label
    val paint = Paint().apply {
        color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 18f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    drawContext.canvas.nativeCanvas.drawText(
        annotation.text,
        screenPos.x,
        screenPos.y - 14f,
        paint
    )
}

