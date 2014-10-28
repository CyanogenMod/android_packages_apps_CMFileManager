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

package com.cyanogenmod.filemanager.preferences;

import com.cyanogenmod.filemanager.util.FileHelper;


/**
 * The enumeration of settings of FileManager application.
 */
public enum FileManagerSettings {
    /**
     * Whether is the first use of the application
     * @hide
     */
    SETTINGS_FIRST_USE("cm_filemanager_first_use", Boolean.TRUE),  //$NON-NLS-1$

    /**
     * The access mode to use
     * @hide
     */
    SETTINGS_ACCESS_MODE("cm_filemanager_access_mode", AccessMode.SAFE), //$NON-NLS-1$

    /**
     * When secondary users will have a chrooted console
     * @hide
     */
    SETTINGS_RESTRICT_SECONDARY_USERS_ACCESS("cm_filemanager_restrict_secondary_users_access",
            Boolean.TRUE), //$NON-NLS-1$

    /**
     * The initial directory to be used.
     * @hide
     */
    SETTINGS_INITIAL_DIR("cm_filemanager_initial_dir", FileHelper.ROOT_DIRECTORY), //$NON-NLS-1$

    /**
     * The view mode to use (simple, details, or icons).
     * @hide
     */
    SETTINGS_LAYOUT_MODE("cm_filemanager_layout_mode", NavigationLayoutMode.DETAILS), //$NON-NLS-1$
    /**
     * The sort mode to use (name or data, ascending or descending).
     * @hide
     */
    SETTINGS_SORT_MODE("cm_filemanager_sort_mode", NavigationSortMode.NAME_ASC), //$NON-NLS-1$

