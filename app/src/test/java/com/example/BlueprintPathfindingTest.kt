package com.example

import androidx.compose.ui.geometry.Offset
import com.example.engine3d.Pathfinding3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintPathfindingTest {

    @Test
    fun calculateCorridorPath_createsValidWaypoints() {
        val start = Offset(0.50f, 0.94f)
        val destination = Offset(0.22f, 0.24f)

        val path = Pathfinding3D.calculateCorridorPath(start, destination)

        assertTrue("Path should have at least 3 waypoints", path.size >= 3)
        assertEquals(start.x, path.first().x, 0.01f)
        assertEquals(start.y, path.first().y, 0.01f)
        assertEquals(destination.x, path.last().x, 0.01f)
        assertEquals(destination.y, path.last().y, 0.01f)
    }

    @Test
    fun calculateDistanceMeters_returnsRealisticMetric() {
        val start = Offset(0.10f, 0.10f)
        val destination = Offset(0.90f, 0.90f)

        val distance = Pathfinding3D.calculateDistanceMeters(start, destination, 60f)
        assertTrue("Distance across diagonal should be positive", distance > 50f)
        assertTrue("Distance should be finite and realistic", distance < 120f)
    }

    @Test
    fun calculateBearingDeg_returnsValidCompassHeading() {
        val start = Offset(0.5f, 0.5f)
        val northTarget = Offset(0.5f, 0.2f)
        val eastTarget = Offset(0.8f, 0.5f)

        val northBearing = Pathfinding3D.calculateBearingDeg(start, northTarget)
        val eastBearing = Pathfinding3D.calculateBearingDeg(start, eastTarget)

        assertEquals("Bearing directly up should be North (0°)", 0f, northBearing, 2f)
        assertEquals("Bearing directly right should be East (90°)", 90f, eastBearing, 2f)
    }
}
