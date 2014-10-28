/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.util;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;

import com.android.internal.util.HexDump;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * A helper class with useful methods for deal with android.
 */
public final class AndroidHelper {

    private static Boolean sIsAppPlatformSigned;

    /**
     * Method that returns if the device is a tablet
     *
     * @param ctx The current context
     * @return boolean If device is a table
     */
    public static boolean isTablet(Context ctx) {
        Configuration configuration = ctx.getResources().getConfiguration();
        return (configuration.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
    * Method that returns if an option menu has to be displayed
    *
    * @param ctx The current context
    * @return boolean If an option menu has to be displayed
    */
    public static boolean showOptionsMenu(Context ctx) {
        // Show overflow button?
        return !ViewConfiguration.get(ctx).hasPermanentMenuKey();
    }

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param ctx The current context
     * @param dp A value in dp (Device independent pixels) unit
     * @return float A float value to represent Pixels equivalent to dp according to device
     */
    public static float convertDpToPixel(Context ctx, float dp) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * This method converts device specific pixels to device independent pixels.
     *
     * @param ctx The current context
     * @param px A value in px (pixels) unit
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(Context ctx, float px) {
        Resources resources = ctx.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);

    }

    /**
     * Method that check if the app is signed with the platform signature
     *
     * @param ctx The current context
     * @return boolean If the app is signed with the platform signature
     */
    public static boolean isAppPlatformSignature(Context ctx) {
        if (sIsAppPlatformSigned == null) {
            try {
                // First check that the app is installed as a system app
                PackageManager pm = ctx.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(ctx.getPackageName(),
                        PackageManager.GET_SIGNATURES);
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    sIsAppPlatformSigned = Boolean.FALSE;
                } else {
                    final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                    final CertificateFactory cf = CertificateFactory.getInstance("X.509");

                    // Get signature of the current package
                    PackageInfo info = pm.getPackageInfo(ctx.getPackageName(),
                            PackageManager.GET_SIGNATURES);
                    Signature[] signatures = info.signatures;
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(
                            new ByteArrayInputStream(signatures[0].toByteArray()));
                    sha1.update(cert.getEncoded());
                    String appHash = HexDump.toHexString(sha1.digest());

                    // Get the signature of the settings package (which is supposed to be signed)
                    info = pm.getPackageInfo("com.android.settings",
                            PackageManager.GET_SIGNATURES);
                    signatures = info.signatures;
                    cert = (X509Certificate) cf.generateCertificate(
                            new ByteArrayInputStream(signatures[0].toByteArray()));
                    sha1.update(cert.getEncoded());
                    String systemHash = HexDump.toHexString(sha1.digest());

                    // Is platform signed?
                    sIsAppPlatformSigned = appHash.equals(systemHash);
                }

            } catch (NameNotFoundException e) {
                sIsAppPlatformSigned = Boolean.FALSE;
            } catch (GeneralSecurityException e) {
                sIsAppPlatformSigned = Boolean.FALSE;
            }
        }
        return sIsAppPlatformSigned.booleanValue();
    }

    public static boolean hasSupportForMultipleUsers(Context context) {
        return UserManager.supportsMultipleUsers();
    }

    public static boolean isUserOwner() {
        return UserHandle.myUserId() == UserHandle.USER_OWNER;
    }

    public static boolean isSecondaryUser(Context context) {
        return AndroidHelper.hasSupportForMultipleUsers(context)
                && !AndroidHelper.isUserOwner();
    }

    public static long getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem;
    }
}
