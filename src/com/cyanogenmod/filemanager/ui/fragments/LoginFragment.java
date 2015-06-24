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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.cyanogen.ambient.common.api.PendingResult;
import com.cyanogen.ambient.common.api.ResultCallback;
import com.cyanogen.ambient.storage.StorageApi;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo.ProviderInfoListResult;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.MainActivity;
import com.cyanogenmod.filemanager.adapters.ProviderAdapter;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;

import java.util.List;


public class LoginFragment extends Fragment implements
        OnItemClickListener, ResultCallback<ProviderInfoListResult> {

    View mView;
    Toolbar mToolBar;
    ListView mListView;
    ProviderAdapter mAdapter;
    static final int LOGIN_TO_PROVIDER = 1;
    List<StorageProviderInfo> mProviderInfoList;

    private static final String TAG = "LoginFragment";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static LoginFragment newInstance() {
        LoginFragment frag = new LoginFragment();
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.login_fragment, container, false);
        mProviderInfoList = ((MainActivity) getActivity()).getProviderList();
        mListView = (ListView) mView.findViewById(R.id.login_list);
        mListView.setOnItemClickListener(this);
        mAdapter = new ProviderAdapter(getActivity(),
                R.layout.login_fragment_item, mProviderInfoList);
        mListView.setAdapter(mAdapter);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mToolBar = (Toolbar) mView.findViewById(
                R.id.material_toolbar);

        ((ActionBarActivity) getActivity()).setSupportActionBar(mToolBar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccountList();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StorageProviderInfo providerItem = (StorageProviderInfo)mAdapter.getItem(position);
        if (providerItem.needAuthentication()) {
            // login
            Intent i = providerItem.authenticateUser();
            startActivityForResult(i, LOGIN_TO_PROVIDER);
        } else {
            // logout
            // TODO: at some point this will apparently be in the storage area of the Settings
        }

    }

    public void updateAccountList() {
        StorageApi storageApi = StorageApi.getInstance();
        PendingResult<ProviderInfoListResult> pendingResult =
                storageApi.fetchProviders(this);
    }


    @Override
    public void onResult(StorageProviderInfo.ProviderInfoListResult providerInfoListResult) {
        List<StorageProviderInfo> providerInfoList =
                providerInfoListResult.getProviderInfoList();
        if (providerInfoList == null) {
            Log.e(TAG, "no results retunred");
            return;
        }
        if (mProviderInfoList != null) {
            mProviderInfoList.clear();
            mProviderInfoList.addAll(providerInfoList);
        }
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == getActivity().RESULT_OK) {
            updateAccountList();
        }
    }

}