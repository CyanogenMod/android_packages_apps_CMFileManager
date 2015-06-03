package com.cyanogenmod.filemanager.activities;


import android.content.Context;
import android.os.AsyncTask;
import android.os.storage.StorageVolume;

import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.parcelables.SearchInfoParcelable;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.StorageHelper;

import java.util.List;


class NavigationTask extends AsyncTask<String, Integer, List<FileSystemObject>> {
        private final boolean mUseCurrent;
        private final boolean mAddToHistory;
        private final boolean mReload;
        private boolean mHasChanged;
        private boolean mIsNewHistory;
        private String mNewDirChecked;
        private final SearchInfoParcelable mSearchInfo;
        private final FileSystemObject mScrollTo;
        private Context mContext;
        private NavActivity mCalling;

        public NavigationTask(boolean useCurrent, boolean addToHistory, boolean reload,
                SearchInfoParcelable searchInfo, FileSystemObject scrollTo, NavActivity
                                      callingContext) {
            super();
            this.mUseCurrent = useCurrent;
            this.mAddToHistory = addToHistory;
            this.mSearchInfo = searchInfo;
            this.mReload = reload;
            this.mScrollTo = scrollTo;
            this.mCalling = callingContext;
            this.mContext = callingContext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<FileSystemObject> doInBackground(String... params) {

            mNewDirChecked = checkChRootedNavigation(params[0]);

            android.util.Log.v("BIRD", "launching file search" + mNewDirChecked);
            try {
                return CommandHelper.listFiles(mContext, mNewDirChecked, null);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.ConsoleAllocException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.NoSuchFileOrDirectory noSuchFileOrDirectory) {
                noSuchFileOrDirectory.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.InsufficientPermissionsException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.CommandNotFoundException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.OperationTimeoutException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.console.ExecutionException e) {
                e.printStackTrace();
            } catch (com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCancelled(List<FileSystemObject> result) {
            onCancelled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(List<FileSystemObject> files) {
            // This means an exception. This method will be recalled then
            if (files != null) {
                mCalling.onPostExecuteTask(files, mAddToHistory, mIsNewHistory, mHasChanged,
                        mSearchInfo, mNewDirChecked, mScrollTo);
            }
        }

        /**
         * Method that ensures that the user don't go outside the ChRooted environment
         *
         * @param newDir The new directory to navigate to
         * @return String
         */
        private String checkChRootedNavigation(String newDir) {
            // If we aren't in ChRooted environment, then there is nothing to check
            //if (!this.mChRooted) return newDir;

            // Check if the path is owned by one of the storage volumes
            if (!StorageHelper.isPathInStorageVolume(newDir)) {
                StorageVolume[] volumes = StorageHelper.getStorageVolumes(mContext, false);
                if (volumes != null && volumes.length > 0) {
                    return volumes[0].getPath();
                }
            }
            return newDir;
        }

};
