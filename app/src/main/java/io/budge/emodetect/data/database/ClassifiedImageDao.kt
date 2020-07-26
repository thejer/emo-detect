package io.budge.emodetect.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.budge.emodetect.data.models.ClassifiedImage

@Dao
interface ClassifiedImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertClassifiedImage(classifiedImage: ClassifiedImage)

    @get:Query("SELECT * FROM classifiedimage")
    val getClassifiedImage: List<ClassifiedImage>
}