package io.budge.emodetect.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.budge.emodetect.R
import io.budge.emodetect.ui.camera.CameraFragment

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS =
    arrayOf(Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE)

class Camera2Activity : AppCompatActivity() {


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (allPermissionsGranted()) {
            openCameraFragment()
        }
        else ActivityCompat.requestPermissions(this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openCameraFragment()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCameraFragment(){
        supportFragmentManager
            .beginTransaction()
            .add(R.id.view_container, CameraFragment.newInstance())
            .commit()
    }
}
