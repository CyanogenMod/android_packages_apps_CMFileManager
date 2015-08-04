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

package com.cyanogenmod.filemanager.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.cyanogen.ambient.storage.provider.ProviderCapabilities;
import com.cyanogen.ambient.storage.provider.StorageProviderInfo;
import com.cyanogenmod.filemanager.FileManagerApplication;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences;
import com.cyanogenmod.filemanager.console.storageapi.StorageApiConsole;
import com.cyanogenmod.filemanager.controllers.NavigationDrawerController;
import com.cyanogenmod.filemanager.model.Bookmark;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment;
import com.cyanogenmod.filemanager.ui.fragments.NavigationFragment.OnGoHomeRequestListener;
import com.cyanogenmod.filemanager.ui.widgets.NavigationView.OnBackRequestListener;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;
import com.cyanogenmod.filemanager.util.StorageHelper;
import com.cyanogenmod.filemanager.util.StorageProviderUtils;

import java.io.File;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.AUDIO;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.IMAGE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.COMPRESS;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.VIDEO;

/**
 * The main navigation activity. This activity is the center of the application.
 * From this the user can navigate, search, make actions.<br/>
 * This activity is singleTop, so when it is displayed no other activities exists in
 * the stack.<br/>
 * This cause an issue with the saved instance of this class, because if another activity
 * is displayed, and the process is killed, MainActivity is started and the saved
 * instance gets corrupted.<br/>
 * For this reason the methods {link {@link Activity#onSaveInstanceState(Bundle)} and
 * {@link Activity#onRestoreInstanceState(Bundle)} are not implemented, and every time
 * the app is killed, is restarted from his initial state.
 */
