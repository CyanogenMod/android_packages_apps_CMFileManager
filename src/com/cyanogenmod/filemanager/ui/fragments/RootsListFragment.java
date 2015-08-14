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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.adapters.FileSystemObjectAdapter;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.model.RootDirectory;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.lang.ref.WeakReference;
import java.util.List;

public class RootsListFragment extends ListFragment {

    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.nav_fragment, container, false);
        GetStorageVolumesTask task =
                new GetStorageVolumesTask(getActivity().getApplicationContext(), (ListView)view);
        task.execute();

        return view;
    }*/

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        new Toast(v.getContext()).makeText(v.getContext(), "item clicked::" + position, 750).show();
        /*mRootListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Navigate to. The navigation view will redirect to the appropriate directory
                        RootDirectory fso = (RootDirectory) ((FileSystemObjectAdapter) parent.getAdapter()).getItem(position);
                        PickerActivity.this.mNavigationView.changeCurrentDir(fso.getRootPath());
                        //PickerActivity.this.mRootListView.setVisibility(View.GONE);
                    }
                });
            }
        });*/
    }

    private static class GetStorageVolumesTask
            extends AsyncTask<Void, String, List<FileSystemObject>> {
        private Context mContext;
        private WeakReference<ListView> mView;

        public GetStorageVolumesTask(final Context context, final ListView view) {
            mContext = context.getApplicationContext();
            mView = new WeakReference<ListView>(view);
        }

        @Override
        protected List<FileSystemObject> doInBackground(Void... params) {
            List<FileSystemObject> volumes =
                    StorageHelper.getStorageVolumesFileSystemObjectList(mContext);
            return volumes;
        }

        @Override
        protected void onPostExecute(List<FileSystemObject> volumes) {
            FileSystemObjectAdapter fsoAdapter = new FileSystemObjectAdapter(mContext, volumes,
                    R.layout.navigation_view_simple_item, true);
            if (mView != null && mView.get() != null) {
                mView.get().setAdapter(fsoAdapter);
            }
        }
    }
}
