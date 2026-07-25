package com.brouken.player;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
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
import java.util.TimeZone;

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
        // pasted/shared report carries at least as much context as a Sentry event. The device and
        // runtime blocks below carry what Sentry's device/app contexts did — they are what makes a
        // manually pasted report a viable replacement for automatic reporting.
        final String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date());
        final StringBuilder sb = new StringBuilder();
        sb.append(BuildConfig.APPLICATION_ID).append('@').append(BuildConfig.VERSION_NAME)
                .append(" (build ").append(BuildConfig.VERSION_CODE)
                .append(", ").append(BuildConfig.FLAVOR)
                .append(' ').append(BuildConfig.DEBUG ? "debug" : "release").append(")\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" (Android ").append(Build.VERSION.RELEASE)
                .append(", API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Time: ").append(time).append('\n');
        appendDevice(sb);
        appendRuntime(sb);
        sb.append('\n').append(body != null ? body : "");
        return sb.toString();
    }

    /**
     * Firmware, hardware and display detail — all of it from Build/Resources, so no permission is
     * involved. The fingerprint pins the exact OEM build (crashes cluster per firmware, not per model);
     * refresh rate and form factor matter because of frame-rate matching and the TV layout.
     */
    private void appendDevice(final StringBuilder sb) {
        sb.append("Build: ").append(Build.FINGERPRINT).append('\n');
        sb.append("Hardware: ").append(Build.DEVICE).append('/').append(Build.HARDWARE);
        if (Build.VERSION.SDK_INT >= 31) {
            sb.append(' ').append(Build.SOC_MODEL);
        }
        sb.append(", ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?")
                .append(", patch ").append(Build.VERSION.SECURITY_PATCH).append('\n');
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        sb.append("Display: ").append(metrics.widthPixels).append('x').append(metrics.heightPixels)
                .append(" @").append(metrics.densityDpi).append("dpi ")
                .append(String.format(Locale.US, "%.2fHz", refreshRate())).append(", ")
                .append(Utils.isTvBox(this) ? "tv" : Utils.isTablet(this) ? "tablet" : "phone")
                .append(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ", landscape" : ", portrait").append('\n');
        sb.append("Locale: ").append(Locale.getDefault()).append(", ")
                .append(TimeZone.getDefault().getID()).append('\n');
    }

    /**
     * How the app was installed, how long it had been running and how much headroom it had — the
     * difference between "playback failed" and "playback failed 4 seconds in with the heap nearly full".
     */
    private void appendRuntime(final StringBuilder sb) {
        sb.append("Runtime: up ").append((SystemClock.elapsedRealtime() - App.START_ELAPSED) / 1000)
                .append("s, installer ").append(installer()).append('\n');
        final Runtime runtime = Runtime.getRuntime();
        sb.append("Memory: heap ").append((runtime.totalMemory() - runtime.freeMemory()) >> 20)
                .append('/').append(runtime.maxMemory() >> 20).append(" MB");
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            final ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memory);
            sb.append(", device ").append(memory.availMem >> 20).append(" MB free")
                    .append(memory.lowMemory ? " (low)" : "");
        }
        sb.append(", storage ").append(new StatFs(getFilesDir().getAbsolutePath()).getAvailableBytes() >> 20)
                .append(" MB free\n");
        // Same SharedPreferences read App.initSentry() uses — cheaper than building a Prefs, which
        // would also load unrelated playback state.
        final android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        sb.append("Prefs: file access ").append(prefs.getString("fileAccess", "auto"))
                .append(prefs.getBoolean("crashReporting", true) ? ", crash reporting on" : ", crash reporting off")
                .append('\n');
    }

    private float refreshRate() {
        final DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        final Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        return display != null ? display.getRefreshRate() : 0f;
    }

    // Deprecated since API 30 in favour of getInstallSourceInformation(), itself replaced by
    // getInstallSourceInfo() in API 36 — this one call still answers "store install or sideload?" on
    // every supported level, which is all the report needs.
    @SuppressWarnings("deprecation")
    private String installer() {
        try {
            final String name = getPackageManager().getInstallerPackageName(getPackageName());
            // No installer at all means adb / a raw APK, which is worth telling apart from a store install.
            return name != null ? name : "none (sideloaded)";
        } catch (Exception e) {
            return "?";
        }
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
