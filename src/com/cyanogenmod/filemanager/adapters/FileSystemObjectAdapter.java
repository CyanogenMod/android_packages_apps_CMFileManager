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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ViewOutlineProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.ParentDirectory;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.IconHolder.ICallback;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.policy.InfoActionPolicy;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

/**
 * An implementation of {@link ArrayAdapter} for display file system objects.
 */
public class FileSystemObjectAdapter
    extends ArrayAdapter<FileSystemObject> implements OnClickListener {
    private static final String TAG = FileSystemObjectAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

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
    }

    private IconHolder mIconHolder;
    private final int mItemViewResourceId;
    private HashSet<FileSystemObject> mSelectedItems;
    private final WeakHashMap<ImageView, GetProviderIconTask> mRequests;
    private final boolean mPickable;
    private Resources mRes;
    private OnSelectionChangedListener mOnSelectionChangedListener;
    private final ViewOutlineProvider mIconViewOutlineProvider;

    private int mPrimaryColor;

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

        this.mIconHolder = new IconHolder(context, displayThumbs);
        this.mItemViewResourceId = itemViewResourceId;
        this.mSelectedItems = new HashSet<FileSystemObject>();
        this.mRequests = new WeakHashMap<ImageView, GetProviderIconTask>();
        this.mPickable = pickable;
        mRes = context.getResources();
        mPrimaryColor = mRes.getColor(R.color.default_primary);

        mIconViewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = (int)mRes.getDimension(R.dimen.circle_icon_wh);
                int radius =
                        (int)mRes.getDimension(R.dimen.rectangle_icon_radius);
                outline.setRoundRect(0, 0, size, size, radius);
            }
        };
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
     * Method that dispose the elements of the adapter.
     */
    public void dispose() {
        clear();
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
                if (viewHolder.mIvIcon != null) {
                    viewHolder.mIvIcon.setOnClickListener(this);
                }
                if (viewHolder.mBtInfo != null) {
                    viewHolder.mBtInfo.setOnClickListener(this);
                }
            } else {
                if (viewHolder.mBtInfo != null) {
                    viewHolder.mBtInfo.setVisibility(View.GONE);
                }
            }
            v.setTag(viewHolder);
        }

        //Retrieve the view holder
        ViewHolder viewHolder = (ViewHolder)v.getTag();

        FileSystemObject fso = getItem(position);

        if (viewHolder.mIvIcon != null) {
            viewHolder.mIvIcon.setOutlineProvider(mIconViewOutlineProvider);
            viewHolder.mIvIcon.setClipToOutline(true);
            setIcon(viewHolder.mIvIcon, fso);
        }

        viewHolder.mTvName.setText(fso.getName());
        theme.setTextColor(getContext(), viewHolder.mTvName, "text_color"); //$NON-NLS-1$

        if (viewHolder.mTvSummary != null) {
            Resources res = getContext().getResources();
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
            viewHolder.mTvSummary.setText(sbSummary);
            theme.setTextColor(getContext(), viewHolder.mTvSummary, "text_color"); //$NON-NLS-1$
        }

        if (!this.mPickable) {
            viewHolder.mBtInfo.setVisibility(
                    TextUtils.equals(fso.getName(), FileHelper.PARENT_DIRECTORY) ?
                            View.INVISIBLE : View.VISIBLE);

            viewHolder.mBtInfo.setImageResource(R.drawable.ic_details);
            viewHolder.mBtInfo.setTag(position);
            viewHolder.mIvIcon.setTag(position);

            boolean selected = isSelected(position);
            v.setActivated(selected);
            viewHolder.mIvIcon.setSelected(selected);
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
        return mSelectedItems.contains(getItem(position));
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
        if (DEBUG) Log.d(TAG,"toggleSelection("+fso.getName()+")");
        boolean selected = !mSelectedItems.remove(fso);
        if (selected) {
            mSelectedItems.add(fso);
        }
        if (v != null) {
            ((View) v.getParent()).setActivated(selected);
            v.setSelected(selected);
        }
        //Communicate event
        if (this.mOnSelectionChangedListener != null) {
            this.mOnSelectionChangedListener.onSelectionChanged(
                    new ArrayList<FileSystemObject>(mSelectedItems));
        }

        notifyDataSetChanged();
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
        int cc = getCount();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = getItem(i);
            if (fso.getName().compareTo(FileHelper.PARENT_DIRECTORY) == 0) {
                // No select the parent directory
                continue;
            }
            if (select) {
                mSelectedItems.add(fso);
            } else {
                mSelectedItems.remove(fso);
            }
        }

        //Communicate event
        if (this.mOnSelectionChangedListener != null) {
            this.mOnSelectionChangedListener.onSelectionChanged(
                    new ArrayList<FileSystemObject>(mSelectedItems));
        }

        notifyDataSetChanged();
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
        mSelectedItems.clear();
        mSelectedItems.addAll(selectedItems);
        notifyDataSetChanged();
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

    /**
     * Method that sets the primary color for the current volume
     *
     * @param color hex color of to be used as primary color for the current volume
     */
    public void setPrimaryColor(int color) {
        mPrimaryColor = color;
    }

    private void setIcon(ImageView view, FileSystemObject fso) {
        // Cancel any previous loads to view
        mIconHolder.cancel(view);
        GetProviderIconTask previousForView = mRequests.get(view);
        if (previousForView != null) {
            previousForView.cancel(true);
            mRequests.remove(view);
        }

        // Commence loading of icon to view
        int mimeTypeIconId = MimeTypeHelper.getIcon(getContext(), fso);
        if (fso instanceof RootDirectory) {
            GetProviderIconTask task = new GetProviderIconTask(view, mimeTypeIconId,
                    (RootDirectory) fso);
            mRequests.put(view, task);
            task.execute();
        } else if (FileHelper.isDirectory(fso)) {
            setFolderIcon(view, mimeTypeIconId, !mPickable);
        } else {
            setFileIcon(view, mimeTypeIconId, fso, !mPickable);
        }
    }

    private static void setRootsListIcon(Resources resources, ImageView view, int iconId,
            RootDirectory rootDirectory) {
        setIcon(resources, view, resources.getDrawable(iconId),
                resources.getColor(R.color.navigation_view_icon_unselected),
                R.drawable.ic_icon_background, rootDirectory.getPrimaryColor());
    }

    // TODO: change folder colors depending on current volume (root, local, sdcard, usb, etc.)
    private void setFolderIcon(ImageView view, int iconId, boolean colorCircle) {
        int transparentColor = mRes.getColor(R.color.white);
        if (colorCircle) {
            float opacity = mRes.getFloat(R.float_type.navigation_view_icon_circle_opacity);
            transparentColor = Color.argb(
                    Math.round(((float) 0xFF) * opacity),
                    Color.red(mPrimaryColor),
                    Color.green(mPrimaryColor),
                    Color.blue(mPrimaryColor));
        }
        setIcon(mRes, view, mRes.getDrawable(iconId), mPrimaryColor,
                R.drawable.ic_icon_background, transparentColor);
    }

    private void setFileIcon(ImageView view, final int iconId, FileSystemObject fso,
            final boolean colorCircle) {
        // Use iconholder to check for thumbnail
        final ICallback callback = new ICallback() {
            @Override
            public void onLoaded(ImageView imageView, Drawable icon) {
                if (icon == null) {
                    // Icon holder didn't have anything at the moment, set default.
                    Drawable iconDrawable =  mRes.getDrawable(iconId);
                    int backgroundId = R.drawable.ic_icon_background;
                    int colorId = MimeTypeHelper.getIconColorFromIconId(getContext(), iconId);
                    int color = mRes.getColor(colorId);
                    int secondaryColor = mRes.getColor(R.color.navigation_view_icon_unselected);
                    if (colorCircle) {
                        setIcon(mRes, imageView, iconDrawable, secondaryColor, backgroundId, color);
                    } else {
                        setIcon(mRes, imageView, iconDrawable, color, backgroundId, secondaryColor);
                    }
                } else {
                    // Thumbnail present, set the background to rectangle to match better.
                    setIconThumbnail(mRes, imageView, icon);
                }
            }
        };
        mIconHolder.loadDrawable(view, fso, iconId, callback);
    }

    // Set drawable as icon
    private static void setIcon(Resources resources, ImageView view, Drawable iconDrawable,
            int iconColor, int backgroundId, int backgroundColor) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        addSelected(resources, stateListDrawable);
        addUnselected(stateListDrawable, iconDrawable, iconColor);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{resources.getColor(R.color.navigation_view_icon_selected),
                        backgroundColor});

        view.setBackgroundResource(backgroundId);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    // Set drawable as icon (thumbnail edition)
    private void setIconThumbnail(Resources resources, ImageView view, Drawable iconDrawable) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        addSelected(resources, stateListDrawable);
        addUnselectedThumbnail(stateListDrawable, iconDrawable);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{resources.getColor(R.color.navigation_view_icon_selected),
                        resources.getColor(R.color.navigation_view_icon_unselected)});

        view.setBackgroundResource(R.drawable.ic_icon_background_rounded_rectagle);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    // state_selected
    private static void addSelected(Resources res, StateListDrawable drawable) {
        int[] selected = {android.R.attr.state_selected};
        Drawable icon = res.getDrawable(R.drawable.ic_check);
        icon.setTint(res.getColor(R.color.navigation_view_icon_fill));
        drawable.addState(selected, icon);
    }

    // default
    private static void addUnselected(StateListDrawable drawable, Drawable iconDrawable,
            int color) {
        iconDrawable.setTint(color);
        drawable.addState(new int[0], iconDrawable);
    }

    // default (thumbnail edition)
    private static void addUnselectedThumbnail(StateListDrawable drawable, Drawable iconDrawable) {
        drawable.addState(new int[0], iconDrawable);
    }

    private static class GetProviderIconTask extends AsyncTask<Void, Void, Integer> {
        private Context mContext;
        private WeakReference<ImageView> mView;
        private StorageProviderInfo mProviderInfo;
        private Drawable mIcon;

        GetProviderIconTask(ImageView view, int mimeTypeIconId, RootDirectory rootDirectory) {
            mContext = view.getContext().getApplicationContext();
            mView = new WeakReference<ImageView>(view);
            final StorageApiConsole console = StorageApiConsole.getStorageApiConsoleForPath(
                    rootDirectory.getRootPath());
            if (console != null && console.getStorageProviderInfo() != null) {
                mProviderInfo = console.getStorageProviderInfo();
            }
            setRootsListIcon(mContext.getResources(), view, mimeTypeIconId, rootDirectory);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (mProviderInfo != null) {
                mIcon = StorageProviderUtils.loadPackageIcon(mContext,
                        mProviderInfo.getAuthority(), mProviderInfo.getIcon());

                return StorageProviderUtils.loadProviderColor(mContext,
                        mProviderInfo.getAuthority(), mProviderInfo.getPrimaryColor());
            }
            // Use default color if none were found
            return mContext.getResources().getColor(R.color.default_primary);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            int color = integer.intValue();
            if (mIcon != null && mProviderInfo != null && mView != null) {
                final ImageView view = mView.get();
                if (view != null) {
                    final Resources resources = mContext.getResources();
                    setIcon(resources, view, mIcon,
                            resources.getColor(R.color.navigation_view_icon_unselected),
                            R.drawable.ic_icon_background, color);
                }
            }
        }
    }
}
