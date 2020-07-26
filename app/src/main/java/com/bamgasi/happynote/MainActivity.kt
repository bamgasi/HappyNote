package com.bamgasi.happynote

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var newVersion: Long = 0
    var toolbarImageCount: Long = 0
    val remoteConfig = Firebase.remoteConfig
    lateinit var firebaseStorage: FirebaseStorage
    val toolbarImageList = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getRemoteConfig()
    }

    private fun getRemoteConfig() {
        val configSettings = remoteConfigSettings {
            //minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    checkVersion()
                }
    }

    private fun checkVersion() {
        newVersion = remoteConfig.getLong("new_version")
        toolbarImageCount = remoteConfig.getLong("toolbar_image_count")
        Log.e(TAG, "new_version: ${newVersion}")
        Log.e(TAG, "toolbar_image_count: ${toolbarImageCount}")

        val pi = packageManager.getPackageInfo(packageName, 0)
        var appVersion: Long? = null

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            appVersion = pi.longVersionCode
        }else{
            appVersion = pi.versionCode.toLong()
        }
        Log.e(TAG, "앱버전: ${appVersion}")

        if (newVersion > appVersion) {
            updateDialog()
            return
        }

        checkToolbarImage()

    }

    private fun checkToolbarImage() {
        val file = getExternalFilesDir("${Environment.DIRECTORY_PICTURES}/toolbar_images")
        if (file != null && !file.isDirectory) file.mkdir()

        if (file != null) {
            toolbarImageList.addAll(file.listFiles())
            Log.e(TAG, "toolbarImageList 사이즈 1: ${toolbarImageList.size}")
            if (toolbarImageList.size < toolbarImageCount) {
                firebaseStorage= FirebaseStorage.getInstance()
                val ref = firebaseStorage.reference
                downloadToolbarImage(ref)
            }
        }
    }

     private fun downloadToolbarImage(storageRef: StorageReference) {
        Log.e(TAG, "toolbarImageList 사이즈 2: ${toolbarImageList.size}")
        if (toolbarImageList.size >= toolbarImageCount) return

        val fileName = "toolbar_${toolbarImageList.size}.jpg"
        val fileDir = getExternalFilesDir("${Environment.DIRECTORY_PICTURES}/toolbar_images")
        val downloadRef = storageRef.child("toolbar_images/toolbar_${toolbarImageList.size}.jpg")

        val islandRef = downloadRef
        val localFile = File(fileDir, fileName)
        islandRef.getFile(localFile).addOnSuccessListener {
            Log.e(TAG, "다운로드: ${localFile.name}")
            toolbarImageList.add(localFile)
            if (toolbarImageList.size < toolbarImageCount) {
                downloadToolbarImage(storageRef)
            }
        }.addOnFailureListener {
            // Handle any errors
        }
    }

    private fun updateDialog() {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.Theme_AppCompat_Light_Dialog_Alert))
        builder.setTitle("업데이트 알림")
        builder.setMessage("최신버전이 등록되었습니다.\n업데이트 진행하세요.")
                .setCancelable(false)
                .setPositiveButton("업데이트") { _, _ ->
                    /*val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse("market://details?id=com.bamgasi.happynote"))
                    startActivity(intent)*/
                    Toast.makeText(applicationContext, "업데이트 버튼 클릭됨", Toast.LENGTH_SHORT).show()
                }
        builder.show()
    }
}