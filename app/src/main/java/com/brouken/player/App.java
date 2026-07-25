package com.brouken.player;

import android.app.Application;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import io.sentry.SentryEvent;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;

import java.util.List;

public class App extends Application {

    /**
     * Process start, captured when this class is loaded — the first app code to run. Used by the error
     * report's uptime line; a static beats Process.getStartElapsedRealtime() only in that it needs no
     * API-level branch (that call arrived in API 24, this app supports 23).
     */
    static final long START_ELAPSED = SystemClock.elapsedRealtime();

    @Override
    public void onCreate() {
        super.onCreate();
        initSentry();
        // Installed after Sentry so it wraps (and chains to) Sentry's crash handler rather than
        // replacing it: any uncaught crash lands on ErrorActivity, then Sentry still reports.
        ErrorActivity.installCrashHandler(this);
    }

    private void initSentry() {
        final String dsn = BuildConfig.SENTRY_DSN;
        if (dsn == null || dsn.isEmpty())
            return;
        SentryAndroid.init(this, options -> {
            options.setDsn(dsn);
            options.setRelease(BuildConfig.APPLICATION_ID + "@" + BuildConfig.VERSION_NAME);
            options.setDist(String.valueOf(BuildConfig.VERSION_CODE));
            options.setEnvironment(BuildConfig.DEBUG ? "debug" : "release");
            options.setBeforeSend((event, hint) -> {
                // Honor the user's consent toggle. Checked per event (and for events cached offline or
                // from a crash and sent on the next launch), so switching it off takes effect immediately.
                if (!PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean("crashReporting", true)) {
                    return null;
                }
                // Drop URL query strings that ExoPlayer bakes into error messages (e.g. "Response code:
                // 403 for https://host/path?token=...") so tokens/session ids never leave the device.
                stripUrlQueries(event);
                return event;
            });
        });
    }

    private static void stripUrlQueries(final SentryEvent event) {
        final Message message = event.getMessage();
        if (message != null) {
            message.setFormatted(Utils.stripUrlQuery(message.getFormatted()));
            message.setMessage(Utils.stripUrlQuery(message.getMessage()));
        }
        final List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            for (SentryException exception : exceptions) {
                exception.setValue(Utils.stripUrlQuery(exception.getValue()));
            }
        }
    }
}
