package com.brouken.player;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Friendly, full-screen surface for a playback error. The on-screen panel shows only the error code
 * and message; the full diagnostic report (device + media URI + stack/cause chain — the same detail
 * sent to Sentry) is what Copy / Share / Upload carry. Reached from the player's error handling.
 */
public class ErrorActivity extends AppCompatActivity {

    /** Optional headline body override; defaults to the playback-error copy in the layout. */
    public static final String EXTRA_MESSAGE = "message";
    /** Short, human-facing text shown in the panel: error code + message. */
    public static final String EXTRA_SUMMARY = "summary";
    /** Full diagnostic report body; a device/app header is prepended for Copy/Share/Upload. */
    public static final String EXTRA_REPORT = "report";

    private String report;
    private String uploadedUrl;

    private View btnUpload;
    private ProgressBar uploadProgress;
    private View uploadResult;
    private TextView uploadUrl;
    private ImageView qrImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);

        // Strip URL query strings (tokens/session ids) from everything shown, copied, shared or uploaded
        // — matching Sentry's beforeSend sanitisation. Critical because the report can be pasted publicly
        // (termbin) and ExoPlayer bakes full URLs into exception messages/stack traces.
        final String summary = Utils.stripUrlQuery(getIntent().getStringExtra(EXTRA_SUMMARY));
        report = buildReport(Utils.stripUrlQuery(getIntent().getStringExtra(EXTRA_REPORT)));

        ((TextView) findViewById(R.id.errorDetails)).setText(summary != null ? summary : "");
        final String message = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (message != null) {
            ((TextView) findViewById(R.id.errorMessage)).setText(message);
        }

        btnUpload = findViewById(R.id.btnUpload);
        uploadProgress = findViewById(R.id.uploadProgress);
        uploadResult = findViewById(R.id.uploadResult);
        uploadUrl = findViewById(R.id.uploadUrl);
        qrImage = findViewById(R.id.qrImage);

        findViewById(R.id.btnCopy).setOnClickListener(v -> copy(report));
        findViewById(R.id.btnShare).setOnClickListener(v -> share(report));
        btnUpload.setOnClickListener(v -> upload());
        uploadUrl.setOnClickListener(v -> copy(uploadUrl.getText().toString()));
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        findViewById(R.id.btnClose).requestFocus();

        animateIn();
    }

    /**
     * Install a process-wide handler so any uncaught crash (not just playback errors) lands on this
     * screen too, then chains to the previously-registered handler — Sentry's when enabled — so crash
     * reporting and process termination still happen. Call once, after Sentry is initialised.
     */
    public static void installCrashHandler(final Context context) {
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                // This activity's empty taskAffinity puts the crash screen in a task of its own, so the
                // app's task keeps its history and is restored as the player. CLEAR_TASK then clears that
                // crash task rather than the app's — needed because a second crash would otherwise reuse
                // the existing task (same affinity, intents equal apart from extras) and, with no
                // onNewIntent handling, relaunch the FIRST crash's report.
                app.startActivity(new Intent(app, ErrorActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(EXTRA_MESSAGE, app.getString(R.string.error_crash_message))
                        .putExtra(EXTRA_SUMMARY, summaryOf(throwable))
                        .putExtra(EXTRA_REPORT, stackTrace(throwable)));
            } catch (Throwable ignored) {
                // Never let the error screen's own failure mask the original crash.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }

    // The exception type as the "code" line, plus the deepest cause message — mirrors the playback path.
    static String summaryOf(final Throwable t) {
        final String message = rootMessage(t);
        final String name = t.getClass().getSimpleName();
        return message != null ? name + "\n" + message : name;
    }

    static String stackTrace(final Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // Deepest non-empty message in the cause chain — the most specific description of what failed.
    static String rootMessage(final Throwable t) {
        String message = null;
        for (Throwable c = t; c != null; c = c.getCause()) {
            final String m = c.getLocalizedMessage();
            if (m != null && !m.isEmpty()) {
                message = m;
            }
        }
        return message;
    }

    private String buildReport(final String body) {
        // Header mirrors the metadata Sentry attaches (release, dist, environment, timestamp) so a
        // pasted/shared report carries at least as much context as a Sentry event.
        final String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date());
        return BuildConfig.APPLICATION_ID + "@" + BuildConfig.VERSION_NAME
                + " (build " + BuildConfig.VERSION_CODE + ", " + (BuildConfig.DEBUG ? "debug" : "release") + ")\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL
                + " (Android " + Build.VERSION.RELEASE + ", API " + Build.VERSION.SDK_INT + ")\n"
                + "Time: " + time + "\n\n"
                + (body != null ? body : "");
    }

    private void animateIn() {
        // A subtle rise+fade so the screen doesn't slam in; skipped when the user disables animations.
        final float scale = Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        if (scale == 0f) {
            return;
        }
        final View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.setTranslationY(getResources().getDisplayMetrics().density * 16f);
        root.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    private void copy(final String text) {
        final ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Just+ Player error", text));
            Toast.makeText(this, R.string.error_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void share(final String text) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.error_share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, getString(R.string.error_share)));
    }

    private void upload() {
        if (uploadedUrl != null) {
            showUploaded(uploadedUrl);
            return;
        }
        btnUpload.setEnabled(false);
        uploadProgress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            final String url = uploadToTermbin(report);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                uploadProgress.setVisibility(View.GONE);
                btnUpload.setEnabled(true);
                if (url != null) {
                    uploadedUrl = url;
                    showUploaded(url);
                } else {
                    Toast.makeText(this, R.string.error_upload_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showUploaded(final String url) {
        // Replace the code panel's content with the QR.
        findViewById(R.id.errorDetails).setVisibility(View.GONE);
        uploadUrl.setText(url);
        uploadResult.setVisibility(View.VISIBLE);
        loadQr(url);
    }

    private void loadQr(final String url) {
        new Thread(() -> {
            final Bitmap bitmap = fetchQr(url);
            runOnUiThread(() -> {
                if (!isFinishing() && bitmap != null) {
                    qrImage.setImageBitmap(bitmap);
                }
            });
        }).start();
    }

    // Raw-socket paste to termbin.com:9999 — it echoes back the public URL of the pasted text.
    private static String uploadToTermbin(final String text) {
        try (Socket socket = new Socket("termbin.com", 9999)) {
            socket.setSoTimeout(10000);
            final OutputStream out = socket.getOutputStream();
            out.write(text.getBytes("UTF-8"));
            out.flush();
            final InputStream in = socket.getInputStream();
            final StringBuilder sb = new StringBuilder();
            final byte[] buf = new byte[256];
            int n;
            while ((n = in.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, "UTF-8"));
            }
            final String url = sb.toString().trim().replace(" ", "");
            return url.isEmpty() ? null : url;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap fetchQr(final String url) {
        HttpURLConnection connection = null;
        try {
            final String api = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data="
                    + URLEncoder.encode(url, "UTF-8");
            connection = (HttpURLConnection) new URL(api).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            try (InputStream in = connection.getInputStream()) {
                return BitmapFactory.decodeStream(in);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
