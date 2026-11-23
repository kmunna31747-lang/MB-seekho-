package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CodeStatus
import com.example.data.model.UserRole

@Database(
    entities = [
        AccessCodeEntity::class,
        UserEntity::class,
        CourseEntity::class,
        PaymentEntity::class,
        SavedCourseEntity::class,
        CourseProgressEntity::class,
        OwnerSecurityEntity::class,
        LoginAttemptEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MbSeekhoDatabase : RoomDatabase() {
    abstract fun dao(): MbSeekhoDao

    companion object {
        @Volatile
        private var INSTANCE: MbSeekhoDatabase? = null

        fun getInstance(context: Context): MbSeekhoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MbSeekhoDatabase::class.java,
                    "mb_seekho_secure.db"
                ).build().also { INSTANCE = it }
            }
        }

        suspend fun ensureInitialHashesSeeded(dao: MbSeekhoDao) {
            if (dao.getAccessCodeCount() > 0) return
            val now = System.currentTimeMillis()
            val studentEntities = InitialCodeHashes.studentCodeHashes.map { (id, hash) ->
                AccessCodeEntity(
                    codeId = id,
                    codeHash = hash,
                    role = UserRole.STUDENT.value,
                    status = CodeStatus.AVAILABLE.value,
                    createdAt = now
                )
            }
            val teacherEntities = InitialCodeHashes.teacherCodeHashes.map { (id, hash) ->
                AccessCodeEntity(
                    codeId = id,
                    codeHash = hash,
                    role = UserRole.TEACHER.value,
                    status = CodeStatus.AVAILABLE.value,
                    createdAt = now
                )
            }
            dao.insertAccessCodes(studentEntities + teacherEntities)
        }
    }
}
