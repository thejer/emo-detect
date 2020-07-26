package io.budge.emodetect.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "ClassifiedImage", indices = [Index(value = ["_id"], unique = true)] )
data class ClassifiedImage(
    var mainImagePath: String,
    var detectedFaces: MutableList<FaceInference>){
    @PrimaryKey(autoGenerate = true)
    var _id: Int = 0
}