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

package com.cyanogenmod.filemanager.ui.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.ShortcutActivity;
import com.cyanogenmod.filemanager.console.secure.SecureConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RegularFile;
import com.cyanogenmod.filemanager.providers.SecureResourceProvider;
import com.cyanogenmod.filemanager.providers.SecureResourceProvider.AuthorizationResource;
import com.cyanogenmod.filemanager.providers.secure.ISecureChoiceCompleteListener;
import com.cyanogenmod.filemanager.providers.secure.SecureCacheCleanupService;
import com.cyanogenmod.filemanager.providers.secure.SecureChoiceClickListener;
import com.cyanogenmod.filemanager.ui.dialogs.AssociationsDialog;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.ResourcesHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class with the convenience methods for resolve intents related actions
 */
public final class IntentsActionPolicy extends ActionsPolicy {

    private static final String TAG = "IntentsActionPolicy"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    // The preferred package when sorting intents
    private static final String PREFERRED_PACKAGE = "com.cyanogenmod.filemanager"; //$NON-NLS-1$

    /**
     * Extra field for the internal action
     */
    public static final String EXTRA_INTERNAL_ACTION =
            "com.cyanogenmod.filemanager.extra.INTERNAL_ACTION"; //$NON-NLS-1$

    /**
     * Category for all the internal app viewers
     */
    public static final String CATEGORY_INTERNAL_VIEWER =
            "com.cyanogenmod.filemanager.category.INTERNAL_VIEWER"; //$NON-NLS-1$

    /**
     * Category for all the app editor
     */
    public static final String CATEGORY_EDITOR =
            "com.cyanogenmod.filemanager.category.EDITOR"; //$NON-NLS-1$

    /**
     * The package name of Gallery2.
     */
    public static final String GALLERY2_PACKAGE = "com.android.gallery3d";

