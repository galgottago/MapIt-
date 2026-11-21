package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.example.data.db.BlueprintDao
import com.example.data.db.SampleData
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.PhotoEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectNoteEntity
import com.example.data.model.RoomEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BlueprintRepository(
    private val context: Context,
    private val dao: BlueprintDao
) {

    val allProjects: Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun initializeSampleDataIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getProjectById("proj-hvac-plant")
        if (existing == null) {
            for (project in SampleData.sampleProjects) {
                dao.insertProject(project)
                dao.insertRooms(SampleData.getRoomsForProject(project.id))
                dao.insertEquipmentList(SampleData.getEquipmentForProject(project.id))
                for (note in SampleData.getNotesForProject(project.id)) {
                    dao.insertNote(note)
                }
            }
        }
    }

    fun getRooms(projectId: String): Flow<List<RoomEntity>> = dao.getRoomsByProject(projectId)
    fun getEquipment(projectId: String): Flow<List<EquipmentEntity>> = dao.getEquipmentByProject(projectId)
    fun getAnnotations(projectId: String): Flow<List<AnnotationEntity>> = dao.getAnnotationsByProject(projectId)
    fun getPhotos(projectId: String): Flow<List<PhotoEntity>> = dao.getPhotosByProject(projectId)
    fun getNotes(projectId: String): Flow<List<ProjectNoteEntity>> = dao.getNotesByProject(projectId)

    suspend fun getProject(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        dao.insertProject(project)
    }

    suspend fun updateRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        dao.updateRoom(room)
    }

    suspend fun addRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        dao.insertRoom(room)
    }

    suspend fun deleteRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        dao.deleteRoom(room)
    }

    suspend fun updateEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        dao.updateEquipment(equipment)
    }

    suspend fun addEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        dao.insertEquipment(equipment)
    }

    suspend fun deleteEquipment(equipment: EquipmentEntity) = withContext(Dispatchers.IO) {
        dao.deleteEquipment(equipment)
    }

    suspend fun getAnnotationsNow(projectId: String): List<AnnotationEntity> = withContext(Dispatchers.IO) {
        dao.getAnnotationsByProjectSync(projectId)
    }

    suspend fun addAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
        dao.insertAnnotation(annotation)
    }

    suspend fun deleteAnnotation(annotation: AnnotationEntity) = withContext(Dispatchers.IO) {
        dao.deleteAnnotation(annotation)
    }

    suspend fun clearAnnotations(projectId: String) = withContext(Dispatchers.IO) {
        dao.clearAnnotations(projectId)
    }

    suspend fun addNote(note: ProjectNoteEntity) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
    }

    suspend fun updateNote(note: ProjectNoteEntity) = withContext(Dispatchers.IO) {
        dao.updateNote(note)
    }

    suspend fun deleteNote(note: ProjectNoteEntity) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }

    suspend fun deletePhoto(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        dao.deletePhoto(photo)
    }

    // Camera photo capture and saving to Android MediaStore / Pictures camera roll
    suspend fun saveInspectionPhoto(
        projectId: String,
        projectName: String,
        locationName: String,
        equipmentTag: String?,
        bitmap: Bitmap,
        note: String
    ): PhotoEntity = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val timeString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        val cleanLoc = locationName.replace(Regex("[^a-zA-Z0-9]"), "_").take(24)
        val cleanTag = equipmentTag?.replace(Regex("[^a-zA-Z0-9]"), "_")?.take(16) ?: "General"
        val fileName = "Blueprint_${cleanLoc}_${cleanTag}_${timeString}.jpg"

        var savedUriString = ""
        var filePath = ""

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Blueprint3D")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                savedUriString = imageUri.toString()
                filePath = fileName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback or local internal copy for fast app display
        val localFile = File(context.filesDir, fileName)
        FileOutputStream(localFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (savedUriString.isEmpty()) {
            savedUriString = Uri.fromFile(localFile).toString()
        }
        filePath = localFile.absolutePath

        val photoEntity = PhotoEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            roomName = locationName,
            equipmentTag = equipmentTag,
            filePath = filePath,
            uriString = savedUriString,
            fileName = fileName,
            timestamp = timestamp,
            note = note
        )

        dao.insertPhoto(photoEntity)
        photoEntity
    }

    // Native Android PDF Renderer to convert imported PDF blueprint into bitmap texture
    suspend fun renderPdfPageToBitmap(pdfUri: Uri, pageIndex: Int = 0): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd != null) {
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > pageIndex) {
                        renderer.openPage(pageIndex).use { page ->
                            val width = (page.width * 2).coerceIn(800, 2048)
                            val height = (page.height * 2).coerceIn(800, 2048)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            return@withContext bitmap
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun importPdfBlueprint(
        pdfUri: Uri,
        displayName: String,
        description: String = "Imported architectural blueprint"
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val projectId = "proj-" + UUID.randomUUID().toString().take(8)
        val project = ProjectEntity(
            id = projectId,
            name = displayName.removeSuffix(".pdf").replace("_", " "),
            description = description,
            pdfUri = pdfUri.toString(),
            pdfName = displayName,
            gpsLat = 37.7749,
            gpsLng = -122.4194
        )
        dao.insertProject(project)

        // Generate baseline standard room subdivisions for the newly imported floor plan
        val defaultRooms = listOf(
            RoomEntity(
                id = "$projectId-r1",
                projectId = projectId,
                name = "Main Entrance & Reception",
                code = "SEC-101",
                floorNumber = 1,
                minX = 0.08f, minY = 0.08f, maxX = 0.48f, maxY = 0.48f,
                wallHeight = 35f,
                colorHex = "#00E5FF",
                notes = "Primary security turnstiles and badge access."
            ),
            RoomEntity(
                id = "$projectId-r2",
                projectId = projectId,
                name = "Operations & Equipment Bay",
                code = "OPS-102",
                floorNumber = 1,
                minX = 0.52f, minY = 0.08f, maxX = 0.92f, maxY = 0.48f,
                wallHeight = 35f,
                colorHex = "#FFD166",
                notes = "Facility utility risers and electrical control panels."
            ),
            RoomEntity(
                id = "$projectId-r3",
                projectId = projectId,
                name = "Mechanical & Plant Suite",
                code = "MECH-201",
                floorNumber = 1,
                minX = 0.08f, minY = 0.54f, maxX = 0.48f, maxY = 0.90f,
                wallHeight = 30f,
                colorHex = "#06D6A0",
                notes = "Air handling units and return ductwork."
            ),
            RoomEntity(
                id = "$projectId-r4",
                projectId = projectId,
                name = "IT Server & Comm Closet",
                code = "IDF-205",
                floorNumber = 1,
                minX = 0.52f, minY = 0.54f, maxX = 0.92f, maxY = 0.90f,
                wallHeight = 30f,
                colorHex = "#A78BFA",
                notes = "Fiber patch panels and intermediate network distribution."
            )
        )
        dao.insertRooms(defaultRooms)

        val defaultEquipment = listOf(
            EquipmentEntity(
                id = "$projectId-eq1",
                projectId = projectId,
                roomId = "$projectId-r2",
                roomName = "Operations & Equipment Bay",
                tag = "PANEL-MDP-1",
                name = "Main Distribution Electrical Panel",
                category = "ELECTRICAL",
                posX = 0.70f, posY = 0.28f, posZ = 0f,
                status = "Operational",
                specifications = "480Y/277V 3-Phase 4-Wire, 800A Main Circuit Breaker"
            ),
            EquipmentEntity(
                id = "$projectId-eq2",
                projectId = projectId,
                roomId = "$projectId-r3",
                roomName = "Mechanical & Plant Suite",
                tag = "AHU-ROOF-01",
                name = "Packaged Rooftop Air Handler",
                category = "HVAC",
                posX = 0.28f, posY = 0.72f, posZ = 0f,
                status = "Operational",
                specifications = "Variable Air Volume, 12.5 Tons, 4600 CFM"
            )
        )
        dao.insertEquipmentList(defaultEquipment)

        dao.insertNote(
            ProjectNoteEntity(
                id = "$projectId-note1",
                projectId = projectId,
                title = "Blueprint Initial Setup",
                content = "Imported PDF blueprint '$displayName'. Calibrate room boundaries and add equipment pins as needed.",
                category = "General"
            )
        )

        project
    }
}
