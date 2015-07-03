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

package com.cyanogenmod.filemanager.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that manage the use of themes inside the application.
 */
public final class ThemeManager {

    private static final String TAG = "ThemeManager"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    /**
     * The permission that MUST have the activity that holds the themes
     */
    public static final String PERMISSION_READ_THEME =
            "com.cyanogenmod.filemanager.permissions.READ_THEME"; //$NON-NLS-1$

    /**
     * The action that MUST have all app that want to register as a theme for this app
     */
    public static final String ACTION_MAIN_THEME =
            "com.cyanogenmod.filemanager.actions.MAIN_THEME"; //$NON-NLS-1$

    /**
     * The category that MUST have all app that want to register as a theme for this app
     */
    public static final String CATEGORY_THEME =
            "com.cyanogenmod.filemanager.categories.THEME"; //$NON-NLS-1$

    private static final String RESOURCE_THEMES_IDS = "themes_ids"; //$NON-NLS-1$
    private static final String RESOURCE_THEMES_NAMES = "themes_names"; //$NON-NLS-1$
    private static final String RESOURCE_THEMES_DESCRIPTIONS = "themes_descriptions"; //$NON-NLS-1$
    private static final String RESOURCE_THEMES_AUTHOR = "themes_author"; //$NON-NLS-1$

    /**
     * @hide
     */
    static Theme mDefaultTheme;
    private static Theme mCurrentTheme;

    /**
     * Method that returns the current theme
     *
     * @param ctx The current context
     * @return Theme The current theme
     */
    public static synchronized Theme getCurrentTheme(Context ctx) {
        if (mCurrentTheme == null) {
            // Use the default theme
            mCurrentTheme = getDefaultTheme(ctx);
        }
        return mCurrentTheme;
    }

    /**
     * Method that returns the default theme
     *
     * @param ctx The current context
     * @return Theme The default theme
     */
    public static synchronized Theme getDefaultTheme(Context ctx) {
        if (mDefaultTheme == null) {
            // Use the default theme
            mDefaultTheme = new Theme();
            String themeSettings = (String)FileManagerSettings.SETTINGS_THEME.getDefaultValue();
            mDefaultTheme.mPackage =
                    themeSettings.substring(0, themeSettings.indexOf(":")); //$NON-NLS-1$
            mDefaultTheme.mId =
                    themeSettings.substring(themeSettings.indexOf(":") + 1); //$NON-NLS-1$
            mDefaultTheme.mName = ctx.getString(R.string.theme_default_name);
            mDefaultTheme.mDescription = ctx.getString(R.string.theme_default_description);
            mDefaultTheme.mAuthor = ctx.getString(R.string.themes_author);

            mDefaultTheme.mContext = ctx;
            mDefaultTheme.mResources = ctx.getResources();
        }
        return mDefaultTheme;
    }

    /**
     * A method for set the current theme that should be applied to the UI.
     *
     * @param ctx The current context (of this application)
     * @param theme The theme of the app (package:id)
     * @return boolean If the theme was set
     */
    public static synchronized boolean setCurrentTheme(Context ctx, String theme) {
        // Retrieve the available themes
        List<Theme> themes = getAvailableThemes(ctx);
        String themePackage = theme.substring(0, theme.indexOf(":")); //$NON-NLS-1$
        String themeId = theme.substring(theme.indexOf(":") + 1); //$NON-NLS-1$
        int cc = themes.size();
        for (int i = 0; i < cc; i++) {
            Theme t = themes.get(i);
            if (t.mPackage.compareTo(themePackage) == 0 && t.mId.compareTo(themeId) == 0) {
                // We have the theme. Save it and notify
                mCurrentTheme = t;

                Intent intent = new Intent(FileManagerSettings.INTENT_THEME_CHANGED);
                intent.putExtra(FileManagerSettings.EXTRA_THEME_PACKAGE, t.mPackage);
                intent.putExtra(FileManagerSettings.EXTRA_THEME_ID, t.mId);
                ctx.sendBroadcast(intent);
                return true;
            }
        }

        // Not found
        return false;
    }

