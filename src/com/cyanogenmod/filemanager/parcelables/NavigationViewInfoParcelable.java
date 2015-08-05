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

package com.cyanogenmod.filemanager.parcelables;

import android.os.Parcel;
import android.os.Parcelable;

import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.util.FileHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A serializer/deserializer class for {@link "NavigationView"}.
 */
public class NavigationViewInfoParcelable extends HistoryNavigable {

    private static final long serialVersionUID = -3400094396685969235L;

    private int mId;
    private String mCurrentDir;
    private FileSystemObject mCurrentFileSystemObject;
    private boolean mChRooted;
    private List<FileSystemObject> mFiles;
    private List<FileSystemObject> mSelectedFiles;
    private FileSystemObject mFirstVisible;

    /**
     * Constructor of <code>NavigationViewInfoParcelable</code>.
     */
    public NavigationViewInfoParcelable() {
        super();
    }

    /**
     * Constructor of <code>NavigationViewInfoParcelable</code>.
     *
     * @param in The parcel information
     */
    public NavigationViewInfoParcelable(Parcel in) {
        readFromParcel(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        if (this.mCurrentDir.compareTo(FileHelper.ROOT_DIRECTORY) == 0) {
            FileManagerApplication.getInstance().getResources().getString(
                    R.string.root_directory_name);
        }
        return new File(this.mCurrentDir).getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.mCurrentDir;
    }

    /**
     * Method that returns the identifier of the view.
     *
     * @return int The identifier of the view
     */
    public int getId() {
        return this.mId;
    }

    /**
     * Method that sets the identifier of the view.
     *
     * @param id The identifier of the view
     */
    public void setId(int id) {
        this.mId = id;
    }

    /**
     * Method that returns the current directory.
     *
     * @return String The current directory
     */
    public String getCurrentDir() {
        return this.mCurrentDir;
    }

    /**
     * Method that sets the current directory.
     *
     * @param currentDir The current directory
     */
    public void setCurrentDir(String currentDir) {
        this.mCurrentDir = currentDir;
    }

    /**
     * Method that returns the current directory's {@link FileSystemObject}.
     *
     * @return String The current directory
     */
    public FileSystemObject getCurrentFso() {
        return this.mCurrentFileSystemObject;
    }

    /**
     * Method that sets the current directory's {@link FileSystemObject}.
     *
     * @param currentFso The current directory
     */
    public void setCurrentFso(FileSystemObject currentFso) {
        this.mCurrentFileSystemObject = currentFso;
    }

    /**
     * Method that returns if the view is in a ChRooted environment.
     *
     * @return boolean If the view is in a ChRooted environment
     */
    public boolean getChRooted() {
        return this.mChRooted;
    }

    /**
     * Method that sets if the view is in a ChRooted environment.
     *
     * @param chRooted If the view is in a ChRooted environment
     */
    public void setChRooted(boolean chRooted) {
        this.mChRooted = chRooted;
    }

    /**
     * Method that returns the current file list.
     *
     * @return List<FileSystemObject> The current file list
     */
    public List<FileSystemObject> getFiles() {
        return this.mFiles;
    }

    /**
     * Method that sets the current file list.
     *
     * @param files The current file list
     */
    public void setFiles(List<FileSystemObject> files) {
        this.mFiles = files;
    }

    /**
     * Method that returns the current selected file list.
     *
     * @return List<FileSystemObject> The current selected file list
     */
    public List<FileSystemObject> getSelectedFiles() {
        return this.mSelectedFiles;
    }

    /**
     * Method that sets the current selected file list.
     *
     * @param selectedFiles The current selected file list
     */
    public void setSelectedFiles(List<FileSystemObject> selectedFiles) {
        this.mSelectedFiles = selectedFiles;
    }

    /**
     * Method that returns the first visible file in the list.
     *
     * @return {@link FileSystemObject} The index of the first visible file
     */
    public FileSystemObject getFirstVisible() {
        return mFirstVisible;
    }

    /**
     * Method that sets the first visible file.
     *
     * @param {@link FileSystemObject} The index of the first visible file
     */
    public void setFirstVisible(FileSystemObject firstVisible) {
        mFirstVisible = firstVisible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // - 0
        dest.writeInt(this.mId);
        // - 1
        dest.writeInt(this.mCurrentDir == null ? 0 : 1);
        if (this.mCurrentDir != null) {
            dest.writeString(this.mCurrentDir);
        }
        // - 2
        dest.writeInt(this.mChRooted ? 1 : 0);
        // - 3
        dest.writeInt(this.mSelectedFiles == null ? 0 : 1);
        if (this.mSelectedFiles != null) {
            dest.writeList(this.mSelectedFiles);
        }
        // - 4
        dest.writeInt(this.mFiles == null ? 0 : 1);
        if (this.mFiles != null) {
            dest.writeList(this.mFiles);
        }

        // - 5
        dest.writeInt(this.mFirstVisible == null ? 0 : 1);
        if (this.mFirstVisible != null) {
            dest.writeSerializable(mFirstVisible);
        }
    }

    /**
     * Fill the object from the parcel information.
     *
     * @param in The parcel information to recreate the object
     */
    private void readFromParcel(Parcel in) {
        // - 0
        this.mId = in.readInt();
        // - 1
        int hasCurrentDir = in.readInt();
        if (hasCurrentDir == 1) {
            this.mCurrentDir = in.readString();
        }
        // - 2
        this.mChRooted = (in.readInt() == 1);
        // - 3
        int hasSelectedFiles = in.readInt();
        if (hasSelectedFiles == 1) {
            List<FileSystemObject> selectedFiles = new ArrayList<FileSystemObject>();
            in.readList(selectedFiles, NavigationViewInfoParcelable.class.getClassLoader());
            this.mSelectedFiles = new ArrayList<FileSystemObject>(selectedFiles);
        }
        // - 4
        int hasFiles = in.readInt();
        if (hasFiles == 1) {
            List<FileSystemObject> files = new ArrayList<FileSystemObject>();
            in.readList(files, NavigationViewInfoParcelable.class.getClassLoader());
            this.mFiles = new ArrayList<FileSystemObject>(files);
        }

        // - 5
        int hasFirstVisible = in.readInt();
        if (hasFirstVisible == 1) {
            Serializable readSerializable = in.readSerializable();
            if (readSerializable instanceof FileSystemObject) {
                this.mFirstVisible = (FileSystemObject) readSerializable;
            }
        }
    }

   /**
     * The {@link android.os.Parcelable.Creator}.
     *
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<NavigationViewInfoParcelable> CREATOR =
            new Parcelable.Creator<NavigationViewInfoParcelable>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public NavigationViewInfoParcelable createFromParcel(Parcel in) {
            return new NavigationViewInfoParcelable(in);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NavigationViewInfoParcelable[] newArray(int size) {
            return new NavigationViewInfoParcelable[size];
        }
    };

}