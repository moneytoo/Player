package com.obsez.android.lib.filechooser.permissions;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class PermissionActivity extends AppCompatActivity {
    @SuppressWarnings("unused")
    private static final String TAG = PermissionActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String[] permissions = intent.getStringArrayExtra(INTENT_EXTRA_PERMISSIONS);
        if (permissions.length == 0) finish();
        _requestCode = intent.getIntExtra(INTENT_EXTRA_REQUEST_CODE, -1);
        if (_requestCode == -1) finish();
        _permissionListener = PermissionsUtil.getPermissionListener(_requestCode);

        for (String permission : permissions) {
            if (permission == null || permission.isEmpty()) {
                throw new RuntimeException("permission can't be null or empty");
            }
            if (ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED) {
                _permissions_granted.add(permission);
            } else {
                _permissions_denied.add(permission);
            }
        }

        if (_permissions_denied.isEmpty()) {
            if (_permissions_granted.isEmpty()) {
                throw new RuntimeException("there are no permissions");
            } else {
                if (_permissionListener != null) {
                    _permissionListener.onPermissionGranted(_permissions_granted.toArray(new String[0]));
                }
                finish();
            }
        } else {
            ActivityCompat.requestPermissions(this, _permissions_denied.toArray(new String[0]), _requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        if (requestCode != _requestCode) {
            finish();
        }
        _permissions_denied.clear();
        for (int i = permissions.length - 1; i >= 0; --i) {
            if (grantResults[i] == PERMISSION_GRANTED) {
                _permissions_granted.add(permissions[i]);
            } else {
                _permissions_denied.add(permissions[i]);
            }
        }
        if (_permissions_denied.isEmpty()) {
            if (_permissions_granted.isEmpty()) {
                throw new RuntimeException("there are no permissions");
            } else {
                if (_permissionListener != null) {
                    _permissionListener.onPermissionGranted(_permissions_granted.toArray(new String[0]));
                }
                finish();
            }
        } else {
            List<String> permissionsShouldRequest = new ArrayList<>();
            for (String permission : _permissions_denied) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    permissionsShouldRequest.add(permission);
                }
            }
            if (_permissionListener != null) {
                _permissionListener.onPermissionDenied(_permissions_denied.toArray(new String[0]));
                _permissionListener.onShouldShowRequestPermissionRationale(permissionsShouldRequest.toArray(new String[0]));
            }
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            overridePendingTransition(0, 0);
        }
    }

    @Nullable
    private PermissionsUtil.OnPermissionListener _permissionListener;
    public int _requestCode;

    private List<String> _permissions_granted = new ArrayList<>();
    private List<String> _permissions_denied = new ArrayList<>();

    public static final String INTENT_EXTRA_PERMISSIONS = "PERMISSIONS";
    public static final String INTENT_EXTRA_REQUEST_CODE = "REQUEST_CODE";
}
