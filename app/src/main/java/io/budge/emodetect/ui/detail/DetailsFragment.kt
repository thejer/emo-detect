package io.budge.emodetect.ui.detail


import android.annotation.SuppressLint
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.util.Log.d
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.common.FirebaseMLException
import io.budge.emodetect.R
import io.budge.emodetect.automl.ImageClassifier
import io.budge.emodetect.data.database.LocalDatabase
import io.budge.emodetect.data.models.ClassifiedImage
import io.budge.emodetect.data.models.FaceInference
import io.budge.emodetect.util.ImageUtils
import io.budge.emodetect.util.MessageUtils
import io.budge.emodetect.util.Provider
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

private var TAG = DetailsFragment::class.java.simpleName
private const val IMAGE_URI_KEY = "image_bitmap"

class DetailsFragment : Fragment(), FaceRecyclerViewAdapter.OnItemClickListener,
    EmotionDialogFragment.OnEmotionSelectedListener {
    private var classifier: ImageClassifier? = null
    private lateinit var facesAdapter: FaceRecyclerViewAdapter
    private var databaseInstance: LocalDatabase? = null
    private val coroutineSupervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + coroutineSupervisor)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    private val faceInferences = mutableListOf<FaceInference>()
    private lateinit var now: Date
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Setup image classifier.

        databaseInstance = Provider.provideLocalDatabase(context)
        val imageUri = arguments!!.getParcelable<Uri>(IMAGE_URI_KEY)
        val imageBitmap = ImageUtils.handleSamplingAndRotationBitmap(
            context,
            imageUri
        )

        setupRecyclerView()
        now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm_ss", now)
        val mainFile = saveImage(imageBitmap, "main")
        if (mainFile.exists())
            detail_main_image.setImageBitmap(imageBitmap)

        Palette.Builder(imageBitmap).generate {
            it?.let { palette ->
                val dominantColor = palette.getDominantColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.white
                    )
                )
             root_layout.setBackgroundColor(dominantColor)
            }
        }

        detail_main_image.setOnClickListener {
            scanning_animation.visibility = View.VISIBLE
            tap_animation.visibility = View.GONE
            pulsar_animation.visibility = View.GONE
            detail_main_image.isEnabled = false
            try {
                classifier = ImageClassifier()
            } catch (e: FirebaseMLException) {
                val snackBarText = getString(R.string.fail_to_initialize_img_classifier)
                Snackbar.make(view, snackBarText, Snackbar.LENGTH_SHORT).show()
                d(TAG, "error: $snackBarText")
            }
            detectFacesAsync(imageBitmap)
        }

        save_inference.setOnClickListener {
            saveClassifiedImageToDb(ClassifiedImage(mainFile.absolutePath, faceInferences))
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun makeInferenceAsync(file: File) {
        object : AsyncTask<File, Void?, Void?>() {
            override fun doInBackground(vararg file: File): Void? {
                classifyImage(file[0])
                return null
            }
        }.execute(file)
    }

    @SuppressLint("StaticFieldLeak")
    fun detectFacesAsync(bitmap: Bitmap) {
        object : AsyncTask<Bitmap, Void?, MutableList<Bitmap>>() {
            override fun doInBackground(vararg params: Bitmap?): MutableList<Bitmap> {
                return detectFaces(params[0]!!)
            }

            override fun onPostExecute(result: MutableList<Bitmap>?) {
                if (result.isNullOrEmpty()) MessageUtils().showToast(
                    "No faces in the image provided",
                    context!!
                )
                else
                    for (croppedFace in result) {
                        val file =
                            saveImage(croppedFace, "face${result.indexOf(croppedFace) + 1}")
                        d(TAG, file.absolutePath)
                        makeInferenceAsync(file)
                    }
                scanning_animation.visibility = View.GONE
                d(TAG, "Inferenced")
            }
        }.execute(bitmap)
    }


    private fun setupRecyclerView() {
        facesAdapter = FaceRecyclerViewAdapter(this)
        val decoration = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        facesRecyclerView.apply {
            adapter = facesAdapter
            addItemDecoration(decoration)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
        }
    }

    private fun saveImage(bitmap: Bitmap, fileName: String): File {
        try {
            val imagesPath = File(context!!.filesDir.path, ".EmoDetect${File.separator}$now")
            if (!imagesPath.exists()) {
                imagesPath.mkdirs()
            }
            val imageFile = File(imagesPath, "${fileName}.jpeg")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return imageFile
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Saving image failed with", e)
        } catch (e: IOException) {
            Log.e(TAG, "Saving image failed with", e)
        }
        return File(
            context!!.filesDir.path,
            ".EmoDetect${File.separator}$now${File.separator}main.jpeg"
        )
    }

    private fun cropFace(face: Face, bitmap: Bitmap): Bitmap {
        val position = face.position
        val x = if (position.x < 0) 0 else position.x.toInt()
        val y = if (position.y < 0) 0 else position.y.toInt()
        var width = face.width.toInt()
        var height = face.height.toInt()
        width =
            if (x + width > bitmap.width)
                bitmap.width - x
            else face.width.toInt()
        height =
            if (y + height > bitmap.height)
                bitmap.height - y
            else
                face.height.toInt()
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun detectFaces(imageBitmap: Bitmap) = runBlocking {
        val facesList = mutableListOf<Bitmap>()
        val faceDetector = FaceDetector.Builder(context).apply {
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
                MessageUtils().showToast("No faces detected", context!!)
            } else {
                faces.forEach { _, value ->
                    facesList.add(cropFace(value, imageBitmap))
                }
            }
        }
        faceDetector.release()
        facesList
    }

    private fun toGreyScale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val bitmapGreyScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapGreyScale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixColorFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return bitmapGreyScale
    }

    private fun classifyImage(file: File) {
        val bitmap = toGreyScale(BitmapFactory.decodeFile(file.absolutePath))
        if (classifier == null) {
            val snackBarText = getString(R.string.uninitialized_img_classifier_or_invalid_context)
            Snackbar.make(view!!, snackBarText, Snackbar.LENGTH_SHORT).show()
            return
        }
        classifier!!.classifyFrame(bitmap).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                faceInferences.add(FaceInference(file.absolutePath, task.result!!))
                activity!!.runOnUiThread {
                    facesAdapter.swapItems(faceInferences)
                }
            } else {
                val e = task.exception
                val snackBarErrorText = e!!.message
                MessageUtils().showSnackBar(snackBarErrorText!!, view!!, true)
            }
        }
    }

    private fun saveClassifiedImageToDb(classifiedImage: ClassifiedImage) {
        val classifiedImageDao = databaseInstance!!.classifiedImageDao()
        scope.launch {
            classifiedImageDao.insertClassifiedImage(classifiedImage)
            d(TAG, "${classifiedImageDao.getClassifiedImage.size}")
        }
        activity!!.onBackPressed()
    }

    companion object {
        fun newInstance(imageUri: Uri): DetailsFragment {
            val args = Bundle()
            args.putParcelable(IMAGE_URI_KEY, imageUri)
            val fragment = DetailsFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onRightClicked(position: Int) {

    }

    override fun onWrongClicked(position: Int) {
        Toast.makeText(context, faceInferences[position].faceEmotion, Toast.LENGTH_SHORT).show()
        val fragmentTransaction = activity!!.supportFragmentManager.beginTransaction()
        val previousDialog = activity!!.supportFragmentManager.findFragmentByTag("emotions_dialog")
        if (previousDialog != null) {
            fragmentTransaction.remove(previousDialog)
        }
        fragmentTransaction.addToBackStack(null)
        val emotionDialogFragment = EmotionDialogFragment.newInstance(position)
        emotionDialogFragment.setTargetFragment(this, 0)
        emotionDialogFragment.show(fragmentTransaction, "emotions_dialog")
    }

    override fun onEmotionSelected(emotion: String, position: Int?) {
        Toast.makeText(context!!, emotion, Toast.LENGTH_SHORT).show()
        val faceInference = faceInferences[position!!]
        val newFace = FaceInference(faceInference.faceFilePath, emotion)
        faceInferences[position] = newFace
        Toast.makeText(context!!, faceInferences[position].faceEmotion, Toast.LENGTH_SHORT).show()
        facesAdapter.swapItems(faceInferences)
    }
}
