/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.cyanogenmod.filemanager.ui.fragments;

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.SearchActivity;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.util.ArrayList;
import java.util.List;

import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.AUDIO;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.IMAGE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.NONE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.VIDEO;

public class HomeFragment extends Fragment {

    View mView;
    Toolbar mToolBar;
    private android.widget.ArrayAdapter<MimeTypeHelper.MimeTypeCategory> mEasyModeAdapter;
    private static final List<MimeTypeCategory> EASY_MODE_LIST = new ArrayList<MimeTypeCategory>() {
        {
            add(NONE);
            add(IMAGE);
            add(VIDEO);
            add(AUDIO);
            add(DOCUMENT);
            add(APP);
        }
    };
    static java.util.Map<MimeTypeHelper.MimeTypeCategory, Drawable> EASY_MODE_ICONS = new
            java.util.HashMap<MimeTypeHelper.MimeTypeCategory, Drawable>();
    static String MIME_TYPE_LOCALIZED_NAMES[];
    private View.OnClickListener mEasyModeItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Integer position = (Integer) view.getTag();
            onClicked(position);
        }
    };

    LayoutInflater mLayoutInflater;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HomeFragment newInstance() {
        HomeFragment frag = new HomeFragment();
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mLayoutInflater = inflater;

        mView = inflater.inflate(R.layout.home_fragment, container, false);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mToolBar = (Toolbar) mView.findViewById(
                com.cyanogenmod.filemanager.R.id.material_toolbar);
        ((ActionBarActivity) getActivity()).setSupportActionBar(mToolBar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initEasyModePlus();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private void initEasyModePlus() {

        MIME_TYPE_LOCALIZED_NAMES = MimeTypeHelper.MimeTypeCategory.getFriendlyLocalizedNames(
                getActivity());

        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.NONE, getResources().getDrawable(
                R.drawable
                        .ic_em_all));
        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.IMAGE, getResources().getDrawable(
                R.drawable
                        .ic_em_image));
        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.VIDEO, getResources().getDrawable(
                R.drawable
                        .ic_em_video));
        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.AUDIO, getResources().getDrawable(
                R.drawable
                        .ic_em_music));
        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.DOCUMENT, getResources().getDrawable(
                R.drawable
                        .ic_em_document));
        EASY_MODE_ICONS.put(MimeTypeHelper.MimeTypeCategory.APP, getResources().getDrawable(
                R.drawable
                        .ic_em_application));


        GridView gridview = (GridView) mView.findViewById(R.id.easy_modeView);

        mEasyModeAdapter = new android.widget.ArrayAdapter<com.cyanogenmod.filemanager.util
                .MimeTypeHelper.MimeTypeCategory>(getActivity(), R.layout
                .navigation_view_simple_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = (convertView == null) ?mLayoutInflater.inflate(
                        R.layout
                                .navigation_view_simple_item, parent, false) : convertView;
                MimeTypeHelper.MimeTypeCategory item = getItem(position);
                String typeTitle = MIME_TYPE_LOCALIZED_NAMES[item.ordinal()];
                TextView typeTitleTV = (TextView) convertView
                        .findViewById(R.id.navigation_view_item_name);
                ImageView typeIconIV = (ImageView) convertView
                        .findViewById(R.id.navigation_view_item_icon);

                typeTitleTV.setText(typeTitle);
                typeIconIV.setImageDrawable(EASY_MODE_ICONS.get(item));
                convertView.setOnClickListener(mEasyModeItemClickListener);
                convertView.setTag(position);
                return convertView;
            }
        };
        mEasyModeAdapter.addAll(EASY_MODE_LIST);
        gridview.setAdapter(mEasyModeAdapter);



        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onClicked(int position) {
        Intent intent = new Intent(getActivity(), SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY, FileHelper.ROOT_DIRECTORY);
        intent.putExtra(SearchManager.QUERY, "*"); // Use wild-card '*'

        if (position == 0) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_fragment_container, new NavigationFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            return;

        } else {
            ArrayList<MimeTypeCategory> searchCategories = new ArrayList<MimeTypeCategory>();
            MimeTypeHelper.MimeTypeCategory selectedCategory = EASY_MODE_LIST.get(position);
            searchCategories.add(selectedCategory);
            // a one off case where we implicitly want to also search for TEXT mimetypes when the
            // DOCUMENTS category is selected
            if (selectedCategory == MimeTypeCategory.DOCUMENT) {
                searchCategories.add(
                        MimeTypeCategory.TEXT);
            }
            intent.putExtra(SearchActivity.EXTRA_SEARCH_MIMETYPE, searchCategories);
        }

        startActivity(intent);
    }
}