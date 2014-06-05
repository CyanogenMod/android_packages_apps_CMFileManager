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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

/**
 * An implementation of {@link SimpleMenuListAdapter} for showing
 * {@link Menu} in a two columns.
 */
public class TwoColumnsMenuListAdapter extends SimpleMenuListAdapter
    implements OnClickListener, OnLongClickListener {

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    private final Context mContext;

    /**
     * Constructor of <code>TwoColumnsMenuListAdapter</code>.
     *
     * @param context The current context
     * @param menuResourceId The resource identifier
     */
    public TwoColumnsMenuListAdapter(Context context, int menuResourceId) {
        super(context, menuResourceId);
        this.mContext = context;

        //Separators are not support in this kind of adapter
        removeSeparators();
    }

    /**
     * Constructor of <code>TwoColumnsMenuListAdapter</code><br/>
     * <br/>.
     * This constructors uses only the menus of the group passed
     *
     * @param context The current context
     * @param menuResourceId The resource identifier
     * @param menuGroupResourceId The menu group resource identifier
     */
    public TwoColumnsMenuListAdapter(
            Context context, int menuResourceId, int menuGroupResourceId) {
        super(context, menuResourceId, menuGroupResourceId);
        this.mContext = context;

        //Separators are not support in this kind of adapter
        removeSeparators();
    }

    /**
     * Method that sets the listener for click events.
     *
     * @param onItemClickListener The listener reference
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    /**
     * Method that sets the listener for long click events.
     *
     * @param onItemLongClickListener The listener reference
     */
    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    /**
     * Method that remove all separator menu items.
     */
    private void removeSeparators() {
        Menu menu = getMenu();
        int cc = menu.size();
        for (int i = cc - 1; i >= 0; i--) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.getTitle() == null || menuItem.getTitle().length() == 0) {
                menu.removeItem(menuItem.getItemId());
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        int count = getMenu().size() / 2;
        if (getMenu().size() % 2 != 0) {
            count++;
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MenuItem getItem(int position) {
        return getMenu().getItem(position * 2);
    }

    /**
     * Method that returns the {@link MenuItem} reference from
     * his identifier.
     *
     * @param id The identifier of the item menu
     * @return MenuItem The reference. <code>null</code> if not found.
     */
    public MenuItem getItemById(int id) {
        return getMenu().findItem(id);
    }

    /**
     * Method that returns the second reference of the row data.
     *
     * @param position The row position
     * @return The second reference data (if exists) at the specified position.
     */
    public MenuItem getItem2(int position) {
        if ((position * 2) + 1 >= getMenu().size()) {
            return null;
        }
        return getMenu().getItem((position * 2) + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        return getMenu().getItem(position).getItemId();
    }

    /**
     * Method that returns the identifier of the second reference of the row data.
     *
     * @param position The row position
     * @return The second reference identifier at the specified position. If not
     * exists the returned identifier is <code>-1</code>
     */
    public long getItemId2(int position) {
        if ((position * 2) + 1 >= getMenu().size()) {
            return -1;
        }
        return getMenu().getItem((position * 2) + 1).getItemId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("boxing")
    public View getView(int position, View convertView, ViewGroup parent) {
        //Recovers the menu item
        MenuItem menuItem1 = getItem(position);
        MenuItem menuItem2 = getItem2(position);

        //Recovery the
        View v = this.mInflater.inflate(R.layout.two_columns_menu_item, parent, false);

        //Set the item1
        TextView tvText1 = (TextView)v.findViewById(R.id.two_columns_menu1_item_text);
        tvText1.setText(menuItem1.getTitle());
        tvText1.setEnabled(menuItem1.isEnabled());
        tvText1.setVisibility(menuItem1.isVisible() ? View.VISIBLE : View.GONE);
        tvText1.setOnClickListener(this);
        tvText1.setOnLongClickListener(this);
        tvText1.setTag(String.format("%d|%d", position, menuItem1.getItemId())); //$NON-NLS-1$

        //Set the item2 if exists
        TextView tvText2 = (TextView)v.findViewById(R.id.two_columns_menu2_item_text);
        if (menuItem2 != null) {
            tvText2.setText(menuItem2.getTitle());
            tvText2.setEnabled(menuItem2.isEnabled());
            tvText2.setVisibility(menuItem2.isVisible() ? View.VISIBLE : View.GONE);
            tvText2.setOnClickListener(this);
            tvText2.setOnLongClickListener(this);
            tvText2.setTag(String.format("%d|%d", position, menuItem2.getItemId())); //$NON-NLS-1$
        } else {
            tvText2.setBackgroundDrawable(null);
            tvText2.setClickable(false);
            tvText2.setOnClickListener(null);
            tvText2.setOnLongClickListener(null);
        }

        // Divider
        TextView divider = (TextView)((ViewGroup)v).getChildAt(1);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(this.mContext);
        theme.setBackgroundDrawable(this.mContext, v, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(this.mContext, tvText1, "text_color"); //$NON-NLS-1$
        theme.setTextColor(this.mContext, tvText2, "text_color"); //$NON-NLS-1$
        theme.setBackgroundDrawable(this.mContext, divider, "vertical_divider_drawable"); //$NON-NLS-1$

        //Return the view
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean onLongClick(View v) {
        if (this.mOnItemLongClickListener != null) {
            String[] data = v.getTag().toString().split("[|]"); //$NON-NLS-1$
            return this.mOnItemLongClickListener.onItemLongClick(
                    (AdapterView<TwoColumnsMenuListAdapter>)v.getParent().getParent(),
                    v, Integer.parseInt(data[0]), Long.parseLong(data[1]));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onClick(View v) {
        if (this.mOnItemClickListener != null) {
            String[] data = v.getTag().toString().split("[|]"); //$NON-NLS-1$
            this.mOnItemClickListener.onItemClick(
                    (AdapterView<TwoColumnsMenuListAdapter>)v.getParent().getParent(),
                    v, Integer.parseInt(data[0]), Long.parseLong(data[1]));
        }
    }

}
