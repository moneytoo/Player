package com.obsez.android.lib.filechooser;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.obsez.android.lib.filechooser.internals.FileUtil;
import com.obsez.android.lib.filechooser.internals.UiUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER;
import static android.view.Gravity.CENTER_HORIZONTAL;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
import static androidx.appcompat.widget.ListPopupWindow.MATCH_PARENT;
import static androidx.appcompat.widget.ListPopupWindow.WRAP_CONTENT;
import static androidx.core.view.GravityCompat.END;
import static androidx.core.view.GravityCompat.START;
import static com.obsez.android.lib.filechooser.ChooserDialog.CHOOSE_MODE_DELETE;
import static com.obsez.android.lib.filechooser.ChooserDialog.CHOOSE_MODE_NORMAL;
import static com.obsez.android.lib.filechooser.ChooserDialog.CHOOSE_MODE_SELECT_MULTIPLE;
import static com.obsez.android.lib.filechooser.internals.UiUtil.getListYScroll;

class onShowListener implements DialogInterface.OnShowListener {
    private WeakReference<ChooserDialog> _c;

    onShowListener(ChooserDialog c) {
        this._c = new WeakReference<>(c);
    }

    @Override
    public void onShow(final DialogInterface dialog) {
        // ensure that the buttons have the right order
        _c.get()._neutralBtn = _c.get()._alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        _c.get()._negativeBtn = _c.get()._alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        _c.get()._positiveBtn = _c.get()._alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        ViewGroup buttonBar = (ViewGroup) _c.get()._positiveBtn.getParent();
        ViewGroup.LayoutParams btnParams = buttonBar.getLayoutParams();
        btnParams.width = MATCH_PARENT;
        buttonBar.setLayoutParams(btnParams);
        buttonBar.removeAllViews();
        btnParams = _c.get()._neutralBtn.getLayoutParams();
        if (buttonBar instanceof LinearLayout) {
            ((LinearLayout.LayoutParams) btnParams).weight = 1;
            ((LinearLayout.LayoutParams) btnParams).width = 0;
        }
        if (_c.get()._enableOptions) {
            buttonBar.addView(_c.get()._neutralBtn, 0, btnParams);
        } else {
            buttonBar.addView(new Space(_c.get()._context), 0, btnParams);
        }
        buttonBar.addView(_c.get()._negativeBtn, 1);
        buttonBar.addView(_c.get()._positiveBtn, 2);

        if (_c.get()._enableMultiple) {
            _c.get()._positiveBtn.setVisibility(View.INVISIBLE);
        }

        if (_c.get()._enableOptions) {
            final int buttonColor = _c.get()._neutralBtn.getCurrentTextColor();
            final PorterDuffColorFilter filter = new PorterDuffColorFilter(buttonColor,
                PorterDuff.Mode.SRC_IN);

            _c.get()._neutralBtn.setText("");
            _c.get()._neutralBtn.setVisibility(View.VISIBLE);
            Drawable dots;
            if (_c.get()._optionsIconRes != -1) {
                dots = ContextCompat.getDrawable(_c.get()._context, _c.get()._optionsIconRes);
            } else if (_c.get()._optionsIcon != null) {
                dots = _c.get()._optionsIcon;
            } else {
                dots = ContextCompat.getDrawable(_c.get()._context, R.drawable.ic_menu_24dp);
            }
            if (dots != null) {
                dots.setColorFilter(filter);
                _c.get()._neutralBtn.setCompoundDrawablesWithIntrinsicBounds(dots, null, null, null);
            }

            final class Integer {
                int Int = 0;
            }
            final Integer scroll = new Integer();

            _c.get()._list.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int oldHeight = oldBottom - oldTop;
                    if (v.getHeight() != oldHeight) {
                        int offset = oldHeight - v.getHeight();
                        int newScroll = getListYScroll(_c.get()._list);
                        if (scroll.Int != newScroll) offset += scroll.Int - newScroll;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            _c.get()._list.scrollListBy(offset);
                        } else {
                            _c.get()._list.scrollBy(0, offset);
                        }
                    }
                });

            final Runnable showOptions = new Runnable() {
                @Override
                public void run() {
                    if (_c.get()._options.getHeight() == 0) {
                        ViewTreeObserver viewTreeObserver = _c.get()._options.getViewTreeObserver();
                        viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                if (_c.get()._options.getHeight() <= 0) {
                                    return false;
                                }
                                viewTreeObserver.removeOnPreDrawListener(this);
                                scroll.Int = getListYScroll(_c.get()._list);
                                if (_c.get()._options.getParent() instanceof FrameLayout) {
                                    final ViewGroup.MarginLayoutParams params =
                                        (ViewGroup.MarginLayoutParams) _c.get()._list.getLayoutParams();
                                    params.bottomMargin = _c.get()._options.getHeight();
                                    _c.get()._list.setLayoutParams(params);
                                }
                                _c.get()._options.setVisibility(View.VISIBLE);
                                _c.get()._options.requestFocus();
                                return true;
                            }
                        });
                    } else {
                        scroll.Int = getListYScroll(_c.get()._list);
                        _c.get()._options.setVisibility(View.VISIBLE);
                        _c.get()._options.requestFocus();
                        if (_c.get()._options.getParent() instanceof FrameLayout) {
                            final ViewGroup.MarginLayoutParams params =
                                (ViewGroup.MarginLayoutParams) _c.get()._list.getLayoutParams();
                            params.bottomMargin = _c.get()._options.getHeight();
                            _c.get()._list.setLayoutParams(params);
                        }
                    }
                }
            };
            final Runnable hideOptions = () -> {
                scroll.Int = getListYScroll(_c.get()._list);
                _c.get()._options.setVisibility(View.GONE);
                if (_c.get()._options.getParent() instanceof FrameLayout) {
                    ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) _c.get()._list.getLayoutParams();
                    params.bottomMargin = 0;
                    _c.get()._list.setLayoutParams(params);
                }
            };

            _c.get()._neutralBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    if (_c.get()._newFolderView != null
                        && _c.get()._newFolderView.getVisibility() == View.VISIBLE) {
                        return;
                    }

                    if (_c.get()._options == null) {
                        // region Draw options view. (this only happens the first time one clicks on options)
                        // Root view (FrameLayout) of the ListView in the AlertDialog.
                        int rootId = _c.get()._context.getResources().getIdentifier("contentPanel", "id", _c.get()._context.getPackageName());
                        ViewGroup tmpRoot = ((AlertDialog) dialog).findViewById(rootId);
                        // In case the root id was changed or not found.
                        if (tmpRoot == null) {
                            rootId = _c.get()._context.getResources().getIdentifier("contentPanel", "id", "android");
                            tmpRoot = ((AlertDialog) dialog).findViewById(rootId);
                            if (tmpRoot == null) return;
                        }
                        final ViewGroup root = tmpRoot;

                        // Create options view.
                        final FrameLayout options = new FrameLayout(_c.get()._context);
                        ViewGroup.MarginLayoutParams params;
                        if (root instanceof LinearLayout) {
                            params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                            LinearLayout.LayoutParams param =
                                ((LinearLayout.LayoutParams) _c.get()._list.getLayoutParams());
                            param.weight = 1;
                            _c.get()._list.setLayoutParams(param);
                        } else {
                            params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, BOTTOM);
                        }
                        root.addView(options, params);
                        options.setFocusable(false);

                        if (root instanceof FrameLayout) {
                            _c.get()._list.bringToFront();
                        }

                        TypedArray ta = _c.get()._context.obtainStyledAttributes(R.styleable.FileChooser);
                        final int style = ta.getResourceId(R.styleable.FileChooser_fileChooserDialogStyle,
                            R.style.FileChooserDialogStyle);
                        ta.recycle();
                        final Context buttonContext = new ContextThemeWrapper(_c.get()._context, style);

                        // Create a button for the option to create a new directory/folder.
                        final Button createDir = new Button(buttonContext, null,
                            android.R.attr.buttonBarButtonStyle);
                        if (_c.get()._createDirRes != -1) {
                            createDir.setText(_c.get()._createDirRes);
                        } else if (_c.get()._createDir != null) {
                            createDir.setText(_c.get()._createDir);
                        } else {
                            createDir.setText(R.string.option_create_folder);
                        }
                        createDir.setTextColor(buttonColor);
                        // Drawable for the button.
                        final Drawable plus;
                        if (_c.get()._createDirIconRes != -1) {
                            plus = ContextCompat.getDrawable(_c.get()._context, _c.get()._createDirIconRes);
                        } else if (_c.get()._createDirIcon != null) {
                            plus = _c.get()._createDirIcon;
                        } else {
                            plus = ContextCompat.getDrawable(_c.get()._context, R.drawable.ic_add_24dp);
                        }
                        if (plus != null) {
                            plus.setColorFilter(filter);
                            createDir.setCompoundDrawablesWithIntrinsicBounds(plus, null, null, null);
                        }
                        params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                            START | CENTER_VERTICAL);
                        params.leftMargin = UiUtil.dip2px(10);
                        options.addView(createDir, params);

                        // Create a button for the option to delete a file.
                        final Button delete = new Button(buttonContext, null,
                            android.R.attr.buttonBarButtonStyle);
                        if (_c.get()._deleteRes != -1) {
                            delete.setText(_c.get()._deleteRes);
                        } else if (_c.get()._delete != null) {
                            delete.setText(_c.get()._delete);
                        } else {
                            delete.setText(R.string.options_delete);
                        }
                        delete.setTextColor(buttonColor);
                        final Drawable bin;
                        if (_c.get()._deleteIconRes != -1) {
                            bin = ContextCompat.getDrawable(_c.get()._context, _c.get()._deleteIconRes);
                        } else if (_c.get()._deleteIcon != null) {
                            bin = _c.get()._deleteIcon;
                        } else {
                            bin = ContextCompat.getDrawable(_c.get()._context, R.drawable.ic_delete_24dp);
                        }
                        if (bin != null) {
                            bin.setColorFilter(filter);
                            delete.setCompoundDrawablesWithIntrinsicBounds(bin, null, null, null);
                        }
                        params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                            END | CENTER_VERTICAL);
                        params.rightMargin = UiUtil.dip2px(10);
                        options.addView(delete, params);

                        _c.get()._options = options;
                        showOptions.run();

                        // Event Listeners.
                        createDir.setOnClickListener(new View.OnClickListener() {
                            private EditText input = null;

                            @Override
                            public void onClick(final View view) {
                                //Toast.makeText(getBaseContext(), "new folder clicked", Toast
                                // .LENGTH_SHORT).show();
                                hideOptions.run();
                                File newFolder = new File(_c.get()._currentDir, "New folder");
                                for (int i = 1; newFolder.exists(); i++) {
                                    newFolder = new File(_c.get()._currentDir, "New folder (" + i + ')');
                                }
                                if (this.input != null) {
                                    this.input.setText(newFolder.getName());
                                }

                                if (_c.get()._newFolderView == null) {
                                    // region Draw a view with input to create new folder. (this only
                                    // happens the first time one clicks on New folder)
                                    TypedArray ta = _c.get()._context.obtainStyledAttributes(
                                        R.styleable.FileChooser);
                                    final int style = ta.getResourceId(
                                        R.styleable.FileChooser_fileChooserNewFolderStyle,
                                        R.style.FileChooserNewFolderStyle);
                                    final Context context = new ContextThemeWrapper(_c.get()._context, style);
                                    ta.recycle();
                                    ta = context.obtainStyledAttributes(R.styleable.FileChooser);

                                    try {
                                        //noinspection ConstantConditions
                                        ((AlertDialog) dialog).getWindow().clearFlags(
                                            FLAG_NOT_FOCUSABLE | FLAG_ALT_FOCUSABLE_IM);
                                        //noinspection ConstantConditions
                                        ((AlertDialog) dialog).getWindow().setSoftInputMode(
                                            SOFT_INPUT_STATE_VISIBLE |
                                                ta.getInt(
                                                    R.styleable.FileChooser_fileChooserNewFolderSoftInputMode,
                                                    0x30));
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }

                                    // A semitransparent background overlay.
                                    final FrameLayout overlay = new FrameLayout(_c.get()._context);
                                    overlay.setBackgroundColor(
                                        ta.getColor(R.styleable.FileChooser_fileChooserNewFolderOverlayColor,
                                            0x60ffffff));
                                    overlay.setScrollContainer(true);
                                    ViewGroup.MarginLayoutParams params;
                                    if (root instanceof FrameLayout) {
                                        params = new FrameLayout.LayoutParams(
                                            MATCH_PARENT, MATCH_PARENT, CENTER);
                                    } else {
                                        params = new LinearLayout.LayoutParams(
                                            MATCH_PARENT, MATCH_PARENT);
                                    }
                                    root.addView(overlay, params);

                                    overlay.setOnClickListener(null);
                                    overlay.setVisibility(View.INVISIBLE);
                                    _c.get()._newFolderView = overlay;

                                    // A LinearLayout and a pair of Space to center views.
                                    LinearLayout linearLayout = new LinearLayout(_c.get()._context);
                                    params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT,
                                        CENTER);
                                    overlay.addView(linearLayout, params);
                                    overlay.setFocusable(false);

                                    float widthWeight = ta.getFloat(
                                        R.styleable.FileChooser_fileChooserNewFolderWidthWeight, 0.56f);
                                    if (widthWeight <= 0) widthWeight = 0.56f;
                                    if (widthWeight > 1f) widthWeight = 1f;

                                    Space leftSpace = new Space(_c.get()._context);
                                    params = new LinearLayout.LayoutParams(0, WRAP_CONTENT,
                                        (1f - widthWeight) / 2);
                                    linearLayout.addView(leftSpace, params);
                                    leftSpace.setFocusable(false);

                                    // A solid holder view for the EditText and Buttons.
                                    final LinearLayout holder = new LinearLayout(_c.get()._context);
                                    holder.setOrientation(LinearLayout.VERTICAL);
                                    holder.setBackgroundColor(
                                        ta.getColor(R.styleable.FileChooser_fileChooserNewFolderBackgroundColor,
                                            0xffffffff));
                                    final int elevation = ta.getInt(
                                        R.styleable.FileChooser_fileChooserNewFolderElevation, 25);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        holder.setElevation(elevation);
                                    } else {
                                        ViewCompat.setElevation(holder, elevation);
                                    }
                                    params = new LinearLayout.LayoutParams(0, WRAP_CONTENT, widthWeight);
                                    linearLayout.addView(holder, params);
                                    holder.setFocusable(false);

                                    Space rightSpace = new Space(_c.get()._context);
                                    params = new LinearLayout.LayoutParams(0, WRAP_CONTENT,
                                        (1f - widthWeight) / 2);
                                    linearLayout.addView(rightSpace, params);
                                    rightSpace.setFocusable(false);

                                    final EditText input = new EditText(_c.get()._context);
                                    final int color = ta.getColor(
                                        R.styleable.FileChooser_fileChooserNewFolderTextColor, buttonColor);
                                    input.setTextColor(color);
                                    input.getBackground().mutate().setColorFilter(color,
                                        PorterDuff.Mode.SRC_ATOP);
                                    input.setText(newFolder.getName());
                                    input.setSelectAllOnFocus(true);
                                    input.setSingleLine(true);
                                    // There should be no suggestions, but...
                                    input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                        | InputType.TYPE_TEXT_VARIATION_FILTER
                                        | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                    input.setFilters(new InputFilter[]{
                                        _c.get()._newFolderFilter != null ? _c.get()._newFolderFilter
                                            : new FileUtil.NewFolderFilter()});
                                    input.setGravity(CENTER_HORIZONTAL);
                                    input.setImeOptions(EditorInfo.IME_ACTION_DONE);
                                    params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                    params.setMargins(3, 2, 3, 0);
                                    holder.addView(input, params);

                                    this.input = input;

                                    // A horizontal LinearLayout to hold buttons
                                    final FrameLayout buttons = new FrameLayout(_c.get()._context);
                                    params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                    holder.addView(buttons, params);

                                    // The Cancel button.
                                    final Button cancel = new Button(buttonContext, null,
                                        android.R.attr.buttonBarButtonStyle);
                                    if (_c.get()._newFolderCancelRes != -1) {
                                        cancel.setText(_c.get()._newFolderCancelRes);
                                    } else if (_c.get()._newFolderCancel != null) {
                                        cancel.setText(_c.get()._newFolderCancel);
                                    } else {
                                        cancel.setText(R.string.new_folder_cancel);
                                    }
                                    cancel.setTextColor(buttonColor);
                                    params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                                        START);
                                    buttons.addView(cancel, params);

                                    // The OK button.
                                    final Button ok = new Button(buttonContext, null,
                                        android.R.attr.buttonBarButtonStyle);
                                    if (_c.get()._newFolderOkRes != -1) {
                                        ok.setText(_c.get()._newFolderOkRes);
                                    } else if (_c.get()._newFolderOk != null) {
                                        ok.setText(_c.get()._newFolderOk);
                                    } else {
                                        ok.setText(R.string.new_folder_ok);
                                    }
                                    ok.setTextColor(buttonColor);
                                    params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT,
                                        END);
                                    buttons.addView(ok, params);

                                    final int id = cancel.hashCode();
                                    cancel.setId(id);
                                    ok.setNextFocusLeftId(id);
                                    input.setNextFocusLeftId(id);

                                    // Event Listeners.
                                    input.setOnEditorActionListener(
                                        (v, actionId, event) -> {
                                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                UiUtil.hideKeyboardFrom(_c.get()._context, input);
                                                _c.get().createNewDirectory(
                                                    input.getText().toString());
                                                overlay.setVisibility(View.GONE);
                                                overlay.clearFocus();
                                                if (_c.get()._enableDpad) {
                                                    Button b = _c.get()._neutralBtn;
                                                    b.setFocusable(true);
                                                    b.requestFocus();
                                                    _c.get()._list.setFocusable(true);
                                                }
                                                return true;
                                            }
                                            return false;
                                        });
                                    cancel.setOnClickListener(v -> {
                                        UiUtil.hideKeyboardFrom(_c.get()._context, input);
                                        overlay.setVisibility(View.GONE);
                                        overlay.clearFocus();
                                        if (_c.get()._enableDpad) {
                                            Button b = _c.get()._neutralBtn;
                                            b.setFocusable(true);
                                            b.requestFocus();
                                            _c.get()._list.setFocusable(true);
                                        }
                                    });
                                    ok.setOnClickListener(v -> {
                                        UiUtil.hideKeyboardFrom(_c.get()._context, input);
                                        _c.get().createNewDirectory(
                                            input.getText().toString());
                                        UiUtil.hideKeyboardFrom(_c.get()._context, input);
                                        overlay.setVisibility(View.GONE);
                                        overlay.clearFocus();
                                        if (_c.get()._enableDpad) {
                                            Button b = _c.get()._neutralBtn;
                                            b.setFocusable(true);
                                            b.requestFocus();
                                            _c.get()._list.setFocusable(true);
                                        }
                                    });
                                    ta.recycle();
                                    // endregion
                                }

                                if (_c.get()._newFolderView.getVisibility() != View.VISIBLE) {
                                    _c.get()._newFolderView.setVisibility(View.VISIBLE);
                                    if (_c.get()._enableDpad) {
                                        _c.get()._newFolderView.requestFocus();
                                        _c.get()._neutralBtn.setFocusable(false);
                                        _c.get()._list.setFocusable(false);
                                    }
                                    if (_c.get()._pathView != null &&
                                        _c.get()._pathView.getVisibility() == View.VISIBLE) {
                                        _c.get()._newFolderView.setPadding(0, UiUtil.dip2px(32),
                                            0, UiUtil.dip2px(12));
                                    } else {
                                        _c.get()._newFolderView.setPadding(0, UiUtil.dip2px(12),
                                            0, UiUtil.dip2px(12));
                                    }
                                } else {
                                    _c.get()._newFolderView.setVisibility(View.GONE);
                                    if (_c.get()._enableDpad) {
                                        _c.get()._newFolderView.clearFocus();
                                        _c.get()._neutralBtn.setFocusable(true);
                                        _c.get()._list.setFocusable(true);
                                    }
                                }
                            }
                        });
                        delete.setOnClickListener(v1 -> {
                            //Toast.makeText(_c.get()._context, "delete clicked", Toast.LENGTH_SHORT).show();
                            hideOptions.run();

                            if (_c.get()._chooseMode == CHOOSE_MODE_SELECT_MULTIPLE) {
                                boolean success = true;
                                for (File file : _c.get()._adapter.getSelected()) {
                                    _c.get()._result.onChoosePath(file.getAbsolutePath(), file);
                                    if (success) {
                                        try {
                                            FileUtil.deleteFileRecursively(file);
                                        } catch (IOException e) {
                                            Toast.makeText(_c.get()._context, e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                            success = false;
                                        }
                                    }
                                }
                                _c.get()._adapter.clearSelected();
                                _c.get()._positiveBtn.setVisibility(
                                    View.INVISIBLE);
                                _c.get()._chooseMode = CHOOSE_MODE_NORMAL;
                                _c.get().refreshDirs();
                                return;
                            }

                            _c.get()._chooseMode =
                                _c.get()._chooseMode != CHOOSE_MODE_DELETE ? CHOOSE_MODE_DELETE
                                    : CHOOSE_MODE_NORMAL;
                            if (_c.get()._deleteModeIndicator == null) {
                                _c.get()._deleteModeIndicator = () -> {
                                    if (_c.get()._chooseMode == CHOOSE_MODE_DELETE) {
                                        final int color1 = 0x80ff0000;
                                        final PorterDuffColorFilter red =
                                            new PorterDuffColorFilter(color1,
                                                PorterDuff.Mode.SRC_IN);
                                        _c.get()._neutralBtn.getCompoundDrawables()
                                            [0].setColorFilter(
                                            red);
                                        _c.get()._neutralBtn.setTextColor(color1);
                                        delete.getCompoundDrawables()[0].setColorFilter(red);
                                        delete.setTextColor(color1);
                                    } else {
                                        _c.get()._neutralBtn.getCompoundDrawables()
                                            [0].clearColorFilter();
                                        _c.get()._neutralBtn.setTextColor(buttonColor);
                                        delete.getCompoundDrawables()[0].clearColorFilter();
                                        delete.setTextColor(buttonColor);
                                    }
                                };
                            }
                            _c.get()._deleteModeIndicator.run();
                        });
                        // endregion
                    } else if (_c.get()._options.getVisibility() == View.VISIBLE) {
                        hideOptions.run();
                    } else {
                        showOptions.run();
                    }
                }
            });
        }
    }
}
