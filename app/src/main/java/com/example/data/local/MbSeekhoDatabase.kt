package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CodeStatus
import com.example.data.model.UserRole

@Database(
    entities = [
        AccessCodeEntity::class,
        CodeBatchEntity::class,
        UserEntity::class,
        CourseEntity::class,
        PaymentEntity::class,
        SavedCourseEntity::class,
        CourseProgressEntity::class,
        OwnerSecurityEntity::class,
        LoginAttemptEntity::class,
        VideoViewEntity::class,
        TeacherCoinsEntity::class,
        CoinTransactionEntity::class,
        VideoLikeEntity::class,
        VideoCommentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MbSeekhoDatabase : RoomDatabase() {
    abstract fun dao(): MbSeekhoDao

    companion object {
        @Volatile
        private var INSTANCE: MbSeekhoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `video_views` (
                        `viewId` TEXT NOT NULL,
                        `videoId` TEXT NOT NULL,
                        `teacherId` TEXT NOT NULL,
                        `viewerId` TEXT NOT NULL,
                        `viewedAt` INTEGER NOT NULL,
                        `validationStatus` TEXT NOT NULL,
                        PRIMARY KEY(`viewId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_views_videoId` ON `video_views` (`videoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_views_teacherId` ON `video_views` (`teacherId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_views_viewedAt` ON `video_views` (`viewedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_views_videoId_viewerId` ON `video_views` (`videoId`, `viewerId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `teacher_coins` (
                        `teacherId` TEXT NOT NULL,
                        `totalCoins` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`teacherId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `coin_transactions` (
                        `transactionId` TEXT NOT NULL,
                        `teacherId` TEXT NOT NULL,
                        `videoId` TEXT NOT NULL,
                        `viewsCount` INTEGER NOT NULL,
                        `coinsEarned` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_coin_transactions_teacherId` ON `coin_transactions` (`teacherId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_coin_transactions_createdAt` ON `coin_transactions` (`createdAt`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `video_likes` (
                        `videoId` TEXT NOT NULL,
                        `studentId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`videoId`, `studentId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_likes_videoId` ON `video_likes` (`videoId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `video_comments` (
                        `commentId` TEXT NOT NULL,
                        `videoId` TEXT NOT NULL,
                        `studentId` TEXT NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `studentPhoto` TEXT,
                        `commentText` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`commentId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_comments_videoId` ON `video_comments` (`videoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_comments_studentId` ON `video_comments` (`studentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_video_comments_createdAt` ON `video_comments` (`createdAt`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `access_codes` ADD COLUMN `batchId` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_access_codes_role_status` ON `access_codes` (`role`, `status`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `code_batches` (
                        `batchId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `sourceLabel` TEXT NOT NULL,
                        `totalSubmitted` INTEGER NOT NULL,
                        `insertedCount` INTEGER NOT NULL,
                        `duplicateCount` INTEGER NOT NULL,
                        `invalidCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`batchId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_code_batches_role` ON `code_batches` (`role`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_code_batches_createdAt` ON `code_batches` (`createdAt`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `courses` ADD COLUMN `videoType` TEXT NOT NULL DEFAULT 'long'")
                db.execSQL("ALTER TABLE `courses` ADD COLUMN `durationMs` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_videoType` ON `courses` (`videoType`)")
            }
        }

        fun getInstance(context: Context): MbSeekhoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MbSeekhoDatabase::class.java,
                    "mb_seekho_secure.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * Ensures the 10 initial private Teacher Access Code SHA-256 hashes are seeded.
         * Zero Student Access Codes are hardcoded or pre-seeded in frontend code —
         * Student Access Codes (10,000+ and unlimited future batches) are imported or generated
         * securely through the Owner Admin system or Firebase backend importer.
         */
        suspend fun ensureInitialHashesSeeded(dao: MbSeekhoDao) {
            if (dao.countCodesByRole(UserRole.TEACHER.value) > 0) return
            val now = System.currentTimeMillis()
            val teacherEntities = InitialCodeHashes.teacherCodeHashes.map { (id, hash) ->
                AccessCodeEntity(
                    codeId = id,
                    codeHash = hash,
                    role = UserRole.TEACHER.value,
                    status = CodeStatus.AVAILABLE.value,
                    createdAt = now,
                    batchId = "INITIAL_TEACHERS"
                )
            }
            dao.insertAccessCodes(teacherEntities)
        }
    }
}
