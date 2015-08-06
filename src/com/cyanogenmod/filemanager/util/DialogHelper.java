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

package com.cyanogenmod.filemanager.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.NavigationActivity;
import com.cyanogenmod.filemanager.adapters.CheckableListAdapter;
import com.cyanogenmod.filemanager.ui.ThemeManager;
import com.cyanogenmod.filemanager.ui.ThemeManager.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class with useful methods for deal with dialogs.
 */
public final class DialogHelper {

    /**
     * An interface to listen the selection make for the user.
     */
    public interface OnSelectChoiceListener {
        /**
         * Method invoked when the user select an option
         *
         * @param choice The selected option
         */
        public void onSelectChoice(int choice);
        /**
         * Method invoked when the user not select any option
         */
        public void onNoSelectChoice();
    }

    /**
     * Constructor of <code>DialogHelper</code>.
     */
    private DialogHelper() {
        super();
    }

    /**
     * Method that creates a new warning {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The resource identifier of the message of the alert dialog
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createWarningDialog(Context context, int title, int message) {
        return createWarningDialog(context, title, context.getString(message));
    }

    /**
     * Method that creates a new warning {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The message of the alert dialog
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createWarningDialog(Context context, int title, String message) {
        return createAlertDialog(
                context,
                0,
                title,
                message,
                false);
    }

    /**
     * Method that creates a new error {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The resource identifier of the message of the alert dialog
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createErrorDialog(Context context, int title, int message) {
        return createErrorDialog(context, title, context.getString(message));
    }

    /**
     * Method that creates a new error {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The message of the alert dialog
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createErrorDialog(Context context, int title, String message) {
        return createAlertDialog(
                context,
                0,
                title,
                message,
                false);
    }

    /**
     * Method that creates a new {@link AlertDialog}.
     *
     * @param context The current context
     * @param icon The icon resource
     * @param title The resource identifier of the title of the alert dialog
     * @param message The resource identifier of the message of the alert dialog
     * @param allCaps If the title must have his text in caps or not
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createAlertDialog(
            Context context, int icon, int title, int message, boolean allCaps) {
        return createAlertDialog(context, icon, title, context.getString(message), allCaps);
    }

    /**
     * Method that creates a new {@link AlertDialog}.
     *
     * @param context The current context
     * @param icon The icon resource
     * @param title The resource identifier of the title of the alert dialog
     * @param message The message of the alert dialog
     * @param allCaps If the title must have his text in caps or not
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createAlertDialog(
            Context context, int icon, int title, String message, boolean allCaps) {
        //Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, icon, context.getString(title), allCaps));
        builder.setView(createMessage(context, message));
        builder.setPositiveButton(context.getString(R.string.ok), null);
        return builder.create();
    }

    /**
     * Method that creates a new {@link AlertDialog} for choice between single options.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param options An array with the options
     * @param defOption The default option
     * @param onSelectChoiceListener The listener for user choice
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createSingleChoiceDialog(
            Context context, int title,
            String[] options, int defOption,
            final OnSelectChoiceListener onSelectChoiceListener) {
        //Create the alert dialog
        final StringBuffer item = new StringBuffer().append(defOption);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, 0, context.getString(title), false));

        // Create the adapter
        List<CheckableListAdapter.CheckableItem> items =
                new ArrayList<CheckableListAdapter.CheckableItem>(options.length);
        int cc = options.length;
        for (int i = 0; i < cc; i++) {
            boolean checked = (i == defOption);
            items.add(new CheckableListAdapter.CheckableItem(options[i], true, checked));
        }
        final CheckableListAdapter adapter = new CheckableListAdapter(context, items, true);

        // Create the list view and set as view
        final ListView listView = new ListView(context);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        listView.setLayoutParams(params);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                item.delete(0, item.length());
                item.append(position);
                adapter.setSelectedItem(position);
            }
        });
        adapter.setSelectedItem(defOption);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        builder.setView(listView);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, listView, "background_drawable"); //$NON-NLS-1$
        listView.setDivider(
                theme.getDrawable(context, "horizontal_divider_drawable")); //$NON-NLS-1$

        builder.setNegativeButton(context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSelectChoiceListener.onNoSelectChoice();
                dialog.cancel();
            }
        });
        builder.setPositiveButton(context.getString(R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSelectChoiceListener.onSelectChoice(Integer.parseInt(item.toString()));
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    /**
     * Method that creates a new YES/NO {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The resource identifier of the message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createYesNoDialog(
            Context context, int title, int message, OnClickListener onClickListener) {
        return createYesNoDialog(context, title, context.getString(message), onClickListener);
    }

    /**
     * Method that creates a new YES/NO {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createYesNoDialog(
            Context context, int title, String message, OnClickListener onClickListener) {
        //Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(
                createTitle(
                        context,
                        0,
                        context.getString(title),
                        false));
        builder.setView(createMessage(context, message));
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(R.string.yes), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.no), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a new YES/ALL/NO {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The resource identifier of the message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createYesNoAllDialog(
            Context context, int title, int message, OnClickListener onClickListener) {
        return createYesNoAllDialog(context, title, context.getString(message), onClickListener);
    }

    /**
     * Method that creates a new YES/ALL/NO {@link AlertDialog}.
     *
     * @param context The current context
     * @param title The resource identifier of the title of the alert dialog
     * @param message The message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createYesNoAllDialog(
            Context context, int title, String message, OnClickListener onClickListener) {
        //Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(
                createTitle(
                        context,
                        0,
                        context.getString(title),
                        false));
        builder.setView(createMessage(context, message));
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(R.string.yes), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.no), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.all), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a two buttons question {@link AlertDialog}.
     *
     * @param context The current context
     * @param button1 The resource identifier of the text of the button 1 (POSITIVE)
     * @param button2 The resource identifier of the text of the button 2 (NEGATIVE)
     * @param title The resource id of the title of the alert dialog
     * @param message The message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createTwoButtonsQuestionDialog(
            Context context, int button1, int button2,
            int title, String message, OnClickListener onClickListener) {
        //Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(
                createTitle(
                        context,
                        0,
                        context.getString(title),
                        false));
        builder.setView(createMessage(context, message));
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(button1), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(button2), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a three buttons question {@link AlertDialog}.
     *
     * @param context The current context
     * @param button1 The resource identifier of the text of the button 1 (POSITIVE)
     * @param button2 The resource identifier of the text of the button 2 (NEUTRAL)
     * @param button3 The resource identifier of the text of the button 3 (NEGATIVE)
     * @param title The resource id of the title of the alert dialog
     * @param message The message of the alert dialog
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createThreeButtonsQuestionDialog(
            Context context, int button1, int button2, int button3,
            int title, String message, OnClickListener onClickListener) {
        //Create the alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(
                createTitle(
                        context,
                        0,
                        context.getString(title),
                        false));
        builder.setView(createMessage(context, message));
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(button1), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(button2), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(button3), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a new {@link AlertDialog}.
     *
     * @param context The current context
     * @param icon The icon resource
     * @param title The resource identifier of the title of the alert dialog
     * @param content The content layout
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createDialog(Context context, int icon, int title, View content) {
        return createDialog(context, icon, context.getString(title), content);
    }

    /**
     * Method that creates a new {@link AlertDialog}.
     *
     * @param context The current context
     * @param icon The icon resource
     * @param title The title of the alert dialog
     * @param content The content layout
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createDialog(Context context, int icon, String title, View content) {
        //Create the alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, icon, title, false));
        builder.setView(content);
        return builder.create();
    }

    /**
     * Method that creates a new {@link AlertDialog} with one buttons.
     *
     * @param context The current context
     * @param button1 The resource identifier of the text of the button 1 (POSITIVE)
     * @param icon The icon resource
     * @param title The title of the alert dialog
     * @param content The content layout
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createOneButtonsDialog(Context context, int button1,
            int icon, String title, View content, OnClickListener onClickListener) {
        //Create the alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, icon, title, false));
        builder.setView(content);
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(button1), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a new {@link AlertDialog} with two buttons.
     *
     * @param context The current context
     * @param button1 The resource identifier of the text of the button 1 (POSITIVE)
     * @param button2 The resource identifier of the text of the button 2 (NEUTRAL)
     * @param icon The icon resource
     * @param title The title of the alert dialog
     * @param content The content layout
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createTwoButtonsDialog(Context context, int button1, int button2,
            int icon, String title, View content, OnClickListener onClickListener) {
        //Create the alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, icon, title, false));
        builder.setView(content);
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(button1), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(button2), onClickListener);
        return dialog;
    }

    /**
     * Method that creates a new {@link AlertDialog} with three buttons.
     *
     * @param context The current context
     * @param button1 The resource identifier of the text of the button 1 (POSITIVE)
     * @param button2 The resource identifier of the text of the button 2 (NEUTRAL)
     * @param button3 The resource identifier of the text of the button 3 (NEGATIVE)
     * @param icon The icon resource
     * @param title The title of the alert dialog
     * @param content The content layout
     * @param onClickListener The listener where returns the button pressed
     * @return AlertDialog The alert dialog reference
     */
    public static AlertDialog createThreeButtonsDialog(Context context, int button1, int button2,
            int button3, int icon, String title, View content, OnClickListener onClickListener) {
        //Create the alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(createTitle(context, icon, title, false));
        builder.setView(content);
        AlertDialog dialog = builder.create();
        dialog.setButton(
                DialogInterface.BUTTON_POSITIVE, context.getString(button1), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(button2), onClickListener);
        dialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, context.getString(button3), onClickListener);
        return dialog;
    }

    /**
     * Method that creates and returns the title of the dialog.
     *
     * @param context The current context
     * @param icon The icon resource
     * @param title The resource identifier of the title of the alert dialog
     * @param allCaps If the title must have his text in caps or not
     * @return The title view
     */
    private static View createTitle(Context context, int icon, String title, boolean allCaps) {
        //Inflate the dialog layouts
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View lyTitle = li.inflate(R.layout.dialog_title, null);
        ImageView vIcon = (ImageView)lyTitle.findViewById(R.id.dialog_title_icon);
        if (icon != 0) {
            vIcon.setBackgroundResource(icon);
        } else {
            vIcon.setVisibility(View.GONE);
        }
        TextView vText = (TextView)lyTitle.findViewById(R.id.dialog_title_text);
        if (allCaps) {
            vText.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        }
        vText.setText(title);

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, lyTitle, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(context, vText, "dialog_text_color"); //$NON-NLS-1$

        return lyTitle;
    }

    /**
     * Method that creates and returns the title of the dialog.
     *
     * @param context The current context
     * @param message The the message of the alert dialog
     * @return The title view
     */
    private static View createMessage(Context context, String message) {
        //Inflate the dialog layouts
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View lyMessage = li.inflate(
                            R.layout.dialog_message,
                            null);
        TextView vMsg = (TextView)lyMessage.findViewById(R.id.dialog_message);
        // Dialog need to be filled with at least two lines to fill the background dialog,
        // so we add a new additional line to the message
        vMsg.setText(message + "\n"); //$NON-NLS-1$

        // Apply the current theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setBackgroundDrawable(context, lyMessage, "background_drawable"); //$NON-NLS-1$
        theme.setTextColor(context, vMsg, "text_color"); //$NON-NLS-1$

        return lyMessage;
    }

    /**
     * Method that creates and returns a {@list ListPopupWindow} reference.
     *
     * @param context The current context
     * @param adapter The adapter to associate with the popup
     * @param anchor The view that is used as an anchor to show the popup
     * @return ListPopupWindow The {@list ListPopupWindow} reference
     */
    public static ListPopupWindow createListPopupWindow(
            Context context, final ListAdapter adapter, View anchor) {
        final ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAdapter(adapter);
        popup.setContentWidth(context.getResources().getDimensionPixelSize(R.dimen.popup_width));
        popup.setAnchorView(anchor);
        popup.setModal(true);

        // HACK: set the height to the size of the device screen width or height depending on
        // which is smaller.
        // Because we are handling the orientation change instead of letting
        // android do its thing, this never redraws. If we remove the orientation
        // change, then all the other popup dialogs break when rotating.
        // This isn't perfect, but without some significant changes to this app we cannot
        // properly do this.
        // Get the screen size, determine what number is lower, use that for the height
        DisplayMetrics dM = context.getResources().getDisplayMetrics();
        float width = dM.widthPixels / dM.density;
        float height = dM.heightPixels / dM.density;
        popup.setHeight((int)Math.min(height, width));

        return popup;
    }

    /**
     * Method that delegates the display of a dialog. This method applies the style to the
     * dialog, so all dialogs of the application MUST used this method to display the dialog.
     *
     * @param context The current context
     * @param dialog The dialog to show
     */
    public static void delegateDialogShow(Context context, AlertDialog dialog) {
        boolean isActivityFinishing = false;
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity instanceof NavigationActivity) {
                ((NavigationActivity) activity).updateActiveDialog(dialog);
            }
            isActivityFinishing = activity.isFinishing();
        }

        if (!isActivityFinishing) {
            // Show the dialog
            dialog.show();
        }

        // Apply theme
        Theme theme = ThemeManager.getCurrentTheme(context);
        theme.setDialogStyle(context, dialog);
    }

    /**
     * Method that creates and display a toast dialog.
     *
     * @param context The context to use.
     * @param msg The message to display.
     * @param duration How long to display the message.
     */
    public static void showToast(Context context, String msg, int duration) {
        Toast.makeText(context, msg, duration).show();
    }

    /**
     * Method that creates and display a toast dialog.
     *
     * @param context The context to use.
     * @param msgResourceId The resource id of the string resource to use.
     * @param duration How long to display the message.
     */
    public static void showToast(Context context, int msgResourceId, int duration) {
        showToast(context, context.getString(msgResourceId), duration);
    }

}
