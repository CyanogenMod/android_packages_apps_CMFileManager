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

package com.cyanogenmod.filemanager.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that restrict the number of queries that can
 * be made to the application search system.
 */
public class Query implements Serializable, Parcelable {

    private static final long serialVersionUID = 638590514968634860L;

    //IMP! This need to be sync which the command_list.xml resource
    //to have the same slots as the filled for the find command
    private static final int SLOTS_COUNT = 5;

    private final String[] mQUERIES = new String[SLOTS_COUNT];

    /**
     * Constructor of <code>Query</code>.
     */
    public Query() {
        super();
    }

    /**
     * Constructor of <code>Query</code>.
     *
     * @param in The parcel information
     */
    public Query(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Method that returns the value of an slot.
     *
     * @param slot The slot number
     * @return String The text of the query at the slot
     */
    public String getSlot(int slot) {
        return this.mQUERIES[slot];
    }

    /**
     * Method that sets the value of an slot.
     *
     * @param query The text of the query at the slot
     * @param slot The slot number
     * @return Query The query reference
     */
    public Query setSlot(String query, int slot) {
        this.mQUERIES[slot] = query;
        return this;
    }

    /**
     * Method that return the number of slots.
     *
     * @return int The number of slots
     */
    public int getSlotsCount() {
        return this.mQUERIES.length;
    }

    /**
     * Method that fill all the available slots (filled from the minimum
     * to the maximum slot).
     *
     * @param queries The queries which fill the slots
     * @return Query The query reference
     */
    public Query fillSlots(List<String> queries) {
        int cc = queries.size();
        for (int i = 0; i < cc; i++) {
            if (i > this.mQUERIES.length) {
                break;
            }
            this.mQUERIES[i] = queries.get(i);
        }
        return this;
    }

    /**
     * Method that returns the list of queries.
     *
     * @return List<String> The list of queries
     */
    public List<String> getQueries() {
        List<String> queries = new ArrayList<String>(getSlotsCount());
        int cc = this.mQUERIES.length;
        for (int i = 0; i < cc; i++) {
            if (this.mQUERIES[i] != null && this.mQUERIES[i].length() > 0) {
                queries.add(this.mQUERIES[i]);
            }
        }
        return queries;
    }

    /**
     * Method that returns the terms of the query in a single string separated by ", " string.
     *
     * @return String The terms of the query
     */
    public String getTerms() {
        String terms = TextUtils.join(", ", getQueries().toArray(new String[]{})); //$NON-NLS-1$;
        if (terms.endsWith(", ")) { //$NON-NLS-1$;
            terms = ""; //$NON-NLS-1$;
        }
        return terms;
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
        int cc = this.mQUERIES.length;
        dest.writeInt(cc);
        for (int i = 0; i < cc; i++) {
            dest.writeString(mQUERIES[i] != null ? mQUERIES[i] : "");
        }
    }

    /**
     * Fill the object from the parcel information.
     *
     * @param in The parcel information to recreate the object
     */
    private void readFromParcel(Parcel in) {
        int len = mQUERIES.length;
        int cc = in.readInt();
        for (int i = 0; i < cc; i++) {
            String query = in.readString();
            if (i >= len) {
                continue;
            }
            if (!TextUtils.isEmpty(query)) {
                mQUERIES[i] = query;
            }
        }
    }

    /**
     * The {@link android.os.Parcelable.Creator}.
     *
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator<Query> CREATOR =
            new Parcelable.Creator<Query>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Query createFromParcel(Parcel in) {
            return new Query(in);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query[] newArray(int size) {
            return new Query[size];
        }
    };
}