    /**
     * Method that opens a {@link FileSystemObject} with the default registered application
     * by the system, or ask the user for select a registered application.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param choose If allow the user to select the application to open with
     * @param onCancelListener The cancel listener
     * @param onDismissListener The dismiss listener
     */
    public static void openFileSystemObject(
            final Context ctx, final FileSystemObject fso, final boolean choose,
            final OnCancelListener onCancelListener, final OnDismissListener onDismissListener) {
        try {
            // Create the intent to open the file
            final Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);

            // [NOTE][MSB]: Short circuit to pop up dialog informing user we need to copy out the
            // file until we find a better solution.
            if (fso.isSecure()) {
                // [TODO][MSB]: Check visible cache for existing file but I need to split up
                // resolveIntent function properly for this to be successful
                DialogHelper.createTwoButtonsQuestionDialog(
                        ctx,
                        R.string.ok,
                        R.string.cancel,
                        R.string.warning_title,
                        ctx.getResources().getString(R.string.secure_storage_open_file_warning),
                        new SecureChoiceClickListener(ctx, fso,
                                new ISecureChoiceCompleteListener() {
                                    private boolean isCancelled = false;
                                    @Override
                                    public void onComplete(File cacheFile) {
                                        if (isCancelled) {
                                            return;
                                        }
                                        // Schedule cleanup alarm
                                        SecureCacheCleanupService.scheduleCleanup(ctx);

                                        FileSystemObject cacheFso = FileHelper
                                                .createFileSystemObject(cacheFile);
                                        // Obtain the mime/type and passed it to intent
                                        String mime = MimeTypeHelper.getMimeType(ctx, cacheFso);
                                        if (mime != null) {
                                            intent.setDataAndType(getUriFromFile(ctx, cacheFso),
                                                    mime);
                                        } else {
                                            intent.setData(getUriFromFile(ctx, cacheFso));
                                        }
                                        // Resolve the intent
                                        resolveIntent(
                                                ctx,
                                                intent,
                                                choose,
                                                createInternalIntents(ctx, cacheFso),
                                                0,
                                                R.string.associations_dialog_openwith_title,
                                                R.string.associations_dialog_openwith_action,
                                                true,
                                                onCancelListener,
                                                onDismissListener);
                                    }

                                    @Override
                                    public void onCancelled() {
                                        isCancelled = true;
                                        Toast.makeText(ctx, R.string.cancelled_message, Toast
                                                .LENGTH_SHORT).show();
                                    }
                                }))
                        .show();
                return;
            }

            // Obtain the mime/type and passed it to intent
            String mime = MimeTypeHelper.getMimeType(ctx, fso);
            if (mime != null) {
                intent.setDataAndType(getUriFromFile(ctx, fso), mime);
            } else {
                intent.setData(getUriFromFile(ctx, fso));
            }

            // Resolve the intent
            resolveIntent(
                    ctx,
                    intent,
                    choose,
                    createInternalIntents(ctx, fso),
                    0,
                    R.string.associations_dialog_openwith_title,
                    R.string.associations_dialog_openwith_action,
                    true, onCancelListener, onDismissListener);

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e);
        }
    }

    /**
     * Method that sends a {@link FileSystemObject} with the default registered application
     * by the system, or ask the user for select a registered application.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param onCancelListener The cancel listener
     * @param onDismissListener The dismiss listener
     */
    public static void sendFileSystemObject(
            final Context ctx, final FileSystemObject fso,
            OnCancelListener onCancelListener, OnDismissListener onDismissListener) {
        try {
            // Create the intent to
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType(MimeTypeHelper.getMimeType(ctx, fso));
            Uri uri = getUriFromFile(ctx, fso);
            intent.putExtra(Intent.EXTRA_STREAM, uri);

            // Resolve the intent
            resolveIntent(
                    ctx,
                    intent,
                    true,
                    null,
                    0,
                    R.string.associations_dialog_sendwith_title,
                    R.string.associations_dialog_sendwith_action,
                    false, onCancelListener, onDismissListener);

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e);
        }
    }

    /**
     * Method that sends a {@link FileSystemObject} with the default registered application
     * by the system, or ask the user for select a registered application.
     *
     * @param ctx The current context
     * @param fsos The file system objects
     * @param onCancelListener The cancel listener
     * @param onDismissListener The dismiss listener
     */
    public static void sendMultipleFileSystemObject(
            final Context ctx, final List<FileSystemObject> fsos,
            OnCancelListener onCancelListener, OnDismissListener onDismissListener) {
        try {
            // Create the intent to
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_SEND_MULTIPLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Create an array list of the uris to send
            ArrayList<Uri> uris = new ArrayList<Uri>();
            int cc = fsos.size();
            String lastMimeType = null;
            boolean sameMimeType = true;
            for (int i = 0; i < cc; i++) {
                FileSystemObject fso = fsos.get(i);

                // Folders are not allowed
                if (FileHelper.isDirectory(fso)) continue;

                // Check if we can use a unique mime/type
                String mimeType = MimeTypeHelper.getMimeType(ctx, fso);
                if (mimeType == null) {
                    sameMimeType = false;
                }
                if (sameMimeType &&
                    (mimeType != null && lastMimeType != null &&
                     mimeType.compareTo(lastMimeType) != 0)) {
                    sameMimeType = false;
                }
                lastMimeType = mimeType;

                // Add the uri
                uris.add(getUriFromFile(ctx, fso));
            }
            if (sameMimeType) {
                intent.setType(lastMimeType);
            } else {
                intent.setType(MimeTypeHelper.ALL_MIME_TYPES);
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

            // Resolve the intent
            resolveIntent(
                    ctx,
                    intent,
                    true,
                    null,
                    0,
                    R.string.associations_dialog_sendwith_title,
                    R.string.associations_dialog_sendwith_action,
                    false, onCancelListener, onDismissListener);

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e);
        }
    }

    /**
     * Method that resolve
     *
     * @param ctx The current context
     * @param intent The intent to resolve
     * @param choose If allow the user to select the application to select the registered
     * application. If no preferred app or more than one exists the dialog is shown.
     * @param internals The list of internals intents that can handle the action
     * @param icon The icon of the dialog
     * @param title The title of the dialog
     * @param action The button title of the dialog
     * @param allowPreferred If allow the user to mark the selected app as preferred
     * @param onCancelListener The cancel listener
     * @param onDismissListener The dismiss listener
     */
    private static void resolveIntent(
            Context ctx, Intent intent, boolean choose, List<Intent> internals,
            int icon, int title, int action, boolean allowPreferred,
            OnCancelListener onCancelListener, OnDismissListener onDismissListener) {
        //Retrieve the activities that can handle the file
        final PackageManager packageManager = ctx.getPackageManager();
        if (DEBUG) {
            intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        }
        List<ResolveInfo> info =
                packageManager.
                    queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        Collections.sort(info, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                boolean isLshCMFM =
                        lhs.activityInfo.packageName.compareTo(PREFERRED_PACKAGE) == 0;
                boolean isRshCMFM =
                        rhs.activityInfo.packageName.compareTo(PREFERRED_PACKAGE) == 0;
                if (isLshCMFM && !isRshCMFM) {
                    return -1;
                }
                if (!isLshCMFM && isRshCMFM) {
                    return 1;
                }
                return lhs.activityInfo.name.compareTo(rhs.activityInfo.name);
            }
        });

        // Add the internal editors
        int count = 0;
        if (internals != null) {
            int cc = internals.size();
            for (int i = 0; i < cc; i++) {
                Intent ii = internals.get(i);
                List<ResolveInfo> ie =
                        packageManager.
                            queryIntentActivities(ii, 0);
                if (ie.size() > 0) {
                    ResolveInfo rie = ie.get(0);

                    // Only if the internal is not in the query list
                    boolean exists = false;
                    int ccc = info.size();
                    for (int j = 0; j < ccc; j++) {
                        ResolveInfo ri = info.get(j);
                        if (ri.activityInfo.packageName.compareTo(
                                rie.activityInfo.packageName) == 0 &&
                            ri.activityInfo.name.compareTo(
                                    rie.activityInfo.name) == 0) {

                            // Mark as internal
                            if (ri.activityInfo.metaData == null) {
                                ri.activityInfo.metaData = new Bundle();
                                ri.activityInfo.metaData.putString(
                                        EXTRA_INTERNAL_ACTION, ii.getAction());
                                ri.activityInfo.metaData.putBoolean(
                                        CATEGORY_INTERNAL_VIEWER, true);
                            }
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        continue;
                    }

                    // Mark as internal
                    if (rie.activityInfo.metaData == null) {
                        rie.activityInfo.metaData = new Bundle();
                        rie.activityInfo.metaData.putString(EXTRA_INTERNAL_ACTION, ii.getAction());
                        rie.activityInfo.metaData.putBoolean(CATEGORY_INTERNAL_VIEWER, true);
                    }

                    // Only one result must be matched
                    info.add(count, rie);
                    count++;
                }
            }
        }

        // No registered application
        if (info.size() == 0) {
            DialogHelper.showToast(ctx, R.string.msgs_not_registered_app, Toast.LENGTH_SHORT);
            if (onDismissListener != null) {
                onDismissListener.onDismiss(null);
            }
            return;
        }

        // Retrieve the preferred activity that can handle the file. We only want the
        // resolved activity if the activity is a preferred activity. Other case, the
        // resolved activity was never added by addPreferredActivity
        ResolveInfo mPreferredInfo = findPreferredActivity(ctx, intent, info);

        // Is a simple open and we have an application that can handle the file?
        //---
        // If we have a preferred application, then use it
        if (!choose && (mPreferredInfo  != null && mPreferredInfo.match != 0)) {
            ctx.startActivity(getIntentFromResolveInfo(mPreferredInfo, intent));
            if (onDismissListener != null) {
                onDismissListener.onDismiss(null);
            }
            return;
        }
        // If there are only one activity (app or internal editor), then use it
        if (!choose && info.size() == 1) {
            ResolveInfo ri = info.get(0);
            ctx.startActivity(getIntentFromResolveInfo(ri, intent));
            if (onDismissListener != null) {
                onDismissListener.onDismiss(null);
            }
            return;
        }

        // If we have multiples apps and there is not a preferred application then show
        // open with dialog
        AssociationsDialog dialog =
                new AssociationsDialog(
                        ctx,
                        icon,
                        ctx.getString(title),
                        ctx.getString(action),
                        intent,
                        info,
                        mPreferredInfo,
                        allowPreferred,
                        onCancelListener,
                        onDismissListener);
        dialog.show();
    }

    /**
     * Method that creates a shortcut in the desktop of the device of {@link FileSystemObject}.
     *
     * @param ctx The current context
     * @param fso The file system object
     */
    public static void createShortcut(Context ctx, FileSystemObject fso) {
        try {
            // Create the intent that will handle the shortcut
            Intent shortcutIntent = new Intent(ctx, ShortcutActivity.class);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (FileHelper.isDirectory(fso)) {
                shortcutIntent.putExtra(
                        ShortcutActivity.EXTRA_TYPE,ShortcutActivity.SHORTCUT_TYPE_NAVIGATE);
            } else {
                shortcutIntent.putExtra(
                        ShortcutActivity.EXTRA_TYPE, ShortcutActivity.SHORTCUT_TYPE_OPEN);
            }
            shortcutIntent.putExtra(ShortcutActivity.EXTRA_FSO, fso.getFullPath());

            // Obtain the icon drawable (don't use here the themeable drawable)
            String resid = MimeTypeHelper.getIcon(ctx, fso);
            int dwid =
                    ResourcesHelper.getIdentifier(
                            ctx.getResources(), "drawable", resid); //$NON-NLS-1$

            // The intent to send to broadcast for register the shortcut intent
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, fso.getName());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(ctx, dwid));
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$
            ctx.sendBroadcast(intent);

            // Show the confirmation
            DialogHelper.showToast(
                    ctx, R.string.shortcut_creation_success_msg, Toast.LENGTH_SHORT);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create the shortcut", e); //$NON-NLS-1$
            DialogHelper.showToast(
                    ctx, R.string.shortcut_creation_failed_msg, Toast.LENGTH_SHORT);
        }
    }

    /**
     * This method creates a list of internal activities that could handle the fso.
     *
     * @param ctx The current context
     * @param fso The file system object to open
     */
    private static List<Intent> createInternalIntents(Context ctx, FileSystemObject fso) {
        List<Intent> intents = new ArrayList<Intent>();
        intents.addAll(createEditorIntent(ctx, fso));
        return intents;
    }

    /**
     * This method creates a list of internal activities for editing files
     *
     * @param ctx The current context
     * @param fso FileSystemObject
     */
    private static List<Intent> createEditorIntent(Context ctx, FileSystemObject fso) {
        List<Intent> intents = new ArrayList<Intent>();
        MimeTypeCategory category = MimeTypeHelper.getCategory(ctx, fso);

        //- Internal Editor. This editor can handle TEXT and NONE mime categories but
        //  not system files, directories, ..., only regular files (no symlinks)
        if (fso instanceof RegularFile &&
            (category.compareTo(MimeTypeCategory.NONE) == 0 ||
             category.compareTo(MimeTypeCategory.EXEC) == 0 ||
             category.compareTo(MimeTypeCategory.TEXT) == 0)) {
            Intent editorIntent = new Intent();
            editorIntent.setAction(Intent.ACTION_VIEW);
            editorIntent.addCategory(CATEGORY_INTERNAL_VIEWER);
            editorIntent.addCategory(CATEGORY_EDITOR);
            intents.add(editorIntent);
        }

        return intents;
    }

    /**
     * Method that returns an {@link Intent} from his {@link ResolveInfo}
     *
     * @param ri The ResolveInfo
     * @param request The requested intent
     * @return Intent The intent
     */
    public static final Intent getIntentFromResolveInfo(ResolveInfo ri, Intent request) {
        Intent intent =
                getIntentFromComponentName(
                    new ComponentName(
                        ri.activityInfo.applicationInfo.packageName,
                        ri.activityInfo.name),
                    request);
        boolean isInternalEditor = isInternalEditor(ri);
        if (isInternalEditor) {
            String a = Intent.ACTION_VIEW;
            if (ri.activityInfo.metaData != null) {
                a = ri.activityInfo.metaData.getString(
                        IntentsActionPolicy.EXTRA_INTERNAL_ACTION,
                        Intent.ACTION_VIEW);
            }
            intent.setAction(a);
        } else {
            // Opening image files with Gallery2 will behave incorrectly when started
            // as a new task. We want to be able to return to CMFM with the back button.
            if (!(Intent.ACTION_VIEW.equals(intent.getAction())
                  && isGallery2(ri)
                  && intent.getData() != null
                  && MediaStore.AUTHORITY.equals(intent.getData().getAuthority()))) {
                // Create a new stack for the activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        // Grant access to resources if needed
        grantSecureAccessIfNeeded(intent, ri);

        return intent;
    }

    /**
     * Method that add grant access to secure resources if needed
     *
     * @param intent The intent to grant access
     * @param ri The resolved info associated with the intent
     */
    public static final void grantSecureAccessIfNeeded(Intent intent, ResolveInfo ri) {
        // If this intent will be serve by the SecureResourceProvider then this uri must
        // be granted before we start it, only for external apps. The internal editor
        // must receive an file scheme uri
        Uri uri = intent.getData();
        String authority = null;
        if (uri != null) {
            authority = uri.getAuthority();
            grantSecureAccess(intent, authority, ri, uri);
        } else if (intent.getExtras() != null) {
            Object obj = intent.getExtras().get(Intent.EXTRA_STREAM);
            if (obj instanceof Uri) {
                uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                authority = uri.getAuthority();
                grantSecureAccess(intent, authority, ri, uri);
            } else if (obj instanceof ArrayList) {
                ArrayList<Uri> uris = (ArrayList<Uri>) intent.getExtras().get(Intent.EXTRA_STREAM);
                for (Uri u : uris) {
                    authority = u.getAuthority();
                    grantSecureAccess(intent, authority, ri, u);
                }
            }
        }
    }

    private static final void grantSecureAccess(Intent intent, String authority, ResolveInfo ri,
            Uri uri) {
        if (authority != null && authority.equals(SecureResourceProvider.AUTHORITY)) {
            boolean isInternalEditor = isInternalEditor(ri);
            if (isInternalEditor) {
                // remove the authorization and change request to file scheme
                AuthorizationResource auth = SecureResourceProvider.revertAuthorization(uri);
                intent.setData(Uri.fromFile(new File(auth.mFile.getFullPath())));

            } else {
                // Grant access to the package
                SecureResourceProvider.grantAuthorizationUri(uri,
                        ri.activityInfo.applicationInfo.packageName);
            }
        }
    }

    /**
     * Method that returns an {@link Intent} from his {@link ComponentName}
     *
     * @param cn The ComponentName
     * @param request The requested intent
     * @return Intent The intent
     */
    public static final Intent getIntentFromComponentName(ComponentName cn, Intent request) {
        Intent intent = new Intent(request);
        intent.setFlags(
                intent.getFlags() &~
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_FORWARD_RESULT |
                Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        intent.setComponent(
                new ComponentName(
                        cn.getPackageName(),
                        cn.getClassName()));
        return intent;
    }

    /**
     * Method that returns if the selected resolve info is about an internal viewer
     *
     * @param ri The resolve info
     * @return boolean  If the selected resolve info is about an internal viewer
     * @hide
     */
    public static final boolean isInternalEditor(ResolveInfo ri) {
        return ri.activityInfo.metaData != null &&
                ri.activityInfo.metaData.getBoolean(
                        IntentsActionPolicy.CATEGORY_INTERNAL_VIEWER, false);
    }

    public static final boolean isGallery2(ResolveInfo ri) {
        return GALLERY2_PACKAGE.equals(ri.activityInfo.packageName);
    }

    /**
     * Method that retrieve the finds the preferred activity, if one exists. In case
     * of multiple preferred activity exists the try to choose the better
     *
     * @param ctx The current context
     * @param intent The query intent
     * @param info The initial info list
     * @return ResolveInfo The resolved info
     */
    private static final ResolveInfo findPreferredActivity(
            Context ctx, Intent intent, List<ResolveInfo> info) {

        final PackageManager packageManager = ctx.getPackageManager();

        // Retrieve the preferred activity that can handle the file. We only want the
        // resolved activity if the activity is a preferred activity. Other case, the
        // resolved activity was never added by addPreferredActivity
        List<ResolveInfo> pref = new ArrayList<ResolveInfo>();
        int cc = info.size();
        for (int i = 0; i < cc; i++) {
            ResolveInfo ri = info.get(i);
            if (isInternalEditor(ri)) continue;
            if (ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
            List<ComponentName> prefActList = new ArrayList<ComponentName>();
            List<IntentFilter> intentList = new ArrayList<IntentFilter>();
            IntentFilter filter = new IntentFilter();
            filter.addAction(intent.getAction());
            try {
                filter.addDataType(intent.getType());
            } catch (Exception ex) {/**NON BLOCK**/}
            intentList.add(filter);
            packageManager.getPreferredActivities(
                    intentList, prefActList, ri.activityInfo.packageName);
            if (prefActList.size() > 0) {
                pref.add(ri);
            }
        }

        // No preferred activity is selected
        if (pref.size() == 0) {
            return null;
        }

        // Sort and return the first activity
        Collections.sort(pref, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                if (lhs.priority > rhs.priority) {
                    return -1;
                } else if (lhs.priority < rhs.priority) {
                    return 1;
                }
                if (lhs.preferredOrder > rhs.preferredOrder) {
                    return -1;
                } else if (lhs.preferredOrder < rhs.preferredOrder) {
                    return 1;
                }
                if (lhs.isDefault && !rhs.isDefault) {
                    return -1;
                } else if (!lhs.isDefault && rhs.isDefault) {
                    return 1;
                }
                if (lhs.match > rhs.match) {
                    return -1;
                } else if (lhs.match > rhs.match) {
                    return 1;
                }
                return 0;
            }
        });
        return pref.get(0);
    }

    /**
     * Method that returns the best Uri for the file (content uri, file uri, ...)
     *
     * @param ctx The current context
     * @param file The file to resolve
     */
    private static Uri getUriFromFile(Context ctx, FileSystemObject fso) {
        // If the passed object is secure file then we have to provide access with
        // the internal resource provider
        if (fso.isSecure() && SecureConsole.isVirtualStorageResource(fso.getFullPath())
                && fso instanceof RegularFile) {
            RegularFile file = (RegularFile) fso;
            return SecureResourceProvider.createAuthorizationUri(file);
        }

        // Try to resolve media data or return a file uri
        final File file = new File(fso.getFullPath());
        Uri uri = MediaHelper.fileToContentUri(ctx, file);
        if (uri == null) {
            uri = Uri.fromFile(file);
        }
        return uri;
    }
}
