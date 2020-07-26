package io.budge.emodetect.data.models

data class FaceInference(
    val faceFilePath: String,
    var faceEmotion: String)