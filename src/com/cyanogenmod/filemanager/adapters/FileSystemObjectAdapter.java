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

package com.cyanogenmod.filemanager.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.dialogs.ActionsDialog;
import com.cyanogenmod.filemanager.ui.policy.InfoActionPolicy;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ArrayAdapter} for display file system objects.
 */
public class FileSystemObjectAdapter
    extends ArrayAdapter<FileSystemObject> implements OnClickListener {
    private static final String TAG = FileSystemObjectAdapter.class.getSimpleName();

    /**
     * An interface to communicate selection changes events.
     */
    public interface OnSelectionChangedListener {
        /**
         * Method invoked when the selection changed.
         *
         * @param selectedItems The new selected items
         */
        void onSelectionChanged(List<FileSystemObject> selectedItems);
    }

    /**
     * A class that conforms with the ViewHolder pattern to performance
     * the list view rendering.
     */
    private static class ViewHolder {
        /**
         * @hide
         */
        public ViewHolder() {
            super();
        }
        ImageButton mBtInfo;
        ImageView mIvIcon;
        TextView mTvName;
        TextView mTvSummary;
        Boolean mHasSelectedBg;
    }

    /**
     * A class that holds the full data information.
     */
    private static class DataHolder {
        /**
         * @hide
         */
        public DataHolder() {
            super();
        }
        boolean mSelected;
        Drawable mDwIcon;
        Drawable mDwInfo;
        String mName;
        String mSummary;
        String mSize;
        Animation mAnimateOut;
        Animation mAnimateIn;
    }

    private DataHolder[] mData;
    private IconHolder mIconHolder;
    private final int mItemViewResourceId;
    private List<FileSystemObject> mSelectedItems;
    private final boolean mPickable;
    private Resources mRes;
    private OnSelectionChangedListener mOnSelectionChangedListener;

    private boolean mDisposed;

    //The resource of the item icon
    private static final int RESOURCE_ITEM_ICON = R.id.navigation_view_item_icon;
    //The resource of the item name
    private static final int RESOURCE_ITEM_NAME = R.id.navigation_view_item_name;
    //The resource of the item summary information
    private static final int RESOURCE_ITEM_SUMMARY = R.id.navigation_view_item_summary;
    //The resource of the item information button
    private static final int RESOURCE_ITEM_INFO = R.id.navigation_view_item_info;

    /**
     * Constructor of <code>FileSystemObjectAdapter</code>.
     *
     * @param context The current context
     * @param files The list of file system objects
     * @param itemViewResourceId The identifier of the layout that represents an item
     * of the list adapter
     * @param pickable If the adapter should act as a pickable browser.
     */
    public FileSystemObjectAdapter(
            Context context, List<FileSystemObject> files,
            int itemViewResourceId, boolean pickable) {
        super(context, RESOURCE_ITEM_NAME, files);

        FileManagerSettings displayThumbsPref = FileManagerSettings.SETTINGS_DISPLAY_THUMBS;
        final boolean displayThumbs =
                Preferences.getSharedPreferences().getBoolean(
                        displayThumbsPref.getId(),
                        ((Boolean)displayThumbsPref.getDefaultValue()).booleanValue());

        this.mDisposed  = false;
        this.mIconHolder = new IconHolder(context, displayThumbs);
        this.mItemViewResourceId = itemViewResourceId;
        this.mSelectedItems = new ArrayList<FileSystemObject>();
        this.mPickable = pickable;
        mRes = context.getResources();
        processData();
    }

    /**
     * Method that sets the listener which communicates selection changes.
     *
     * @param onSelectionChangedListener The listener reference
     */
    public void setOnSelectionChangedListener(
            OnSelectionChangedListener onSelectionChangedListener) {
        this.mOnSelectionChangedListener = onSelectionChangedListener;
    }

    /**
     * Method that loads the default icons (known icons and more common icons).
     */
    private void loadDefaultIcons() {
        this.mIconHolder.getDrawable("ic_fso_folder_drawable"); //$NON-NLS-1$
        this.mIconHolder.getDrawable("ic_fso_default_drawable"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        if (this.mDisposed) {
            return;
        }
        processData();
        super.notifyDataSetChanged();
    }

    private void notifyDataSetChanged(boolean processData) {
        if (this.mDisposed) {
            return;
        }
        if (processData) {
            processData();
        }
        super.notifyDataSetChanged();
    }

    /**
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        this.mDisposed = true;
        clear();
        this.mData = null;
        if (mIconHolder != null) {
            mIconHolder.cleanup();
            mIconHolder = null;
        }
        this.mSelectedItems.clear();
    }

    /**
     * Method that returns the {@link FileSystemObject} reference from his path.
     *
     * @param path The path of the file system object
     * @return FileSystemObject The file system object reference
     */
    public FileSystemObject getItem(String path) {
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
          //File system object info
            FileSystemObject fso = getItem(i);
            if (fso.getFullPath().compareTo(path) == 0) {
                return fso;
            }
        }
        return null;
    }

    /**
     * Method that process the data before use {@link #getView} method.
     */
    private void processData() {
        Theme theme = ThemeManager.getCurrentTheme(getContext());
        Resources res = getContext().getResources();
        int cc = getCount();

        this.mData = new DataHolder[cc];

        for (int i = 0; i < cc; i++) {
            //File system object info
            FileSystemObject fso = getItem(i);

            //Parse the last modification time and permissions
            StringBuilder sbSummary = new StringBuilder();
            if (fso instanceof ParentDirectory) {
                sbSummary.append(res.getString(R.string.parent_dir));
            } else if (fso instanceof RootDirectory) {
                // TODO: add summary for root list directories
                // Currently RootDirectory is only used in picker activity, which uses simple view
                // by default (no summary).
                // Roots List needs to add a summary if the user is in privileged mode
            } else {
                if (!FileHelper.isDirectory(fso)) {
                    sbSummary.append(FileHelper.getHumanReadableSize(fso));
                    sbSummary.append(" - "); //$NON-NLS-1$
                }
                sbSummary.append(
                        FileHelper.getRelativeDateString(
                        getContext(), fso.getLastModifiedTime().getTime(),
                                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE |
                                        DateUtils.FORMAT_SHOW_YEAR));
            }

            //Build the data holder
            this.mData[i] = new FileSystemObjectAdapter.DataHolder();
            this.mData[i].mSelected = this.mSelectedItems.contains(fso);
            this.mData[i].mDwIcon =
                    this.mIconHolder.getDrawable(MimeTypeHelper.getIcon(getContext(), fso));
            this.mData[i].mName = fso.getName();
            this.mData[i].mSummary = sbSummary.toString();
            this.mData[i].mSize = FileHelper.getHumanReadableSize(fso);
            this.mData[i].mDwInfo = mRes.getDrawable(R.drawable.ic_details);
            this.mData[i].mAnimateOut = AnimationUtils.loadAnimation(getContext(), R.anim.flip_out);
            this.mData[i].mAnimateIn = AnimationUtils.loadAnimation(getContext(), R.anim.flip_in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Check to reuse view
        View v = convertView;
        Theme theme = ThemeManager.getCurrentTheme(getContext());

        if (v == null) {
            //Create the view holder
            LayoutInflater li =
                    (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(this.mItemViewResourceId, parent, false);
            ViewHolder viewHolder = new FileSystemObjectAdapter.ViewHolder();
            viewHolder.mIvIcon = (ImageView)v.findViewById(RESOURCE_ITEM_ICON);
            viewHolder.mTvName = (TextView)v.findViewById(RESOURCE_ITEM_NAME);
            viewHolder.mTvSummary = (TextView)v.findViewById(RESOURCE_ITEM_SUMMARY);
            viewHolder.mBtInfo = (ImageButton) v.findViewById(RESOURCE_ITEM_INFO);
            if (!mPickable) {
                viewHolder.mIvIcon.setOnClickListener(this);
                viewHolder.mBtInfo.setOnClickListener(this);
            } else {
                viewHolder.mBtInfo.setVisibility(View.GONE);
            }
            v.setTag(viewHolder);
        }

        //Retrieve data holder
        if (mData == null || this.mData[position] == null) {
            return v;
        }
        final DataHolder dataHolder = this.mData[position];

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

        //Set the data
        if (convertView != null) {
            // Cancel load for previous usage
            mIconHolder.cancelLoad(viewHolder.mIvIcon);
        }
        if (dataHolder.mSelected) {
            viewHolder.mIvIcon.setImageResource(R.drawable.ic_check_selected);
        } else {
            mIconHolder.loadDrawable(viewHolder.mIvIcon, getItem(position), dataHolder.mDwIcon);
        }

        viewHolder.mTvName.setText(dataHolder.mName);
        theme.setTextColor(getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$

        if (!mPickable) {
            if (viewHolder.mTvSummary != null) {
                viewHolder.mTvSummary.setText(dataHolder.mSummary);
                theme.setTextColor(getContext(), viewHolder.mTvSummary, "text_color"); //$NON-NLS-1$
            }

            viewHolder.mBtInfo.setVisibility(
                    TextUtils.equals(dataHolder.mName, FileHelper.PARENT_DIRECTORY) ?
                            View.INVISIBLE : View.VISIBLE);

            viewHolder.mBtInfo.setImageDrawable(dataHolder.mDwInfo);
            viewHolder.mBtInfo.setTag(Integer.valueOf(position));
            viewHolder.mIvIcon.setTag(Integer.valueOf(position));

            if (viewHolder.mHasSelectedBg == null
                    || viewHolder.mHasSelectedBg != dataHolder.mSelected) {
                int drawableId = dataHolder.mSelected
                        ? R.drawable.selectors_selected_drawable //$NON-NLS-1$
                        : R.drawable.selectors_deselected_drawable; //$NON-NLS-1$

                v.setBackgroundDrawable(mRes.getDrawable(drawableId));
                viewHolder.mHasSelectedBg = dataHolder.mSelected;
            }
        }

        //Return the view
        return v;
    }

    /**
     * Method that returns if the item of the passed position is selected.
     *
     * @param position The position of the item
     * @return boolean If the item of the passed position is selected
     */
    public boolean isSelected(int position) {
        return this.mData[position].mSelected;
    }

    /**
     * Method that selects in the {@link ArrayAdapter} the passed item.
     *
     * @param fso The file system object to select
     */
    public void toggleSelection(FileSystemObject fso) {
        toggleSelection(null, fso);
    }

    /**
     * Method that selects in the {@link ArrayAdapter} the passed item.
     *
     * @param v The icon view object (can be null)
     * @param fso The file system object to select
     */
    public void toggleSelection(View v, FileSystemObject fso) {
        if (this.mData != null) {
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            int cc = this.mData.length;
            for (int i = 0; i < cc; i++) {
                DataHolder data = this.mData[i];
                if (data.mName.compareTo(fso.getName()) == 0) {
                    data.mSelected = !data.mSelected;
                    if (v != null) {
                        ((View)v.getParent()).setSelected(data.mSelected);
                        setAnimationListener(v, data, fso);
                        v.clearAnimation();
                        v.setAnimation(data.mAnimateOut);
                        v.startAnimation(data.mAnimateOut);
                    } else {
                        //Add or remove from the global selected items
                        final List<FileSystemObject> selectedItems =
                                FileSystemObjectAdapter.this.mSelectedItems;
                        if (data.mSelected) {
                            if (!selectedItems.contains(fso)) {
                                selectedItems.add(fso);
                            }
                        } else {
                            if (selectedItems.contains(fso)) {
                                selectedItems.remove(fso);
                            }
                        }

                        //Communicate event
                        if (this.mOnSelectionChangedListener != null) {
                            List<FileSystemObject> selection =
                                    new ArrayList<FileSystemObject>(selectedItems);
                            this.mOnSelectionChangedListener.onSelectionChanged(selection);
                        }

                        // The internal structure was update, only super adapter need to be notified
                        super.notifyDataSetChanged();
                    }

                    //Found
                    return;
                }
            }
        }
    }

    /**
     * Method that deselect all items.
     */
    public void deselectedAll() {
        this.mSelectedItems.clear();
        doSelectDeselectAllVisibleItems(false);
    }

    /**
     * Method that select all visible items.
     */
    public void selectedAllVisibleItems() {
        doSelectDeselectAllVisibleItems(true);
    }

    /**
     * Method that deselect all visible items.
     */
    public void deselectedAllVisibleItems() {
        doSelectDeselectAllVisibleItems(false);
    }

    /**
     * Method that select/deselect all items.
     *
     * @param select Indicates if select (true) or deselect (false) all items.
     */
    private void doSelectDeselectAllVisibleItems(boolean select) {
        if (this.mData != null && this.mData.length > 0) {
            Theme theme = ThemeManager.getCurrentTheme(getContext());
            int cc = this.mData.length;
            for (int i = 0; i < cc; i++) {
                DataHolder data = this.mData[i];
                if (data.mName.compareTo(FileHelper.PARENT_DIRECTORY) == 0) {
                    // No select the parent directory
                    continue;
                }
                data.mSelected = select;

                //Add or remove from the global selected items
                FileSystemObject fso = getItem(i);
                final List<FileSystemObject> selectedItems =
                        FileSystemObjectAdapter.this.mSelectedItems;
                if (data.mSelected) {
                    if (!selectedItems.contains(fso)) {
                        selectedItems.add(fso);
                    }
                } else {
                    if (selectedItems.contains(fso)) {
                        selectedItems.remove(fso);
                    }
                }
            }

            //Communicate event
            if (this.mOnSelectionChangedListener != null) {
                List<FileSystemObject> selection =
                        new ArrayList<FileSystemObject>(
                                FileSystemObjectAdapter.this.mSelectedItems);
                this.mOnSelectionChangedListener.onSelectionChanged(selection);
            }

            // The internal structure was update, only super adapter need to be notified
            super.notifyDataSetChanged();
        }
    }

    /**
     * Method that returns the selected items.
     *
     * @return List<FileSystemObject> The selected items
     */
    public List<FileSystemObject> getSelectedItems() {
        return new ArrayList<FileSystemObject>(this.mSelectedItems);
    }

    /**
     * Method that sets the selected items.
     *
     * @param selectedItems The selected items
     */
    public void setSelectedItems(List<FileSystemObject> selectedItems) {
        this.mSelectedItems = selectedItems;
    }

    /**
     * Method that opens the file properties dialog
     *
     * @param item The path or the {@link FileSystemObject}
     */
    private void openPropertiesDialog(Object item) {
        FileSystemObject fso = null;
        // Resolve the full path
        String path = String.valueOf(item);
        if (item instanceof FileSystemObject) {
            path = ((FileSystemObject)item).getFullPath();
            fso = (FileSystemObject)item;
        } else {
            Log.e(TAG, "Failed to open Properties Dialog. Invalid file object.");
            return;
        }

        // Prior to show the dialog, refresh the item reference
        /*FileSystemObject fso = null;
        try {
            fso = CommandHelper.getFileInfo(getContext(), path, false, null);
            if (fso == null) {
                throw new NoSuchFileOrDirectory(path);
            }

        } catch (Exception e) {
            // Notify the user
            ExceptionUtil.translateException(getContext(), e);

            // Remove the object
            if (e instanceof FileNotFoundException || e instanceof NoSuchFileOrDirectory) {
                // If have a FileSystemObject reference then there is no need to search
                // the path (less resources used)
                if (item instanceof FileSystemObject) {
                    //removeItem((FileSystemObject)item);
                } else {
                    //removeItem((String)item);
                }
            }
            return;
        }*/

        // Show the dialog
        InfoActionPolicy.showPropertiesDialog(getContext(), fso, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {

        //Select or deselect the item
        int pos = ((Integer)v.getTag()).intValue();

        if (pos >= getCount() || pos < 0) {
            return;
        }

        //Retrieve data holder
        final FileSystemObject fso = getItem(pos);

        // Parent directory hasn't actions
        if (fso instanceof ParentDirectory) {
            return;
        }

        //What button was pressed?
        switch (v.getId()) {
            case RESOURCE_ITEM_ICON:
                //Get the row item view
                toggleSelection(v, fso);
                break;
            case RESOURCE_ITEM_INFO:
                // Launch item info
                openPropertiesDialog(fso);
                break;
            default:
                break;
        }
    }

    private void setAnimationListener(final View view, final DataHolder data,
            final FileSystemObject fso) {
        if (data.mAnimateOut == null) {
            data.mAnimateOut = AnimationUtils.loadAnimation(getContext(), R.anim.flip_out);
        }
        if (data.mAnimateOut == null) {
            data.mAnimateIn = AnimationUtils.loadAnimation(getContext(), R.anim.flip_in);
        }

        AnimationListener animationListener = new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (animation == data.mAnimateOut) {
                    ImageView iv = (ImageView)view;
                    if (data.mSelected) {
                        iv.setImageResource(R.drawable.ic_check_selected);
                    } else {
                        mIconHolder.loadDrawable(iv, fso, data.mDwIcon);
                    }
                    view.clearAnimation();
                    view.setAnimation(data.mAnimateIn);
                    view.startAnimation(data.mAnimateIn);
                } else if (animation == data.mAnimateIn) {
                    view.clearAnimation();
                    //Add or remove from the global selected items
                    final List<FileSystemObject> selectedItems =
                            FileSystemObjectAdapter.this.mSelectedItems;
                    if (data.mSelected) {
                        if (!selectedItems.contains(fso)) {
                            selectedItems.add(fso);
                        }
                    } else {
                        if (selectedItems.contains(fso)) {
                            selectedItems.remove(fso);
                        }
                    }

                    //Communicate event
                    FileSystemObjectAdapter fsoAdapter = FileSystemObjectAdapter.this;
                    if (fsoAdapter.mOnSelectionChangedListener != null) {
                        List<FileSystemObject> selection =
                                new ArrayList<FileSystemObject>(selectedItems);
                        fsoAdapter.mOnSelectionChangedListener.onSelectionChanged(selection);
                    }

                    notifyDataSetChanged(false);
                }
            }
        };

        data.mAnimateOut.setAnimationListener(animationListener);
        data.mAnimateIn.setAnimationListener(animationListener);
    }
}