public class MainActivity extends ActionBarActivity
        implements OnItemClickListener, OnBackRequestListener, OnGoHomeRequestListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static boolean DEBUG = false;

    // Bookmark list XML tags
    private static final String TAG_BOOKMARKS = "Bookmarks"; //$NON-NLS-1$
    private static final String TAG_BOOKMARK = "bookmark"; //$NON-NLS-1$

    private static final String STR_USB = "usb"; // $NON-NLS-1$

    /**
     * Intent code for request a search.
     */
    public static final int INTENT_REQUEST_SETTINGS = 20001;

    /**
     * Constant for extra information about selected search entry.
     */
    public static final String EXTRA_SEARCH_ENTRY_SELECTION =
            "extra_search_entry_selection"; //$NON-NLS-1$

    /**
     * Constant for extra information about last search data.
     */
    public static final String EXTRA_SEARCH_LAST_SEARCH_DATA =
            "extra_search_last_search_data"; //$NON-NLS-1$

    /**
     * Constant for extra information for request a navigation to the passed path.
     */
    public static final String EXTRA_NAVIGATE_TO =
            "extra_navigate_to"; //$NON-NLS-1$

    /**
     * Constant for extra information for request to add navigation to the history
     */
    public static final String EXTRA_ADD_TO_HISTORY =
            "extra_add_to_history"; //$NON-NLS-1$

    private ArrayAdapter<MimeTypeCategory> mQuickSearchAdapter;
    private static final List<MimeTypeCategory> QUICK_SEARCH_LIST
            = new ArrayList<MimeTypeCategory>() {
        {
            add(IMAGE);
            add(AUDIO);
            add(VIDEO);
            add(DOCUMENT);
            add(APP);
            add(COMPRESS);
        }
    };

    static Map<MimeTypeCategory, Integer> QUICK_SEARCH_ICONS
            = new HashMap<MimeTypeCategory, Integer>();
    static {
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.IMAGE, R.drawable.ic_category_images);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.AUDIO, R.drawable.ic_category_audio);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.VIDEO, R.drawable.ic_category_video);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.DOCUMENT, R.drawable.ic_category_docs);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.APP, R.drawable.ic_category_apps);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.COMPRESS, R.drawable.ic_category_archives);
    }


    private Toolbar mToolBar;

    private ArrayAdapter<MimeTypeCategory> mEasyModeAdapter;

    private IconHolder mIconHolder;

    private View.OnClickListener mQuickSearchItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Integer position = (Integer) view.getTag();
            onClicked(position);
        }
    };

    /**
     * Fragment types
     */
    public enum FragmentType {
        // Home fragment
        HOME,

        // Navigation fragment
        NAVIGATION,
    }

    static String MIME_TYPE_LOCALIZED_NAMES[];

    private Fragment currentFragment;
    private DrawerLayout mDrawerLayout;
    private NavigationDrawerController mNavigationDrawerController;

    private List<StorageProviderInfo> mProviderInfoList;
    private boolean mPopBackStack = false;

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ||
                        intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    mNavigationDrawerController.loadNavigationDrawerItems();
                }
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        //Save state
        super.onCreate(state);

        IntentFilter newFilter = new IntentFilter();
        newFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        newFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        newFilter.addDataScheme(ContentResolver.SCHEME_FILE);
        registerReceiver(mNotificationReceiver, newFilter);

        //Set the main layout of the activity
        setContentView(R.layout.navigation);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationDrawer =
                (NavigationView) findViewById(R.id.navigation_view);
        mNavigationDrawerController = new NavigationDrawerController(this, navigationDrawer);

        MIME_TYPE_LOCALIZED_NAMES = MimeTypeCategory.getFriendlyLocalizedNames(this);

        mIconHolder = new IconHolder(this, false);

        showWelcomeMsg();

        //FragmentManager.OnBackStackChangedListener
        getSupportFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                updateCurrentFragment();
            }
        });

        final CardView cV = (CardView) findViewById(R.id.add_provider);
        cV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StorageProviderUtils.loadProviderLogin(getApplicationContext());
            }
        });

        Button dismiss =(Button) findViewById(R.id.dismiss_card);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cV.setVisibility(View.GONE);
                // TODO: Save that the card has been dismissed
            }
        });

        handleSearchBar();

        setHomeStatusBarColor();

        //Initialize nfc adapter
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            mNfcAdapter.setBeamPushUrisCallback(new NfcAdapter.CreateBeamUrisCallback() {
                @Override
                public android.net.Uri[] createBeamUris(android.nfc.NfcEvent event) {
                    if (currentFragment instanceof NavigationFragment) {
                        List<FileSystemObject> selectedFiles =
                                ((NavigationFragment)currentFragment).getCurrentNavigationView()
                                        .getSelectedFiles();
                        if (selectedFiles.size() > 0) {
                            List<android.net.Uri> fileUri = new ArrayList<Uri>();
                            for (FileSystemObject f : selectedFiles) {
                                //Beam ignores folders and system files
                                if (!FileHelper.isDirectory(f) && !FileHelper.isSystemFile(f)) {
                                    fileUri.add(Uri.fromFile(new File(f.getFullPath())));
                                }
                            }
                            if (fileUri.size() > 0) {
                                return fileUri.toArray(new android.net.Uri[fileUri.size()]);
                            }
                        }
                    }
                    return null;
                }
            }, this);
        }

        handleNavigateIntent(getIntent());
    }

    private void handleSearchBar() {
        SearchView searchView = (SearchView) findViewById(R.id.homepage_search_bar);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);

        int searchPlateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            int searchTextId = searchPlate.getContext().getResources()
                    .getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
            if (searchText != null) {
                int searchColor = getResources().getColor(R.color.search_bar_hint_text_color);
                searchText.setTextColor(searchColor);
                searchText.setHintTextColor(searchColor);
            }

            // Update all the image views to our assets
            int imageViewId = getResources().getIdentifier("android:id/search_button", null, null);
            ImageView imageView = (ImageView) searchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search);
            }
            imageViewId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
            imageView = (ImageView) searchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search);
            }
            imageViewId = getResources().getIdentifier("android:id/search_voice_btn", null, null);
            imageView = (ImageView) searchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search_voice);
            }
            imageViewId = getResources().getIdentifier("android:id/search_close_btn", null, null);
            imageView = (ImageView) searchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_cancel_close);
            }
        }

        searchView.setFocusable(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "MainActivity.onDestroy"); //$NON-NLS-1$
        }

        // Unregister the receiver
        unregisterReceiver(this.mNotificationReceiver);

        super.onDestroy();
    }

    public void setCurrentFragment(FragmentType fragmentType) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        boolean noBackStack = false;
        String fragmentTag = null;

        switch (fragmentType) {
            case NAVIGATION:
                mPopBackStack = false;
                currentFragment = new NavigationFragment();
                ((NavigationFragment) currentFragment).setOnBackRequestListener(this);
                ((NavigationFragment) currentFragment).setOnGoHomeRequestListener(this);
                ((NavigationFragment)currentFragment)
                        .setOnDirectoryChangedListener(mNavigationDrawerController);
                fragmentTag = fragmentType.name();
                break;
            case HOME:
            default:
                mPopBackStack = false;
                currentFragment = null;
                int fragmentCount = fragmentManager.getBackStackEntryCount();
                for (int i = 0; i < fragmentCount; i++) {
                    FragmentManager.BackStackEntry backStackEntry =
                            fragmentManager.getBackStackEntryAt(i);
                    Fragment fragment = fragmentManager.findFragmentByTag(backStackEntry.getName());
                    if (fragment != null) {
                        fragmentManager.beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss();
                        fragmentManager.popBackStack();
                    }
                }
                mNavigationDrawerController.setSelected(R.id.navigation_item_home);
                setHomeStatusBarColor();

                return;
        }

        if (noBackStack) {
            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_fragment_container, currentFragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_fragment_container, currentFragment, fragmentTag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .addToBackStack(fragmentTag)
                    .commitAllowingStateLoss();
        }
    }

    private void setHomeStatusBarColor() {
        int foregroundColor = getResources().getColor(R.color.status_bar_foreground_color);
        int backgroundColor = getResources().getColor(R.color.default_primary);
        int statusBarColor = ColorUtils.compositeColors(foregroundColor, backgroundColor);
        getWindow().setStatusBarColor(statusBarColor);
    }

    private void updateCurrentFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry backEntry = fragmentManager.getBackStackEntryAt(
                    fragmentManager.getBackStackEntryCount() - 1);
            currentFragment = fragmentManager.findFragmentByTag(backEntry.getName());
        } else {
            // current fragment is Home
            currentFragment = null;
            mNavigationDrawerController.setSelected(R.id.navigation_item_home);
            setHomeStatusBarColor();
        }
    }

    private boolean isCurrentFragment(FragmentType fragmentType) {
        if (fragmentType == FragmentType.HOME) {
            return getSupportFragmentManager().getFragments().size() <= 0;
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentType.name());
            return (fragment != null && fragment.isVisible());
        }
    }

    public void navigateToPath(String path) {
        if (isCurrentFragment(FragmentType.NAVIGATION)) {
            NavigationFragment fragment = (NavigationFragment) currentFragment;
            fragment.getCurrentNavigationView().changeCurrentDir(path, true);
        } else {
            getIntent().putExtra(EXTRA_NAVIGATE_TO, path);
            setCurrentFragment(FragmentType.NAVIGATION);
        }
    }

    public void addBookmark(Bookmark bookmark) {
        // stub
    }

    public void updateActiveDialog(Dialog dialog) {
        // stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(Intent intent) {
        if (!handleNavigateIntent(intent)) {
            handleSearchIntent(intent);
        }
    }

    private boolean handleNavigateIntent(Intent intent) {
        if (intent != null) {
            String path = intent.getStringExtra(EXTRA_NAVIGATE_TO);
            if (!TextUtils.isEmpty(path)) {
                navigateToPath(path);
                return true;
            }
        }
        return false;
    }

    public void handleSearchIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent searchIntent = new Intent(this, SearchActivity.class);
            searchIntent.setAction(Intent.ACTION_SEARCH);
            //- SearchActivity.EXTRA_SEARCH_DIRECTORY
            String extraDir = null;
            if (currentFragment instanceof NavigationFragment) {
                extraDir = ((NavigationFragment)currentFragment)
                        .getCurrentNavigationView().getCurrentDir();
            }
            extraDir = TextUtils.isEmpty(extraDir) ? FileHelper.ROOT_DIRECTORY : extraDir;
            searchIntent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY, extraDir);
            //- SearchManager.APP_DATA
            if (intent.getBundleExtra(SearchManager.APP_DATA) != null) {
                Bundle bundle = new Bundle();
                bundle.putAll(intent.getBundleExtra(SearchManager.APP_DATA));
                searchIntent.putExtra(SearchManager.APP_DATA, bundle);
            }
            //-- SearchManager.QUERY
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                searchIntent.putExtra(SearchManager.QUERY, query);
            }
            //- android.speech.RecognizerIntent.EXTRA_RESULTS
            ArrayList<String> extraResults =
                    intent.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (extraResults != null) {
                searchIntent.putStringArrayListExtra(
                        android.speech.RecognizerIntent.EXTRA_RESULTS, extraResults);
            }
            startActivityForResult(searchIntent, NavigationFragment.INTENT_REQUEST_SEARCH);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mToolBar = (Toolbar) findViewById(R.id.material_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        initQuickSearch();
    }

    private void initQuickSearch() {
        MIME_TYPE_LOCALIZED_NAMES = MimeTypeCategory.getDefinedLocalizedNames(this);
        GridView gridview = (GridView) findViewById(R.id.quick_search_view);

        mQuickSearchAdapter = new ArrayAdapter<MimeTypeCategory>(this, R.layout
                .quick_search_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = (convertView == null) ? getLayoutInflater().inflate(
                        R.layout.quick_search_item, parent, false) : convertView;

                MimeTypeCategory item = getItem(position);
                String typeTitle = MIME_TYPE_LOCALIZED_NAMES[item.ordinal()];
                TextView typeTitleTV = (TextView) convertView
                        .findViewById(R.id.navigation_view_item_name);
                ImageView typeIconIV = (ImageView) convertView
                        .findViewById(R.id.navigation_view_item_icon);

                typeTitleTV.setText(typeTitle);
                convertView.setOnClickListener(mQuickSearchItemClickListener);
                convertView.setTag(position);

                setFileIcon(typeIconIV, QUICK_SEARCH_ICONS.get(item), null);

                return convertView;
            }
        };
        mQuickSearchAdapter.addAll(QUICK_SEARCH_LIST);
        gridview.setAdapter(mQuickSearchAdapter);
    }

    private void setFileIcon(ImageView view, final int iconId, FileSystemObject fso) {
        // Use iconholder to check for thumbnail
        final IconHolder.ICallback callback = new IconHolder.ICallback() {
            @Override
            public void onLoaded(ImageView imageView, Drawable icon) {
                if (icon == null) {
                    // Icon holder didn't have anything at the moment, set default.
                    int colorId = MimeTypeHelper.getIconColorFromIconId(getApplicationContext(), iconId);
                    setIcon(imageView, getResources().getDrawable(iconId),
                            R.color.navigation_view_icon_unselected, R.drawable.ic_icon_background,
                            getResources().getColor(colorId));

                }
            }
        };
        mIconHolder.loadDrawable(view, fso, iconId, callback);
    }

    // Set drawable as icon
    private void setIcon(ImageView view, Drawable iconDrawable, int iconColorId, int backgroundId,
                         int backgroundColor) {

        StateListDrawable stateListDrawable = new StateListDrawable();
        addUnselected(getResources(), stateListDrawable, iconDrawable, iconColorId);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{getResources().getColor(R.color.navigation_view_icon_selected),
                        backgroundColor});

        view.setBackgroundResource(backgroundId);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    // default
    private void addUnselected(Resources res, StateListDrawable drawable, Drawable iconDrawable,
                               int colorId) {
        iconDrawable.setTint(res.getColor(colorId));
        drawable.addState(new int[0], iconDrawable);
    }

    private void onClicked(int position) {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY, FileHelper.ROOT_DIRECTORY);
        intent.putExtra(SearchManager.QUERY, "*"); // Use wild-card '*'

        if (position == 0) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_fragment_container, new NavigationFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            return;

        } else {
            ArrayList<MimeTypeCategory> searchCategories = new ArrayList<MimeTypeCategory>();
            MimeTypeCategory selectedCategory = QUICK_SEARCH_LIST.get(position);
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

    @Override
    public void onResume() {
        super.onResume();
        mNavigationDrawerController.loadNavigationDrawerItems();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        int itemId = view.getId();
        switch (itemId) {
            case R.id.navigation_item_home:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_home");
                setCurrentFragment(FragmentType.HOME);
                mNavigationDrawerController.setSelected(itemId);
                break;
            case R.id.navigation_item_favorites:
                // TODO: Implement this path
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_favorites");
                mNavigationDrawerController.setSelected(itemId);
                break;
            case R.id.navigation_item_internal:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_favorites");
                navigateToPath(StorageHelper.getLocalStoragePath(this));
                break;
            case R.id.navigation_item_root_d:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_root_d");
                navigateToPath(FileHelper.ROOT_DIRECTORY);
                break;
            case R.id.navigation_item_manage:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_manage");
                StorageProviderUtils.loadProviderLogin(this);
                break;
            case R.id.navigation_item_settings:
                if (DEBUG) Log.d(TAG, "onNavigationItemSelected::navigation_item_settings");
                openSettings();
                break;
            default:
                if (DEBUG) {
                    Log.d(TAG, String.format("onNavigationItemSelected::default (%d)", itemId));
                }
                String path = null;
                // Check for item id in storage bookmarks
                Bookmark bookmark = mNavigationDrawerController.getBookmarkFromMenuItem(itemId);
                if (bookmark != null) {
                    path = bookmark.getPath();
                } else {
                    // Check for item id in remote roots
                    StorageProviderInfo providerInfo =
                            mNavigationDrawerController.getProviderInfoFromMenuItem(itemId);

                    if (providerInfo != null) {
                        ProviderCapabilities providerCapabilities = providerInfo.getCapabilities();
                        if (providerCapabilities != null &&
                                providerCapabilities.requiresSession() &&
                                providerInfo.needAuthentication()) {
                            startActivity(providerInfo.authenticateUser());
                            return;
                        }
                        path = StorageApiConsole.constructStorageApiFilePathFromProvider(
                                providerInfo.getRootDocumentId(),
                                StorageApiConsole.getHashCodeFromProvider(providerInfo));
                    }
                }

                if (!TextUtils.isEmpty(path)) {
                    // Check for item id in remote roots
                    navigateToPath(path);
                } else {
                    return;
                }
                break;
        }
        mDrawerLayout.closeDrawers();
    }

    public List<StorageProviderInfo> getProviderList() {
        return mNavigationDrawerController.getProviderList();
    }

    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(android.view.View view) {

        if (currentFragment instanceof NavigationFragment) {

            switch (view.getId()) {
                //######################
                //Breadcrumb Actions
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_filesystem_info:
                    //Show information of the filesystem
                    com.cyanogenmod.filemanager.model.MountPoint mp =
                            ((NavigationFragment)currentFragment)
                                    .getCurrentNavigationView().getBreadcrumb().getMountPointInfo();
                    com.cyanogenmod.filemanager.model.DiskUsage du =
                            ((NavigationFragment)currentFragment)
                                    .getCurrentNavigationView().getBreadcrumb().getDiskUsageInfo();
                    ((NavigationFragment)currentFragment).showMountPointInfo(mp, du);
                    break;
                //######################
                //Selection Actions
                //######################
                case com.cyanogenmod.filemanager.R.id.ab_selection_done:
                    //Show information of the filesystem
                    ((NavigationFragment)currentFragment)
                            .getCurrentNavigationView().onDeselectAll();
                    break;
                case R.id.ab_actions:
                    // Show the actions dialog
                    ((NavigationFragment) currentFragment).openActionsDialog(null, true);
                default:
                    break;
            }
        }
    }

    /**
     * Method that opens the settings activity.
     *
     * @hide
     */
    void openSettings() {
        Intent settingsIntent = new Intent(MainActivity.this,
                SettingsPreferences.class);
        startActivityForResult(settingsIntent, INTENT_REQUEST_SETTINGS);
    }

    /**
     * Method that displays a welcome message the first time the user
     * access the application
     */
    private void showWelcomeMsg() {
        boolean firstUse = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_FIRST_USE.getId(),
                ((Boolean) FileManagerSettings.SETTINGS_FIRST_USE
                        .getDefaultValue()).booleanValue());

        //Display the welcome message?
        if (firstUse && FileManagerApplication.hasShellCommands()) {

            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);

            try {
                Preferences.savePreference(FileManagerSettings.SETTINGS_FIRST_USE, Boolean.FALSE,
                        true);
            } catch (InvalidClassException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mPopBackStack) {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    }
                } else {
                    mDrawerLayout.openDrawer(Gravity.START);
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (currentFragment instanceof NavigationFragment) {
            if (((NavigationFragment)currentFragment).back()) {
                return;
            }
        }
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
        super.onBackPressed();
    }

    @Override
    public void onBackRequested() {
        onBackPressed();
    }

    @Override
    public void onGoHomeRequested(String message) {
        if (DEBUG) Log.d(TAG, "onGoHomeRequested");
        setCurrentFragment(FragmentType.HOME);
        mNavigationDrawerController.setSelected(R.id.navigation_item_home);

        if (!TextUtils.isEmpty(message)) {
            // Alert the user of what happened.
            final View view = findViewById(R.id.navigation_fragment_container);
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
    }

    public int getColorForPath(String path) {
        return mNavigationDrawerController.getColorForPath(path);
    }
}
