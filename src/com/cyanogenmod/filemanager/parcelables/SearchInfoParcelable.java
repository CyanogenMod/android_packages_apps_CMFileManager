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
import com.cyanogenmod.filemanager.model.Query;
import com.cyanogenmod.filemanager.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * A serializer/deserializer class for {@link "SearchActivity"}.
 */
public class SearchInfoParcelable extends HistoryNavigable {

    private static final long serialVersionUID = -124315348462060329L;

    private String mSearchDirectory;
    private List<SearchResult> mSearchResultList;
    private Query mSearchQuery;
    private String mTitle;
    private boolean mSuccessNavigation = false;

    /**
     * Constructor of <code>SearchInfoParcelable</code>.
     */
    public SearchInfoParcelable(String searchDirectory, List<SearchResult> searchResultList, Query searchQuery) {
        super();
        mSearchDirectory = searchDirectory;
        mSearchResultList = searchResultList;
        mSearchQuery = searchQuery;
        setTitle();
    }

    /**
     * Constructor of <code>SearchInfoParcelable</code>.
     *
     * @param in The parcel information
     */
    public SearchInfoParcelable(Parcel in) {
        readFromParcel(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return mTitle;
    }

    private void setTitle() {
        String terms = "";
        if (this.mSearchQuery != null) {
            terms = this.mSearchQuery.getTerms();
        }
        mTitle = FileManagerApplication.getInstance().getResources().getString(
                R.string.search_result_name, terms);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.mSearchDirectory;
    }

    /**
     * Method that returns the directory where to search.
     *
     * @return String The directory where to search
     */
    public String getSearchDirectory() {
        return this.mSearchDirectory;
    }

    /**
     * Method that returns the search result list.
     *
     * @return List<SearchResult> The search result list
     */
    public List<SearchResult> getSearchResultList() {
        return this.mSearchResultList;
    }

    /**
     * Method that returns the query terms of the search.
     *
     * @return Query The query terms of the search
     */
    public Query getSearchQuery() {
        return this.mSearchQuery;
    }

    /**
     * Method that returns if the search navigation was success.
     *
     * @return boolean If the search navigation was success
     */
    public boolean isSuccessNavigation() {
        return this.mSuccessNavigation;
    }

    /**
     * Method that returns if the search navigation was success.
     *
     * @param successNavigation If the search navigation was success
     */
    public void setSuccessNavigation(boolean successNavigation) {
        this.mSuccessNavigation = successNavigation;
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
        //- 0
        dest.writeInt(this.mSearchDirectory == null ? 0 : 1);
        if (this.mSearchDirectory != null) {
            dest.writeString(this.mSearchDirectory);
        }
        //- 1
        dest.writeInt(this.mSearchResultList == null ? 0 : 1);
        if (this.mSearchResultList != null) {
            dest.writeList(this.mSearchResultList);
        }
        //- 2
        dest.writeInt(this.mSearchQuery == null ? 0 : 1);
        if (this.mSearchQuery != null) {
            dest.writeParcelable(this.mSearchQuery, 0);
        }
        //- 3
        dest.writeInt(this.mSuccessNavigation ? 1 : 0);
    }

    /**
     * Fill the object from the parcel information.
     *
     * @param in The parcel information to recreate the object
     */
    private void readFromParcel(Parcel in) {
        //- 0
        int hasSearchDirectory = in.readInt();
        if (hasSearchDirectory == 1) {
            this.mSearchDirectory = in.readString();
        }
        //- 1
        int hasSearchResultList = in.readInt();
        if (hasSearchResultList == 1) {
            List<SearchResult> searchResultList = new ArrayList<SearchResult>();
            in.readList(searchResultList, SearchInfoParcelable.class.getClassLoader());
            this.mSearchResultList = new ArrayList<SearchResult>(searchResultList);
        }
        //- 2
        int hasSearchQuery = in.readInt();
        if (hasSearchQuery == 1) {
            this.mSearchQuery = (Query)in.readParcelable(
                    SearchInfoParcelable.class.getClassLoader());
        }
        setTitle();
        //- 3
        this.mSuccessNavigation = in.readInt() != 1;
    }

    /**
     * The {@link android.os.Parcelable.Creator}.
     *
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<SearchInfoParcelable> CREATOR =
            new Parcelable.Creator<SearchInfoParcelable>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public SearchInfoParcelable createFromParcel(Parcel in) {
            return new SearchInfoParcelable(in);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SearchInfoParcelable[] newArray(int size) {
            return new SearchInfoParcelable[size];
        }
    };

}