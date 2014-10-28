/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.cyanogenmod.filemanager.ui.dialogs;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.PatternMatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.AssociationsAdapter;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.policy.IntentsActionPolicy;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A class that wraps a dialog for showing the list of available intents that can handle an
 * action. This dialog allows predetermined
 */
public class AssociationsDialog implements OnItemClickListener {

    private static final String TAG = "AssociationsDialog"; //$NON-NLS-1$

    private final Context mContext;
    private final List<ResolveInfo> mIntents;
    private final ResolveInfo mPreferred;
    private final boolean mAllowPreferred;
    /**
     * @hide
     */
    final Intent mRequestIntent;

    private AlertDialog mDialog;
    /**
     * @hide
     */
    GridView mGrid;
    /**
     * @hide
     */
    CheckBox mRemember;

    private boolean mLoaded;

    /**
     * Constructor of <code>AssociationsDialog</code>.
     *
     * @param context The current context
     * @param icon The icon of the dialog
     * @param title The title dialog
     * @param action The title of the action button
     * @param requestIntent The original request
     * @param intents The list of available intents that can handle an action
     * @param preferred The preferred intent. null if no preferred exists
     * @param allowPreferred If allow the user to mark the selected app as preferred
     * @param onCancelListener The cancel listener
     * @param onDismissListener The dismiss listener
     */
    public AssociationsDialog(
            Context context, int icon, String title, String action,
            Intent requestIntent, List<ResolveInfo> intents, ResolveInfo preferred,
            boolean allowPreferred, OnCancelListener onCancelListener,
            OnDismissListener onDismissListener) {
        super();

        //Save the data
        this.mContext = context;
        this.mRequestIntent = requestIntent;
        this.mIntents = intents;
        this.mPreferred = preferred;
        this.mAllowPreferred = allowPreferred;
        this.mLoaded = false;

        //Initialize dialog
        init(icon, title, action, onCancelListener, onDismissListener);
    }

    /**
     * Method that initializes the dialog.
     *
     * @param context The current context
     * @param icon The icon of the dialog
     * @param title The title of the dialog
     * @param action The title of the action button
     * @param onCancelListener The cancel listener
     * @param onCancelListener The dismiss listener
     */
    private void init(int icon, String title, String action,
            OnCancelListener onCancelListener, OnDismissListener onDismissListener) {
        boolean isPlatformSigned = AndroidHelper.isAppPlatformSignature(this.mContext);

        //Create the layout, and retrieve the views
        LayoutInflater li =
                (LayoutInflater)this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.associations_dialog, null, false);
        this.mRemember = (CheckBox)v.findViewById(R.id.associations_remember);
        this.mRemember.setVisibility(
                isPlatformSigned && this.mAllowPreferred ? View.VISIBLE : View.GONE);
        this.mGrid = (GridView)v.findViewById(R.id.associations_gridview);
        AssociationsAdapter adapter =
                new AssociationsAdapter(this.mContext, this.mGrid, this.mIntents, this);
        this.mGrid.setAdapter(adapter);

        // Ensure a default title dialog
        String dialogTitle = title;
        if (dialogTitle == null) {
            dialogTitle = this.mContext.getString(R.string.associations_dialog_title);
        }

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(this.mContext);
        theme.setBackgroundDrawable(this.mContext, v, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(this.mContext, this.mRemember, "text_color"); //$NON-NLS-1$

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        this.mContext,
                                        icon,
                                        dialogTitle,
                                        v);
        this.mDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                action,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ResolveInfo ri = getSelected();
                        Intent intent =
                                IntentsActionPolicy.getIntentFromResolveInfo(
                                        ri, AssociationsDialog.this.mRequestIntent);

