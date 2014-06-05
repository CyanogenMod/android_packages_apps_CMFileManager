/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package afzkl.development.mColorPicker.views;

import afzkl.development.mColorPicker.views.ColorPickerView.OnColorChangedListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A view use directly into a dialog. It contains a one {@link ColorPickerView}
 * and two {@link ColorPanelView} (the current color and the new color)
 */
public class ColorDialogView extends RelativeLayout
    implements OnColorChangedListener, TextWatcher {

    private static final int DEFAULT_MARGIN_DP = 16;
    private static final int DEFAULT_PANEL_HEIGHT_DP = 32;
    private static final int DEFAULT_TEXT_SIZE_SP = 12;
    private static final int DEFAULT_LABEL_TEXT_SIZE_SP = 18;

    private ColorPickerView mPickerView;
    private ColorPanelView mCurrentColorView;
    private ColorPanelView mNewColorView;
    private TextView tvCurrent;
    private TextView tvNew;
    private TextView tvColorLabel;
    private EditText etColor;

    private String mCurrentLabelText = "Current:"; //$NON-NLS-1$
    private String mNewLabelText = "New:"; //$NON-NLS-1$

    private String mColorLabelText = "Color:"; //$NON-NLS-1$

    /**
     * Constructor of <code>ColorDialogView</code>
     *
     * @param context The current context
     */
    public ColorDialogView(Context context) {
        this(context, null);
    }

    /**
     * Constructor of <code>ColorDialogView</code>
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public ColorDialogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor of <code>ColorDialogView</code>
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public ColorDialogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    @SuppressLint("NewApi") 
    private void init() {
        // To fight color branding.
        ((Activity)getContext()).getWindow().setFormat(PixelFormat.RGBA_8888);

        // Create the scrollview over the dialog
        final int dlgMarging = (int)convertDpToPixel(DEFAULT_MARGIN_DP);
        ScrollView sv = new ScrollView(getContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	sv.setId(generateViewId());
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dlgMarging, 0, dlgMarging, 0);
        sv.setLayoutParams(lp);
        sv.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);

        // Now the vertical layout
        LinearLayout ll = new LinearLayout(getContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	ll.setId(generateViewId());
        }
        lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        ll.setLayoutParams(lp);
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);

        // Creates the color input field
        int id = createColorInput(ll);

        // Creates the color picker
        id = createColorPicker(ll, id);

        // Creates the current color and new color panels
        id = createColorsPanel(ll, id);

        // Add the scrollview
        addView(sv);

        // Sets the input color
        this.etColor.setText(toHex(this.mNewColorView.getColor()));
    }

    /**
     * Method that creates the color input
     *
     * @param parent The parent layout
     */
    @SuppressLint("NewApi")
    private int createColorInput(ViewGroup parent) {
        final int dlgMarging = (int)convertDpToPixel(DEFAULT_MARGIN_DP);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        lp2.setMargins(0, 0, dlgMarging, 0);
        this.tvColorLabel = new TextView(getContext());
        this.tvColorLabel.setText(this.mColorLabelText);
        this.tvColorLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_LABEL_TEXT_SIZE_SP);
        this.tvColorLabel.setGravity(Gravity.BOTTOM | Gravity.LEFT);
        this.tvColorLabel.setLayoutParams(lp2);

        lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        this.etColor = new EditText(getContext());
        this.etColor.setSingleLine();
        this.etColor.setGravity(Gravity.TOP | Gravity.LEFT);
        this.etColor.setCursorVisible(true);
        this.etColor.setImeOptions(
                EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        this.etColor.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.etColor.setLayoutParams(lp2);
        InputFilter[] filters = new InputFilter[2];
        filters[0] = new InputFilter.LengthFilter(8);
        filters[1] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start,
                    int end, Spanned dest, int dstart, int dend) {
                if (start >= end) return ""; //$NON-NLS-1$
                String s = source.subSequence(start, end).toString();
                StringBuilder sb = new StringBuilder();
                int cc = s.length();
                for (int i = 0; i < cc; i++) {
                    char c = s.charAt(i);
                    if ((c >= '0' && c <= '9') ||
                        (c >= 'a' && c <= 'f') ||
                        (c >= 'A' && c <= 'F')) {
                        sb.append(c);
                    }
                }
                return sb.toString().toUpperCase();
            }
        };
        this.etColor.setFilters(filters);
        this.etColor.addTextChangedListener(this);

        LinearLayout ll1 = new LinearLayout(getContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	ll1.setId(generateViewId());
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dlgMarging, 0, dlgMarging, 0);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        ll1.setLayoutParams(lp);
        ll1.addView(this.tvColorLabel);
        ll1.addView(this.etColor);
        parent.addView(ll1);

        return ll1.getId();
    }

    /**
     * Method that creates the color picker
     *
     * @param parent The parent layout
     * @param belowOf The anchor view
     * @return id The layout id
     */
    @SuppressLint("NewApi") 
    private int createColorPicker(ViewGroup parent, int belowOf) {
        final int dlgMarging = (int)convertDpToPixel(DEFAULT_MARGIN_DP);
        this.mPickerView = new ColorPickerView(getContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	this.mPickerView.setId(generateViewId());
        }
        this.mPickerView.setOnColorChangedListener(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dlgMarging, 0, dlgMarging, 0);
        lp.addRule(RelativeLayout.BELOW, belowOf);
        this.mPickerView.setLayoutParams(lp);
        parent.addView(this.mPickerView);
        return this.mPickerView.getId();
    }

    /**
     * Method that creates the colors panel (current and new)
     *
     * @param parent The parent layout
     * @param belowOf The anchor view
     * @return id The layout id
     */
    @SuppressLint("NewApi")
    private int createColorsPanel(ViewGroup parent, int belowOf) {
        final int dlgMarging = (int)convertDpToPixel(DEFAULT_MARGIN_DP);
        final int panelHeight = (int)convertDpToPixel(DEFAULT_PANEL_HEIGHT_DP);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                1);

        // Titles
        this.tvCurrent = new TextView(getContext());
        lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1);
        this.tvCurrent.setLayoutParams(lp2);
        this.tvCurrent.setText(this.mCurrentLabelText);
        this.tvCurrent.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP);
        this.tvNew = new TextView(getContext());
        this.tvNew.setLayoutParams(lp2);
        this.tvNew.setText(this.mNewLabelText);
        this.tvNew.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP);
        TextView sep1 = new TextView(getContext());
        lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                0);
        lp2.setMargins(dlgMarging, 0, dlgMarging, 0);
        sep1.setLayoutParams(lp2);
        sep1.setText(" "); //$NON-NLS-1$
        sep1.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP);

        LinearLayout ll1 = new LinearLayout(getContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
        	ll1.setId(generateViewId());
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dlgMarging, 0, dlgMarging, dlgMarging/2);
        lp.addRule(RelativeLayout.BELOW, belowOf);
        ll1.setLayoutParams(lp);
        ll1.addView(this.tvCurrent);
        ll1.addView(sep1);
        ll1.addView(this.tvNew);
        parent.addView(ll1);

        // Color panels
        lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                1);
        this.mCurrentColorView = new ColorPanelView(getContext());
        this.mCurrentColorView.setLayoutParams(lp2);
        this.mNewColorView = new ColorPanelView(getContext());
        this.mNewColorView.setLayoutParams(lp2);
        TextView sep2 = new TextView(getContext());
        lp2 = new LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                0);
        lp2.setMargins(dlgMarging, 0, dlgMarging, 0);
        sep2.setLayoutParams(lp2);
        sep2.setText("-"); //$NON-NLS-1$
        sep2.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP);

        LinearLayout ll2 = new LinearLayout(getContext());
        ll2.setId(generateViewId());
        lp = new RelativeLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, panelHeight);
        lp.setMargins(dlgMarging, 0, dlgMarging, dlgMarging/2);
        lp.addRule(RelativeLayout.BELOW, ll1.getId());
        ll2.setLayoutParams(lp);
        ll2.addView(this.mCurrentColorView);
        ll2.addView(sep2);
        ll2.addView(this.mNewColorView);
        parent.addView(ll2);

        return ll2.getId();
    }

    /**
     * Method that returns the color of the picker
     *
     * @return The ARGB color
     */
    public int getColor() {
        return this.mPickerView.getColor();
    }

    /**
     * Method that set the color of the picker
     *
     * @param argb The ARGB color
     */
    public void setColor(int argb) {
        setColor(argb, false);
    }

    /**
     * Method that set the color of the picker
     *
     * @param argb The ARGB color
     * @param fromEditText If the call comes from the <code>EditText</code>
     */
    private void setColor(int argb, boolean fromEditText) {
        this.mPickerView.setColor(argb, false);
        this.mCurrentColorView.setColor(argb);
        this.mNewColorView.setColor(argb);
        if (!fromEditText) {
            this.etColor.setText(toHex(this.mNewColorView.getColor()));
        }
    }

    /**
     * Method that display/hide the alpha slider
     *
     * @param show If the alpha slider should be shown
     */
    public void showAlphaSlider(boolean show) {
        this.mPickerView.setAlphaSliderVisible(show);
    }

    /**
     * Set the text that should be shown in the alpha slider.
     * Set to null to disable text.
     *
     * @param text Text that should be shown.
     */
    public void setAlphaSliderText(String text) {
        this.mPickerView.setAlphaSliderText(text);
    }

    /**
     * Set the text that should be shown in the actual color panel.
     * Set to null to disable text.
     *
     * @param text Text that should be shown.
     */
    public void setCurrentColorText(String text) {
        this.mCurrentLabelText = text;
        this.tvCurrent.setText(this.mCurrentLabelText);
    }

    /**
     * Set the text that should be shown in the new color panel.
     * Set to null to disable text.
     *
     * @param text Text that should be shown.
     */
    public void setNewColorText(String text) {
        this.mNewLabelText = text;
        this.tvNew.setText(this.mNewLabelText);
    }

    /**
     * Set the text that should be shown in the label of the color input.
     * Set to null to disable text.
     *
     * @param text Text that should be shown.
     */
    public void setColorLabelText(String text) {
        this.mColorLabelText = text;
        this.tvColorLabel.setText(this.mColorLabelText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onColorChanged(int color) {
        this.mNewColorView.setColor(color);
        this.etColor.removeTextChangedListener(this);
        this.etColor.setText(toHex(this.mNewColorView.getColor()));
        this.etColor.addTextChangedListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {/**NON BLOCK**/}

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 8) {
            try {
                setColor(toARGB(s.toString()), true);
            } catch (Exception e) {/**NON BLOCK**/}
        }
    }

    /**
     * This method converts dp unit to equivalent device specific value in pixels.
     *
     * @param ctx The current context
     * @param dp A value in dp (Device independent pixels) unit
     * @return float A float value to represent Pixels equivalent to dp according to device
     */
    private float convertDpToPixel(float dp) {
        Resources resources = getContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * Method that converts an ARGB color to its hex string color representation
     *
     * @param argb The ARGB color
     * @return String The hex string representation of the color
     */
    private static String toHex(int argb) {
        StringBuilder sb = new StringBuilder();
        sb.append(toHexString((byte)Color.alpha(argb)));
        sb.append(toHexString((byte)Color.red(argb)));
        sb.append(toHexString((byte)Color.green(argb)));
        sb.append(toHexString((byte)Color.blue(argb)));
        return sb.toString();
    }

    /**
     * Method that converts an hex string color representation to an ARGB color
     *
     * @param hex The hex string representation of the color
     * @return int The ARGB color
     */
    private static int toARGB(String hex) {
        return Color.parseColor("#" + hex); //$NON-NLS-1$
    }

    /**
     * Method that converts a byte into its hex string representation
     *
     * @param v The value to convert
     * @return String The hex string representation
     */
    private static String toHexString(byte v) {
        String hex = Integer.toHexString(v & 0xff);
        if (hex.length() == 1) {
            hex = "0" + hex; //$NON-NLS-1$
        }
        return hex.toUpperCase();
    }
}
