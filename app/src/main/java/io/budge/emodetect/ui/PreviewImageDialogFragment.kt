package io.budge.emodetect.ui


import android.app.Dialog
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.util.Pair
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.util.isEmpty
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.common.FirebaseMLException
import io.budge.emodetect.R
import io.budge.emodetect.automl.ImageClassifier
import kotlinx.android.synthetic.main.fragment_preview_image_dialog.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A simple [Fragment] subclass.
 */
class PreviewImageDialogFragment : DialogFragment() {
    // Max width (portrait mode)
    private var mImageMaxWidth: Int? = null
    // Max height (portrait mode)
    private var mImageMaxHeight: Int? = null

    private val imageBuffer = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    private var classifier: ImageClassifier? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_preview_image_dialog, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imageBitmap = arguments!!.getParcelable<Bitmap>("image_bitmap")
        dialog_preview_image.setImageBitmap(imageBitmap)
        dialog_preview_image.isEnabled = true
        dialog_preview_image.setOnClickListener {
            d(TAG, "Clicked")
//            scanning_animation.visibility = View.VISIBLE
//            scanning_animation.speed = 1f
//            scanning_animation.playAnimation()
//            dialog_preview_image.visibility = View.GONE
            detectFace(imageBitmap!!)
            dialog_preview_image.isEnabled = false
            classifyImage(toGreyScale(detectFace(imageBitmap)))
        }
        close_dialog.setOnClickListener {
            dialog!!.dismiss()
        }

        // Setup image classifier.
        try {
            classifier = ImageClassifier()
        } catch (e: FirebaseMLException) {
            val snackBarText = getString(R.string.fail_to_initialize_img_classifier)
            Snackbar.make(view, snackBarText, Snackbar.LENGTH_SHORT).show()
            d(TAG, "error: $snackBarText")
        }
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    companion object {
        fun newInstance(imageBitmap: Bitmap): PreviewImageDialogFragment {
            val args = Bundle()
            args.putParcelable("image_bitmap", imageBitmap)
            val fragment = PreviewImageDialogFragment()
            fragment.arguments = args
            return fragment
        }

        private val TAG = PreviewImageDialogFragment::class.java.simpleName
        /** Dimensions of inputs. */
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3
        private const val DIM_IMG_SIZE_X = 224
        private const val DIM_IMG_SIZE_Y = 224

    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** Writes Image data into a `ByteBuffer`. */
    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE
        ).apply {
            order(ByteOrder.nativeOrder())
            rewind()
        }
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)
        scaledBitmap.getPixels(
            imageBuffer, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height
        )
        // Convert the image to int points.
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val `val` = imageBuffer[pixel++]
                imgData.put((`val` shr 16 and 0xFF).toByte())
                imgData.put((`val` shr 8 and 0xFF).toByte())
                imgData.put((`val` and 0xFF).toByte())
            }
        }
        return imgData
    }

    private fun resizeImage(faceImage: Bitmap): Bitmap {
        // Get the dimensions of the View
        // Get the dimensions of the View
        val targetedSize: Pair<Int, Int> = getTargetedWidthHeight()

        val targetWidth = targetedSize.first
        val maxHeight = targetedSize.second

        // Determine how much to scale down the image
        // Determine how much to scale down the image
        val scaleFactor = Math.max(
            faceImage.width.toFloat() / targetWidth.toFloat(),
            faceImage.height.toFloat() / maxHeight.toFloat()
        )

        val resizedBitmap = Bitmap.createScaledBitmap(
            faceImage,
            (faceImage.width / scaleFactor).toInt(),
            (faceImage.height / scaleFactor).toInt(),
            true
        )

        dialog_preview_image.setImageBitmap(resizedBitmap)
        return resizedBitmap
    }

    // Functions for loading images from app assets.

    // Functions for loading images from app assets.
    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.
    private fun getImageMaxWidth(): Int {
        if (mImageMaxWidth == null) { // Calculate the max width in portrait mode. This is done lazily since we need to
            // wait for a UI layout pass to get the right values. So delay it to first time image rendering time.
            mImageMaxWidth = dialog_preview_image.width
        }
        return mImageMaxWidth!!
    }

    // Returns max image height, always for portrait mode. Caller needs to swap width / height for landscape mode.
    private fun getImageMaxHeight(): Int {
        if (mImageMaxHeight == null) {
            // Calculate the max width in portrait mode. This is done lazily since we need to wait for
            // a UI layout pass to get the right values. So delay it to first time image rendering time.
            mImageMaxHeight = dialog_preview_image.height
        }
        return mImageMaxHeight!!
    }

    // Gets the targeted width / height.

    private fun getTargetedWidthHeight(): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int
        val maxWidthForPortraitMode: Int = getImageMaxWidth()
        val maxHeightForPortraitMode: Int = getImageMaxHeight()
        targetWidth = maxWidthForPortraitMode
        targetHeight = maxHeightForPortraitMode
        return Pair(targetWidth, targetHeight)
    }

    /** Run image classification on the given [Bitmap] */
    private fun classifyImage(bitmap: Bitmap) {
        d(TAG, "Classifying")
        if (classifier == null) {
            val snackBartext = getString(R.string.uninitialized_img_classifier_or_invalid_context)
            Snackbar.make(view!!, snackBartext, Snackbar.LENGTH_SHORT).show()
            d(TAG, "error classify: $snackBartext")
            return
        }

        // Show image on screen.
        dialog_preview_image?.setImageBitmap(bitmap)

        // Classify image.
        classifier?.classifyFrame(bitmap)?.
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snackbarResultText = task.result
                    val resultSnackbar = Snackbar.make(view!!, snackbarResultText!!, Snackbar.LENGTH_INDEFINITE)
                    resultSnackbar.setAction("Dismiss") { resultSnackbar.dismiss() }
                    resultSnackbar.show()
                    d(TAG, "result: $snackbarResultText")

                } else {
                    val e = task.exception
                    Log.e(TAG, "Error classifying frame", e)
                    val snackBarErrorText = e?.message
                    Snackbar.make(view!!, snackBarErrorText!!, Snackbar.LENGTH_SHORT).show()
                    d(TAG, "error classify exception: $snackBarErrorText")
                }