    /**
     * Method that returns the list of available themes for the file manager app.
     *
     * @param ctx The current context
     * @return List<Theme> List of themes
     */
    public static List<Theme> getAvailableThemes(Context ctx) {
        Intent intent = new Intent(ACTION_MAIN_THEME);
        intent.addCategory(CATEGORY_THEME);
        if (DEBUG) {
            intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        }

        // Obtain the list of packages that matches with theme requirements for register as
        // a file manager theme
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> result =
                pm.queryIntentActivities(intent, 0);

        // Read now the information about the themes
        List<Theme> themes = new ArrayList<Theme>();
        int cc = result.size();
        for (int i = 0; i < cc; i++) {
            try {
                ResolveInfo info = result.get(i);
                String appPackage = info.activityInfo.packageName;

                // Check permission for read theme
                String appPermission = info.activityInfo.permission;
                if (appPermission == null || appPermission.compareTo(PERMISSION_READ_THEME) != 0) {
                    Log.w(TAG, String.format(
                            "\"%s\" hasn't READ_THEME permission. Ignored.", //$NON-NLS-1$
                            appPackage));
                    continue;
                }

                Resources appResources = pm.getResourcesForApplication(appPackage);
                if (appResources != null) {
                    // We need the ids, names, descriptions and author of every
                    // theme in the application

                    //- Identifiers
                    int identifiers =
                            appResources.getIdentifier(
                                    RESOURCE_THEMES_IDS,
                                    "array", //$NON-NLS-1$
                                    appPackage);
                    if (identifiers == 0) continue;
                    String[] ids = appResources.getStringArray(identifiers);

                    //- Name
                    int namesId =
                            appResources.getIdentifier(
                                    RESOURCE_THEMES_NAMES,
                                    "array", //$NON-NLS-1$
                                    appPackage);
                    if (namesId == 0) continue;
                    String[] names = appResources.getStringArray(namesId);

                    //- Descriptions
                    int descriptionsId =
                            appResources.getIdentifier(
                                    RESOURCE_THEMES_DESCRIPTIONS,
                                    "array", //$NON-NLS-1$
                                    appPackage);
                    if (descriptionsId == 0) continue;
                    String[] descriptions = appResources.getStringArray(descriptionsId);

                    //- Author
                    int authorId =
                            appResources.getIdentifier(
                                    RESOURCE_THEMES_AUTHOR,
                                    "string", //$NON-NLS-1$
                                    appPackage);
                    if (authorId == 0) continue;
                    String author = appResources.getString(authorId);

                    // Get the resources and the context
                    Context context = ctx.createPackageContext(
                            appPackage, Context.CONTEXT_RESTRICTED);
                    Resources resources = pm.getResourcesForApplication(appPackage);

                    // Add every theme found
                    for (int j = 0; j < names.length; j++) {
                        Theme theme = new Theme();
                        theme.mPackage = appPackage;
                        theme.mId = ids[j];
                        theme.mName = names[j];
                        theme.mDescription = descriptions[j];
                        theme.mAuthor = author;
                        theme.mContext = context;
                        theme.mResources = resources;
                        themes.add(theme);

                        if (DEBUG) {
                            Log.v(TAG, String.format("Found theme: %s", theme)); //$NON-NLS-1$
                        }
                    }
                }

            } catch (Exception e) {/**NON BLOCK**/}
        }

        // Return the themes found
        themes.add(0, getDefaultTheme(ctx));
        return themes;
    }

    /**
     * Method that returns if the theme is the default theme
     *
     * @param theme The theme to check
     * @return boolean Id the current theme is the default theme
     */
    public static boolean isDefaultTheme(Theme theme) {
        String themeSettings = (String)FileManagerSettings.SETTINGS_THEME.getDefaultValue();
        String defaultPackage =
                themeSettings.substring(0, themeSettings.indexOf(":")); //$NON-NLS-1$
        String defaultId =
                themeSettings.substring(themeSettings.indexOf(":") + 1); //$NON-NLS-1$
        return theme.mPackage.compareTo(defaultPackage) == 0 &&
               theme.mId.compareTo(defaultId) == 0;
    }


    /**
     * A class that represents a theme for the file manager app.
     */
    public static class Theme implements Comparable<Theme> {

        String mPackage;
        String mId;
        String mName;
        String mDescription;
        String mAuthor;

        Context mContext;
        Resources mResources;

        /**
         * Constructor of <code>Theme</code>
         */
        Theme() {
            super();
        }

        /**
         * Method that returns the composed identifier
         *
         * @return String The composed identifier
         */
        public String getComposedId() {
            return String.format("%s:%s", this.mPackage, this.mId); //$NON-NLS-1$
        }

        /**
         * Method that returns the package name of the apk that contains the theme
         *
         * @return String The package name of the apk that contains the theme
         */
        public String getPackage() {
            return this.mPackage;
        }

