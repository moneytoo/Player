package com.brouken.player;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener {

    private final int REQUEST_CODE = 0x100;
    private ArrayAdapter<String> mAdapter;
    private TextView tilRemoteVideo;
    private EditText etRemoteVideo;
    private final static int VIDEO_REQUEST = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ListView videoList = findViewById(R.id.video_list);
        mAdapter = new ArrayAdapter<String>(this, R.layout.textview);
        videoList.setAdapter(mAdapter);
        videoList.setOnItemClickListener(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            initData(this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }

        tilRemoteVideo = findViewById(R.id.tilRemoteVideo);
        etRemoteVideo = (EditText) findViewById(R.id.etRemoteVideo);

        //本地
        Button btnLocalVideo = (Button) findViewById(R.id.btnLocalVideo);
        btnLocalVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*|audio/*");
                startActivityForResult(intent, VIDEO_REQUEST);
            }
        });

        //远程
        Button btnRemoteVideo = (Button) findViewById(R.id.btnRemoteVideo);
        etRemoteVideo.setText("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
        btnRemoteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etRemoteVideo.getText().toString().equals("")) {
                    tilRemoteVideo.setError("远程视频地址不能为空！");
                    return;
                }
                String url = etRemoteVideo.getText().toString().trim();
                PlayerActivity.start(MainActivity.this, url);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_CODE == requestCode && permissions.length > 0 && grantResults.length > 0) {
            if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[0])
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initData(this);
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == VIDEO_REQUEST && data != null) {
              //  String url = NyFileUtil.getPath(this, data.getData());
                PlayerActivity.start(MainActivity.this, data.getData());

            }
        } catch (Exception e) {
            Log.d("Local", e.toString());
        }
    }

    private void initData(Context context) {
        mAdapter.addAll(getLocalVideo(context));
    }

    public List<String> getLocalVideo(Context context) {
        List<String> videos = new ArrayList<>();
        Uri originalUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = context.getContentResolver();
        String selection = MediaStore.Video.Media.MIME_TYPE + "=? or "
                + MediaStore.Video.Media.MIME_TYPE + "=?";
        String[] selectionArgs = new String[]{"video/mp4"};
        Cursor cursor = cr.query(originalUri, null, selection, selectionArgs, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                try {
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                    videos.add(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        Collections.sort(videos);
        return videos;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String path = mAdapter.getItem(position);
        PlayerActivity.start(this, path);
    }
}
