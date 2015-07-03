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

package com.cyanogenmod.filemanager.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.android.internal.util.HexDump;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.preferences.EditorPreferenceFragment;
import com.cyanogenmod.filemanager.activities.preferences.EditorSHColorSchemePreferenceFragment;
import com.cyanogenmod.filemanager.activities.preferences.SettingsPreferences;
import com.cyanogenmod.filemanager.adapters.HighlightedSimpleMenuListAdapter;
import com.cyanogenmod.filemanager.adapters.SimpleMenuListAdapter;
import com.cyanogenmod.filemanager.ash.HighlightColors;
import com.cyanogenmod.filemanager.ash.ISyntaxHighlightResourcesResolver;
import com.cyanogenmod.filemanager.ash.SyntaxHighlightFactory;
import com.cyanogenmod.filemanager.ash.SyntaxHighlightProcessor;
import com.cyanogenmod.filemanager.commands.AsyncResultListener;
import com.cyanogenmod.filemanager.commands.WriteExecutable;
import com.cyanogenmod.filemanager.commands.shell.InvalidCommandDefinitionException;
import com.cyanogenmod.filemanager.console.Console;
import com.cyanogenmod.filemanager.console.ConsoleAllocException;
import com.cyanogenmod.filemanager.console.ConsoleBuilder;
import com.cyanogenmod.filemanager.console.InsufficientPermissionsException;
import com.cyanogenmod.filemanager.console.java.JavaConsole;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.preferences.FileManagerSettings;
import com.cyanogenmod.filemanager.preferences.Preferences;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.ui.policy.PrintActionPolicy;
import com.cyanogenmod.filemanager.ui.widgets.ButtonItem;
import com.cyanogenmod.filemanager.util.AndroidHelper;
import com.cyanogenmod.filemanager.util.CommandHelper;
import com.cyanogenmod.filemanager.util.DialogHelper;
import com.cyanogenmod.filemanager.util.ExceptionUtil;
import com.cyanogenmod.filemanager.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MediaHelper;
import com.cyanogenmod.filemanager.util.ResourcesHelper;
import com.cyanogenmod.filemanager.util.StringHelper;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * An internal activity for view and edit files.
 */
public class EditorActivity extends Activity implements TextWatcher {

    private static final String TAG = "EditorActivity"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    private static final int WRITE_RETRIES = 3;

