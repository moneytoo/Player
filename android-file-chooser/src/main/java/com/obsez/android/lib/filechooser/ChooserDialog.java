package com.obsez.android.lib.filechooser;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.obsez.android.lib.filechooser.internals.ExtFileFilter;
import com.obsez.android.lib.filechooser.internals.FileUtil;
import com.obsez.android.lib.filechooser.internals.RegexFileFilter;
import com.obsez.android.lib.filechooser.permissions.PermissionsUtil;
import com.obsez.android.lib.filechooser.tool.DirAdapter;
import com.obsez.android.lib.filechooser.tool.RootFile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static androidx.appcompat.widget.ListPopupWindow.MATCH_PARENT;
import static androidx.appcompat.widget.ListPopupWindow.WRAP_CONTENT;
import static com.obsez.android.lib.filechooser.internals.FileUtil.NewFolderFilter;

/**
 * Created by coco on 6/7/15.
 */
public class ChooserDialog implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener,
    AdapterView.OnItemLongClickListener, AdapterView.OnItemSelectedListener {
    @FunctionalInterface
    public interface Result {
        void onChoosePath(String dir, File dirFile);
    }

    public ChooserDialog(Context cxt, @StyleRes int fileChooserTheme) {
        this._context = cxt;
        init(fileChooserTheme);
    }

    public ChooserDialog(Activity activity, @StyleRes int fileChooserTheme) {
        this._context = activity;
        init(fileChooserTheme);
    }

    public ChooserDialog(Fragment fragment, @StyleRes int fileChooserTheme) {
        this._context = fragment.getActivity();
        init(fileChooserTheme);
    }

    public ChooserDialog(Context cxt) {
        this._context = cxt;
        init();
    }

    public ChooserDialog(Activity activity) {
        this._context = activity;
        init();
    }

    public ChooserDialog(Fragment fragment) {
        this._context = fragment.getActivity();
        init();
    }

    private void init() {
        init(null);
    }

    private void init(@Nullable @StyleRes Integer fileChooserTheme) {
        _onBackPressed = new defBackPressed(this);

        if (fileChooserTheme == null) {
            TypedValue typedValue = new TypedValue();
            if (!this._context.getTheme().resolveAttribute(
                R.attr.fileChooserStyle, typedValue, true)) {
                this._context = new ContextThemeWrapper(this._context, R.style.FileChooserStyle);
            } else {
                this._context = new ContextThemeWrapper(this._context, typedValue.resourceId);
            }
        } else {
            //noinspection UnnecessaryUnboxing
            this._context = new ContextThemeWrapper(this._context, fileChooserTheme.intValue());
        }
    }

    public ChooserDialog withFilter(FileFilter ff) {
        withFilter(false, false, (String[]) null);
        this._fileFilter = ff;
        return this;
    }

    public ChooserDialog withFilter(boolean dirOnly, boolean allowHidden, FileFilter ff) {
        withFilter(dirOnly, allowHidden, (String[]) null);
        this._fileFilter = ff;
        return this;
    }

    public ChooserDialog withFilter(boolean allowHidden, String... suffixes) {
        return withFilter(false, allowHidden, suffixes);
    }

    public ChooserDialog withFilter(boolean dirOnly, final boolean allowHidden, String... suffixes) {
        this._dirOnly = dirOnly;
        if (suffixes == null || suffixes.length == 0) {
            this._fileFilter = dirOnly ?
                file -> file.isDirectory() && (!file.isHidden() || allowHidden) : file ->
                !file.isHidden() || allowHidden;
        } else {
            this._fileFilter = new ExtFileFilter(_dirOnly, allowHidden, suffixes);
        }
        return this;
    }

    public ChooserDialog withFilterRegex(boolean dirOnly, boolean allowHidden, String pattern, int flags) {
        this._dirOnly = dirOnly;
        this._fileFilter = new RegexFileFilter(_dirOnly, allowHidden, pattern, flags);
        return this;
    }

    public ChooserDialog withFilterRegex(boolean dirOnly, boolean allowHidden, String pattern) {
        this._dirOnly = dirOnly;
        this._fileFilter = new RegexFileFilter(_dirOnly, allowHidden, pattern, Pattern.CASE_INSENSITIVE);
        return this;
    }

    public ChooserDialog withStartFile(String startFile) {
        if (startFile != null) {
            _currentDir = new File(startFile);
        } else {
            _currentDir = new File(FileUtil.getStoragePath(_context, false));
        }

        if (!_currentDir.isDirectory()) {
            _currentDir = _currentDir.getParentFile();
        }

        if (_currentDir == null) {
            _currentDir = new File(FileUtil.getStoragePath(_context, false));
        }

        return this;
    }

    public ChooserDialog withChosenListener(Result r) {
        this._result = r;
        return this;
    }

    /**
     *  called every time {@link KeyEvent#KEYCODE_BACK} is caught,
     *  and current directory is not the root of Primary/SdCard storage.
     */
    public ChooserDialog withOnBackPressedListener(OnBackPressedListener listener) {
        if (this._onBackPressed instanceof defBackPressed) {
            ((defBackPressed) this._onBackPressed)._onBackPressed = listener;
        }
        return this;
    }

    /**
     * called if {@link KeyEvent#KEYCODE_BACK} is caught,
     * and current directory is the root of Primary/SdCard storage.
     */
    public ChooserDialog withOnLastBackPressedListener(OnBackPressedListener listener) {
        if (this._onBackPressed instanceof defBackPressed) {
            ((defBackPressed) this._onBackPressed)._onLastBackPressed = listener;
        }
        return this;
    }

    public ChooserDialog withResources(@StringRes int titleRes, @StringRes int okRes, @StringRes int cancelRes) {
        this._titleRes = titleRes;
        this._okRes = okRes;
        this._negativeRes = cancelRes;
        return this;
    }

    public ChooserDialog withStringResources(@Nullable String titleRes, @Nullable String okRes,
        @Nullable String cancelRes) {
        this._title = titleRes;
        this._ok = okRes;
        this._negative = cancelRes;
        return this;
    }

    /**
     * To enable the option pane with create/delete folder on the fly.
     * When u set it true, you may need WRITE_EXTERNAL_STORAGE declaration too.
     *
     * @param enableOptions true/false
     * @return this
     */
    public ChooserDialog enableOptions(boolean enableOptions) {
        this._enableOptions = enableOptions;
        return this;
    }

    public ChooserDialog withOptionResources(@StringRes int createDirRes, @StringRes int deleteRes,
        @StringRes int newFolderCancelRes, @StringRes int newFolderOkRes) {
        this._createDirRes = createDirRes;
        this._deleteRes = deleteRes;
        this._newFolderCancelRes = newFolderCancelRes;
        this._newFolderOkRes = newFolderOkRes;
        return this;
    }

    public ChooserDialog withOptionStringResources(@Nullable String createDir, @Nullable String delete,
        @Nullable String newFolderCancel, @Nullable String newFolderOk) {
        this._createDir = createDir;
        this._delete = delete;
        this._newFolderCancel = newFolderCancel;
        this._newFolderOk = newFolderOk;
        return this;
    }

    public ChooserDialog withOptionIcons(@DrawableRes int optionsIconRes, @DrawableRes int createDirIconRes,
        @DrawableRes int deleteRes) {
        this._optionsIconRes = optionsIconRes;
        this._createDirIconRes = createDirIconRes;
        this._deleteIconRes = deleteRes;
        return this;
    }

    public ChooserDialog withOptionIcons(@Nullable Drawable optionsIcon, @Nullable Drawable createDirIcon,
        @Nullable Drawable deleteIcon) {
        this._optionsIcon = optionsIcon;
        this._createDirIcon = createDirIcon;
        this._deleteIcon = deleteIcon;
        return this;
    }

    public ChooserDialog withNewFolderFilter(NewFolderFilter filter) {
        this._newFolderFilter = filter;
        return this;
    }

    public ChooserDialog withIcon(@DrawableRes int iconId) {
        this._iconRes = iconId;
        return this;
    }

    public ChooserDialog withIcon(@Nullable Drawable icon) {
        this._icon = icon;
        return this;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ChooserDialog withLayoutView(@LayoutRes int layoutResId) {
        this._layoutRes = layoutResId;
        return this;
    }

    public ChooserDialog withDateFormat() {
        return this.withDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    public ChooserDialog withDateFormat(String format) {
        this._dateFormat = format;
        return this;
    }

    public ChooserDialog withNegativeButton(@StringRes int cancelTitle,
        final DialogInterface.OnClickListener listener) {
        this._negativeRes = cancelTitle;
        this._negativeListener = listener;
        return this;
    }

    public ChooserDialog withNegativeButton(@Nullable String cancelTitle,
        final DialogInterface.OnClickListener listener) {
        this._negative = cancelTitle;
        if (cancelTitle != null) this._negativeRes = -1;
        this._negativeListener = listener;
        return this;
    }

    public ChooserDialog withNegativeButtonListener(final DialogInterface.OnClickListener listener) {
        this._negativeListener = listener;
        return this;
    }

    /**
     * onCancelListener will be triggered on back pressed or clicked outside of dialog
     */
    public ChooserDialog withOnCancelListener(final DialogInterface.OnCancelListener listener) {
        this._cancelListener = listener;
        return this;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ChooserDialog withOnDismissListener(final DialogInterface.OnDismissListener listener) {
        _onDismissListener = listener;
        return this;
    }

    public ChooserDialog withFileIcons(final boolean tryResolveFileTypeAndIcon, final Drawable fileIcon,
        final Drawable folderIcon) {
        _adapterSetter = adapter -> {
            if (fileIcon != null) adapter.setDefaultFileIcon(fileIcon);
            if (folderIcon != null) adapter.setDefaultFolderIcon(folderIcon);
            adapter.setResolveFileType(tryResolveFileTypeAndIcon);
        };
        return this;
    }

    public ChooserDialog withFileIconsRes(final boolean tryResolveFileTypeAndIcon, final int fileIcon,
        final int folderIcon) {
        _adapterSetter = adapter -> {
            if (fileIcon != -1) {
                adapter.setDefaultFileIcon(ContextCompat.getDrawable(_context, fileIcon));
            }
            if (folderIcon != -1) {
                adapter.setDefaultFolderIcon(
                    ContextCompat.getDrawable(_context, folderIcon));
            }
            adapter.setResolveFileType(tryResolveFileTypeAndIcon);
        };
        return this;
    }

    /**
     * @param setter you can override {@link DirAdapter#getView(int, View, ViewGroup)}
     *               see {@link AdapterSetter} for more information
     * @return this
     */
    public ChooserDialog withAdapterSetter(AdapterSetter setter) {
        _adapterSetter = setter;
        return this;
    }

    /**
     * @param cb give a hook at navigating up to a directory
     * @return this
     */
    public ChooserDialog withNavigateUpTo(CanNavigateUp cb) {
        _folderNavUpCB = cb;
        return this;
    }

    /**
     * @param cb give a hook at navigating to a child directory
     * @return this
     */
    public ChooserDialog withNavigateTo(CanNavigateTo cb) {
        _folderNavToCB = cb;
        return this;
    }

    public ChooserDialog disableTitle(boolean disableTitle) {
        _disableTitle = disableTitle;
        return this;
    }

    /**
     * allows dialog title follows the current folder name
     *
     * @param followDir dialog title will follow the changing of directory
     * @return this
     */
    public ChooserDialog titleFollowsDir(boolean followDir) {
        _followDir = followDir;
        return this;
    }

    public ChooserDialog displayPath(boolean displayPath) {
        _displayPath = displayPath;
        return this;
    }

    public ChooserDialog customizePathView(CustomizePathView callback) {
        _customizePathView = callback;
        return this;
    }

    public ChooserDialog enableMultiple(boolean enableMultiple) {
        this._enableMultiple = enableMultiple;
        return this;
    }

    public ChooserDialog cancelOnTouchOutside(boolean cancelOnTouchOutside) {
        this._cancelOnTouchOutside = cancelOnTouchOutside;
        return this;
    }

    public ChooserDialog enableDpad(boolean enableDpad) {
        this._enableDpad = enableDpad;
        return this;
    }

    public ChooserDialog build() {
        TypedArray ta = _context.obtainStyledAttributes(R.styleable.FileChooser);
        final AlertDialog.Builder builder = new AlertDialog.Builder(_context,
            ta.getResourceId(R.styleable.FileChooser_fileChooserDialogStyle, R.style.FileChooserDialogStyle));
        final int style = ta.getResourceId(R.styleable.FileChooser_fileChooserListItemStyle,
            R.style.FileChooserListItemStyle);
        ta.recycle();
        final Context context = new ContextThemeWrapper(_context, style);
        ta = context.obtainStyledAttributes(R.styleable.FileChooser);
        final int listview_item_selector = ta.getResourceId(R.styleable.FileChooser_fileListItemFocusedDrawable,
            R.drawable.listview_item_selector);
        ta.recycle();

        _adapter = new DirAdapter(context, this._dateFormat);
        if (_adapterSetter != null) _adapterSetter.apply(_adapter);

        refreshDirs();
        builder.setAdapter(_adapter, this);

        if (!_disableTitle) {
            if (_titleRes != -1) {
                builder.setTitle(_titleRes);
            } else if (_title != null) {
                builder.setTitle(_title);
            } else {
                builder.setTitle(R.string.choose_file);
            }
        }

        if (_iconRes != -1) {
            builder.setIcon(_iconRes);
        } else if (_icon != null) {
            builder.setIcon(_icon);
        }

        if (_layoutRes != -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setView(_layoutRes);
            }
        }

        if (_dirOnly || _enableMultiple) {
            // choosing folder, or multiple files picker
            DialogInterface.OnClickListener listener = (dialog, which) -> {
                if (_result != null) {
                    _result.onChoosePath(_currentDir.getAbsolutePath(), _currentDir);
                }
            };
            if (_okRes != -1) {
                builder.setPositiveButton(_okRes, listener);
            } else if (_ok != null) {
                builder.setPositiveButton(_ok, listener);
            } else {
                builder.setPositiveButton(R.string.title_choose, listener);
            }
        }

        if (_negativeRes != -1) {
            builder.setNegativeButton(_negativeRes, _negativeListener);
        } else if (_negative != null) {
            builder.setNegativeButton(_negative, _negativeListener);
        } else {
            builder.setNegativeButton(R.string.dialog_cancel, _negativeListener);
        }

        if (_cancelListener != null) {
            builder.setOnCancelListener(_cancelListener);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && _onDismissListener != null) {
            builder.setOnDismissListener(_onDismissListener);
        }

        builder.setOnKeyListener(new keyListener(this));

        _alertDialog = builder.create();

        _alertDialog.setCanceledOnTouchOutside(this._cancelOnTouchOutside);
        _alertDialog.setOnShowListener(new onShowListener(this));

        _list = _alertDialog.getListView();
        _list.setOnItemClickListener(this);
        if (_enableMultiple) {
            _list.setOnItemLongClickListener(this);
        }

        if (_enableDpad) {
            _list.setSelector(listview_item_selector);
            _list.setDrawSelectorOnTop(true);
            _list.setItemsCanFocus(true);
            _list.setOnItemSelectedListener(this);
            _list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        _list.requestFocus();
        return this;
    }

    private void showDialog() {
        Window window = _alertDialog.getWindow();
        if (window != null) {
            TypedArray ta = _context.obtainStyledAttributes(R.styleable.FileChooser);
            window.setGravity(ta.getInt(R.styleable.FileChooser_fileChooserDialogGravity, Gravity.CENTER));
            ta.recycle();
        }
        _alertDialog.show();
    }

    public ChooserDialog show() {
        if (_alertDialog == null || _list == null) {
            build();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            showDialog();
            return this;
        }

        if (_permissionListener == null) {
            _permissionListener = new PermissionsUtil.OnPermissionListener() {
                @Override
                public void onPermissionGranted(String[] permissions) {
                    boolean show = false;
                    for (String permission : permissions) {
                        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            show = true;
                            break;
                        }
                    }
                    if (!show) return;
                    if (_enableOptions) {
                        show = false;
                        for (String permission : permissions) {
                            if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                show = true;
                                break;
                            }
                        }
                    }
                    if (!show) return;
                    if (_adapter.isEmpty()) refreshDirs();
                    showDialog();
                }

                @Override
                public void onPermissionDenied(String[] permissions) {
                    //
                }

                @Override
                public void onShouldShowRequestPermissionRationale(final String[] permissions) {
                    Toast.makeText(_context, "You denied the Read/Write permissions on SDCard.",
                        Toast.LENGTH_LONG).show();
                }
            };
        }

        final String[] permissions =
            /*_enableOptions ?*/ new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}
                /*: new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}*/;

        PermissionsUtil.checkPermissions(_context, _permissionListener, permissions);

        return this;
    }

    private boolean displayRoot;

    private void displayPath(String path) {
        if (_pathView == null) {
            int rootId = _context.getResources().getIdentifier("contentPanel", "id", _context.getPackageName());
            ViewGroup root = ((AlertDialog) _alertDialog).findViewById(rootId);
            // In case the root id was changed or not found.
            if (root == null) {
                rootId = _context.getResources().getIdentifier("contentPanel", "id", "android");
                root = ((AlertDialog) _alertDialog).findViewById(rootId);
                if (root == null) return;
            }

            ViewGroup.MarginLayoutParams params;
            if (root instanceof LinearLayout) {
                params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            } else {
                params = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP);
            }

            TypedArray ta = _context.obtainStyledAttributes(R.styleable.FileChooser);
            int style = ta.getResourceId(R.styleable.FileChooser_fileChooserPathViewStyle,
                R.style.FileChooserPathViewStyle);
            final Context context = new ContextThemeWrapper(_context, style);
            ta.recycle();
            ta = context.obtainStyledAttributes(R.styleable.FileChooser);

            displayRoot = ta.getBoolean(R.styleable.FileChooser_fileChooserPathViewDisplayRoot, true);

            _pathView = new TextView(context);
            root.addView(_pathView, 0, params);

            int elevation = ta.getInt(R.styleable.FileChooser_fileChooserPathViewElevation, 2);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                _pathView.setElevation(elevation);
            } else {
                ViewCompat.setElevation(_pathView, elevation);
            }
            ta.recycle();

            if (_customizePathView != null) {
                _customizePathView.customize(_pathView);
            }
        }

        if (path == null) {
            _pathView.setVisibility(View.GONE);

            ViewGroup.MarginLayoutParams param = ((ViewGroup.MarginLayoutParams) _list.getLayoutParams());
            if (_pathView.getParent() instanceof FrameLayout) {
                param.topMargin = 0;
            }
            _list.setLayoutParams(param);
        } else {
            if (removableRoot == null || primaryRoot == null) {
                removableRoot = FileUtil.getStoragePath(_context, true);
                primaryRoot = FileUtil.getStoragePath(_context, false);
            }
            if (path.contains(removableRoot)) {
                path = path.substring(displayRoot ? removableRoot.lastIndexOf('/') + 1 : removableRoot.length());
            }
            if (path.contains(primaryRoot)) {
                path = path.substring(displayRoot ? primaryRoot.lastIndexOf('/') + 1 : primaryRoot.length());
            }
            _pathView.setText(path);

            while (_pathView.getLineCount() > 1) {
                int i = path.indexOf("/");
                i = path.indexOf("/", i + 1);
                if (i == -1) break;
                path = "..." + path.substring(i);
                _pathView.setText(path);
            }

            _pathView.setVisibility(View.VISIBLE);

            ViewGroup.MarginLayoutParams param = ((ViewGroup.MarginLayoutParams) _list.getLayoutParams());
            if (_pathView.getHeight() == 0) {
                ViewTreeObserver viewTreeObserver = _pathView.getViewTreeObserver();
                viewTreeObserver.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (_pathView.getHeight() <= 0) {
                            return false;
                        }
                        viewTreeObserver.removeOnPreDrawListener(this);
                        if (_pathView.getParent() instanceof FrameLayout) {
                            param.topMargin = _pathView.getHeight();
                        }
                        _list.setLayoutParams(param);
                        _list.post(() -> _list.setSelection(0));
                        return true;
                    }
                });
            } else {
                if (_pathView.getParent() instanceof FrameLayout) {
                    param.topMargin = _pathView.getHeight();
                }
                _list.setLayoutParams(param);
            }
        }
    }

    private String removableRoot = null;
    private String primaryRoot = null;

    private void listDirs() {
        _entries.clear();

        if (_currentDir == null) {
            _currentDir = new File(FileUtil.getStoragePath(_context, false));
        }

        // Get files
        File[] files = _currentDir.listFiles(_fileFilter);

        // Add the ".." entry
        if (removableRoot == null || primaryRoot == null) {
            removableRoot = FileUtil.getStoragePath(_context, true);
            primaryRoot = FileUtil.getStoragePath(_context, false);
        }
        if (!removableRoot.equals(primaryRoot)) {
            if (_currentDir.getAbsolutePath().equals(primaryRoot)) {
                _entries.add(new RootFile(removableRoot, sSdcardStorage)); //⇠
            } else if (_currentDir.getAbsolutePath().equals(removableRoot)) {
                _entries.add(new RootFile(primaryRoot, sPrimaryStorage)); //⇽
            }
        }
        boolean displayPath = false;
        if (_entries.isEmpty() && _currentDir.getParentFile() != null && _currentDir.getParentFile().canRead()) {
            _entries.add(new RootFile(_currentDir.getParentFile().getAbsolutePath(), ".."));
            displayPath = true;
        }

        if (files == null) return;

        List<File> dirList = new LinkedList<>();
        List<File> fileList = new LinkedList<>();

        for (File f : files) {
            if (f.isDirectory()) {
                dirList.add(f);
            } else {
                fileList.add(f);
            }
        }

        sortByName(dirList);
        sortByName(fileList);
        _entries.addAll(dirList);
        _entries.addAll(fileList);

        // #45: setup dialog title too
        if (_alertDialog != null && !_disableTitle) {
            if (_followDir) {
                if (displayPath) {
                    _alertDialog.setTitle(_currentDir.getName());
                } else {
                    if (_titleRes != -1) {
                        _alertDialog.setTitle(_titleRes);
                    } else if (_title != null) {
                        _alertDialog.setTitle(_title);
                    } else {
                        _alertDialog.setTitle(R.string.choose_file);
                    }
                }

            }
        }

        // don't display path before alert dialog is shown
        // to avoid the exception under android M:
        //   Caused by android.util.AndroidRuntimeException: requestFeature() must be called before adding
        // content
        // issue #60
        if (_alertDialog != null && _alertDialog.isShowing() && _displayPath) {
            if (displayPath) {
                displayPath(_currentDir.getPath());
            } else {
                displayPath(null);
            }
        }
    }

    private void sortByName(List<File> list) {
        Collections.sort(list, (f1, f2) -> f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase()));
    }

    void createNewDirectory(String name) {
        if (FileUtil.createNewDirectory(name, _currentDir)) {
            refreshDirs();
            return;
        }

        final File newDir = new File(_currentDir, name);
        Toast.makeText(_context,
            "Couldn't create folder " + newDir.getName() + " at " + newDir.getAbsolutePath(),
            Toast.LENGTH_LONG).show();
    }

    Runnable _deleteModeIndicator;
    private int scrollTo;

    @Override
    public void onItemClick(AdapterView<?> parent_, View list_, int position, long id_) {
        if (position < 0 || position >= _entries.size()) return;

        scrollTo = 0;
        File file = _entries.get(position);
        if (file instanceof RootFile) {
            if (_folderNavUpCB == null) _folderNavUpCB = _defaultNavUpCB;
            if (_folderNavUpCB.canUpTo(file)) {
                _currentDir = file;
                _chooseMode = _chooseMode == CHOOSE_MODE_DELETE ? CHOOSE_MODE_NORMAL : _chooseMode;
                if (_deleteModeIndicator != null) _deleteModeIndicator.run();
                lastSelected = false;
                if (!_adapter.getIndexStack().empty()) {
                    scrollTo = _adapter.getIndexStack().pop();
                }
            }
        } else {
            switch (_chooseMode) {
                case CHOOSE_MODE_NORMAL:
                    if (file.isDirectory()) {
                        if (_folderNavToCB == null) _folderNavToCB = _defaultNavToCB;
                        if (_folderNavToCB.canNavigate(file)) {
                            _currentDir = file;
                            scrollTo = 0;
                            _adapter.getIndexStack().push(position);
                        }
                    } else if ((!_dirOnly) && _result != null) {
                        _alertDialog.dismiss();
                        _result.onChoosePath(file.getAbsolutePath(), file);
                        if (_enableMultiple) {
                            _result.onChoosePath(_currentDir.getAbsolutePath(), _currentDir);
                        }
                        return;
                    }
                    lastSelected = false;
                    break;
                case CHOOSE_MODE_SELECT_MULTIPLE:
                    if (file.isDirectory()) {
                        if (_folderNavToCB == null) _folderNavToCB = _defaultNavToCB;
                        if (_folderNavToCB.canNavigate(file)) {
                            _currentDir = file;
                            scrollTo = 0;
                            _adapter.getIndexStack().push(position);
                        }
                    } else {
                        _adapter.selectItem(position);
                        if (!_adapter.isAnySelected()) {
                            _chooseMode = CHOOSE_MODE_NORMAL;
                            _positiveBtn.setVisibility(View.INVISIBLE);
                        }
                        _result.onChoosePath(file.getAbsolutePath(), file);
                        return;
                    }
                    break;
                case CHOOSE_MODE_DELETE:
                    try {
                        FileUtil.deleteFileRecursively(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(_context, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    _chooseMode = CHOOSE_MODE_NORMAL;
                    if (_deleteModeIndicator != null) _deleteModeIndicator.run();
                    scrollTo = -1;
                    break;
                default:
                    // ERROR! It shouldn't get here...
                    return;
            }
        }
        refreshDirs();
        if (scrollTo != -1) {
            _list.setSelection(scrollTo);
            _list.post(() -> _list.setSelection(scrollTo));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View list, int position, long id) {
        File file = _entries.get(position);
        if (file instanceof RootFile || file.isDirectory()) {
            return true;
        }
        if (_adapter.isSelected(position)) return true;
        _result.onChoosePath(file.getAbsolutePath(), file);
        _adapter.selectItem(position);
        _chooseMode = CHOOSE_MODE_SELECT_MULTIPLE;
        _positiveBtn.setVisibility(View.VISIBLE);
        if (_deleteModeIndicator != null) _deleteModeIndicator.run();
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //
    }

    void refreshDirs() {
        listDirs();
        _adapter.setEntries(_entries);
    }

    public void dismiss() {
        _alertDialog.dismiss();
    }

    boolean lastSelected = false;

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
        lastSelected = position == _entries.size() - 1;
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {
        lastSelected = false;
    }

    List<File> _entries = new ArrayList<>();
    DirAdapter _adapter;
    File _currentDir;
    Context _context;
    AlertDialog _alertDialog;
    ListView _list;
    Result _result = null;
    private boolean _dirOnly;
    private FileFilter _fileFilter;
    private @StringRes
    int _titleRes = -1, _okRes = -1, _negativeRes = -1;
    private @Nullable
    String _title, _ok, _negative;
    private @DrawableRes
    int _iconRes = -1;
    private @Nullable
    Drawable _icon;
    private @LayoutRes
    int _layoutRes = -1;
    private String _dateFormat;
    private DialogInterface.OnClickListener _negativeListener;
    private DialogInterface.OnCancelListener _cancelListener;
    private DialogInterface.OnDismissListener _onDismissListener;
    private boolean _disableTitle;
    boolean _enableOptions;
    private boolean _followDir;
    private boolean _displayPath = true;
    TextView _pathView;
    private CustomizePathView _customizePathView;
    View _options;
    @StringRes
    int _createDirRes = -1, _deleteRes = -1, _newFolderCancelRes = -1, _newFolderOkRes = -1;
    @Nullable
    String _createDir, _delete, _newFolderCancel, _newFolderOk;
    @DrawableRes
    int _optionsIconRes = -1, _createDirIconRes = -1, _deleteIconRes = -1;
    @Nullable
    Drawable _optionsIcon, _createDirIcon, _deleteIcon;
    @Nullable
    View _newFolderView;
    boolean _enableMultiple;
    private PermissionsUtil.OnPermissionListener _permissionListener;
    private boolean _cancelOnTouchOutside;
    boolean _enableDpad = true;
    Button _neutralBtn;
    Button _negativeBtn;
    Button _positiveBtn;


    @FunctionalInterface
    public interface AdapterSetter {
        void apply(DirAdapter adapter);
    }

    private AdapterSetter _adapterSetter = null;

    @FunctionalInterface
    public interface CanNavigateUp {
        boolean canUpTo(File dir);
    }

    @FunctionalInterface
    public interface CanNavigateTo {
        boolean canNavigate(File dir);
    }

    private CanNavigateUp _folderNavUpCB;
    private CanNavigateTo _folderNavToCB;

    private final static CanNavigateUp _defaultNavUpCB = dir -> dir != null && dir.canRead();

    private final static CanNavigateTo _defaultNavToCB = dir -> true;

    /**
     * attempts to move to the parent directory
     *
     * @return true if successful. false otherwise
     */
    public boolean goBack() {
        if (_entries.size() > 0 &&
            (_entries.get(0).getName().equals(".."))) {
            _list.performItemClick(_list, 0, 0);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface OnBackPressedListener {
        void onBackPressed(AlertDialog dialog);
    }

    OnBackPressedListener _onBackPressed;

    private final static String sSdcardStorage = ".. SDCard Storage";
    private final static String sPrimaryStorage = ".. Primary Storage";

    static final int CHOOSE_MODE_NORMAL = 0;
    static final int CHOOSE_MODE_DELETE = 1;
    static final int CHOOSE_MODE_SELECT_MULTIPLE = 2;

    int _chooseMode = CHOOSE_MODE_NORMAL;

    NewFolderFilter _newFolderFilter;

    @FunctionalInterface
    public interface CustomizePathView {
        void customize(TextView pathView);
    }
}
