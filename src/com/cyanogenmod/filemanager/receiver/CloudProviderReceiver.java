package com.cyanogenmod.filemanager.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

/**
 * Created by bird on 8/4/15.
 */
public class CloudProviderReceiver extends BroadcastReceiver {

    private static final String TAG = "FMCloudReceiver";
    private static final boolean DEBUG = true;
    public static final String PROVIDER_NAME = "provider_name";
    public static final String PROVIDER_UPDATED = "com.android.settings.PROVIDER_UPDATED";
    public static final String PROVIDER_LOGIN = "provider_login";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) Log.v(TAG, "Got Provider update");
        Bundle bundle = intent.getExtras();
        StorageProviderInfo mProviderToAdd = (StorageProviderInfo)bundle.get(PROVIDER_NAME);
        boolean loggedIn = bundle.getBoolean(PROVIDER_LOGIN);

        if (DEBUG) Log.v(TAG, "Logging In: " + loggedIn);
        if (loggedIn) {
            // we have logged in, add a provider
            StorageProviderUtils.addProvider(context.getApplicationContext(), mProviderToAdd);
        } else {
            // we logged out, remove that provider
            StorageProviderUtils.removeProvider(context.getApplicationContext(), mProviderToAdd);
        }
    }
}
