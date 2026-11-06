package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.PhotoEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectNoteEntity
import com.example.data.model.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlueprintDao {

    // Projects
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    // Rooms
    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY name ASC")
    fun getRoomsByProject(projectId: String): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getRoomsByProjectSync(projectId: String): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    // Equipment
    @Query("SELECT * FROM equipment WHERE projectId = :projectId ORDER BY tag ASC")
    fun getEquipmentByProject(projectId: String): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE projectId = :projectId ORDER BY tag ASC")
    suspend fun getEquipmentByProjectSync(projectId: String): List<EquipmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentList(equipment: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentEntity)

    @Update
    suspend fun updateEquipment(equipment: EquipmentEntity)

    @Delete
    suspend fun deleteEquipment(equipment: EquipmentEntity)

    // Annotations
    @Query("SELECT * FROM annotations WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun getAnnotationsByProject(projectId: String): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE projectId = :projectId")
    suspend fun clearAnnotations(projectId: String)

    // Photos
    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getPhotosByProject(projectId: String): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    // Project Notes
    @Query("SELECT * FROM project_notes WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getNotesByProject(projectId: String): Flow<List<ProjectNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ProjectNoteEntity)

    @Update
    suspend fun updateNote(note: ProjectNoteEntity)

    @Delete
    suspend fun deleteNote(note: ProjectNoteEntity)
}
