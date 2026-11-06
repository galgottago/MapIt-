package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val pdfUri: String? = null,
    val pdfName: String? = null,
    val gpsLat: Double? = null,
    val gpsLng: Double? = null,
    val compassDeclination: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val code: String,
    val floorNumber: Int = 1,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val wallHeight: Float = 40f,
    val colorHex: String = "#00E5FF",
    val notes: String = "",
    val polygonJson: String = "" // JSON list of normalized [x,y] coordinates
)

@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val roomId: String?,
    val roomName: String = "",
    val tag: String, // e.g. "HVAC-CH-01"
    val name: String,
    val category: String, // HVAC, ELECTRICAL, PLUMBING, NETWORK, SENSOR, SAFETY
    val posX: Float, // Normalized 0..1 in floor coordinates
    val posY: Float,
    val posZ: Float = 0f, // Height off ground
    val status: String = "Operational", // Operational, Maintenance, Offline
    val specifications: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val type: String, // "MARKER", "HIGHLIGHTER", "TEXT_3D"
    val color: Long, // Color value as Long (ARGB)
    val strokeWidth: Float = 4f,
    val text: String = "",
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val pointsJson: String = "", // JSON list of normalized points for freehand drawing
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val roomName: String,
    val equipmentTag: String? = null,
    val filePath: String,
    val uriString: String,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "project_notes")
data class ProjectNoteEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val content: String,
    val category: String = "Field Note", // Inspection, Maintenance, Hazard, Specs, General
    val linkedRoomId: String? = null,
    val linkedRoomName: String? = null,
    val linkedEquipmentTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
