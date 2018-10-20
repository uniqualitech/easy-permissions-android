package com.libraryproject

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.FileProvider
import android.util.Log

import com.libraryproject.permissionutils.PermissionHandler
import com.libraryproject.permissionutils.PermissionInterface

import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.app.Activity.RESULT_OK

class GalleryCameraPhotoHandlerKotlin : Fragment() {

    private var photoHandler: PhotoHandler? = null
    private var pickPhotoCallback: PickPhotoCallback? = null
    private var tempUriCapture: Uri? = null
    private var weakReference: WeakReference<FragmentActivity>? = null
    private val PICK_GALLERY = 1005
    private val PICK_CAMERA = 1006
    private val DIRECTORY_NAME = ".PhotoCamera"

    private val weakActivity: FragmentActivity?
        get() = weakReference?.get()

    private val outputMediaFileUri: Uri?
        get() = weakActivity?.let {
            FileProvider.getUriForFile(it,
                    BuildConfig.APPLICATION_ID + ".provider",
                    outputMediaFile)
        }

    private val outputMediaFile: File
        get() {
            val mediaStorageDir = File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    DIRECTORY_NAME)
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.e(DIRECTORY_NAME, "Oops! Failed create "
                            + DIRECTORY_NAME + " directory")
                    return File("")
                }
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale("en")).format(Date())
            val mediaFile: File
            mediaFile = File(mediaStorageDir.path + File.separator
                    + "IMG_" + timeStamp + ".jpg")

            return mediaFile
        }

    enum class PhotoHandler {
        GALLERY, CAMERA
    }

    interface PickPhotoCallback {
        fun photoPath(path: String, photoHandler: PhotoHandler?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weakReference = WeakReference<FragmentActivity>(activity)
        pickPhoto()
    }

    private fun pickPhoto() {
        when (photoHandler) {
            PhotoHandler.GALLERY -> chooseGallery()
            PhotoHandler.CAMERA -> chooseCamera()
        }
    }

    private fun getGalleryCameraPhoto(fragmentActivity: FragmentActivity, photoHandler: PhotoHandler?, pickPhotoCallback: PickPhotoCallback?): GalleryCameraPhotoHandlerKotlin {

        val galleryCameraPhotoHandler: GalleryCameraPhotoHandlerKotlin? = fragmentActivity.supportFragmentManager.findFragmentByTag(GalleryCameraPhotoHandlerKotlin::class.java.name) as GalleryCameraPhotoHandlerKotlin

        galleryCameraPhotoHandler?.let {
            it.pickPhotoCallback = pickPhotoCallback
            it.photoHandler = photoHandler
            it.pickPhoto()
        } ?: let {
            val handler = GalleryCameraPhotoHandlerKotlin()
            handler.pickPhotoCallback = pickPhotoCallback
            handler.photoHandler = photoHandler
            fragmentActivity.supportFragmentManager
                    .beginTransaction()
                    .add(handler, GalleryCameraPhotoHandlerKotlin::class.java.name)
                    .commitAllowingStateLoss()
        }
        return this
    }


    private fun chooseGallery() {
        PermissionHandler.Builder().setContext(weakActivity)
                .setAllPermission(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .setPermissionCallback(object : PermissionInterface {
                    override fun permissionGranted() {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, PICK_GALLERY)
                    }

                    override fun permissionDenied() {
                        removeFragment()
                    }
                }).build()
    }

    private fun chooseCamera() {

        PermissionHandler.Builder().setContext(weakActivity)
                .setAllPermission(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA))
                .setPermissionCallback(object : PermissionInterface {
                    override fun permissionGranted() {

                        tempUriCapture = outputMediaFileUri

                        tempUriCapture?.let {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUriCapture)
                            startActivityForResult(intent, PICK_CAMERA)
                        } ?: removeFragment()

                    }

                    override fun permissionDenied() {
                        removeFragment()
                    }
                }).build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {

            val fileProfilePic: File

            if (requestCode == PICK_GALLERY) {

                if (data != null) {
                    val imagePath: String? = UriHelper.getPath(weakActivity, data.data)

                    if (imagePath != null && imagePath.isNotEmpty()) {

                        fileProfilePic = File(imagePath)

                        pickPhotoCallback?.photoPath(fileProfilePic.absolutePath, photoHandler)

                    }
                }
            } else if (requestCode == PICK_CAMERA) {

                if (tempUriCapture != null && tempUriCapture?.path != null) {

                    val capturePathName = tempUriCapture!!.toString().substring(tempUriCapture!!.toString().lastIndexOf("/") + 1, tempUriCapture!!.toString().length)

                    fileProfilePic = File(File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), DIRECTORY_NAME), capturePathName)

                    pickPhotoCallback?.photoPath(fileProfilePic.absolutePath, photoHandler)

                }

            }
        }
        removeFragment()
    }

    private fun removeFragment() {
        fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()
    }

    class Builder {
        private var fragmentActivity: FragmentActivity? = null
        private var photoHandler: PhotoHandler? = null
        private var pickPhotoCallback: PickPhotoCallback? = null

        fun setContext(fragmentActivity: FragmentActivity): Builder {
            this.fragmentActivity = fragmentActivity
            return this
        }

        fun setPhotoHandler(photoHandler: PhotoHandler): Builder {
            this.photoHandler = photoHandler
            return this
        }

        fun setPhotoCallback(pickPhotoCallback: PickPhotoCallback): Builder {
            this.pickPhotoCallback = pickPhotoCallback
            return this
        }

        fun build() {
            if (fragmentActivity == null) {
                throw NullPointerException("Context must not be null")
            }
            GalleryCameraPhotoHandlerKotlin().getGalleryCameraPhoto(fragmentActivity!!, photoHandler, pickPhotoCallback)
        }

    }
}