//                scanning_animation.pauseAnimation()
//                scanning_animation.visibility = View.GONE
//                dialog_preview_image.visibility = View.VISIBLE
//                dialog_preview_image?.setImageBitmap(bitmap)
            }
    }

    private fun toGreyScale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapGreyscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapGreyscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixColorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return  bitmapGreyscale
    }

    override fun onDestroyView() {
        classifier?.close()
        super.onDestroyView()
    }

    private fun detectFace(imageBitmap: Bitmap): Bitmap {
        var faceBitmap = imageBitmap
        val faceDetector =  FaceDetector.Builder(context).apply {
            setProminentFaceOnly(false)
            setMode(FaceDetector.ACCURATE_MODE)
            setTrackingEnabled(false)
        }.build()
        val frame = Frame.Builder().apply {
            setBitmap(imageBitmap)
        }.build()
        if (faceDetector.isOperational) {
            val faces = faceDetector.detect(frame)
            d(TAG, "Faces: ${faces.size()}")
            if (faces.isEmpty()) {
                showToast("No faces detected")
                return faceBitmap
            } else {
                faceBitmap = cropFaces(faces, imageBitmap)
            }
        } else {
            d(TAG,  "Detection un-operational")
        }
        faceDetector.release()
        return faceBitmap
    }

    private fun cropFaces(faces: SparseArray<Face>, bitmap: Bitmap): Bitmap {
        val face = faces.valueAt(0) ?: return bitmap
        val position = face.position
        val width = face.width.toInt()
        val height = face.height.toInt()
        d(TAG, "x coor: ${position.x}")
        return Bitmap.createBitmap(bitmap, position.x.toInt(), position.y.toInt(), width, height)
    }
}
