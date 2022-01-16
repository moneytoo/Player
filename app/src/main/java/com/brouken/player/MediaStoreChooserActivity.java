package com.brouken.player;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MediaStoreChooserActivity extends Activity {

    public static final String BUCKET_ID = "BUCKET_ID";
    public static final String SUBTITLES = "SUBTITLES";
    public static final String TITLE = "TITLE";

    final int REQUEST_PERMISSION_STORAGE = 0;

    Integer bucketId;
    boolean subtitles;
    String title;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent.hasExtra(BUCKET_ID)) {
            this.bucketId = intent.getIntExtra(BUCKET_ID, Integer.MIN_VALUE);
        }
        this.subtitles = intent.getBooleanExtra(SUBTITLES, false);
        this.title = intent.getStringExtra(TITLE);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            start();
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_STORAGE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        } else {
            start();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void start() {
        if (bucketId == null) {
            showBuckets();
        } else {
            showFiles(bucketId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    start();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    HashMap<Integer, String> query(String projectionId, String projectionName, String selection) {
        Uri collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        if (subtitles) {
            collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        HashMap<Integer, String> hashMap = new HashMap<>();
        try (Cursor cursor = getContentResolver().query(collection, new String[] { projectionId, projectionName }, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnId = cursor.getColumnIndex(projectionId);
                int columnName = cursor.getColumnIndex(projectionName);

                do {
                    int id = cursor.getInt(columnId);
                    String name = cursor.getString(columnName);
                    if (name == null) {
                        continue;
                    }
                    if (!hashMap.containsKey(id)) {
                        hashMap.put(id, name);
                    }
                } while (cursor.moveToNext());
            }
        }
        // Sort map by value
        List<Map.Entry<Integer, String>> list = new LinkedList<>(hashMap.entrySet());
        Collections.sort(list, (o1, o2) -> o1.getValue().compareToIgnoreCase(o2.getValue()));
        HashMap<Integer, String> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> map : list) {
            sortedMap.put(map.getKey(), map.getValue());
        }
        return sortedMap;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void showBuckets() {
        String selection = null;
        if (subtitles) {
            selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE;
        }

        HashMap<Integer, String> buckets = query(MediaStore.MediaColumns.BUCKET_ID, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, selection);

        Integer[] bucketIds = buckets.keySet().toArray(new Integer[0]);
        String[] bucketDisplayNames = buckets.values().toArray(new String[0]);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setItems(bucketDisplayNames, (dialogInterface, i) -> {
            Intent intent = new Intent(MediaStoreChooserActivity.this, MediaStoreChooserActivity.class);
            intent.putExtra(SUBTITLES, subtitles);
            intent.putExtra(BUCKET_ID, bucketIds[i]);
            intent.putExtra(TITLE, bucketDisplayNames[i]);
            startActivityForResult(intent, 0);
        });
        alertDialogBuilder.setOnCancelListener(dialogInterface -> finish());
        alertDialogBuilder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    void showFiles(int bucketId) {
        String selection = MediaStore.MediaColumns.BUCKET_ID + "=" + bucketId;

        if (subtitles) {
            selection += " AND " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE;
        }

        HashMap<Integer, String> files = query(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, selection);

        Integer[] ids = files.keySet().toArray(new Integer[0]);
        String[] displayNames = files.values().toArray(new String[0]);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        if (title != null) {
            alertDialogBuilder.setTitle(title);
        }
        alertDialogBuilder.setItems(displayNames, (dialogInterface, i) -> {
            Uri contentUri;
            if (subtitles) {
                contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL, ids[i]);
            } else {
                contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ids[i]);
            }
            Intent data = new Intent("RESULT", contentUri);
            setResult(RESULT_OK, data);
            finish();
        });
        alertDialogBuilder.setOnCancelListener(dialogInterface -> finish());
        alertDialogBuilder.show();
    }
}
