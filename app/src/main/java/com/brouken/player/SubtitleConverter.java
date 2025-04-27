package com.brouken.player;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.sigpwned.chardet4j.Chardet;
import com.sigpwned.chardet4j.io.DecodedInputStreamReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SubtitleConverter {

    private volatile OkHttpClient okHttpClient;

    public List<Uri> convertSubtitles(Context context, List<Uri> uris) {
        CountDownLatch countDownLatch = new CountDownLatch(uris.size());
        Uri[] results = new Uri[uris.size()];

        for (int i = 0; i < uris.size(); i++) {
            convertSubtitle(context, countDownLatch, results, i, uris.get(i));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }

        return Arrays.asList(results);
    }

    private void convertSubtitle(Context context, CountDownLatch countDownLatch, Uri[] results, int positionOnResults, Uri sourceUri) {
        String scheme = sourceUri.getScheme();

        if (scheme == null) {
            results[positionOnResults] = sourceUri;
            countDownLatch.countDown();
            return;
        }

        if (scheme.equals("http") || scheme.equals("https")) {
            convertSubtitleFromHttp(context, countDownLatch, results, positionOnResults, sourceUri);
        } else {
            results[positionOnResults] = sourceUri;
        }
    }

    private void convertSubtitleFromHttp(Context context, CountDownLatch countDownLatch, Uri[] results, int positionOnResults, Uri sourceUri) {
        new Thread(() -> {
            OkHttpClient client = getOrCreateOkHttpClient();

            Request request = new Request.Builder()
                    .url(sourceUri.toString())
                    .build();

            Uri convertedUri = sourceUri;

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    //noinspection DataFlowIssue
                    try (DecodedInputStreamReader reader = Chardet.decode(responseBody.byteStream(), StandardCharsets.UTF_8)) {
                        File subtitleCacheDir = getSubtitleCacheDir(context);
                        String fileName = Utils.getFileName(context, sourceUri, true);
                        File subtitleFile = new File(subtitleCacheDir, fileName);
                        try (Writer writer = new FileWriter(subtitleFile)) {
                            char[] buffer = new char[4096];
                            int read;
                            while ((read = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, read);
                            }
                            writer.flush();
                            convertedUri = Uri.fromFile(subtitleFile);
                        }
                    }
                }
            } catch (IOException e) {
                Log.w(Utils.TAG, e);
            }

            results[positionOnResults] = convertedUri;
            countDownLatch.countDown();


        }).start();
    }

    private OkHttpClient getOrCreateOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (this) {
                if (okHttpClient == null) {
                    okHttpClient = new OkHttpClient.Builder().build();
                }
            }
        }
        return okHttpClient;
    }

    private static synchronized File getSubtitleCacheDir(Context context) throws IOException {
        File subtitleCacheDir = new File(context.getCacheDir(), "subtitles");
        if (!subtitleCacheDir.exists()) {
            if (!subtitleCacheDir.mkdirs()) {
                throw new IOException("Couldn't create subtitles cache directory");
            }
        }
        return subtitleCacheDir;
    }

}
