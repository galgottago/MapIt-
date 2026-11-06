package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun toOffset(camera: Camera3D, canvasWidth: Float, canvasHeight: Float): Offset {
        return camera.project(this, canvasWidth, canvasHeight)
    }
}

data class Camera3D(
    var yawDeg: Float = 45f,          // Horizontal rotation angle in degrees (0..360)
    var pitchDeg: Float = 55f,        // Vertical tilt angle (90 = 2D top-down, 35..55 = 3D isometric, 20 = eye-level)
    var zoom: Float = 1.0f,           // Camera zoom factor
    var panOffsetX: Float = 0f,       // Canvas pan offset
    var panOffsetY: Float = 0f,
    var focalPointX: Float = 0.5f,    // Center focus coordinate (0..1)
    var focalPointY: Float = 0.5f,
    var is3DMode: Boolean = true,     // Instant toggle between 2D and 3D mode
    var isImmersiveMode: Boolean = false, // Eye-level room interior immersion
    var immersedRoomName: String? = null
) {
    /**
     * Projects a 3D coordinate (where x: 0..1, y: 0..1, z: height) onto 2D screen Canvas pixels.
     */
    fun project(point: Point3D, canvasWidth: Float, canvasHeight: Float): Offset {
        val centerX = canvasWidth / 2f + panOffsetX
        val centerY = canvasHeight / 2f + panOffsetY
        val baseSize = minOf(canvasWidth, canvasHeight) * 0.85f * zoom

        if (!is3DMode) {
            // Pure 2D Top-Down Projection
            val screenX = centerX + (point.x - focalPointX) * baseSize
            val screenY = centerY + (point.y - focalPointY) * baseSize
            return Offset(screenX, screenY)
        }

        // 3D Perspective Projection centered at (focalPointX, focalPointY)
        val relX = (point.x - focalPointX) * baseSize
        val relY = (point.y - focalPointY) * baseSize
        val relZ = point.z * (baseSize / 280f)

        val radYaw = Math.toRadians(yawDeg.toDouble())
        val radPitch = Math.toRadians(pitchDeg.toDouble())

        val cosYaw = cos(radYaw).toFloat()
        val sinYaw = sin(radYaw).toFloat()
        val cosPitch = cos(radPitch).toFloat()
        val sinPitch = sin(radPitch).toFloat()

        // Rotate around Z axis (Yaw)
        val rotX = relX * cosYaw - relY * sinYaw
        val rotY = relX * sinYaw + relY * cosYaw

        // Tilt by Pitch
        val screenX = centerX + rotX
        val screenY = centerY + (rotY * sinPitch) - (relZ * cosPitch)

        return Offset(screenX, screenY)
    }

    /**
     * Unprojects a 2D screen tap offset onto the floor plane (z = 0), returning normalized (0..1) coords.
     */
    fun unprojectToFloor(screenOffset: Offset, canvasWidth: Float, canvasHeight: Float): Offset {
        val centerX = canvasWidth / 2f + panOffsetX
        val centerY = canvasHeight / 2f + panOffsetY
        val baseSize = minOf(canvasWidth, canvasHeight) * 0.85f * zoom

        val clickRelX = screenOffset.x - centerX
        val clickRelY = screenOffset.y - centerY

        if (!is3DMode) {
            val normX = (clickRelX / baseSize) + focalPointX
            val normY = (clickRelY / baseSize) + focalPointY
            return Offset(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
        }

        val radYaw = Math.toRadians(yawDeg.toDouble())
        val radPitch = Math.toRadians(pitchDeg.toDouble().coerceAtLeast(15.0))

        val cosYaw = cos(radYaw).toFloat()
        val sinYaw = sin(radYaw).toFloat()
        val cosPitch = cos(radPitch).toFloat()
        val sinPitch = sin(radPitch).toFloat()

        val rotX = clickRelX
        val rotY = clickRelY / sinPitch

        // Inverse yaw rotation
        val origRelX = rotX * cosYaw + rotY * sinYaw
        val origRelY = -rotX * sinYaw + rotY * cosYaw

        val normX = (origRelX / baseSize) + focalPointX
        val normY = (origRelY / baseSize) + focalPointY

        return Offset(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
    }
}