                        // Open the intent (and remember the action is the check is marked)
                        onIntentSelected(
                                ri,
                                intent,
                                AssociationsDialog.this.mRemember.isChecked());
                    }
                });
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                this.mContext.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener)null);
        this.mDialog.setOnCancelListener(onCancelListener);
        this.mDialog.setOnDismissListener(onDismissListener);
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        DialogHelper.delegateDialogShow(this.mContext, this.mDialog);

        // Set user preferences
        this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
        this.mGrid.post(new Runnable() {
            @Override
            public void run() {
                if (!checkUserPreferences()) {
                    // Recall for check user preferences
                    AssociationsDialog.this.mGrid.postDelayed(this, 50L);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // If the item is selected, then just open it like ActivityChooserView
        // If there is no parent, that means an internal call. In this case ignore it.
        if (parent != null && ((ViewGroup)view).isSelected()) {
            this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        } else {
            deselectAll();
            ((ViewGroup)view).setSelected(true);

            // Internal editors can be associated
            boolean isPlatformSigned = AndroidHelper.isAppPlatformSignature(this.mContext);
            if (isPlatformSigned && this.mAllowPreferred) {
                ResolveInfo ri = getSelected();
                this.mRemember.setVisibility(
                        IntentsActionPolicy.isInternalEditor(ri) ?
                               View.INVISIBLE :
                               View.VISIBLE);
            }

            // Enable action button
            this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
        }
    }

    /**
     * Method that check the user preferences
     *
     * @return boolean Indicates if the user preferences was set
     * @hide
     */
    boolean checkUserPreferences() {
        boolean ret = false;
        if (!this.mLoaded) {
            // Check that the view is loaded
            if ((ViewGroup)this.mGrid.getChildAt(0) == null) return false;

            if (this.mPreferred != null) {
                boolean found = false;
                int cc = this.mIntents.size();
                for (int i = 0; i < cc; i++) {
                    ResolveInfo info = this.mIntents.get(i);
                    if (info.activityInfo.name.equals(this.mPreferred.activityInfo.name)) {
                        // Select the item
                        ViewGroup item = (ViewGroup)this.mGrid.getChildAt(i);
                        if (item != null) {
                            if (!item.isSelected()) {
                                onItemClick(null, item, i, item.getId());
                                this.mRemember.setChecked(true);
                                ret = false;
                            } else {
                                this.mLoaded = true;
                                ret = true;
                            }
                        }
                        found = true;
                        break;
                    }
                }

                // Is there is no user preferences?
                if (!found) {
                    this.mLoaded = true;
                    ret = true;
                }
            } else {
                // There is no user preferences
                this.mLoaded = true;
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Method that returns if the preferred intent is still selected
     *
     * @return  if the preferred intent is selected
     */
    private boolean isPreferredSelected() {
        if (this.mPreferred != null) {
            int cc = this.mIntents.size();
            for (int i = 0; i < cc; i++) {
                ResolveInfo info = this.mIntents.get(i);
                if (info.activityInfo.name.equals(this.mPreferred.activityInfo.name)) {
                    ViewGroup item = (ViewGroup)this.mGrid.getChildAt(i);
                    if (item != null && item.isSelected()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Method that deselect all the items of the grid view
     */
    private void deselectAll() {
        int cc = this.mGrid.getChildCount();
        for (int i = 0; i < cc; i++) {
            ViewGroup item = (ViewGroup)this.mGrid.getChildAt(i);
            if (item != null) {
                item.setSelected(false);
            }
        }
    }

    /**
     * Method that returns the selected item of the grid view
     *
     * @return ResolveInfo The selected item
     * @hide
     */
    ResolveInfo getSelected() {
        AssociationsAdapter adapter = (AssociationsAdapter)this.mGrid.getAdapter();
        int cc = this.mGrid.getChildCount();
        int firstVisible = this.mGrid.getFirstVisiblePosition();
        for (int i = 0; i < cc; i++) {
            ViewGroup item = (ViewGroup)this.mGrid.getChildAt(i);
            if (item != null && item.isSelected()) {
                return adapter.getItem(i + firstVisible);
            }
        }
        return null;
    }

    /**
     * Method that opens the associated intent.
     *
     * @param ri The resolve information
     * @param intent The user selection
     * @param remember If the user selection, must be remembered
     * @see ResolverActivity (copied from)
     * @hide
     */
    @SuppressWarnings({"deprecation"})
    void onIntentSelected(ResolveInfo ri, Intent intent, boolean remember) {

        boolean isPlatformSigned = AndroidHelper.isAppPlatformSignature(this.mContext);

        // Register preferred association is only allowed by platform signature
        // The app will be signed with this signature, but when is launch from
        // inside ADT, the app is signed with testkey.
        if (isPlatformSigned && this.mAllowPreferred) {

            PackageManager pm = this.mContext.getPackageManager();

            // Remove preferred application if user don't want to remember it
            if (this.mPreferred != null && !remember) {
                pm.clearPackagePreferredActivities(
                        this.mPreferred.activityInfo.packageName);
            }

            // Associate the activity under these circumstances:
            //  - The user has selected the remember option
            //  - The selected intent is not an internal editor (internal editors are private and
            //    can be associated)
            //  - The selected intent is not the current preferred selection
            if (remember && !IntentsActionPolicy.isInternalEditor(ri) && !isPreferredSelected()) {

                // Build a reasonable intent filter, based on what matched.
                IntentFilter filter = new IntentFilter();

                if (intent.getAction() != null) {
                    filter.addAction(intent.getAction());
                }
                Set<String> categories = intent.getCategories();
                if (categories != null) {
                    for (String cat : categories) {
                        filter.addCategory(cat);
                    }
                }
                filter.addCategory(Intent.CATEGORY_DEFAULT);

                int cat = ri.match & IntentFilter.MATCH_CATEGORY_MASK;
                Uri data = intent.getData();
                if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
                    String mimeType = intent.resolveType(this.mContext);
                    if (mimeType != null) {
                        try {
                            filter.addDataType(mimeType);
                        } catch (IntentFilter.MalformedMimeTypeException e) {
                            Log.w(TAG, e);
                            filter = null;
                        }
                    }
                }
                if (data != null && data.getScheme() != null && filter != null) {
                    // We need the data specification if there was no type,
                    // OR if the scheme is not one of our magical "file:"
                    // or "content:" schemes (see IntentFilter for the reason).
                    if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                            || (!"file".equals(data.getScheme()) //$NON-NLS-1$
                                    && !"content".equals(data.getScheme()))) { //$NON-NLS-1$
                        filter.addDataScheme(data.getScheme());

                        // Look through the resolved filter to determine which part
                        // of it matched the original Intent.
                        // ri.filter should not be null here because the activity matches a filter
                        // Anyway protect the access
                        if (ri.filter != null) {
                            Iterator<IntentFilter.AuthorityEntry> aIt =
                                                        ri.filter.authoritiesIterator();
                            if (aIt != null) {
                                while (aIt.hasNext()) {
                                    IntentFilter.AuthorityEntry a = aIt.next();
                                    if (a.match(data) >= 0) {
                                        int port = a.getPort();
                                        filter.addDataAuthority(a.getHost(),
                                                port >= 0 ? Integer.toString(port) : null);
                                        break;
                                    }
                                }
                            }
                            Iterator<PatternMatcher> pIt = ri.filter.pathsIterator();
                            if (pIt != null) {
                                String path = data.getPath();
                                while (path != null && pIt.hasNext()) {
                                    PatternMatcher p = pIt.next();
                                    if (p.match(path)) {
                                        filter.addDataPath(p.getPath(), p.getType());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                // If we don't have a filter then don't try to associate
                if (filter != null) {
                    try {
                        AssociationsAdapter adapter =
                                (AssociationsAdapter)this.mGrid.getAdapter();
                        final int cc = adapter.getCount();
                        ComponentName[] set = new ComponentName[cc];
                        int bestMatch = 0;
                        for (int i = 0; i < cc; i++) {
                            ResolveInfo r = adapter.getItem(i);
                            set[i] = new ComponentName(
                                    r.activityInfo.packageName, r.activityInfo.name);
                            // Use the match of the selected intent
                            if (intent.getComponent().compareTo(set[i]) == 0) {
                                bestMatch = r.match;
                            }
                        }

                        // The only way i found to ensure of the use of the preferred activity
                        // selected is to clear preferred activity associations
                        if (this.mPreferred != null) {
                            pm.clearPackagePreferredActivities(
                                    this.mPreferred.activityInfo.packageName);
                        }

                        // This is allowed for now in AOSP, but probably in the future this will
                        // not work at all
                        pm.addPreferredActivity(filter, bestMatch, set, intent.getComponent());

                    } catch (Exception e) {
                        // Capture the exception
                        ExceptionUtil.translateException(this.mContext, e, true, false);
                        DialogHelper.showToast(
                                this.mContext,
                                R.string.msgs_action_association_failed,
                                Toast.LENGTH_SHORT);
                    }
                }
            }
        }

        if (intent != null) {
            // Capture security exceptions
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                ExceptionUtil.translateException(this.mContext, e);
            }
        }
    }
}
