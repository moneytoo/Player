package com.brouken.player;

import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class SubtitleFetcher {

    private PlayerActivity activity;
    private CountDownLatch countDownLatch;
    private final List<Uri> urls;
    private Uri subtitleUri;
    private final List<Uri> foundUrls;

    public SubtitleFetcher(PlayerActivity activity, List<Uri> urls) {
        this.activity = activity;
        this.urls = urls;
        this.foundUrls = new ArrayList<>();
    }

    public void start() {

        new Thread(() -> {

            OkHttpClient client = new OkHttpClient.Builder()
                    //.callTimeout(15, TimeUnit.SECONDS)
                    .build();

            Callback callback = new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    countDownLatch.countDown();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Uri url = Uri.parse(response.request().url().toString());
                    Utils.log(response.code() + ": " + url);
                    if (response.isSuccessful()) {
                        foundUrls.add(url);
                    }
                    response.close();
                    countDownLatch.countDown();
                }
            };

            countDownLatch = new CountDownLatch(urls.size());

            for (Uri url : urls) {
                // Total Commander 3.24 / LAN plugin 3.20 does not support HTTP HEAD
                //Request request = new Request.Builder().url(url.toString()).head().build();
                if (HttpUrl.parse(url.toString()) == null) {
                    countDownLatch.countDown();
                    continue;
                }
                Request request = new Request.Builder().url(url.toString()).build();
                client.newCall(request).enqueue(callback);
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (Uri url : urls) {
                if (foundUrls.contains(url)) {
                    subtitleUri = url;
                    break;
                }
            }

            if (subtitleUri == null) {
                return;
            }

            Utils.log(subtitleUri.toString());

            // ProtocolException when reusing client:
            // java.net.ProtocolException: Unexpected status line: 1
            client = new OkHttpClient.Builder()
                    //.callTimeout(15, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(subtitleUri.toString()).build();
            try (Response response = client.newCall(request).execute()) {
                final ResponseBody responseBody = response.body();

                if (responseBody == null || responseBody.contentLength() > 2_000_000) {
                    return;
                }

                InputStream inputStream = responseBody.byteStream();
                Uri convertedSubtitleUri = SubtitleUtils.convertInputStreamToUTF(activity, subtitleUri, inputStream);

                if (convertedSubtitleUri == null) {
                    return;
                }

                activity.runOnUiThread(() -> {
                    activity.mPrefs.updateSubtitle(convertedSubtitleUri);
                    if (PlayerActivity.player != null) {
                        MediaItem mediaItem = PlayerActivity.player.getCurrentMediaItem();
                        if (mediaItem != null) {
                            MediaItem.SubtitleConfiguration subtitle = SubtitleUtils.buildSubtitle(activity, convertedSubtitleUri, null, true);
                            mediaItem = mediaItem.buildUpon().setSubtitleConfigurations(Collections.singletonList(subtitle)).build();
                            PlayerActivity.player.setMediaItem(mediaItem, false);
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(activity, "Subtitle found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            } catch (IOException e) {
                Utils.log(e.toString());
                e.printStackTrace();
            }
        }).start();
    }

}
