package io.budge.emodetect.automl

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import java.io.IOException

class ImageClassifier
@Throws(FirebaseMLException::class)
internal constructor() {
    private val labeler: FirebaseVisionImageLabeler?
    init {
        FirebaseModelManager.getInstance()
            .registerLocalModel(
                FirebaseLocalModel.Builder(LOCAL_MODEL_NAME)
                    .setAssetFilePath(LOCAL_MODEL_PATH)
                    .build()
            )
        val options = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
            .setConfidenceThreshold(CONFIDENCE_THRESHOLD)
            .setLocalModelName(LOCAL_MODEL_NAME)
            .build()

        labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options)
    }

    internal fun classifyFrame(bitmap: Bitmap): Task<String> {
        if (labeler == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            val e = IllegalStateException("Uninitialized Classifier.")
            val completionSource = TaskCompletionSource<String>()
            completionSource.setException(e)
            return completionSource.task
        }

        val image = FirebaseVisionImage.fromBitmap(bitmap)

        return labeler.processImage(image).continueWith {
            val labelProbList = it.result
            val textToShow = if (labelProbList.isNullOrEmpty())
                "no"
            else
                labelProbList[0].text
            textToShow
        }
    }

    internal fun close() {
        try {
            labeler?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to close the labeler instance", e)
        }
    }

    companion object {
        private var TAG: String = ImageClassifier.javaClass.simpleName
        private const val LOCAL_MODEL_NAME = "graph"
        private const val LOCAL_MODEL_PATH = "model/manifest.json"
        private const val CONFIDENCE_THRESHOLD = 0.4f
    }
}
