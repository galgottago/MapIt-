package com.example

import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity
import com.example.ui.blueprint.DrawingEffect
import com.example.ui.blueprint.DrawingHistory
import com.example.ui.blueprint.DrawingOp
import com.example.ui.blueprint.DrawingShortcut
import com.example.ui.blueprint.drawingShortcut
import com.example.ui.blueprint.effects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingHistoryTest {

    @Test
    fun undoAndRedo_coverWallsShapesCirclesEditsAndDeletes() {
        val wall = stroke("wall", line(0.1f, 0.2f, 0.4f, 0.2f))
        val shape = stroke("shape", rectangle(0.5f, 0.5f, 0.2f, 0.15f))
        val circle = stroke("circle", circle(0.7f, 0.3f, 0.08f))
        val room = sampleRoom(name = "Lobby")
        val renamed = room.copy(name = "Lobby A")
        val gear = sampleEquipment(name = "Plotter")
        val editedGear = gear.copy(name = "Plotter 2", status = "Maintenance")

        val history = DrawingHistory()
        var board = Board()

        board = board.commit(history, DrawingOp.AddAnnotation(wall))
        board = board.commit(history, DrawingOp.AddAnnotation(shape))
        board = board.commit(history, DrawingOp.AddAnnotation(circle))
        board = board.commit(history, DrawingOp.EditRoom(room, renamed))
        board = board.commit(history, DrawingOp.EditEquipment(gear, editedGear))
        board = board.commit(history, DrawingOp.DeleteAnnotation(circle))
        board = board.commit(
            history,
            DrawingOp.ClearAnnotations("proj", board.annotations)
        )

        assertTrue(board.annotations.isEmpty())
        assertEquals("Lobby A", board.rooms.single().name)
        assertEquals("Plotter 2", board.equipment.single().name)

        board = board.undo(history)
        assertEquals(listOf("wall", "shape"), board.annotations.map { it.id })
        board = board.undo(history)
        assertEquals(listOf("wall", "shape", "circle"), board.annotations.map { it.id })
        board = board.undo(history)
        assertEquals("Plotter", board.equipment.single().name)
        board = board.undo(history)
        assertEquals("Lobby", board.rooms.single().name)
        board = board.undo(history)
        assertFalse(board.annotations.any { it.id == "circle" })
        board = board.undo(history)
        assertFalse(board.annotations.any { it.id == "shape" })
        board = board.undo(history)
        assertTrue(board.annotations.isEmpty())
        assertFalse(history.canUndo)

        repeat(7) { board = board.redo(history) }
        assertTrue(board.annotations.isEmpty())
        assertEquals("Lobby A", board.rooms.single().name)
        assertEquals("Maintenance", board.equipment.single().status)
        assertFalse(history.canRedo)
    }

    @Test
    fun newDrawing_dropsRedoStack() {
        val history = DrawingHistory()
        var board = Board()
        val first = stroke("a", line(0f, 0f, 1f, 0f))
        val second = stroke("b", line(0f, 1f, 1f, 1f))
        board = board.commit(history, DrawingOp.AddAnnotation(first))
        board = board.undo(history)
        assertTrue(board.annotations.isEmpty())
        assertTrue(history.canRedo)

        board = board.commit(history, DrawingOp.AddAnnotation(second))
        assertFalse(history.canRedo)
        assertEquals(listOf("b"), board.annotations.map { it.id })
        assertNull(history.popRedo())
    }

    @Test
    fun clearUndo_restoresEveryRemovedStroke() {
        val history = DrawingHistory()
        var board = Board()
        val marks = listOf(
            stroke("wall", line(0.1f, 0.1f, 0.2f, 0.4f)),
            stroke("shape", rectangle(0.3f, 0.3f, 0.2f, 0.2f)),
            stroke("circle", circle(0.6f, 0.6f, 0.1f))
        )
        marks.forEach { board = board.commit(history, DrawingOp.AddAnnotation(it)) }
        val snapshot = board.annotations
        board = board.commit(history, DrawingOp.ClearAnnotations("proj", snapshot))
        assertTrue(board.annotations.isEmpty())

        board = board.undo(history)
        assertEquals(snapshot.map { it.id }, board.annotations.map { it.id })
        assertEquals(snapshot.map { it.pointsJson }, board.annotations.map { it.pointsJson })
    }

    @Test
    fun history_capsAtLimit() {
        val history = DrawingHistory(limit = 2)
        val first = stroke("1", line(0f, 0f, 1f, 0f))
        val second = stroke("2", line(0f, 0f, 1f, 1f))
        val third = stroke("3", circle(0.5f, 0.5f, 0.2f))
        history.record(DrawingOp.AddAnnotation(first))
        history.record(DrawingOp.AddAnnotation(second))
        history.record(DrawingOp.AddAnnotation(third))

        assertEquals("3", (history.popUndo() as DrawingOp.AddAnnotation).annotation.id)
        assertEquals("2", (history.popUndo() as DrawingOp.AddAnnotation).annotation.id)
        assertNull(history.popUndo())
    }

    @Test
    fun shortcuts_matchCtrlCmdUndoAndRedo() {
        assertEquals(DrawingShortcut.Undo, drawingShortcut(ctrlOrMeta = true, shift = false, isZ = true, isY = false))
        assertEquals(DrawingShortcut.Redo, drawingShortcut(ctrlOrMeta = true, shift = true, isZ = true, isY = false))
        assertEquals(DrawingShortcut.Redo, drawingShortcut(ctrlOrMeta = true, shift = false, isZ = false, isY = true))
        assertNull(drawingShortcut(ctrlOrMeta = false, shift = false, isZ = true, isY = false))
        assertNull(drawingShortcut(ctrlOrMeta = true, shift = true, isZ = false, isY = true))
    }

    private data class Board(
        val annotations: List<AnnotationEntity> = emptyList(),
        val rooms: List<RoomEntity> = emptyList(),
        val equipment: List<EquipmentEntity> = emptyList()
    ) {
        fun commit(history: DrawingHistory, op: DrawingOp): Board {
            history.record(op)
            return apply(op.effects(inverse = false))
        }

        fun undo(history: DrawingHistory): Board {
            val op = history.popUndo() ?: return this
            return apply(op.effects(inverse = true))
        }

        fun redo(history: DrawingHistory): Board {
            val op = history.popRedo() ?: return this
            return apply(op.effects(inverse = false))
        }

        private fun apply(effects: List<DrawingEffect>): Board {
            var next = this
            for (effect in effects) {
                next = when (effect) {
                    is DrawingEffect.InsertAnnotation ->
                        next.copy(annotations = next.annotations + effect.annotation)
                    is DrawingEffect.RemoveAnnotation ->
                        next.copy(annotations = next.annotations.filter { it.id != effect.annotation.id })
                    is DrawingEffect.ClearProjectAnnotations ->
                        next.copy(annotations = next.annotations.filter { it.projectId != effect.projectId })
                    is DrawingEffect.UpsertRoom ->
                        next.copy(rooms = next.rooms.filter { it.id != effect.room.id } + effect.room)
                    is DrawingEffect.UpsertEquipment ->
                        next.copy(equipment = next.equipment.filter { it.id != effect.equipment.id } + effect.equipment)
                }
            }
            return next
        }
    }

    private fun stroke(id: String, points: String) = AnnotationEntity(
        id = id,
        projectId = "proj",
        type = "MARKER",
        color = 0xFF00E5FF,
        strokeWidth = 6f,
        pointsJson = points
    )

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float): String =
        """[{"x":$x1,"y":$y1},{"x":$x2,"y":$y2}]"""

    private fun rectangle(x: Float, y: Float, w: Float, h: Float): String {
        val pts = listOf(
            x to y,
            x + w to y,
            x + w to y + h,
            x to y + h,
            x to y
        )
        return pts.joinToString(prefix = "[", postfix = "]") { (px, py) -> """{"x":$px,"y":$py}""" }
    }

    private fun circle(cx: Float, cy: Float, radius: Float): String {
        val pts = (0..16).map { step ->
            val angle = (Math.PI * 2.0 * step) / 16.0
            val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
            val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
            """{"x":$x,"y":$y}"""
        }
        return pts.joinToString(prefix = "[", postfix = "]")
    }

    private fun sampleRoom(name: String) = RoomEntity(
        id = "room-1",
        projectId = "proj",
        name = name,
        code = "LBY",
        minX = 0.1f,
        minY = 0.1f,
        maxX = 0.4f,
        maxY = 0.4f
    )

    private fun sampleEquipment(name: String) = EquipmentEntity(
        id = "eq-1",
        projectId = "proj",
        roomId = "room-1",
        tag = "PLT-1",
        name = name,
        category = "TOOL",
        posX = 0.5f,
        posY = 0.5f
    )
}
