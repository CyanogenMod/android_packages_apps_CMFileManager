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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.compat.CompatUtils;

/**
 * A helper class with useful methods for deal with android.
 */
public final class AndroidHelper {

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
        // TODO This need to be improved, checking if the app is really with the platform signature
        try {
            // For now only check that the app is installed in system directory
            PackageManager pm = ctx.getPackageManager();
            String appDir = pm.getApplicationInfo(ctx.getPackageName(), 0).sourceDir;
            String systemDir = ctx.getString(R.string.system_dir);
            return appDir.startsWith(systemDir);

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e, true, false);
        }
        return false;
    }

    public static boolean hasSupportForMultipleUsers(Context context) {
        return CompatUtils.getClsHideBooleanStaticMethod(UserManager.class, "supportsMultipleUsers", true);
//        return UserManager.supportsMultipleUsers();
    }

    public static boolean isUserOwner() {
        return CompatUtils.getClsHideIntStaticMethod(UserHandle.class, "myUserId", Process.myUid()) == CompatUtils.getClsHideIntStaticField(UserHandle.class, "USER_OWNER", 0);
    }

    public static boolean isSecondaryUser(Context context) {
        return AndroidHelper.hasSupportForMultipleUsers(context)
                && !AndroidHelper.isUserOwner();
    }
}
