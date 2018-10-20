package com.libraryproject;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.libraryproject.permissionutils.PermissionHandler;
import com.libraryproject.permissionutils.PermissionInterface;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class GalleryCameraPhotoHandler extends Fragment {

    private PhotoHandler photoHandler;
    private PickPhotoCallback pickPhotoCallback;
    private Uri temp_uri_capture = null;
    private WeakReference<FragmentActivity> weakReference;
    private final int PICK_GALLERY = 1005;
    private final int PICK_CAMERA = 1006;
    private final String DIRECTORY_NAME = ".PhotoCamera";

    public enum PhotoHandler {
        GALLERY, CAMERA
    }

    public interface PickPhotoCallback {
        void photoPath(String path, PhotoHandler photoHandler);
    }

    private FragmentActivity getWeakActivity() {
        return weakReference.get();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weakReference = new WeakReference<>(getActivity());
        pickPhoto();
    }

    private void pickPhoto() {
        if (photoHandler == PhotoHandler.GALLERY) {
            chooseGallery();
        } else if (photoHandler == PhotoHandler.CAMERA) {
            chooseCamera();
        }
    }

    private GalleryCameraPhotoHandler getGalleryCameraPhoto(FragmentActivity fragmentActivity, PhotoHandler photoHandler, PickPhotoCallback pickPhotoCallback) {

        GalleryCameraPhotoHandler galleryCameraPhotoHandler = (GalleryCameraPhotoHandler) fragmentActivity.getSupportFragmentManager().findFragmentByTag(GalleryCameraPhotoHandler.class.getName());
        if (galleryCameraPhotoHandler != null) {
            galleryCameraPhotoHandler.pickPhotoCallback = pickPhotoCallback;
            galleryCameraPhotoHandler.photoHandler = photoHandler;
            galleryCameraPhotoHandler.pickPhoto();
        } else {
            galleryCameraPhotoHandler = new GalleryCameraPhotoHandler();
            galleryCameraPhotoHandler.pickPhotoCallback = pickPhotoCallback;
            galleryCameraPhotoHandler.photoHandler = photoHandler;
            fragmentActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(galleryCameraPhotoHandler, GalleryCameraPhotoHandler.class.getName())
                    .commitAllowingStateLoss();
        }
        return this;
    }


    private void chooseGallery() {
        new PermissionHandler.Builder().setContext(getWeakActivity())
                .setAllPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
                .setPermissionCallback(new PermissionInterface() {
                    @Override
                    public void permissionGranted() {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        startActivityForResult(galleryIntent, PICK_GALLERY);
                    }

                    @Override
                    public void permissionDenied() {
                        removeFragment();
                    }
                }).build();
    }

    private void chooseCamera() {

        new PermissionHandler.Builder().setContext(getWeakActivity())
                .setAllPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA})
                .setPermissionCallback(new PermissionInterface() {
                    @Override
                    public void permissionGranted() {
                        temp_uri_capture = getOutputMediaFileUri();

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, temp_uri_capture);
                        startActivityForResult(intent, PICK_CAMERA);
                    }

                    @Override
                    public void permissionDenied() {
                        removeFragment();
                    }
                }).build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            File fileProfilePic;

            if (requestCode == PICK_GALLERY) {

                if (data != null) {
                    String image_path;

                    image_path = UriHelper.getPath(getWeakActivity(), data.getData());

                    if (image_path != null && image_path.length() > 0) {

                        fileProfilePic = new File(image_path);

                        if (pickPhotoCallback != null) {
                            pickPhotoCallback.photoPath(fileProfilePic.getAbsolutePath(), photoHandler);
                        }
                    }
                }
            } else if (requestCode == PICK_CAMERA) {

                if (temp_uri_capture != null && temp_uri_capture.getPath() != null) {

                    String capture_path_name = temp_uri_capture.toString().substring(temp_uri_capture.toString().lastIndexOf("/") + 1, temp_uri_capture.toString().length());

                    fileProfilePic = new File(new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                            , DIRECTORY_NAME), capture_path_name);

                    if (pickPhotoCallback != null) {
                        pickPhotoCallback.photoPath(fileProfilePic.getAbsolutePath(), photoHandler);
                    }

                }

            }
        }
        removeFragment();
    }

    private void removeFragment() {
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    private Uri getOutputMediaFileUri() {
        return FileProvider.getUriForFile(getWeakActivity(),
                BuildConfig.APPLICATION_ID + ".provider",
                getOutputMediaFile());
    }

    private File getOutputMediaFile() {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(DIRECTORY_NAME, "Oops! Failed create "
                        + DIRECTORY_NAME + " directory");
                return new File("");
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                new Locale("en")).format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    public static class Builder {
        private FragmentActivity fragmentActivity;
        private PhotoHandler photoHandler;
        private PickPhotoCallback pickPhotoCallback;

        public Builder setContext(FragmentActivity fragmentActivity) {
            this.fragmentActivity = fragmentActivity;
            return this;
        }

        public Builder setPhotoHandler(PhotoHandler photoHandler) {
            this.photoHandler = photoHandler;
            return this;
        }

        public Builder setPhotoCallback(PickPhotoCallback pickPhotoCallback) {
            this.pickPhotoCallback = pickPhotoCallback;
            return this;
        }

        public void build() {
            if (fragmentActivity == null) {
                throw new NullPointerException("Context must not be null");
            }
            new GalleryCameraPhotoHandler().getGalleryCameraPhoto(fragmentActivity, photoHandler, pickPhotoCallback);
        }

    }
}