    /**
     * When to sort the directories before the files.
     * @hide
     */
    SETTINGS_SHOW_DIRS_FIRST("cm_filemanager_show_dirs_first", Boolean.TRUE), //$NON-NLS-1$
    /**
     * When to show the hidden files.
     * @hide
     */
    SETTINGS_SHOW_HIDDEN("cm_filemanager_show_hidden", Boolean.TRUE), //$NON-NLS-1$
    /**
     * When to show the system files.
     * @hide
     */
    SETTINGS_SHOW_SYSTEM("cm_filemanager_show_system", Boolean.TRUE), //$NON-NLS-1$
    /**
     * When to show the symlinks files.
     * @hide
     */
    SETTINGS_SHOW_SYMLINKS("cm_filemanager_show_symlinks", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to use case sensitive comparison in sorting of files
     * @hide
     */
    SETTINGS_CASE_SENSITIVE_SORT("cm_filemanager_case_sensitive_sort", Boolean.FALSE), //$NON-NLS-1$
    /**
     * Defines the filetime format mode to use
     * @hide
     */
    SETTINGS_FILETIME_FORMAT_MODE(
            "cm_filemanager_filetime_format_mode", FileTimeFormatMode.LOCALE), //$NON-NLS-1$
    /**
     * When display a warning in free disk widget
     * @hide
     */
    SETTINGS_DISK_USAGE_WARNING_LEVEL(
            "cm_filemanager_disk_usage_warning_level", //$NON-NLS-1$
            new String("95")), //$NON-NLS-1$
    /**
     * When to compute folder statistics in folder properties dialog
     * @hide
     */
    SETTINGS_COMPUTE_FOLDER_STATISTICS(
            "cm_filemanager_compute_folder_statistics", Boolean.FALSE), //$NON-NLS-1$
    /**
     * When to display thumbs of pictures, videos, ...
     * @hide
     */
    SETTINGS_DISPLAY_THUMBS(
            "cm_filemanager_show_thumbs", Boolean.FALSE), //$NON-NLS-1$
    /**
     * Whether use flinger to remove items
     * @hide
     */
    SETTINGS_USE_FLINGER("cm_filemanager_use_flinger", Boolean.FALSE),  //$NON-NLS-1$


    /**
     * When to highlight the terms of the search in the search results
     * @hide
     */
    SETTINGS_HIGHLIGHT_TERMS("cm_filemanager_highlight_terms", Boolean.TRUE), //$NON-NLS-1$
    /**
     * When to show the relevance widget on searches
     * @hide
     */
    SETTINGS_SHOW_RELEVANCE_WIDGET(
            "cm_filemanager_show_relevance_widget", //$NON-NLS-1$
            Boolean.TRUE),
    /**
     * How to sort the search results
     * @hide
     */
    SETTINGS_SORT_SEARCH_RESULTS_MODE(
            "cm_filemanager_sort_search_results_mode", //$NON-NLS-1$
            SearchSortResultMode.RELEVANCE),
    /**
     * When to save the search terms
     * @hide
     */
    SETTINGS_SAVE_SEARCH_TERMS("cm_filemanager_save_search_terms", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to delayed filesystem synchronization in secure storages
     * @hide
     */
    SETTINGS_SECURE_STORAGE_DELAYED_SYNC("cm_filemanager_secure_storage_delayed_sync",
            Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to show debug traces
     * @hide
     */
    SETTINGS_SHOW_TRACES("cm_filemanager_show_debug_traces", Boolean.FALSE), //$NON-NLS-1$

    /**
     * When to editor should display suggestions
     * @hide
     */
    SETTINGS_EDITOR_NO_SUGGESTIONS(
            "cm_filemanager_editor_no_suggestions", Boolean.FALSE), //$NON-NLS-1$

    /**
     * When to editor should use word wrap
     * @hide
     */
    SETTINGS_EDITOR_WORD_WRAP("cm_filemanager_editor_word_wrap", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to editor should open a binary file in a hex viewer
     * @hide
     */
    SETTINGS_EDITOR_HEXDUMP("cm_filemanager_editor_hexdump", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to editor should use the syntax highlight
     * @hide
     */
    SETTINGS_EDITOR_SYNTAX_HIGHLIGHT(
            "cm_filemanager_editor_syntax_highlight", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to editor should use the default color scheme of the theme for syntax highlight
     * @hide
     */
    SETTINGS_EDITOR_SH_USE_THEME_DEFAULT(
            "cm_filemanager_editor_sh_use_theme_default", Boolean.TRUE), //$NON-NLS-1$

    /**
     * When to editor should use the default color scheme of the theme for syntax highlight
     * @hide
     */
    SETTINGS_EDITOR_SH_COLOR_SCHEME(
            "cm_filemanager_editor_sh_color_scheme", ""), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * The current theme to use in the app
     * @hide
     */
    SETTINGS_THEME("cm_filemanager_theme", //$NON-NLS-1$
                        "com.cyanogenmod.filemanager:light"),

    /**
     * The current theme to use in the app
     * @hide
     */
    USER_PREF_LAST_DRAWER_TAB("last_drawer_tab", //$NON-NLS-1$
                        Integer.valueOf(0));



    /**
     * A broadcast intent that is sent when a setting was changed
     */
    public final static String INTENT_SETTING_CHANGED =
                        "com.cyanogenmod.filemanager.INTENT_SETTING_CHANGED"; //$NON-NLS-1$

    /**
     * A broadcast intent that is sent when a theme was changed
     */
    public final static String INTENT_THEME_CHANGED =
                        "com.cyanogenmod.filemanager.INTENT_THEME_CHANGED"; //$NON-NLS-1$

    /**
     * A broadcast intent that is sent when a setting was changed
     */
    public final static String INTENT_MOUNT_STATUS_CHANGED =
                        "com.cyanogenmod.filemanager.INTENT_MOUNT_STATUS_CHANGED"; //$NON-NLS-1$

    /**
     * A broadcast intent that is sent when a file was changed
     */
    public final static String INTENT_FILE_CHANGED =
                        "com.cyanogenmod.filemanager.INTENT_FILE_CHANGED"; //$NON-NLS-1$

    /**
     * The extra key with the preference key that was changed
     */
    public final static String EXTRA_SETTING_CHANGED_KEY = "preference"; //$NON-NLS-1$

    /**
     * The extra key with the file key that was changed
     */
    public final static String EXTRA_FILE_CHANGED_KEY = "file"; //$NON-NLS-1$

    /**
     * The extra key with the file key that was changed
     */
    public final static String EXTRA_THEME_PACKAGE = "package"; //$NON-NLS-1$

    /**
     * The extra key with the identifier of theme that was changed
     */
    public final static String EXTRA_THEME_ID = "id"; //$NON-NLS-1$

    /**
     * The extra key with the identifier a mountpoint event
     */
    public final static String EXTRA_MOUNTPOINT = "mount_point"; //$NON-NLS-1$

    /**
     * The extra key with the notify the status of an object
     */
    public final static String EXTRA_STATUS = "status"; //$NON-NLS-1$




    private final String mId;
    private final Object mDefaultValue;

    /**
     * Constructor of <code>FileManagerSettings</code>.
     *
     * @param id The unique identifier of the setting
     * @param defaultValue The default value of the setting
     */
    private FileManagerSettings(String id, Object defaultValue) {
        this.mId = id;
        this.mDefaultValue = defaultValue;
    }

    /**
     * Method that returns the unique identifier of the setting.
     * @return the mId
     */
    public String getId() {
        return this.mId;
    }

    /**
     * Method that returns the default value of the setting.
     *
     * @return Object The default value of the setting
     */
    public Object getDefaultValue() {
        return this.mDefaultValue;
    }

    /**
     * Method that returns an instance of {@link FileManagerSettings} from its.
     * unique identifier
     *
     * @param id The unique identifier
     * @return FileManagerSettings The navigation sort mode
     */
    public static FileManagerSettings fromId(String id) {
        FileManagerSettings[] values = values();
        int cc = values.length;
        for (int i = 0; i < cc; i++) {
            if (values[i].mId == id) {
                return values[i];
            }
        }
        return null;
    }
}
