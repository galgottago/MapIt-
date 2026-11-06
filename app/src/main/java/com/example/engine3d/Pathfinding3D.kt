package com.example.engine3d

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.sqrt

object Pathfinding3D {

    /**
     * Calculates an illuminated 3D navigation path from the user's virtual location
     * to the target room or equipment. Routes cleanly through facility corridors.
     */
    fun calculateCorridorPath(
        start: Offset,
        destination: Offset
    ): List<Point3D> {
        val path = mutableListOf<Point3D>()

        // Elevate path slightly above the floor slab (z = 3f) for 3D visibility
        val pathZ = 3f

        // Waypoint 1: Starting point
        path.add(Point3D(start.x, start.y, pathZ))

        // Determine intermediate hallway corridor nexus
        val corridorCenterX = 0.50f
        val corridorCenterY = 0.51f

        // If start and dest are in different quadrants, route through corridors
        val startInNorth = start.y < corridorCenterY
        val destInNorth = destination.y < corridorCenterY
        val startInWest = start.x < corridorCenterX
        val destInWest = destination.x < corridorCenterX

        if (startInNorth != destInNorth || startInWest != destInWest) {
            // First step out to closest hallway axis
            if (startInNorth) {
                path.add(Point3D(start.x, corridorCenterY, pathZ))
            } else {
                path.add(Point3D(start.x, corridorCenterY, pathZ))
            }

            // Cross corridor junction if needed
            path.add(Point3D(corridorCenterX, corridorCenterY, pathZ))

            // Step along to destination column
            path.add(Point3D(destination.x, corridorCenterY, pathZ))
        } else {
            // Same wing, take a clean L-shaped corridor turn
            val midX = destination.x
            val midY = start.y
            path.add(Point3D(midX, midY, pathZ))
        }

        // Final destination waypoint
        path.add(Point3D(destination.x, destination.y, pathZ))

        return path
    }

    /**
     * Calculates real-world distance in meters (assuming ~50m x 50m typical plant scale).
     */
    fun calculateDistanceMeters(start: Offset, destination: Offset, facilityScaleMeters: Float = 60f): Float {
        val dx = (destination.x - start.x) * facilityScaleMeters
        val dy = (destination.y - start.y) * facilityScaleMeters
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * Calculates compass bearing angle from start to destination in degrees (0 = North).
     */
    fun calculateBearingDeg(start: Offset, destination: Offset): Float {
        // Blueprint Y increases downwards (South), X increases right (East)
        val dx = destination.x - start.x
        val dy = -(destination.y - start.y) // Invert so North is up
        var deg = Math.toDegrees(atan2(dx.toDouble(), dy.toDouble())).toFloat()
        if (deg < 0) deg += 360f
        return deg
    }
}