        /**
         * Method that returns the id of the theme inside the themes apk
         *
         * @return String The id of the theme inside the themes apk
         */
        public String getId() {
            return this.mId;
        }

        /**
         * Method that returns the name of the theme
         *
         * @return String The name of the theme
         */
        public String getName() {
            return this.mName;
        }

        /**
         * Method that returns the description of the theme
         *
         * @return String The description of the theme
         */
        public String getDescription() {
            return this.mDescription;
        }

        /**
         * Method that returns the author of the theme
         *
         * @return String The author of the theme
         */
        public String getAuthor() {
            return this.mAuthor;
        }

        /**
         * Method that returns the preview image of the current theme
         *
         * @param ctx The current context
         * @return Drawable The drawable
         */
        public Drawable getPreviewImage(Context ctx) {
            String resId = "theme_preview_drawable"; //$NON-NLS-1$
            if (this.compareTo(ThemeManager.getDefaultTheme(ctx)) != 0) {
                resId = mId + "_theme_preview_drawable"; //$NON-NLS-1$
            }
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                return this.mResources.getDrawable(id);
            }
            return null;
        }

        /**
         * Method that returns the preview image of the current theme
         *
         * @param ctx The current context
         * @return Drawable The drawable
         */
        public Drawable getNoPreviewImage(Context ctx) {
            String resId = mId + "_theme_no_preview_drawable"; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                return this.mResources.getDrawable(id);
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    "theme_no_preview_drawable", //$NON-NLS-1$
                    "drawable", //$NON-NLS-1$
                    mDefaultTheme.mPackage);
            return mDefaultTheme.mResources.getDrawable(id);
        }

        /**
         * Method that sets the base theme of the current context
         *
         * @param ctx The current context
         * @param overlay Indicates if the theme should be the overlay one
         */
        public void setBaseTheme(Context ctx, boolean overlay) {
            String resId = mId + "_base_theme"; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "string", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                String base = this.mResources.getString(id, "material_light"); //$NON-NLS-1$
                int themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                                R.style.FileManager_Theme_Material :
                                R.style.FileManager_Theme_Material_Light;
                if (overlay) {
                    themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                            R.style.FileManager_Theme_Material_Overlay :
                            R.style.FileManager_Theme_Material_Light_Overlay;
                }
                ctx.setTheme(themeId);
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    "base_theme", "string", mDefaultTheme.mPackage); //$NON-NLS-1$ //$NON-NLS-2$
            String base = this.mResources.getString(id, "material_light"); //$NON-NLS-1$
            int themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                            R.style.FileManager_Theme_Material :
                            R.style.FileManager_Theme_Material_Light;
            if (overlay) {
                themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                        R.style.FileManager_Theme_Material_Overlay :
                        R.style.FileManager_Theme_Material_Light_Overlay;
            }
            ctx.setTheme(themeId);
        }

        /**
         * Method that sets the base theme of the current context with no actionbar
         *
         * @param ctx
         */
        public void setBaseThemeNoActionBar(Context ctx) {
            String resId = mId + "_base_theme"; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "string", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                String base = this.mResources.getString(id, "material_light"); //$NON-NLS-1$
                int themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                        R.style.FileManager_Theme_Material_NoActionBar :
                        R.style.FileManager_Theme_Material_Light_NoActionBar;
                ctx.setTheme(themeId);
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    "base_theme", "string", mDefaultTheme.mPackage); //$NON-NLS-1$ //$NON-NLS-2$
            String base = this.mResources.getString(id, "material_light"); //$NON-NLS-1$
            int themeId = base.compareTo("material") == 0 ? //$NON-NLS-1$
                    R.style.FileManager_Theme_Material_NoActionBar :
                    R.style.FileManager_Theme_Material_Light_NoActionBar;
            ctx.setTheme(themeId);
        }

        /**
         * Method that sets the titlebar drawable of an ActionBar
         *
         * @param ctx The current context
         * @param actionBar The action bar
         * @param resource The string resource
         */
        public void setTitlebarDrawable(Context ctx, ActionBar actionBar, String resource) {
            String resId = this.mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                actionBar.setBackgroundDrawable(this.mResources.getDrawable(id));
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "drawable", mDefaultTheme.mPackage); //$NON-NLS-1$
            actionBar.setBackgroundDrawable(mDefaultTheme.mResources.getDrawable(id));
        }

        /**
         * Method that sets the background drawable of a View
         *
         * @param ctx The current context
         * @param view The view which apply the style
         * @param resource The string resource
         */
        public void setBackgroundDrawable(Context ctx, View view, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                view.setBackground(this.mResources.getDrawable(id));
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "drawable", mDefaultTheme.mPackage); //$NON-NLS-1$
            view.setBackground(mDefaultTheme.mResources.getDrawable(id));
        }

        /**
         * Method that sets the image drawable of a ImageView
         *
         * @param ctx The current context
         * @param view The view which apply the style
         * @param resource The string resource
         */
        public void setImageDrawable(Context ctx, ImageView view, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                view.setImageDrawable(this.mResources.getDrawable(id));
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "drawable", mDefaultTheme.mPackage); //$NON-NLS-1$
            view.setImageDrawable(mDefaultTheme.mResources.getDrawable(id));
        }

        /**
         * Method that returns the resource id of a drawable in the current theme
         *
         * @param ctx The current context
         * @param resource The string resource
         * @return int The resource id
         */
        public int getResourceId(Context ctx, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                return id;
            }

            // Default theme
            return mDefaultTheme.mResources.getIdentifier(
                    resource, "drawable", mDefaultTheme.mPackage); //$NON-NLS-1$
        }

        /**
         * Method that returns an image drawable of the current theme
         *
         * @param ctx The current context
         * @param resource The string resource
         * @return Drawable The drawable
         */
        public Drawable getDrawable(Context ctx, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "drawable", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                return this.mResources.getDrawable(id);
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "drawable", mDefaultTheme.mPackage); //$NON-NLS-1$
            return mDefaultTheme.mResources.getDrawable(id);
        }

        /**
         * Method that sets the text color of a TextView
         *
         * @param ctx The current context
         * @param view The view which apply the style
         * @param resource The string resource
         */
        public void setTextColor(Context ctx, TextView view, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "color", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                view.setTextColor(this.mResources.getColor(id));
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "color", mDefaultTheme.mPackage); //$NON-NLS-1$
            view.setTextColor(mDefaultTheme.mResources.getColor(id));
        }

        /**
         * Method that returns a color from the theme
         *
         * @param ctx The current context
         * @param resource The string resource
         * @return int The color reference
         */
        public int getColor(Context ctx, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "color", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                return this.mResources.getColor(id);
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "color", mDefaultTheme.mPackage); //$NON-NLS-1$
            return mDefaultTheme.mResources.getColor(id);
        }

        /**
         * Method that sets the background color of a View
         *
         * @param ctx The current context
         * @param view The view which apply the style
         * @param resource The string resource
         */
        public void setBackgroundColor(Context ctx, View view, String resource) {
            String resId = mId + "_" + resource; //$NON-NLS-1$
            int id = this.mResources.getIdentifier(resId, "color", this.mPackage); //$NON-NLS-1$
            if (id != 0) {
                view.setBackgroundColor(this.mResources.getColor(id));
                return;
            }

            // Default theme
            id = mDefaultTheme.mResources.getIdentifier(
                    resource, "color", mDefaultTheme.mPackage); //$NON-NLS-1$
            view.setBackgroundColor(mDefaultTheme.mResources.getColor(id));
        }

        /**
         * Method that set the style of the dialog.
         *
         * @param ctx The current context
         * @param dialog The dialog which apply the style
         */
        @SuppressWarnings("deprecation")
        public void setDialogStyle(Context ctx, AlertDialog dialog) {
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON1));
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON2));
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON3));
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON_NEGATIVE));
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON_NEUTRAL));
            applyButtonStyle(ctx, dialog.getButton(DialogInterface.BUTTON_POSITIVE));
        }

        /**
         * Method that apply the current style to a button
         *
         * @param ctx The current context
         * @param button The button which apply the style
         */
        private void applyButtonStyle(Context ctx, Button button) {
            if (button != null) {
                setTextColor(ctx, button, "dialog_text_color"); //$NON-NLS-1$
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Theme [Package=" + this.mPackage + //$NON-NLS-1$
                    ", Id=" + this.mId + //$NON-NLS-1$
                    ", Name=" + this.mName //$NON-NLS-1$
                    + ", Description=" + this.mDescription //$NON-NLS-1$
                    + ", Author=" + this.mAuthor + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Theme another) {
            return getComposedId().compareTo(another.getComposedId());
        }
    }

}
