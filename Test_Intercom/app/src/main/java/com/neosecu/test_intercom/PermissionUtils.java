package com.neosecu.test_intercom;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.io.IOException;

/**
 * Created by OpenFace on 2017-02-09.
 */

public class PermissionUtils {
    public static int getVersion () {
        return Build.VERSION.SDK_INT ;
    }

    // Permission denied check and grant
    public static boolean isPermissionDenied (Context context) {
        return (PermissionUtils.isPermissionChecked()
                && (!PermissionUtils.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO))) ;
    }

    // 권한 확인 필요 할 경우
    private static boolean isPermissionChecked() {
        return Build.VERSION_CODES.M <= getVersion() ;
    }

    // want to Permission granted state
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isPermissionGranted (Context context, String permission) {
        return (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) ;
    }
}
