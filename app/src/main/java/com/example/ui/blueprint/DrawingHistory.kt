package com.example.ui.blueprint

import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.RoomEntity

/**
 * One committed drawing change. Undo applies the inverse effects; redo applies them forward.
 * Strokes cover freehand walls, shapes, and circles. Edits and deletes are their own ops.
 */
sealed class DrawingOp {
    data class AddAnnotation(val annotation: AnnotationEntity) : DrawingOp()
    data class DeleteAnnotation(val annotation: AnnotationEntity) : DrawingOp()
    data class ClearAnnotations(
        val projectId: String,
        val removed: List<AnnotationEntity>
    ) : DrawingOp()
    data class EditRoom(val before: RoomEntity, val after: RoomEntity) : DrawingOp()
    data class EditEquipment(val before: EquipmentEntity, val after: EquipmentEntity) : DrawingOp()
}

sealed interface DrawingEffect {
    data class InsertAnnotation(val annotation: AnnotationEntity) : DrawingEffect
    data class RemoveAnnotation(val annotation: AnnotationEntity) : DrawingEffect
    data class ClearProjectAnnotations(val projectId: String) : DrawingEffect
    data class UpsertRoom(val room: RoomEntity) : DrawingEffect
    data class UpsertEquipment(val equipment: EquipmentEntity) : DrawingEffect
}

fun DrawingOp.effects(inverse: Boolean): List<DrawingEffect> = when (this) {
    is DrawingOp.AddAnnotation -> listOf(
        if (inverse) DrawingEffect.RemoveAnnotation(annotation) else DrawingEffect.InsertAnnotation(annotation)
    )
    is DrawingOp.DeleteAnnotation -> listOf(
        if (inverse) DrawingEffect.InsertAnnotation(annotation) else DrawingEffect.RemoveAnnotation(annotation)
    )
    is DrawingOp.ClearAnnotations -> if (inverse) {
        removed.map { DrawingEffect.InsertAnnotation(it) }
    } else {
        listOf(DrawingEffect.ClearProjectAnnotations(projectId))
    }
    is DrawingOp.EditRoom -> listOf(DrawingEffect.UpsertRoom(if (inverse) before else after))
    is DrawingOp.EditEquipment -> listOf(DrawingEffect.UpsertEquipment(if (inverse) before else after))
}

class DrawingHistory(private val limit: Int = 50) {
    private val undoStack = ArrayDeque<DrawingOp>()
    private val redoStack = ArrayDeque<DrawingOp>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(op: DrawingOp) {
        undoStack.addLast(op)
        redoStack.clear()
        while (undoStack.size > limit) undoStack.removeFirst()
    }

    fun popUndo(): DrawingOp? {
        val op = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(op)
        return op
    }

    fun popRedo(): DrawingOp? {
        val op = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(op)
        return op
    }
}

enum class DrawingShortcut { Undo, Redo }

/**
 * Hardware-keyboard mapping used on device and on web/desktop keyboards:
 * Ctrl/Cmd+Z undo, Ctrl+Y redo, Shift+Ctrl+Z and Shift+Cmd+Z redo.
 */
fun drawingShortcut(ctrlOrMeta: Boolean, shift: Boolean, isZ: Boolean, isY: Boolean): DrawingShortcut? {
    if (!ctrlOrMeta) return null
    if (isZ && !shift) return DrawingShortcut.Undo
    if (isZ && shift) return DrawingShortcut.Redo
    if (isY && !shift) return DrawingShortcut.Redo
    return null
}
