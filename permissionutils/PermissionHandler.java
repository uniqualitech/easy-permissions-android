package com.libraryproject.permissionutils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

/**
 * Created by iRoid on 06-Mar-17.
 */
public class PermissionHandler extends Fragment {

    private PermissionInterface permissionInterface;
    private final int REQUEST_PERMISSION = 5000;
    private WeakReference<Fragment> fragmentWeakReference;
    private String[] permission;

    private Fragment getWeakFragment() {
        return fragmentWeakReference.get();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentWeakReference = new WeakReference<Fragment>(this);
        checkPermission(permission);
    }

    private PermissionHandler checkPermissionHandler(FragmentActivity fragmentActivity, String[] permission, PermissionInterface permissionInterface) {

        PermissionHandler permissionHandler = (PermissionHandler) fragmentActivity.getSupportFragmentManager().findFragmentByTag(PermissionHandler.class.getName());
        if (permissionHandler != null) {
            permissionHandler.permissionInterface = permissionInterface;
            permissionHandler.checkPermission(permission);
        } else {
            permissionHandler = new PermissionHandler();
            permissionHandler.permissionInterface = permissionInterface;
            permissionHandler.permission = permission;
            fragmentActivity.getSupportFragmentManager()
                    .beginTransaction()
                    .add(permissionHandler, PermissionHandler.class.getName())
                    .commitAllowingStateLoss();
        }
        return this;
    }

    private void checkPermission(final String[] permission) {
        if (!checkSelfPermission(permission)) {
            requestPermissions(permission, REQUEST_PERMISSION);
            return;
        }
        removeFragment();
        if (permissionInterface != null) {
            permissionInterface.permissionGranted();
        }
    }

    private boolean checkSelfPermission(String[] permission) {

        boolean isAllPermissionAccepted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            for (String aPermission : permission) {

                if (getWeakFragment().getActivity() != null) {
                    if (getWeakFragment().getActivity().checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionAccepted = false;
                        break;
                    }

                } else {
                    isAllPermissionAccepted = false;
                }
            }
        }
        return isAllPermissionAccepted;
    }

    public boolean shouldShowRequestPermissionRationaleDialog(@NonNull String permission) {
        return shouldShowRequestPermissionRationale(permission);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {

            boolean isAllPermissionAccepted = true;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    isAllPermissionAccepted = false;
                    if (!shouldShowRequestPermissionRationaleDialog(permissions[i])) {
                        showPermissionDialog();
                        return;
                    }
                }
            }
            removeFragment();
            if (permissionInterface != null) {
                if (isAllPermissionAccepted) {
                    permissionInterface.permissionGranted();
                } else {
                    permissionInterface.permissionDenied();
                }
            }
        }
    }

    private void removeFragment() {
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }

    private Activity getBaseActivity() {
        return getWeakFragment().getActivity();
    }

    private void showPermissionDialog() {

        new AlertDialog.Builder(getBaseActivity()).setTitle("Permission")
                .setMessage("Please enable permission")
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        removeFragment();
                        if (permissionInterface != null) {
                            permissionInterface.permissionDenied();
                        }
                    }
                })
                .setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getBaseActivity().getPackageName(), null);
                            intent.setData(uri);
                            getBaseActivity().startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        removeFragment();
                        if (permissionInterface != null) {
                            permissionInterface.permissionDenied();
                        }

                    }
                }).show();

    }

    public static class Builder {
        private FragmentActivity fragmentActivity;
        private String[] permission;
        private PermissionInterface permissionInterface;

        public PermissionHandler.Builder setContext(FragmentActivity fragmentActivity) {
            this.fragmentActivity = fragmentActivity;
            return this;
        }

        public PermissionHandler.Builder setAllPermission(String[] permission) {
            this.permission = permission;
            return this;
        }

        public PermissionHandler.Builder setPermissionCallback(PermissionInterface permissionInterface) {
            this.permissionInterface = permissionInterface;
            return this;
        }

        public void build() {
            if (fragmentActivity == null) {
                throw new NullPointerException("Context must not be null");
            } else if (permission == null || permission.length == 0) {
                throw new NullPointerException("permission must not be null");
            }
            new PermissionHandler().checkPermissionHandler(fragmentActivity, permission, permissionInterface);
        }

    }

}
