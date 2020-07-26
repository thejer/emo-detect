package io.budge.emodetect.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.budge.emodetect.data.models.FaceInference

class FaceInferenceConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun toFaceInferences(value: String): MutableList<FaceInference> {
            val type = object : TypeToken<MutableList<FaceInference>>() {}.type
            return Gson().fromJson(value, type)
        }

        @TypeConverter
        @JvmStatic
        fun fromFaceInferences(faceInferences: MutableList<FaceInference>): String {
            return Gson().toJson(faceInferences)
        }
    }
}