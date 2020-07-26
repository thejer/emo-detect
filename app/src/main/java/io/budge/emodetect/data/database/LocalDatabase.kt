package io.budge.emodetect.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.budge.emodetect.data.models.ClassifiedImage

@Database(entities = [ClassifiedImage::class], version = 1, exportSchema = false)
@TypeConverters(FaceInferenceConverter::class)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun classifiedImageDao(): ClassifiedImageDao

    companion object {
        private const val DATABASE_NAME = "emodetect.db"
        private val LOCK = Any()
        @Volatile
        private var sInstance: LocalDatabase? = null

        @JvmStatic
        fun getsInstance(context: Context): LocalDatabase? {
            if (sInstance == null) {
                synchronized(LOCK) {
                    if (sInstance == null) {
                        sInstance = Room.databaseBuilder(context.applicationContext,
                            LocalDatabase::class.java, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return sInstance
        }
    }
}