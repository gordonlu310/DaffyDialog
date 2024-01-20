package com.gordonlu.daffydialog;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.Options;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.HorizontalAlignment;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.VerticalAlignment;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.MediaUtil;

import com.gordonlu.daffydialog.helpers.Font;
import com.gordonlu.daffydialog.helpers.InputType;

import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.method.PasswordTransformationMethod;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.view.inputmethod.InputMethodManager;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@DesignerComponent(
        version = 9,
        description = "A non-visible extension that offers additional features compared to the Notifier component in App" +
            " Inventor. Created by Gordon Lu.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "aiwebres/icon.png")
@SimpleObject(external = true)

public class DaffyDialog extends AndroidNonvisibleComponent {

    boolean html = false;
    float dimAmount = 0.5f;

    boolean lightTheme = true;
    boolean fullscreen = false;
    boolean classic = false;

    int verticalAlignment = 2;
    int horizontalAlignment = 3;

    boolean dismissWhenBackgroundClicked = false;

    HashMap<Integer, AlertDialog> customDialogs = new HashMap<>();

    HashMap<Integer, ProgressBar> progressBars = new HashMap<>();
    HashMap<Integer, AlertDialog> progressDialogs = new HashMap<>();

    final HashMap<String, Typeface> fonts = new HashMap<String, Typeface>() {{
        put("MONOSPACE", Typeface.MONOSPACE);
        put("SANS SERIF", Typeface.SANS_SERIF);
        put("SERIF", Typeface.SERIF);
    }};

    final List<Integer> verticalGravities = new ArrayList<>(Arrays.asList(Gravity.TOP, Gravity.CENTER_VERTICAL, Gravity.BOTTOM));
    final List<Integer> horizontalGravities = new ArrayList<>(Arrays.asList(Gravity.LEFT, Gravity.RIGHT, Gravity.CENTER_HORIZONTAL));

    public DaffyDialog(ComponentContainer container){
        super(container.$form());
    }

    // The following blocks are related to custom dialogs.

    @SimpleFunction(description = "Creates a dialog of a component. You can use arrangements, images, or other visible components in the custom dialog." + 
    " Your chosen layout or component will be removed from the screen and only visible in the custom dialog. Please" +
    " make sure the layout you want to use is visible. The ID parameter is used for identification so that " +
    " you can create multiple custom dialogs with one DaffyDialog extension.")
    public void CreateCustomDialog(AndroidViewComponent component, int id) {
        if (customDialogs.containsKey(id)) {
            Error("Sorry, a custom dialog with the id " + id +
                " has already been used. Please create a custom dialog with a new ID.", "CreateCustomDialog");
        } else {
            AlertDialog.Builder builder = createAlertDialogBuilder("CreateCustomDialog", null, null, null, component.getView());

            ((ViewGroup) component.getView().getParent()).removeView(component.getView());
            customDialogs.put(id, builder.create());
        }
    }

    @SimpleFunction(description = "Shows the custom dialog that you have created with the ID.")
    public void ShowCustomDialog(int id) {
        if (customDialogs.containsKey(id))
           showAlertDialog(customDialogs.get(id));
        else
            Error("Sorry, a custom dialog with the id " + id + " does not exist.", "ShowCustomDialog");
    }

    @SimpleFunction (description = "Dismisses the custom dialog.")
    public void DismissCustomDialog(int id){
        if (customDialogs.containsKey(id)) {
            customDialogs.get(id).dismiss();
            CustomDialogDismissed(id);
        } else {
            Error("Sorry, a custom dialog with the id " + id + " does not exist.", "DismissCustomDialog");
        }
    }

    @SimpleEvent(description = "This event is called when the custom dialog has been dismissed.")
    public void CustomDialogDismissed(int id) {
        EventDispatcher.dispatchEvent(this, "CustomDialogDismissed", id);
    }

    // The following blocks are related to linear progress dialogs.

    @SimpleFunction(description = "Shows a progress dialog with a horizontal progress bar. The Cancel button will" +
        " not be shown if 'cancelable' is set to true. If progressIndeterminacy is true, maxValue and the UpdateProgress" +
        " method will have no effect.")
    public void ShowLinearProgressDialog(final int id, String title, String message, String icon,
            boolean progressIndeterminacy, int progressColor, int progressMaxValue, boolean cancelable,
            String cancelButtonText) {        
        ProgressBar bar = new ProgressBar(form, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(progressIndeterminacy);
        bar.setPadding(20, 20, 20, 20);
        if (progressIndeterminacy) {
            bar.getIndeterminateDrawable().setColorFilter(new BlendModeColorFilter(progressColor, BlendMode.SRC_IN));
        } else {
            bar.getProgressDrawable().setColorFilter(new BlendModeColorFilter(progressColor, BlendMode.SRC_IN));
            bar.setMax(progressMaxValue);
        }
        progressBars.put(id, bar);

        AlertDialog.Builder builder = createAlertDialogBuilder("ShowLinearProgressDialog", title, message, icon, null);
        builder.setView(bar);
        
        if (cancelable)
            builder.setPositiveButton(getHtml(cancelButtonText), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LinearProgressDismissed(id);
                }
            });

        progressDialogs.put(id, showAlertDialog(builder.create()));
    }

    @SimpleFunction (description = "Dismisses the linear progress dialog that is associated with the given ID.")
    public void DismissLinearProgressDialog(int id){
        AlertDialog dialog = progressDialogs.get(id);
        if (dialog == null){
            Error("The linear progress dialog has not been created yet, or it cannot be found.", "DismissLinearProgressDialog");
        } else {
            dialog.dismiss();
            LinearProgressDismissed(id);
        }
    }
    
    @SimpleEvent(description = "This event is called when a linear progress dialog with the given ID has been dismissed.")
    public void LinearProgressDismissed(int id) {
        EventDispatcher.dispatchEvent(this, "LinearProgressDismissed", id);
    }

    @SimpleFunction(description = "Sets the current progress of the linear progress dialog to the specified value." +
    " Does not do anything if the progress bar is in indeterminate mode.")
    public final void UpdateProgress(int id, int progress) {
        if (progressBars.containsKey(id) && progressDialogs.containsKey(id)) {
            ProgressBar bar = progressBars.get(id);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                bar.setProgress(progress, true);
            else
                bar.setProgress(progress);
        } else {
            Error("The linear progress dialog has not been created yet.", "DismissLinearProgressDialog");
        }
    }

    // The following blocks are related to text input dialogs.

    @SimpleFunction(description = "Shows a text input dialog. The id parameter is an ID to specify the notifier, in case you want to show two dialogs" + 
    " with the same extension. The title parameter is for specifying the title of this dialog. defaultText is the default text for the input" + 
    " in which the user will first see in the textbox when they open the dialog, and hint is the hint of that textbox." + 
    " Use inputBold, inputItalic, hintColor and inputColor to customize the textbox." + 
    " buttonText is the text of the OK button, while cancelButtonText is the text of the cancel button.") 
    public void ShowTextInputDialog(final int id, String title, String message, String defaultText, String icon,
            String hint, int hintColor, boolean inputBold, boolean inputItalic, int inputColor,
            @Options(InputType.class) int inputType, @Options(Font.class) String inputFont, String buttonText,
            boolean cancelable, String cancelButtonText) {
        final EditText editText = new EditText(form);
        editText.setInputType(inputType);
        editText.setHint(hint);
        editText.setHintTextColor(hintColor);
        editText.setText(defaultText);
        editText.setTextColor(inputColor);
        editText.setTypeface(getFont(inputFont), getTypeface(inputBold, inputItalic));

        AlertDialog.Builder builder = createAlertDialogBuilder("ShowTextInputDialog", title, message, icon, (View) editText);

        builder.setPositiveButton(getHtml(buttonText), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GotTextInputDialog(id, editText.getText().toString());
                ((InputMethodManager) form.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        });

        if (cancelable)
            builder.setNegativeButton(getHtml(cancelButtonText), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TextInputDialogCanceled(id);
                    ((InputMethodManager) form.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            });

        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the OK button in a text input dialog.")
    public void GotTextInputDialog(int id, String input) {
        EventDispatcher.dispatchEvent(this, "GotTextInputDialog", id, input);
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the cancel button in a text input dialog.")
    public void TextInputDialogCanceled(int id) {
        EventDispatcher.dispatchEvent(this, "TextInputDialogCanceled", id);
    }

    // The following blocks are related to custom message dialogs.

    @SimpleFunction(description = "Shows a custom message dialog.")
    public void CustomMessageDialog(final int id, String title, String message, String icon, String buttonText) {
        AlertDialog.Builder builder = createAlertDialogBuilder("CustomMessageDialog", title, message, icon, null);

        builder.setPositiveButton(getHtml(buttonText), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CustomMessageDialogClosed(id);
            }
        });

        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the button in a custom message dialog.")
    public void CustomMessageDialogClosed(int id) {
        EventDispatcher.dispatchEvent(this, "CustomMessageDialogClosed", id);
    }

    // These are blocks for the number picker dialogs.

    @SimpleFunction(description = "Displays a number picker dialog that enables the user to select a number from a predefined range.")
    public void ShowNumberPickerDialog(final int id, String title, String icon, String buttonText,
            String cancelButtonText, String message, int minValue, int maxValue, boolean cancelable) {
        final NumberPicker numberPicker = new NumberPicker(form);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setMinValue(minValue);

        AlertDialog.Builder builder = createAlertDialogBuilder("ShowNumberPickerDialog", title, message, icon, numberPicker);

        builder.setPositiveButton(getHtml(buttonText), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GotNumberPickerDialog(id, numberPicker.getValue());
            }
        });

        if (cancelable)
            builder.setNegativeButton(getHtml(cancelButtonText), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    NumberPickerDialogCanceled(id);
                }
            });

        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the OK button in a number picker dialog.")
    public void GotNumberPickerDialog(int id, int number) {
        EventDispatcher.dispatchEvent(this, "GotNumberPickerDialog", id, number);
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the cancel button in a number picker dialog.")
    public void NumberPickerDialogCanceled(int id) {
        EventDispatcher.dispatchEvent(this, "NumberPickerDialogCanceled", id);
    }

    // These are the blocks for image dialogs.

    @SimpleFunction(description = "Displays an image in a dialog. This requires an absolute path pointing to the image location." + 
    " All supported file types are PNG, JPEG and JPG. After the user has pressed the button, the extension will fire the ImageDialogClosed event.")
    public void ShowImageDialog(final int id, String title, String message, String icon, String image, String buttonText) {
        final ImageView imageView = new ImageView(form);
        Drawable imageDrawable = getDrawableFromPath(image, "ShowImageDialog");
        if (imageDrawable != null) imageView.setImageDrawable(imageDrawable);

        AlertDialog.Builder builder = createAlertDialogBuilder("ShowImageDialog", title, message, icon, imageView);

        builder.setPositiveButton(getHtml(buttonText), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ImageDialogClosed(id);
            }
        });

        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the button in an image dialog.")
    public void ImageDialogClosed(int id) {
        EventDispatcher.dispatchEvent(this, "ImageDialogClosed", id);
    }

    // These are the blocks for custom choose dialogs.

    @SimpleFunction(description = "Shows a custom choose dialog. The id parameter is an ID to specify the notifier, in case you want to show two dialogs"
    + " with the same extension. The title and message parameter are for specifying the title and message of this dialog respectively. " + 
    " When the user has tapped button1 or button2 in this dialog, the extension fires the GotCustomChooseDialog event. " + 
    "If it is canceled, the extension will call the CustomChooseDialogCanceled event.") 
    public void CustomChooseDialog(final int id, String message, String title, String icon, final String button1Text,
            final String button2Text, String cancelButtonText, boolean cancelable) {
        AlertDialog.Builder builder = createAlertDialogBuilder("CustomChooseDialog", title, message, icon, null);

        builder.setPositiveButton(getHtml(button1Text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { GotCustomChooseDialog(id, getHtml(button1Text).toString()); }
        });

        builder.setNeutralButton(getHtml(button2Text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { GotCustomChooseDialog(id, getHtml(button2Text).toString()); }
        });

        if (cancelable)
            builder.setNegativeButton(getHtml(cancelButtonText), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id13) { CustomChooseDialogCanceled(id); }
            });
        
        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed button 1 or button 2 in a custom choose dialog.")
    public void GotCustomChooseDialog(int id, String choice) {
        EventDispatcher.dispatchEvent(this, "GotCustomChooseDialog", id, choice);
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the cancel button in a custom choose dialog.")
    public void CustomChooseDialogCanceled(int id) {
        EventDispatcher.dispatchEvent(this, "CustomChooseDialogCanceled", id);
    }

    // The following blocks are related to password input dialogs.
    
    @SimpleFunction(description = "Shows a password input dialog. The id parameter is an ID to specify the notifier, in case you want to show two dialogs" + 
    " with the same extension. The title parameter is for specifying the title of this dialog. defaultInputText is the default text for the input" + 
    " in which the user will first see in the textbox when they open the dialog, and hint is the hint of that textbox." + 
    " Use inputBold, inputItalic, hintColor and inputColor to customize the textbpx, and use the property blocks to specify inputFont." + 
    " buttonText is the text of the OK button, while cancelButtonText is the text of the cancel button.") 
    public void ShowPasswordInputDialog(final int id, String title, String message, String icon, String defaultInputText, String hint, 
            int hintColor, int inputColor, @Options(Font.class) String inputFont, boolean inputBold, boolean inputItalic,
            String buttonText, String cancelButtonText, boolean cancelable) {
        final EditText editText = new EditText(form);
        editText.setHint(hint);
        editText.setHintTextColor(hintColor);
        editText.setText(defaultInputText);
        editText.setTextColor(inputColor);
        editText.setTypeface(getFont(inputFont), getTypeface(inputBold, inputItalic));
        editText.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog.Builder builder = createAlertDialogBuilder("ShowPasswordInputDialog", title, message, icon, editText);

        builder.setPositiveButton(getHtml(buttonText), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                GotPasswordInputDialog(id, editText.getText().toString());
                ((InputMethodManager) form.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        });

        if (cancelable)
            builder.setNegativeButton(getHtml(cancelButtonText), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PasswordInputDialogCanceled(id);
                    ((InputMethodManager) form.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            });

        showAlertDialog(builder.create());
    }

    @SimpleEvent(description = "This event is invoked when the user has entered a password in a password input dialog.")
    public void GotPasswordInputDialog(int id, String password) {
        EventDispatcher.dispatchEvent(this, "GotPasswordInputDialog", id, password);
    }

    @SimpleEvent(description = "This event is invoked when the user has pressed the cancel button in a password input dialog.")
    public void PasswordInputDialogCanceled(int id) {
        EventDispatcher.dispatchEvent(this, "PasswordInputDialogCanceled", id);
    }

    public AlertDialog.Builder createAlertDialogBuilder(String methodName, String title, String message, String icon, View childView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(form, getTheme());
        if (title != null) builder.setTitle(getHtml(title));
        if (message != null) builder.setMessage(getHtml(message));
        if (childView != null) builder.setView(childView);
        builder.setCancelable(dismissWhenBackgroundClicked);

        if (icon != null) {
            Drawable iconDrawable = getDrawableFromPath(icon, methodName);
            if (iconDrawable != null) builder.setIcon(iconDrawable);
        }

        return builder;
    }

    public Drawable getDrawableFromPath(String path, String event) {
        if (path.startsWith("//")) {
            Bitmap bitmap = null;

            if (form.isRepl()) {
                String absolutePathOfAsset;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    absolutePathOfAsset = form.getExternalFilesDir(null).getAbsolutePath();
                else
                    absolutePathOfAsset = Environment.getExternalStorageDirectory().getAbsolutePath();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                    absolutePathOfAsset += "/AppInventor";
                absolutePathOfAsset += "/assets/" + path.substring(2);

                bitmap = BitmapFactory.decodeFile(absolutePathOfAsset);
            } else {
                AssetManager assetManager = form.getAssets();
                InputStream istr;
                try {
                    istr = assetManager.open(path.substring(2));
                    bitmap = BitmapFactory.decodeStream(istr);
                    istr.close();
                } catch (IOException e) {
                    Error("Error while trying to read the assets: " + e.getMessage(), event);
                }
            }
            return new BitmapDrawable(form.getResources(), bitmap);
        } else
            try {
                return MediaUtil.getBitmapDrawable(form, path);
            } catch (IOException e) {
                Error("Error while trying to read the assets: " + e.getMessage(), event);
                return null;
            }
    }

    public Spanned getHtml(String src) {
        if (!html) return new SpannedString(src);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Html.fromHtml(src, Html.FROM_HTML_MODE_COMPACT);
        return Html.fromHtml(src);
    }

    @SimpleEvent(description = "This event is invoked when an error has occurred with the given block of this extension.")
    public void Error(String error, String block){
        EventDispatcher.dispatchEvent(this, "Error", error, block);
    }

    public int getTheme() {
        if (classic) return 16974374;
        if (!lightTheme)
            return(fullscreen ? 16974122 : 16974545);
        return (fullscreen ? 16974125 : 16974546);
    }

    public Typeface getFont(String name) {
      return fonts.getOrDefault(name, Typeface.DEFAULT);
    }

    public int getTypeface(boolean bold, boolean italic) {
        if (bold && italic) return Typeface.BOLD_ITALIC;
        else if (bold && !italic) return Typeface.BOLD;
        else if (italic && !bold) return Typeface.ITALIC;
        else return Typeface.NORMAL;
    }

    public AlertDialog showAlertDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null){
            window.addFlags(2);
            window.setDimAmount(dimAmount);
            window.setGravity(verticalGravities.get(verticalAlignment - 1) | horizontalGravities.get(horizontalAlignment - 1));
        }
        dialog.show();
        return dialog;
    }

    // The following blocks are property blocks.

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT, defaultValue = "0.5")
    @SimpleProperty (description = "Sets the amount of dim behind the dialog. Use 0.0 for no dim and 1.0 for full dim.")
    public void DimAmount(float input) {
        dimAmount = input;
    }

    @SimpleProperty(description = "Sets the amount of dim behind the dialog. Use 0.0 for no dim and 1.0 for full dim.", category = PropertyCategory.APPEARANCE)
    public float DimAmount() {
        return dimAmount;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "false")
    @SimpleProperty (description = "Specifies whether the dialog should be displayed in fullscreen mode.")
    public void Fullscreen(boolean input) {
        fullscreen = input;
    }

    @SimpleProperty(description = "Specifies whether the dialog should be displayed in fullscreen mode.",
         category = PropertyCategory.APPEARANCE)
    public boolean Fullscreen() {
        return fullscreen;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "false")
    @SimpleProperty (description = "Specifies whether HTML tags should be enabled for the titles, messages and buttons of all dialogs.")
    public void HTMLFormat(boolean input) {
        html = input;
    }

    @SimpleProperty(description = "Specifies whether HTML tags should be enabled for the titles, messages and buttons of all dialogs.",
         category = PropertyCategory.APPEARANCE)
    public boolean HTMLFormat() {
        return html;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "true")
    @SimpleProperty (description = "Specifies whether the theme of the dialog should be in light theme. If Classic is " +
            "set to TRUE, the dialog will be displayed in Classic mode and this property will be ignored")
    public void LightTheme(boolean lightTheme) {
        this.lightTheme = lightTheme;
    }
    
    @SimpleProperty(description = "Specifies whether the theme of the dialog should be in light theme. If Classic is " +
            "set to TRUE, the dialog will be displayed in Classic mode and this property will be ignored",
            category = PropertyCategory.APPEARANCE)
    public boolean LightTheme() {
        return lightTheme;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "false")
    @SimpleProperty(description = "Specifies whether the dialogs should be dismissed when the user clicks anywhere " +
            "on the dinputMethodManagered background.")
    public void DismissWhenBackgroundClicked(boolean dismissWhenBackgroundClicked) {
        this.dismissWhenBackgroundClicked = dismissWhenBackgroundClicked;
    }

    @SimpleProperty(description = "Specifies whether the dialogs should be dismissed when the user clicks anywhere " +
            "on the dinputMethodManagered background.", category = PropertyCategory.BEHAVIOR)
    public boolean DismissWhenBackgroundClicked() {
        return dismissWhenBackgroundClicked;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_VERTICAL_ALIGNMENT,
     defaultValue = ComponentConstants.GRAVITY_CENTER_VERTICAL + "")
    @SimpleProperty(description = "Specifies the vertical position of the dialog when it is shown. Options are Top, Center and Bottom.",
        category = PropertyCategory.APPEARANCE)
    public void GravityVertical(@Options(VerticalAlignment.class) int verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    @SimpleProperty(description = "Specifies the vertical position of the dialog when it is shown. Options are Top," +
            " Center and Bottom.", category = PropertyCategory.APPEARANCE)
    public int GravityVertical() {
        return verticalAlignment;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_HORIZONTAL_ALIGNMENT,
            defaultValue = ComponentConstants.GRAVITY_CENTER_HORIZONTAL + "")
    @SimpleProperty(description = "Specifies the horizontal position of the dialog when it is shown. Options are Left, " +
            "Center and Right.", category = PropertyCategory.APPEARANCE)
    public void GravityHorizontal(@Options(HorizontalAlignment.class) int alignment) {
        horizontalAlignment = alignment;
    }

    @SimpleProperty(description = "Specifies the horizontal position of the dialog when it is shown. Options are Left," +
            " Center and Right.", category = PropertyCategory.APPEARANCE)
    public int GravityHorizontal() {
        return horizontalAlignment;
    }
}