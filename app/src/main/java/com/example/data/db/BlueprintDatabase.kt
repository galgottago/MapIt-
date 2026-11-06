package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.EquipmentEntity
import com.example.data.model.PhotoEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectNoteEntity
import com.example.data.model.RoomEntity

@Database(
    entities = [
        ProjectEntity::class,
        RoomEntity::class,
        EquipmentEntity::class,
        AnnotationEntity::class,
        PhotoEntity::class,
        ProjectNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BlueprintDatabase : RoomDatabase() {
    abstract fun blueprintDao(): BlueprintDao

    companion object {
        @Volatile
        private var INSTANCE: BlueprintDatabase? = null

        fun getDatabase(context: Context): BlueprintDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlueprintDatabase::class.java,
                    "blueprint_3d_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
