package com.obsez.android.lib.filechooser;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;

import java.lang.ref.WeakReference;

class keyListener implements DialogInterface.OnKeyListener {
    private WeakReference<ChooserDialog> _c;

    keyListener(ChooserDialog c) {
        this._c = new WeakReference<>(c);
    }

    /**
     * Called when a key is dispatched to a dialog. This allows listeners to
     * get a chance to respond before the dialog.
     *
     * @param dialog  the dialog the key has been dispatched to
     * @param keyCode the code for the physical key that was pressed
     * @param event   the KeyEvent object containing full information about
     *                the event
     * @return {@code true} if the listener has consumed the event,
     * {@code false} otherwise
     */
    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            if (_c.get()._newFolderView != null && _c.get()._newFolderView.getVisibility() == VISIBLE) {
                _c.get()._newFolderView.setVisibility(GONE);
                return true;
            }
            _c.get()._onBackPressed.onBackPressed(_c.get()._alertDialog);
            return true;
        }

        if (!_c.get()._enableDpad) return true;

        if (!_c.get()._list.hasFocus()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (_c.get()._neutralBtn == null) {
                        return false;
                    }
                    if (_c.get()._neutralBtn.hasFocus() || _c.get()._negativeBtn.hasFocus()
                        || _c.get()._positiveBtn.hasFocus()) {
                        if (_c.get()._options != null && _c.get()._options.getVisibility() == VISIBLE) {
                            _c.get()._options.requestFocus(
                                _c.get()._neutralBtn.hasFocus() ? View.FOCUS_RIGHT : View.FOCUS_LEFT);
                            return true;
                        } else if (_c.get()._newFolderView != null
                            && _c.get()._newFolderView.getVisibility() == VISIBLE) {
                            _c.get()._newFolderView.requestFocus(View.FOCUS_LEFT);
                            return true;
                        } else {
                            _c.get()._list.requestFocus();
                            _c.get().lastSelected = true;
                            return true;
                        }
                    }
                    if (_c.get()._options != null && _c.get()._options.hasFocus()) {
                        _c.get()._list.requestFocus();
                        _c.get().lastSelected = true;
                        return true;
                    }
                    break;
                default:
                    return false;
            }
        }

        if (_c.get()._list.hasFocus()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    _c.get()._onBackPressed.onBackPressed(_c.get()._alertDialog);
                    _c.get().lastSelected = false;
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    _c.get()._list.performItemClick(_c.get()._list, _c.get()._list.getSelectedItemPosition(),
                        _c.get()._list.getSelectedItemId());
                    _c.get().lastSelected = false;
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (_c.get().lastSelected) {
                        _c.get().lastSelected = false;
                        if (_c.get()._options != null && _c.get()._options.getVisibility() == VISIBLE) {
                            _c.get()._options.requestFocus();
                        } else {
                            if (_c.get()._neutralBtn.getVisibility() == VISIBLE) {
                                _c.get()._neutralBtn.requestFocus();
                            } else {
                                _c.get()._negativeBtn.requestFocus();
                            }
                        }
                        return true;
                    }
                    break;
                default:
                    return false;
            }
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        this._c.clear();
        this._c = null;
        super.finalize();
    }
}
