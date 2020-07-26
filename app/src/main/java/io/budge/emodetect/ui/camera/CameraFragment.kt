package io.budge.emodetect.ui.camera


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import io.budge.emodetect.R
import io.budge.emodetect.ui.detail.DetailsFragment
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.util.concurrent.Executors

private const val IMAGE_PICKER_CODE = 22

class CameraFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }
    private var lensFacing = CameraX.LensFacing.BACK

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera_preview.post {
            startCamera()
        }
        camera_switch.setOnClickListener {
            if (CameraX.LensFacing.FRONT == lensFacing) {
                lensFacing = CameraX.LensFacing.BACK
                camera_switch.setImageResource(R.drawable.ic_front_camera)
            } else {
                lensFacing = CameraX.LensFacing.FRONT
                camera_switch.setImageResource(R.drawable.ic_rear_camera)
            }
            try {
                CameraX.getCameraWithLensFacing(lensFacing)
                CameraX.unbindAll()
                startCamera()
            } catch (exc: Exception) {
            }
        }

        camera_preview.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        photo_gallery.setOnClickListener {
            pickImageFromGallery()
        }
    }

    private val onImageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onImageSaved(file: File) {
            camera_preview.post {
                CameraX.unbindAll()
                replaceFragment(
                   Uri.fromFile(file)
                )
            }
        }

        override fun onError(
            imageCaptureError: ImageCapture.ImageCaptureError,
            message: String,
            cause: Throwable?
        ) {
            val errorMessage = "Photo capture failed: $message"
            camera_preview.post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICKER_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICKER_CODE) {
            if (data != null) {
                CameraX.unbindAll()
                replaceFragment(
                    data.data!!
                )
            }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(Size(640, 480))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val parent = camera_preview.parent as ViewGroup
            val index = parent.indexOfChild(camera_preview)
            parent.removeView(camera_preview)
            parent.addView(camera_preview, index)
            camera_preview.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setLensFacing(lensFacing)
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()
        val imageCapture = ImageCapture(imageCaptureConfig)
        capture_image.setOnClickListener {
            val file = File(
                context!!.externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg"
            )
            imageCapture.takePicture(file, executor, onImageSavedListener)
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = camera_preview.width / 2f
        val centerY = camera_preview.height / 2f
        val rotationDegree = when (camera_preview.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegree.toFloat(), centerX, centerY)
        camera_preview.setTransform(matrix)
    }

    private fun replaceFragment(imageUri: Uri) {
        val fragment = DetailsFragment.newInstance(imageUri)
        val tag = fragment.javaClass.simpleName
        activity!!.supportFragmentManager
            .beginTransaction()
            .replace(R.id.view_container, fragment, tag)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    companion object {
        fun newInstance(): CameraFragment {
            val args = Bundle()
            val fragment = CameraFragment()
            fragment.arguments = args
            return fragment
        }
    }
}