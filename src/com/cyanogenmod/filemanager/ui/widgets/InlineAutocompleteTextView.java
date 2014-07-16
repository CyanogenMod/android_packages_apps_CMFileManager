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

package com.cyanogenmod.filemanager.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import os.ransj.filemanager.R;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;
import com.cyanogenmod.filemanager.util.DialogHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A widget that performs an inline autocomplete for an {@link EditText}
 * control.
 */
public class InlineAutocompleteTextView extends RelativeLayout
            implements View.OnClickListener, View.OnLongClickListener,
            View.OnKeyListener {

    /**
     * An interface to communicate events when a text value is changed.
     */
    public interface OnTextChangedListener {
        /**
         * Method invoked when the value of the initial directory was changed.
         *
         * @param newValue The new value of the text
         * @param currentFilterData The current set of string for filter
         */
        void onTextChanged(String newValue, List<String> currentFilterData);
    }

    private List<String> mData;
    private OnTextChangedListener mOnTextChangedListener;

    private EditText mBackgroundText;
    private EditText mForegroundText;
    private String mCompletionString = null;

    private int mFilter = 0;
    private FilteredTextWatcher mTextWatcher;

    /**
     * Constructor of <code>InlineAutocompleteTextView</code>.
     *
     * @param context The current context
     */
    public InlineAutocompleteTextView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructor of <code>InlineAutocompleteTextView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public InlineAutocompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor of <code>InlineAutocompleteTextView</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public InlineAutocompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        //Initialize data
        this.mData = new ArrayList<String>();
        this.mOnTextChangedListener = null;
        this.mTextWatcher = new FilteredTextWatcher();

        //Inflate the view
        ViewGroup v = (ViewGroup)inflate(getContext(), R.layout.inline_autocomplete, null);
        addView(v);

        //Retrieve views
        this.mBackgroundText = (EditText)findViewById(R.id.inline_autocomplete_bg_text);
        this.mBackgroundText.setOnKeyListener(this);
        this.mForegroundText = (EditText)findViewById(R.id.inline_autocomplete_fg_text);
        this.mForegroundText.setMovementMethod(new ScrollingMovementMethod());
        this.mForegroundText.addTextChangedListener(this.mTextWatcher);
        this.mForegroundText.setOnKeyListener(this);
        this.mForegroundText.requestFocus();
        this.mForegroundText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                doDone(true);
                return false;
            }
        });

        View button = findViewById(R.id.inline_autocomplete_button_tab);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);

        // Apply the theme
        applyTheme();

        //Initialize text views
        setText(""); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Show the soft keyboard (only if the device has't a hardware keyboard)
        Configuration config = getContext().getResources().getConfiguration();
        if (config.keyboard == Configuration.KEYBOARD_NOKEYS) {
            Activity activity = (Activity)getContext();
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInputFromInputMethod(
                    this.mForegroundText.getWindowToken(), 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Hide the soft keyboard
        Configuration config = getContext().getResources().getConfiguration();
        if (config.keyboard == Configuration.KEYBOARD_NOKEYS) {
            Activity activity = (Activity)getContext();
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.mForegroundText.getWindowToken(), 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.FLAG_EDITOR_ACTION:
                //Consume the event
                doDone(false);
                return true;
            case KeyEvent.KEYCODE_TAB:
                //Do tab, and consume the event
                doTab();
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * Method that sets the current text value.
     *
     * @param value The value to set to current text value
     */
    public void setText(String value) {
        this.mBackgroundText.setVisibility(View.INVISIBLE);
        this.mForegroundText.setText(value);
        this.mBackgroundText.setText(""); //$NON-NLS-1$
        onTextChanged(value);
        this.mForegroundText.requestFocus();
        this.mForegroundText.setSelection(value.length());
        int lines = this.mBackgroundText.getLineCount();
        this.mForegroundText.setLines(lines <= 0 ? 1 : lines);
    }

    /**
     * Method that returns the current text value.
     *
     * @return The current text value
     */
    public String getText() {
        return this.mForegroundText.getText().toString();
    }

    /**
     * Method that sets the listener for send text change events.
     *
     * @param onTextChangedListener The text changed listener
     */
    public void setOnTextChangedListener(OnTextChangedListener onTextChangedListener) {
        this.mOnTextChangedListener = onTextChangedListener;
    }

    /**
     * Method that returns a string for autocomplete.
     *
     * @return String The autocomplete string
     */
    public String getCompletionString() {
        return this.mCompletionString;
    }

    /**
     * Method that sets a string for autocomplete.
     *
     * @param completionString The autocomplete string
     */
    public void setCompletionString(String completionString) {
        this.mCompletionString = completionString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.inline_autocomplete_button_tab:
                doTab();
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.inline_autocomplete_button_tab:
                //Complete with current suggestion
                String filter = this.mBackgroundText.getText().toString();
                if (this.mBackgroundText.getVisibility() == View.VISIBLE && filter.length() > 0) {
                    setText(filter);
                } else {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            DialogHelper.showToast(
                                    getContext(),
                                    R.string.inline_autocomplete_tab_nothing_to_complete_msg,
                                    Toast.LENGTH_SHORT);
                        }
                    });
                }
                return true;

            default:
                break;
        }
        return false;
    }

    /**
     * Method that need to be invoked when a string was changed
     *
     * @param value The new string
     * @hide
     */
    void onTextChanged(String value) {
        this.mFilter = 0;
        if (this.mOnTextChangedListener != null) {
            //Communicate the change
            this.mOnTextChangedListener.onTextChanged(
                                value.toString(), this.mData);

            if (this.mCompletionString != null &&
                    !value.toString().endsWith(this.mCompletionString)) {
                //Autocomplete
                autocomplete();
            } else {
                this.mBackgroundText.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Method that autocompletes the text, showing the best matches.
     */
    private void autocomplete() {
        final String currentText = getText();
        if (!this.mData.isEmpty()) {
            Iterator<String> it = this.mData.iterator();
            while (it.hasNext()) {
                String filterData = it.next();
                if (filterData.startsWith(currentText)) {
                    this.mBackgroundText.setText(filterData);
                    this.mBackgroundText.setVisibility(View.VISIBLE);
                    int lines = this.mBackgroundText.getLineCount();
                    this.mForegroundText.setLines(lines <= 0 ? 1 : lines);
                    return;
                }
            }
        }
        this.mBackgroundText.setVisibility(View.INVISIBLE);
    }

    /**
     * Method invoked when a tab key event is requested (button or keyboard)
     * @hide
     */
    void doTab() {
        //Complete with current text
        String current = this.mForegroundText.getText().toString();
        if (current.length() == 0) {
            return;
        }

        //Get the data
        List<String> filteredData = filter(this.mData, current);
        if (filteredData.size() <= this.mFilter) {
            this.mFilter = 0;
        }
        if (filteredData.size() == 1 && this.mFilter == 0) {
            //Autocomplete with the only autocomplete option
            setText(filteredData.get(this.mFilter));
        } else {
            //Show the autocomplete options
            if (filteredData.size() > 0) {
                this.mBackgroundText.setText(filteredData.get(this.mFilter));
                this.mBackgroundText.setVisibility(View.VISIBLE);
                this.mFilter++;
            }
        }
    }

    /**
     * Method that creates a temporary filter based in the current text
     *
     * @param data The global data array
     * @param current The current text
     * @return The filtered data array
     */
    private static List<String> filter(List<String> data, String current) {
        List<String> filter = new ArrayList<String>(data);
        int size = filter.size();
        for (int i = size-1; i >= 0; i--) {
            String s = filter.get(i);
            if (!s.startsWith(current)) {
                filter.remove(i);
            }
        }
        return filter;
    }

    /**
     * Method invoked when a enter key event is requested (button or keyboard)
     *
     * @param fromEditorAction It this method was invoked from editor action
     * @hide
     */
    void doDone(boolean fromEditorAction) {
        if (fromEditorAction) {
            // Hide the soft keyboard
            Configuration config = getContext().getResources().getConfiguration();
            if (config.keyboard == Configuration.KEYBOARD_NOKEYS) {
                Activity activity = (Activity)getContext();
                InputMethodManager imm =
                        (InputMethodManager) activity.getSystemService(
                                                Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(this.mForegroundText.getWindowToken(), 0);
            }
        }
    }

    /**
     * A class for filter the text introduced by the user
     */
    private class FilteredTextWatcher implements TextWatcher {

        private String mText;
        private int mStart;
        private int mCount;

        public FilteredTextWatcher() {/**NON BLOCK**/}

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            /**NON BLOCK**/
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            this.mStart = start;
            this.mCount = count;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterTextChanged(Editable s) {
            // Enter and Tab are not allowed, and have their own treatment
            final String orig = s.toString();
            String text = orig;
            final int start = this.mStart;
            final int count = this.mCount;

            // Avoid recursive calls
            if (this.mText != null && this.mText.compareTo(orig) == 0) {
                return;
            }

            // Force to ignore Tab and Enter keys
            text = replaceKeyEvent(text, '\t', true);
            text = replaceKeyEvent(text, '\r', false);
            text = replaceKeyEvent(text, '\n', false);
            if (text.compareTo(orig) != 0) {
                // Some key has internally changed
                this.mText = text;
                s.replace(0, orig.length(), text);
            }

            // Does the user input enter or tab keys?
            boolean tab = false;
            boolean enter = false;
            String userInput = orig.substring(start, start + count);
            tab = (userInput.indexOf("\t") != -1); //$NON-NLS-1$
            enter = (userInput.indexOf("\r") != -1 ||  //$NON-NLS-1$
                     userInput.indexOf("\n") != -1); //$NON-NLS-1$

            // Check events
            if (enter) {
                // Broadcast enter event
                doDone(false);
                return;
            }
            if (tab) {
                // Broadcast enter event
                doTab();
                return;
            }

            // Broadcast the new text value
            InlineAutocompleteTextView.this.onTextChanged(text);
        }

        /**
         * Method that replace a key char
         *
         * @param value The string in which search
         * @param key The key char to search
         * @param removeAfter If true the string will truncate after the first key char found
         * @return String The replaced string
         */
        private String replaceKeyEvent(String value, char key, boolean removeAfter) {
            String text = value;
            int pos = text.indexOf(key);
            while (pos != -1) {
                if (removeAfter) {
                    text = text.substring(0, pos);
                } else {
                    text = text.replaceAll(String.valueOf(key), ""); //$NON-NLS-1$
                }
                pos = text.indexOf(key);
            }
            return text;
        }
    }

    /**
     * Method that applies the current theme to the widget components
     */
    public void applyTheme() {
        Theme theme = ThemeManager.getCurrentTheme(getContext());
        View v = findViewById(R.id.inline_autocomplete_bg_text);
        theme.setTextColor(getContext(), (TextView)v, "text_color"); //$NON-NLS-1$
        v = findViewById(R.id.inline_autocomplete_fg_text);
        theme.setTextColor(getContext(), (TextView)v, "text_color"); //$NON-NLS-1$
        v = findViewById(R.id.inline_autocomplete_button_tab);
        theme.setImageDrawable(
                getContext(), (ImageView)v, "ab_tab_drawable"); //$NON-NLS-1$
    }

}