    private final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_THEME_CHANGED) == 0) {
                    applyTheme();
                    return;
                }
                if (intent.getAction().compareTo(FileManagerSettings.INTENT_SETTING_CHANGED) == 0) {
                    // The settings has changed
                    String key = intent.getStringExtra(FileManagerSettings.EXTRA_SETTING_CHANGED_KEY);
                    if (key != null) {
                        final EditorActivity activity = EditorActivity.this;

                        // No suggestions
                        if (key.compareTo(FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.getId()) == 0) {
                            // Ignore in binary files
                            if (activity.mBinary) return;

                            // Do we have a different setting?
                            boolean noSuggestionsSetting =
                                    Preferences.getSharedPreferences().getBoolean(
                                        FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.getId(),
                                        ((Boolean)FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.
                                                getDefaultValue()).booleanValue());
                            if (noSuggestionsSetting != activity.mNoSuggestions) {
                                activity.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        toggleNoSuggestions();
                                    }
                                });
                            }

                        // Word wrap
                        } else if (key.compareTo(FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.getId()) == 0) {
                            // Ignore in binary files
                            if (activity.mBinary) return;

                            // Do we have a different setting?
                            boolean wordWrapSetting = Preferences.getSharedPreferences().getBoolean(
                                    FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.getId(),
                                    ((Boolean)FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.
                                            getDefaultValue()).booleanValue());
                            if (wordWrapSetting != activity.mWordWrap) {
                                activity.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        toggleWordWrap();
                                    }
                                });
                            }

                        // Syntax highlight
                        // Default theme color scheme
                        // Color scheme
                        } else if (key.compareTo(FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.getId()) == 0) {
                            // Ignore in binary files
                            if (activity.mBinary) return;

                            // Do we have a different setting?
                            boolean syntaxHighlightSetting =
                                    Preferences.getSharedPreferences().getBoolean(
                                        FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.getId(),
                                        ((Boolean)FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.
                                                getDefaultValue()).booleanValue());
                            if (syntaxHighlightSetting != activity.mSyntaxHighlight) {
                                activity.mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        toggleSyntaxHighlight();
                                    }
                                });
                            }

                        } else if (key.compareTo(FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME.getId()) == 0 ) {
                            // Ignore in binary files
                            if (activity.mBinary) return;

                            // Reload the syntax highlight
                            activity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    reloadSyntaxHighlight();
                                }
                            });
                        }
                    }
                    return;
                }
            }
        }
    };

    private class HexDumpAdapter extends ArrayAdapter<String> {
        private class ViewHolder {
            TextView mTextView;
        }

        public HexDumpAdapter(Context context, List<String> data) {
            super(context, R.layout.hexdump_line, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                final Context context = getContext();
                LayoutInflater inflater = LayoutInflater.from(context);
                Theme theme = ThemeManager.getCurrentTheme(context);

                v = inflater.inflate(R.layout.hexdump_line, parent, false);
                ViewHolder viewHolder = new EditorActivity.HexDumpAdapter.ViewHolder();
                viewHolder.mTextView = (TextView)v.findViewById(android.R.id.text1);

                viewHolder.mTextView.setTextAppearance(context, R.style.hexeditor_text_appearance);
                viewHolder.mTextView.setTypeface(mHexTypeface);
                theme.setTextColor(context, viewHolder.mTextView, "text_color"); //$NON-NLS-1$

                v.setTag(viewHolder);
            }

            String text = getItem(position);
            ViewHolder viewHolder = (ViewHolder)v.getTag();
            viewHolder.mTextView.setText(text);

            return v;
        }

        /**
         * Return the view as a document
         *
         * @return StringBuilder a buffer to the document
         */
        public StringBuilder toStringDocument() {
            StringBuilder sb = new StringBuilder();
            int c = getCount();
            for (int i = 0; i < c; i++) {
                sb.append(getItem(i));
                sb.append("\n");
            }
            return sb;
        }
    }

    /**
     * Internal interface to notify progress update
     */
    private interface OnProgressListener {
        void onProgress(int progress);
    }

    /**
     * An internal listener for read a file
     */
    private class AsyncReader implements AsyncResultListener {

        final Object mSync = new Object();
        ByteArrayOutputStream mByteBuffer = null;
        ArrayList<String> mBinaryBuffer = null;
        SpannableStringBuilder mBuffer = null;
        Exception mCause;
        long mSize;
        FileSystemObject mReadFso;
        OnProgressListener mListener;
        boolean mDetectEncoding = false;
        UniversalDetector mDetector;
        String mDetectedEncoding;

        /**
         * Constructor of <code>AsyncReader</code>. For enclosing access.
         */
        public AsyncReader(boolean detectEncoding) {
            super();
            mDetectEncoding = detectEncoding;
            if (mDetectEncoding) {
                mDetector = new UniversalDetector(null);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncStart() {
            this.mByteBuffer = new ByteArrayOutputStream((int)this.mReadFso.getSize());
            this.mSize = 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncEnd(boolean cancelled) {
            if (!cancelled && StringHelper.isBinaryData(mByteBuffer.toByteArray())) {
                EditorActivity.this.mBinary = true;
                EditorActivity.this.mReadOnly = true;
            } else if (mDetector != null) {
                mDetector.dataEnd();
                mDetectedEncoding = mDetector.getDetectedCharset();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncExitCode(int exitCode) {
            synchronized (this.mSync) {
                this.mSync.notify();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPartialResult(Object result) {
            try {
                if (result == null) return;
                byte[] partial = (byte[]) result;
                if (mDetectEncoding) {
                    mDetector.handleData(partial, 0, partial.length);
                }
                this.mByteBuffer.write(partial, 0, partial.length);
                this.mSize += partial.length;
                if (this.mListener != null && this.mReadFso != null) {
                    int progress = 0;
                    if (this.mReadFso.getSize() != 0) {
                        progress = (int)((this.mSize*100) / this.mReadFso.getSize());
                    }
                    this.mListener.onProgress(progress);
                }
            } catch (Exception e) {
                this.mCause = e;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onException(Exception cause) {
            this.mCause = cause;
        }
    }

    /**
     * An internal listener for write a file
     */
    private class AsyncWriter implements AsyncResultListener {

        Exception mCause;

        /**
         * Constructor of <code>AsyncWriter</code>. For enclosing access.
         */
        public AsyncWriter() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncStart() {/**NON BLOCK**/}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncEnd(boolean cancelled) {/**NON BLOCK**/}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAsyncExitCode(int exitCode) {/**NON BLOCK**/}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPartialResult(Object result) {/**NON BLOCK**/}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onException(Exception cause) {
            this.mCause = cause;
        }
    }

    /**
     * An internal class to resolve resources for the syntax highlight library.
     * @hide
     */
    class ResourcesResolver implements ISyntaxHighlightResourcesResolver {
        @Override
        public CharSequence getString(String id, String resid) {
            return EditorActivity.this.getString(
                        ResourcesHelper.getIdentifier(
                            EditorActivity.this.getResources(), "string", resid)); //$NON-NLS-1$
        }

        @Override
        public int getInteger(String id, String resid, int def) {
            return EditorActivity.this.getResources().
                    getInteger(
                        ResourcesHelper.getIdentifier(
                            EditorActivity.this.getResources(), "integer", resid)); //$NON-NLS-1$
        }

        @Override
        public int getColor(String id, String resid, int def) {
            final Context ctx = EditorActivity.this;
            try {
                // Use the user-defined settings
                int[] colors = getUserColorScheme();
                HighlightColors[] schemeColors = HighlightColors.values();
                int cc = schemeColors.length;
                int cc2 = colors.length;
                for (int i = 0; i < cc; i++) {
                    if (schemeColors[i].getId().compareTo(id) == 0) {
                        if (cc2 >= i) {
                            // User-defined
                            return colors[i];
                        }

                        // Theme default
                        return ThemeManager.getCurrentTheme(ctx).getColor(ctx, resid);
                    }

                }

            } catch (Exception ex) {
                // Resource not found
            }
            return def;
        }

        /**
         * Method that returns the user-defined color scheme
         *
         * @return int[] The user-defined color scheme
         */
        private int[] getUserColorScheme() {
            String defaultValue =
                    (String)FileManagerSettings.
                                SETTINGS_EDITOR_SH_COLOR_SCHEME.getDefaultValue();
            String value = Preferences.getSharedPreferences().getString(
                                FileManagerSettings.SETTINGS_EDITOR_SH_COLOR_SCHEME.getId(),
                                defaultValue);
            return EditorSHColorSchemePreferenceFragment.toColorShemeArray(value);
        }
    }

    /**
     * @hide
     */
    FileSystemObject mFso;

    private int mBufferSize;
    private long mMaxFileSize;

    /**
     * @hide
     */
    boolean mDirty;
    /**
     * @hide
     */
    boolean mReadOnly;
    /**
     * @hide
     */
    boolean mBinary;

    /**
     * @hide
     */
    TextView mTitle;
    /**
     * @hide
     */
    EditText mEditor;
    /**
     * @hide
     */
    ListView mBinaryEditor;
    /**
     * @hide
     */
    View mProgress;
    /**
     * @hide
     */
    ProgressBar mProgressBar;
    /**
     * @hide
     */
    TextView mProgressBarMsg;
    /**
     * @hide
     */
    ButtonItem mSave;
    /**
     * @hide
     */
    ButtonItem mPrint;

    // No suggestions status
    /**
     * @hide
     */
    boolean mNoSuggestions;

    // Word wrap status
    private ViewGroup mWordWrapView;
    private ViewGroup mNoWordWrapView;
    /**
     * @hide
     */
    boolean mWordWrap;

    // Syntax highlight status
    /**
     * @hide
     */
    boolean mSyntaxHighlight;
    /**
     * @hide
     */
    SyntaxHighlightProcessor mSyntaxHighlightProcessor;
    private int mEditStart;
    private int mEditEnd;

    private View mOptionsAnchorView;

    private Typeface mHexTypeface;

    private final Object mExecSync = new Object();

    /**
     * @hide
     */
    Handler mHandler;

    /**
     * @hide
     */
    String mHexLineSeparator;

    private boolean mHexDump;

    /**
     * Intent extra parameter for the path of the file to open.
     */
    public static final String EXTRA_OPEN_FILE = "extra_open_file";  //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle state) {
        if (DEBUG) {
            Log.d(TAG, "EditorActivity.onCreate"); //$NON-NLS-1$
        }

        this.mHandler = new Handler();

        // Load typeface for hex editor
        mHexTypeface = Typeface.createFromAsset(getAssets(), "fonts/Courier-Prime.ttf");

        // Save hexdump user preference
        mHexDump = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_EDITOR_HEXDUMP.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_EDITOR_HEXDUMP.
                        getDefaultValue()).booleanValue());

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileManagerSettings.INTENT_THEME_CHANGED);
        filter.addAction(FileManagerSettings.INTENT_SETTING_CHANGED);
        registerReceiver(this.mNotificationReceiver, filter);

        // Generate a random separator
        this.mHexLineSeparator = UUID.randomUUID().toString() + UUID.randomUUID().toString();

        // Set the theme before setContentView
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, false);

        //Set the main layout of the activity
        setContentView(R.layout.editor);

        // Get the limit vars
        this.mBufferSize = getResources().getInteger(R.integer.buffer_size);
        long availMem = AndroidHelper.getAvailableMemory(this);
        this.mMaxFileSize = Math.min(availMem,
                getResources().getInteger(R.integer.editor_max_file_size));

        //Initialize
        initTitleActionBar();
        initLayout();

        // Apply the theme
        applyTheme();

        // Initialize the console
        initializeConsole();

        // Read the file
        readFile();

        //Save state
        super.onCreate(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "EditorActivity.onDestroy"); //$NON-NLS-1$
        }

        // Unregister the receiver
        try {
            unregisterReceiver(this.mNotificationReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }

        //All destroy. Continue
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
        //Configure the action bar options
        getActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.bg_material_titlebar));
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View customTitle = getLayoutInflater().inflate(R.layout.simple_customtitle, null, false);
        this.mTitle = (TextView)customTitle.findViewById(R.id.customtitle_title);
        this.mTitle.setText(R.string.editor);
        this.mTitle.setContentDescription(getString(R.string.editor));

        this.mSave = (ButtonItem)customTitle.findViewById(R.id.ab_button0);
        this.mSave.setImageResource(R.drawable.ic_material_light_save);
        this.mSave.setContentDescription(getString(R.string.actionbar_button_save_cd));
        this.mSave.setVisibility(View.GONE);

        this.mPrint = (ButtonItem)customTitle.findViewById(R.id.ab_button1);
        this.mPrint.setImageResource(R.drawable.ic_material_light_print);
        this.mPrint.setContentDescription(getString(R.string.actionbar_button_print_cd));
        this.mPrint.setVisibility(View.VISIBLE);

        ButtonItem configuration = (ButtonItem)customTitle.findViewById(R.id.ab_button2);
        configuration.setImageResource(R.drawable.ic_material_light_overflow);
        configuration.setContentDescription(getString(R.string.actionbar_button_overflow_cd));

        View status = findViewById(R.id.editor_status);
        boolean showOptionsMenu = AndroidHelper.showOptionsMenu(this);
        configuration.setVisibility(showOptionsMenu ? View.VISIBLE : View.GONE);
        this.mOptionsAnchorView = showOptionsMenu ? configuration : status;

        getActionBar().setCustomView(customTitle);
    }

    /**
     * Method that initializes the layout and components of the activity.
     */
    private void initLayout() {
        this.mEditor = (EditText)findViewById(R.id.editor);
        this.mEditor.setText(null);
        this.mEditor.addTextChangedListener(this);
        this.mEditor.setEnabled(false);
        this.mWordWrapView = (ViewGroup)findViewById(R.id.editor_word_wrap_view);
        this.mNoWordWrapView = (ViewGroup)findViewById(R.id.editor_no_word_wrap_view);
        this.mWordWrapView.setVisibility(View.VISIBLE);
        this.mNoWordWrapView.setVisibility(View.GONE);

        this.mBinaryEditor = (ListView)findViewById(R.id.editor_binary);

        this.mNoSuggestions = false;
        this.mWordWrap = true;
        this.mSyntaxHighlight = true;

        // Load the no suggestions setting
        boolean noSuggestionsSetting = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_EDITOR_NO_SUGGESTIONS.
                        getDefaultValue()).booleanValue());
        if (noSuggestionsSetting != this.mNoSuggestions) {
            toggleNoSuggestions();
        }

        // Load the word wrap setting
        boolean wordWrapSetting = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_EDITOR_WORD_WRAP.
                        getDefaultValue()).booleanValue());
        if (wordWrapSetting != this.mWordWrap) {
            toggleWordWrap();
        }

        // Load the syntax highlight setting
        boolean syntaxHighlighSetting = Preferences.getSharedPreferences().getBoolean(
                FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.getId(),
                ((Boolean)FileManagerSettings.SETTINGS_EDITOR_SYNTAX_HIGHLIGHT.
                        getDefaultValue()).booleanValue());
        if (syntaxHighlighSetting != this.mSyntaxHighlight) {
            toggleSyntaxHighlight();
        }

        this.mProgress = findViewById(R.id.editor_progress);
        this.mProgressBar = (ProgressBar)findViewById(R.id.editor_progress_bar);
        this.mProgressBarMsg = (TextView)findViewById(R.id.editor_progress_msg);
    }

    /**
     * Method that toggle the no suggestions property of the editor
     * @hide
     */
    /**package**/ void toggleNoSuggestions() {
        synchronized (this.mExecSync) {
            int type = InputType.TYPE_CLASS_TEXT |
                       InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                       InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE;
            if (!this.mNoSuggestions) {
                type |= InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            }
            this.mEditor.setInputType(type);
            this.mNoSuggestions = !this.mNoSuggestions;
        }
    }

    /**
     * Method that toggle the word wrap property of the editor
     * @hide
     */
    /**package**/ void toggleWordWrap() {
        synchronized (this.mExecSync) {
            ViewGroup vSrc = this.mWordWrap ? this.mWordWrapView : this.mNoWordWrapView;
            ViewGroup vDst = this.mWordWrap ? this.mNoWordWrapView : this.mWordWrapView;
            ViewGroup vSrcParent = this.mWordWrap
                                                ? this.mWordWrapView
                                                : (ViewGroup)this.mNoWordWrapView.getChildAt(0);
            ViewGroup vDstParent = this.mWordWrap
                                                ? (ViewGroup)this.mNoWordWrapView.getChildAt(0)
                                                : this.mWordWrapView;
            vSrc.setVisibility(View.GONE);
            vSrcParent.removeView(this.mEditor);
            vDstParent.addView(this.mEditor);
            vDst.setVisibility(View.VISIBLE);
            vDst.scrollTo(0, 0);
            this.mWordWrap = !this.mWordWrap;
        }
    }

    /**
     * Method that toggles the syntax highlight property of the editor
     * @hide
     */
    /**package**/ void toggleSyntaxHighlight() {
        synchronized (this.mExecSync) {
            if (this.mSyntaxHighlightProcessor != null) {
                try {
                    if (this.mSyntaxHighlight) {
                        this.mSyntaxHighlightProcessor.clear(this.mEditor.getText());
                    } else {
                        this.mSyntaxHighlightProcessor.process(this.mEditor.getText());
                    }
                } catch (Exception ex) {
                    // An error in a syntax library, should not break down app.
                    Log.e(TAG, "Syntax highlight failed.", ex); //$NON-NLS-1$
                }
            }

            this.mSyntaxHighlight = !this.mSyntaxHighlight;
        }
    }

    /**
     * Method that reloads the syntax highlight of the current file
     * @hide
     */
    /**package**/ void reloadSyntaxHighlight() {
        synchronized (this.mExecSync) {
            if (this.mSyntaxHighlightProcessor != null) {
                try {
                    this.mSyntaxHighlightProcessor.initialize();
                    this.mSyntaxHighlightProcessor.process(this.mEditor.getText());
                } catch (Exception ex) {
                    // An error in a syntax library, should not break down app.
                    Log.e(TAG, "Syntax highlight failed.", ex); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflowPopUp(this.mOptionsAnchorView);
                return true;
            case KeyEvent.KEYCODE_BACK:
                checkDirtyState();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
              if ((getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP)
                      == ActionBar.DISPLAY_HOME_AS_UP) {
                  checkDirtyState();
              }
              return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    /**
     * Method that shows a popup with the activity main menu.
     *
     * @param anchor The anchor of the popup
     */
    private void showOverflowPopUp(View anchor) {
        SimpleMenuListAdapter adapter =
                new HighlightedSimpleMenuListAdapter(this, R.menu.editor, true);
        MenuItem noSuggestions = adapter.getMenu().findItem(R.id.mnu_no_suggestions);
        if (noSuggestions != null) {
            if (this.mBinary) {
                adapter.getMenu().removeItem(R.id.mnu_no_suggestions);
            } else {
                noSuggestions.setChecked(this.mNoSuggestions);
            }
        }
        MenuItem wordWrap = adapter.getMenu().findItem(R.id.mnu_word_wrap);
        if (wordWrap != null) {
            if (this.mBinary) {
                adapter.getMenu().removeItem(R.id.mnu_word_wrap);
            } else {
                wordWrap.setChecked(this.mWordWrap);
            }
        }
        MenuItem syntaxHighlight = adapter.getMenu().findItem(R.id.mnu_syntax_highlight);
        if (syntaxHighlight != null) {
            if (this.mBinary) {
                adapter.getMenu().removeItem(R.id.mnu_syntax_highlight);
            } else {
                syntaxHighlight.setChecked(this.mSyntaxHighlight);
            }
        }

        final ListPopupWindow popup =
                DialogHelper.createListPopupWindow(this, adapter, anchor);
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(
                    final AdapterView<?> parent, final View v,
                    final int position, final long id) {
                final int itemId = (int)id;
                switch (itemId) {
                    case R.id.mnu_no_suggestions:
                        toggleNoSuggestions();
                        break;
                    case R.id.mnu_word_wrap:
                        toggleWordWrap();
                        break;
                    case R.id.mnu_syntax_highlight:
                        toggleSyntaxHighlight();
                        break;
                    case R.id.mnu_settings:
                        //Settings
                        Intent settings = new Intent(EditorActivity.this, SettingsPreferences.class);
                        settings.putExtra(
                                PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                EditorPreferenceFragment.class.getName());
                        startActivity(settings);
                        break;
                }
                popup.dismiss();
            }
        });
        popup.show();
    }

    /**
     * Method invoked when an action item is clicked.
     *
     * @param view The button pushed
     */
    public void onActionBarItemClick(View view) {
        switch (view.getId()) {
            case R.id.ab_button0:
                // Save the file
                checkAndWrite();
                break;

            case R.id.ab_button1:
                // Print the file
                StringBuilder sb = mBinary
                        ? ((HexDumpAdapter)mBinaryEditor.getAdapter()).toStringDocument()
                        : new StringBuilder(mEditor.getText().toString());
                PrintActionPolicy.printStringDocument(this, mFso, sb);
                break;

            case R.id.ab_button2:
                // Show overflow menu
                showOverflowPopUp(this.mOptionsAnchorView);
                break;

            default:
                break;
        }
    }

    /**
     * Method that initializes a console
     */
    private boolean initializeConsole() {
        try {
            ConsoleBuilder.getConsole(this);
            // There is a console allocated. Use it.
            return true;
        } catch (Throwable _throw) {
            // Capture the exception
            ExceptionUtil.translateException(this, _throw, false, true);
        }
        return false;
    }

    /**
     * Method that reads the requested file
     */
    private void readFile() {
        // For now editor is not dirty and editable.
        setDirty(false);
        this.mBinary = false;

        // Check for a valid action
        String action = getIntent().getAction();
        if (action == null ||
                (action.compareTo(Intent.ACTION_VIEW) != 0) &&
                (action.compareTo(Intent.ACTION_EDIT) != 0)) {
            DialogHelper.showToast(
                    this, R.string.editor_invalid_file_msg, Toast.LENGTH_SHORT);
            return;
        }
        // This var should be set depending on ACTION_VIEW or ACTION_EDIT action, but for
        // better compatibility, IntentsActionPolicy use always ACTION_VIEW, so we have
        // to ignore this check here
        this.mReadOnly = false;

        // Read the intent and check that is has a valid request
        String path = uriToPath(this, getIntent().getData());
        if (path == null || path.length() == 0) {
            DialogHelper.showToast(
                    this, R.string.editor_invalid_file_msg, Toast.LENGTH_SHORT);
            return;
        }

        // Set the title of the dialog
        File f = new File(path);
        this.mTitle.setText(f.getName());

        // Check that the file exists (the real file, not the symlink)
        try {
            this.mFso = CommandHelper.getFileInfo(this, path, true, null);
            if (this.mFso == null) {
                DialogHelper.showToast(
                        this, R.string.editor_file_not_found_msg, Toast.LENGTH_SHORT);
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get file reference", e); //$NON-NLS-1$
            DialogHelper.showToast(
                    this, R.string.editor_file_not_found_msg, Toast.LENGTH_SHORT);
            return;
        }

        // Check that we can handle the length of the file (by device)
        if (this.mMaxFileSize < this.mFso.getSize()) {
            DialogHelper.showToast(
                    this, R.string.editor_file_exceed_size_msg, Toast.LENGTH_SHORT);
            return;
        }

        // Get the syntax highlight processor
        SyntaxHighlightFactory shpFactory =
                SyntaxHighlightFactory.getDefaultFactory(new ResourcesResolver());
        this.mSyntaxHighlightProcessor = shpFactory.getSyntaxHighlightProcessor(f);
        if (this.mSyntaxHighlightProcessor != null) {
            this.mSyntaxHighlightProcessor.initialize();
        }

        // Check that we have read access
        try {
            FileHelper.ensureReadAccess(
                    ConsoleBuilder.getConsole(this),
                    this.mFso,
                    null);

            // Read the file in background
            asyncRead();

        } catch (Exception ex) {
            ExceptionUtil.translateException(
                    this, ex, false, true, new OnRelaunchCommandResult() {
                @Override
                public void onSuccess() {
                    // Read the file in background
                    asyncRead();
                }

                @Override
                public void onFailed(Throwable cause) {
                    finish();
                }

                @Override
                public void onCancelled() {
                    finish();
                }
            });
        }
    }

    /**
     * Method that does the read of the file in background
     * @hide
     */
    void asyncRead() {
        // Do the load of the file
        AsyncTask<FileSystemObject, Integer, Boolean> mReadTask =
                            new AsyncTask<FileSystemObject, Integer, Boolean>() {

            private Exception mCause;
            private AsyncReader mReader;
            private boolean changeToBinaryMode;
            private boolean changeToDisplaying;

            @Override
            protected void onPreExecute() {
                // Show the progress
                this.changeToBinaryMode = false;
                this.changeToDisplaying = false;
                doProgress(true, 0);
            }

            @Override
            protected Boolean doInBackground(FileSystemObject... params) {
                final EditorActivity activity = EditorActivity.this;

                // Only one argument (the file to open)
                FileSystemObject fso = params[0];
                this.mCause = null;

                // Read the file in an async listener
                try {
                    while (true) {
                        // Configure the reader
                        this.mReader = new AsyncReader(true);
                        this.mReader.mReadFso = fso;
                        this.mReader.mListener = new OnProgressListener() {
                            @Override
                            @SuppressWarnings("synthetic-access")
                            public void onProgress(int progress) {
                                publishProgress(Integer.valueOf(progress));
                            }
                        };

                        // Execute the command (read the file)
                        CommandHelper.read(activity, fso.getFullPath(), this.mReader,
                                           null);

                        // Wait for
                        synchronized (this.mReader.mSync) {
                            this.mReader.mSync.wait();
                        }

                        // 100%
                        publishProgress(new Integer(100));

                        // Check if the read was successfully
                        if (this.mReader.mCause != null) {
                            this.mCause = this.mReader.mCause;
                            return Boolean.FALSE;
                        }
                        break;
                    }

                    // Now we have the byte array with all the data. is a binary file?
                    // Then dump them byte array to hex dump string (only if users settings
                    // to dump file)
                    if (activity.mBinary && mHexDump) {
                        // we do not use the Hexdump helper class, because we need to show the
                        // progress of the dump process
                        final String data = toHexPrintableString(toHexDump(
                                this.mReader.mByteBuffer.toByteArray()));
                        this.mReader.mBinaryBuffer = new ArrayList<String>();
                        BufferedReader reader = new BufferedReader(new StringReader(data));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            this.mReader.mBinaryBuffer.add(line);
                        }
                        Log.i(TAG, "Bytes read: " + data.length()); //$NON-NLS-1$
                    } else {
                        String data;
                        if (this.mReader.mDetectedEncoding != null) {
                            data = new String(this.mReader.mByteBuffer.toByteArray(),
                                              this.mReader.mDetectedEncoding);
                        } else {
                            data = new String(this.mReader.mByteBuffer.toByteArray());
                        }
                        this.mReader.mBuffer = new SpannableStringBuilder(data);
                        Log.i(TAG, "Bytes read: " + data.getBytes().length); //$NON-NLS-1$
                    }
                    this.mReader.mByteBuffer = null;

                    // 100%
                    this.changeToDisplaying = true;
                    publishProgress(new Integer(0));

                } catch (Exception e) {
                    this.mCause = e;
                    return Boolean.FALSE;
                }

                return Boolean.TRUE;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                // Do progress
                doProgress(true, values[0].intValue());
            }

            @Override
            protected void onPostExecute(Boolean result) {
                final EditorActivity activity = EditorActivity.this;
                // Is error?
                if (!result.booleanValue()) {
                    if (this.mCause != null) {
                        ExceptionUtil.translateException(activity, this.mCause);
                        activity.mEditor.setEnabled(false);
                    }
                } else {
                    // Now we have the buffer, set the text of the editor
                    if (activity.mBinary && mHexDump) {
                        HexDumpAdapter adapter = new HexDumpAdapter(EditorActivity.this,
                                this.mReader.mBinaryBuffer);
                        mBinaryEditor.setAdapter(adapter);

                        // Cleanup
                        this.mReader.mBinaryBuffer = null;
                    } else {
                        activity.mEditor.setText(
                                this.mReader.mBuffer, BufferType.EDITABLE);

                        // Highlight editor text syntax
                        if (activity.mSyntaxHighlight &&
                            activity.mSyntaxHighlightProcessor != null) {
                            try {
                                activity.mSyntaxHighlightProcessor.process(
                                        activity.mEditor.getText());
                            } catch (Exception ex) {
                                // An error in a syntax library, should not break down app.
                                Log.e(TAG, "Syntax highlight failed.", ex); //$NON-NLS-1$
                            }
                        }

                        //Cleanup
                        this.mReader.mBuffer = null;
                    }

                    setDirty(false);
                    activity.mEditor.setEnabled(!activity.mReadOnly);

                    // Notify read-only mode
                    if (activity.mReadOnly) {
                        DialogHelper.showToast(
                                activity,
                                R.string.editor_read_only_mode,
                                Toast.LENGTH_SHORT);
                    }
                }

                doProgress(false, 0);
            }

            @Override
            protected void onCancelled() {
                // Hide the progress
                doProgress(false, 0);
            }

            /**
             * Method that update the progress status
             *
             * @param visible If the progress bar need to be hidden
             * @param progress The progress
             */
            private void doProgress(boolean visible, int progress) {
                final EditorActivity activity = EditorActivity.this;

                // Show the progress bar
                activity.mProgressBar.setProgress(progress);
                activity.mProgress.setVisibility(visible ? View.VISIBLE : View.GONE);

                if (this.changeToBinaryMode) {
                    mWordWrapView.setVisibility(View.GONE);
                    mNoWordWrapView.setVisibility(View.GONE);
                    mBinaryEditor.setVisibility(View.VISIBLE);

                    // Show hex dumping text
                    activity.mProgressBarMsg.setText(R.string.dumping_message);
                    this.changeToBinaryMode = false;
                }
                else if (this.changeToDisplaying) {
                    activity.mProgressBarMsg.setText(R.string.displaying_message);
                    this.changeToDisplaying = false;
                }
            }

            /**
             * Create a hex dump of the data while show progress to user
             *
             * @param data The data to hex dump
             * @return StringBuilder The hex dump buffer
             */
            private String toHexDump(byte[] data) {
                //Change to binary mode
                this.changeToBinaryMode = true;

                // Start progress
                publishProgress(Integer.valueOf(0));

                // Calculate max dir size
                int length = data.length;

                final int DISPLAY_SIZE = 16;  // Bytes per line
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                byte[] line = new byte[DISPLAY_SIZE];
                int read = 0;
                int offset = 0;
                StringBuilder sb = new StringBuilder();
                while ((read = bais.read(line, 0, DISPLAY_SIZE)) != -1) {
                    //offset   dump(16)   data\n
                    String linedata = new String(line, 0, read);
                    sb.append(HexDump.toHexString(offset));
                    sb.append(" "); //$NON-NLS-1$
                    String hexDump = HexDump.toHexString(line, 0, read);
                    if (hexDump.length() != (DISPLAY_SIZE * 2)) {
                        char[] array = new char[(DISPLAY_SIZE * 2) - hexDump.length()];
                        Arrays.fill(array, ' ');
                        hexDump += new String(array);
                    }
                    sb.append(hexDump);
                    sb.append(" "); //$NON-NLS-1$
                    sb.append(linedata);
                    sb.append(EditorActivity.this.mHexLineSeparator);
                    offset += DISPLAY_SIZE;
                    if (offset % 5 == 0) {
                        publishProgress(Integer.valueOf((offset * 100) / length));
                    }
                }

                // End of the dump process
                publishProgress(Integer.valueOf(100));

                return sb.toString();
            }

            /**
             * Method that converts to a visual printable hex string
             *
             * @param string The string to check
             */
            private String toHexPrintableString(String string) {
                // Remove characters without visual representation
                final String REPLACED_SYMBOL = "."; //$NON-NLS-1$
                final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
                String printable = string.replaceAll("\\p{Cntrl}", REPLACED_SYMBOL); //$NON-NLS-1$
                printable = printable.replaceAll("[^\\p{Print}]", REPLACED_SYMBOL); //$NON-NLS-1$
                printable = printable.replaceAll("\\p{C}", REPLACED_SYMBOL); //$NON-NLS-1$
                printable = printable.replaceAll(EditorActivity.this.mHexLineSeparator, NEWLINE);
                return printable;
            }
        };
        mReadTask.execute(this.mFso);
    }

    private void checkAndWrite() {
        // Check that we have write access
        try {
            FileHelper.ensureWriteAccess(
                    ConsoleBuilder.getConsole(this),
                    this.mFso,
                    null);

            // Write the file
            ensureSyncWrite();

        } catch (Exception ex) {
            ExceptionUtil.translateException(
                    this, ex, false, true, new OnRelaunchCommandResult() {
                @Override
                public void onSuccess() {
                    // Write the file
                    ensureSyncWrite();
                }

                @Override
                public void onFailed(Throwable cause) {/**NON BLOCK**/}

                @Override
                public void onCancelled() {/**NON BLOCK**/}
            });
        }
    }

    /**
     * Method that checks that the write to disk operation was successfully and the
     * expected bytes are written to disk.
     * @hide
     */
    void ensureSyncWrite() {
        try {
            for (int i = 0; i < WRITE_RETRIES; i++) {
                // Configure the writer
                AsyncWriter writer = new AsyncWriter();

                // Write to disk
                final byte[] data = this.mEditor.getText().toString().getBytes();
                long expected = data.length;
                syncWrite(writer, data);

                // Sleep a bit
                Thread.sleep(150L);

                // Is error?
                if (writer.mCause != null) {
                    Log.e(TAG, "Write operation failed. Retries: " + i, writer.mCause);
                    if (i == (WRITE_RETRIES-1)) {
                        // Something was wrong. The file probably is corrupted
                        DialogHelper.showToast(
                                this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
                        break;
                    }

                    // Retry
                    continue;
                }

                // Check that all the bytes were written
                FileSystemObject fso =
                        CommandHelper.getFileInfo(this, this.mFso.getFullPath(), true, null);
                if (fso == null || fso.getSize() != expected) {
                    Log.e(TAG, String.format(
                            "Size is not the same. Expected: %d, Written: %d. Retries: %d",
                            expected, fso == null ? -1 : fso.getSize(), i));
                    if (i == (WRITE_RETRIES-1)) {
                        // Something was wrong. The destination data is not the same
                        // as the source data
                        DialogHelper.showToast(
                                this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
                        break;
                    }

                    // Retry
                    continue;
                }

                // Success. The file was saved
                DialogHelper.showToast(
                        this, R.string.editor_successfully_saved, Toast.LENGTH_SHORT);
                setDirty(false);

                // Send a message that allow other activities to update his data
                Intent intent = new Intent(FileManagerSettings.INTENT_FILE_CHANGED);
                intent.putExtra(
                        FileManagerSettings.EXTRA_FILE_CHANGED_KEY, this.mFso.getFullPath());
                sendBroadcast(intent);

                // Done
                break;

            }
        } catch (Exception ex) {
            // Something was wrong, but the file was NOT written
            Log.e(TAG, "The file wasn't written.", ex);
            DialogHelper.showToast(
                    this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
        }
    }

    /**
     * Method that write the file.
     *
     * @param writer The command listener
     * @param bytes The bytes to write
     * @throws Exception If something was wrong
     */
    private void syncWrite(AsyncWriter writer, byte[] bytes) throws Exception {
        // Create the writable command
        WriteExecutable cmd =
                CommandHelper.write(this, this.mFso.getFullPath(), writer, null);

        // Obtain access to the buffer (IMP! don't close the buffer here, it's manage
        // by the command)
        OutputStream os = cmd.createOutputStream();
        try {
            // Retrieve the text from the editor
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try {
                // Buffered write
                byte[] data = new byte[this.mBufferSize];
                int read = 0, written = 0;
                while ((read = bais.read(data, 0, this.mBufferSize)) != -1) {
                    os.write(data, 0, read);
                    written += read;
                }
                Log.i(TAG, "Bytes written: " + written); //$NON-NLS-1$
            } finally {
                try {
                    bais.close();
                } catch (Exception e) {/**NON BLOCK**/}
            }

        } finally {
            // Ok. Data is written or ensure buffer close
            cmd.end();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(
            CharSequence s, int start, int count, int after) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.mEditStart = start;
        this.mEditEnd = start + count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        setDirty(true);
        if (this.mSyntaxHighlightProcessor != null) {
            this.mSyntaxHighlightProcessor.process(s, this.mEditStart, this.mEditEnd);
        }
    }

    /**
     * Method that sets if the editor is dirty (has changed)
     *
     * @param dirty If the editor is dirty
     * @hide
     */
    void setDirty(boolean dirty) {
        this.mDirty = dirty;
        this.mSave.setVisibility(dirty ? View.VISIBLE : View.GONE);
    }

    /**
     * Check the dirty state of the editor, and ask the user to save the changes
     * prior to exit.
     */
    public void checkDirtyState() {
        if (this.mDirty) {
            AlertDialog dlg = DialogHelper.createYesNoDialog(
                    this,
                    R.string.editor_dirty_ask_title,
                    R.string.editor_dirty_ask_msg,
                    new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                dialog.dismiss();
                                setResult(Activity.RESULT_OK);
                                finish();
                            }
                        }
                    });
            DialogHelper.delegateDialogShow(this, dlg);
            return;
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    /**
     * Method that applies the current theme to the activity
     * @hide
     */
    void applyTheme() {
        Theme theme = ThemeManager.getCurrentTheme(this);
        theme.setBaseTheme(this, false);

        //- ActionBar
        theme.setTitlebarDrawable(this, getActionBar(), "titlebar_drawable"); //$NON-NLS-1$
        View v = getActionBar().getCustomView().findViewById(R.id.customtitle_title);
        theme.setTextColor(this, (TextView)v, "action_bar_text_color"); //$NON-NLS-1$
        v = findViewById(R.id.ab_button0);
        theme.setImageDrawable(this, (ImageView)v, "ab_save_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_button1);
        theme.setImageDrawable(this, (ImageView)v, "ab_print_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.ab_button2);
        theme.setImageDrawable(this, (ImageView)v, "ab_overflow_drawable"); //$NON-NLS-1$
        //- View
        v = findViewById(R.id.editor_layout);
        theme.setBackgroundDrawable(this, v, "background_drawable"); //$NON-NLS-1$
        v = findViewById(R.id.editor);
        theme.setTextColor(this, (TextView)v, "text_color"); //$NON-NLS-1$
        //- ProgressBar
        Drawable dw = theme.getDrawable(this, "horizontal_progress_bar"); //$NON-NLS-1$
        this.mProgressBar.setProgressDrawable(dw);
        v = findViewById(R.id.editor_progress_msg);
        theme.setTextColor(this, (TextView)v, "text_color"); //$NON-NLS-1$

        // Need a full process of syntax highlight
        if (!this.mBinary && this.mSyntaxHighlight && this.mSyntaxHighlightProcessor != null) {
            reloadSyntaxHighlight();
        }
    }

    /**
     * Method that resolves the content uri to a valid system path
     *
     * @param ctx The current context
     * @param uri The content uri
     * @return String The system path
     */
    private static String uriToPath(Context ctx, Uri uri) {
        File file = MediaHelper.contentUriToFile(ctx.getContentResolver(), uri);
        if (file == null) {
            file = new File(uri.getPath());
        }
        return file.getAbsolutePath();
    }
}
