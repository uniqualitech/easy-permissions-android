package com.libraryproject.permissionutils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

import java.lang.ref.WeakReference

/**
 * Created by iRoid on 06-Mar-17.
 */
class PermissionHandlerKotlin : Fragment() {

    private var permissionInterface: PermissionInterface? = null
    private val REQUEST_PERMISSION = 5000
    private var fragmentWeakReference: WeakReference<Fragment>? = null
    private lateinit var permission: Array<String>

    private val weakFragment: Fragment?
        get() = fragmentWeakReference?.get()

    private val baseActivity: Activity?
        get() = weakFragment?.activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentWeakReference = WeakReference(this)
        checkPermission(permission)
    }

    private fun checkPermissionHandler(fragmentActivity: FragmentActivity, permission: Array<String>, permissionInterface: PermissionInterface?): PermissionHandlerKotlin {

        val permissionHandler: PermissionHandlerKotlin? = fragmentActivity.supportFragmentManager.findFragmentByTag(PermissionHandlerKotlin::class.java.name) as PermissionHandlerKotlin

        permissionHandler?.let {
            it.permissionInterface = permissionInterface
            it.checkPermission(permission)
        } ?: let {
            val handler = PermissionHandlerKotlin()
            handler.permissionInterface = permissionInterface
            handler.permission = permission
            fragmentActivity.supportFragmentManager
                    .beginTransaction()
                    .add(handler, PermissionHandlerKotlin::class.java.name)
                    .commitAllowingStateLoss()
        }

        return this
    }

    private fun checkPermission(permission: Array<String>) {
        if (!checkSelfPermission(permission)) {
            requestPermissions(permission, REQUEST_PERMISSION)
            return
        }
        removeFragment()
        permissionInterface?.permissionGranted()
    }

    private fun checkSelfPermission(permission: Array<String>): Boolean {

        var isAllPermissionAccepted = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            for (aPermission in permission) {

                if (weakFragment?.activity != null) {
                    if (weakFragment?.activity?.checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionAccepted = false
                        break
                    }

                } else {
                    isAllPermissionAccepted = false
                }
            }
        }
        return isAllPermissionAccepted
    }

    private fun shouldShowRequestPermissionRationaleDialog(permission: String): Boolean {
        return shouldShowRequestPermissionRationale(permission)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION) {

            var isAllPermissionAccepted = true

            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    isAllPermissionAccepted = false
                    if (!shouldShowRequestPermissionRationaleDialog(permissions[i])) {
                        showPermissionDialog()
                        return
                    }
                }
            }
            removeFragment()
            if (isAllPermissionAccepted) {
                permissionInterface?.permissionGranted()
            } else {
                permissionInterface?.permissionDenied()
            }
        }
    }

    private fun removeFragment() {
        fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()
    }

    private fun showPermissionDialog() {

        AlertDialog.Builder(baseActivity).setTitle("Permission")
                .setMessage("Please enable permission")
                .setOnDismissListener {
                    removeFragment()
                    permissionInterface?.permissionDenied()
                }
                .setPositiveButton("Open Setting") { _, _ ->
                    try {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", baseActivity?.packageName, null)
                        intent.data = uri
                        baseActivity?.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    removeFragment()
                    permissionInterface?.permissionDenied()
                }.show()

    }

    class Builder {
        private var fragmentActivity: FragmentActivity? = null
        private var permission: Array<String>? = null
        private var permissionInterface: PermissionInterface? = null

        fun setContext(fragmentActivity: FragmentActivity): PermissionHandlerKotlin.Builder {
            this.fragmentActivity = fragmentActivity
            return this
        }

        fun setAllPermission(permission: Array<String>): PermissionHandlerKotlin.Builder {
            this.permission = permission
            return this
        }

        fun setPermissionCallback(permissionInterface: PermissionInterface): PermissionHandlerKotlin.Builder {
            this.permissionInterface = permissionInterface
            return this
        }

        fun build() {
            when {
                fragmentActivity == null -> throw NullPointerException("Context must not be null")
                permission == null -> throw NullPointerException("permission must not be null")
                permission?.isEmpty() == true -> throw NullPointerException("permission must not be null")
                else -> PermissionHandlerKotlin().checkPermissionHandler(fragmentActivity!!, permission!!, permissionInterface)
            }
        }

    }

}
