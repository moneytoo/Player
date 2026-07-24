package com.brouken.player;

import static android.content.pm.PackageManager.FEATURE_EXPANDED_PICTURE_IN_PICTURE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.accessibility.CaptioningManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.VideoSize;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.media3.ui.TimeBar;

import com.brouken.player.dtpv.DoubleTapPlayerView;
import com.brouken.player.dtpv.youtube.YouTubeOverlay;
import com.brouken.player.skip.IntentSegmentsSource;
import com.brouken.player.skip.NetworkSegmentsSource;
import com.brouken.player.skip.SegmentFinder;
import com.brouken.player.skip.SkipManager;
import com.brouken.player.skip.SkipSegment;
import com.brouken.player.update.UpdateUi;
import com.brouken.player.update.Updater;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends Activity {

    private PlayerListener playerListener;
    private BroadcastReceiver mReceiver;
    private AudioManager mAudioManager;
    private MediaSession mediaSession;
    private DefaultTrackSelector trackSelector;
    public static LoudnessEnhancer loudnessEnhancer;

    private CustomDefaultTrackNameProvider trackNameProvider;
    // Track names read from the container (MP4 udta/name, MKV TrackEntry/Name), and the resolved
    // Format.id -> name map that the track list and header read from once tracks are known.
    private final java.util.List<TrackMetadata> containerTracks = new java.util.ArrayList<>();
    private final java.util.Map<String, String> resolvedTrackNames = new java.util.HashMap<>();
    // Streaming manifest type (HLS/DASH/SS) discovered from the real HTTP response of a media item
    // whose request URL had no telling extension. Keyed by the requested URI (== MediaItem URI).
    // Written from a load thread, read on the player thread — hence concurrent.
    private final java.util.Map<String, String> resolvedMediaTypes = new java.util.concurrent.ConcurrentHashMap<>();
    private final TrackNameParsingDataSource.Listener trackNameListener = new TrackNameParsingDataSource.Listener() {
        @Override
        public void onMetadataParsed(java.util.List<TrackMetadata> tracks) {
            // Parser runs on a background thread; hop to the UI thread to touch player/views.
            runOnUiThread(() -> onContainerMetadata(tracks));
        }

        @Override
        public boolean isMetadataParsed() {
            return !containerTracks.isEmpty();
        }

        @Override
        public void onMediaTypeResolved(Uri originalUri, String mimeType) {
            if (originalUri != null && mimeType != null) {
                resolvedMediaTypes.put(originalUri.toString(), mimeType);
            }
        }

        @Override
        public void onResolverNotReady(Uri originalUri) {
            if (originalUri != null) {
                resolverNotReadyUri = originalUri.toString();
            }
        }
    };

    // Set (on a load thread) to the URI whose response was a Lampac resolver handshake instead of
    // media; read in onPlayerError to show a friendly message rather than retrying/reporting.
    private volatile String resolverNotReadyUri;

    public CustomPlayerView playerView;
    public static ExoPlayer player;
    private YouTubeOverlay youTubeOverlay;

    private Object mPictureInPictureParamsBuilder;

    public Prefs mPrefs;
    public BrightnessControl mBrightnessControl;
    public static boolean haveMedia;
    private boolean videoLoading;
    // Watchdog for a silent load failure: if the player never reaches STATE_READY within this window
    // (stuck buffering, a broken next-episode URL, etc.) a friendly LOAD_TIMEOUT message is shown.
    // Such stalls often produce no PlaybackException, so onPlayerError alone would never catch them.
    private static final long VIDEO_LOAD_TIMEOUT_MS = 30_000L;
    // Heavy MKV audio codecs whose platform MediaCodec decoder can wedge on init on some TV boxes
    // (JPP-1005). On TV+MKV these are hidden from the platform decoder via a MediaCodecSelector, so
    // MediaCodecAudioRenderer either bitstreams to a receiver that advertises passthrough (Atmos kept)
    // or the track falls through to the ffmpeg software renderer — never the wedging platform decoder.
    // Set matches the ffmpeg build's decoders (app/libs/README.md); AC4 / DTS:X-UHD are excluded since
    // ffmpeg has no decoder for them and blocking would leave the track unplayable (muted audio).
    private static final List<String> HEAVY_MKV_AUDIO_MIMES = Arrays.asList(
            MimeTypes.AUDIO_AC3, MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC,
            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS,
            MimeTypes.AUDIO_TRUEHD);
    private final Runnable loadTimeoutRunnable = this::reportVideoLoadTimeout;
    // One-shot recovery for a mid-playback stall (Media3 StuckPlayerDetector → ERROR_CODE_TIMEOUT),
    // typically the device's Dolby Vision decoder wedging on a stream: re-decode the DV track as plain
    // HEVC (HDR10). forceHevcForDolbyVision drives the codec selector at the next player build;
    // pendingStuckRecovery marks that rebuild so the reset in initializePlayer keeps the flag;
    // stuckRecoveryAttemptedUri guards against retrying the same URI in a loop.
    private boolean forceHevcForDolbyVision;
    private boolean pendingStuckRecovery;
    private String stuckRecoveryAttemptedUri;
    public static boolean controllerVisible;
    public static boolean controllerVisibleFully;
    public static Snackbar snackbar;
    private ExoPlaybackException errorToShow;
    public static int boostLevel = 0;
    private boolean isScaling = false;
    private boolean isScaleStarting = false;
    private float scaleFactor = 1.0f;

    private static final int REQUEST_CHOOSER_VIDEO = 1;
    private static final int REQUEST_CHOOSER_SUBTITLE = 2;
    private static final int REQUEST_CHOOSER_SCOPE_DIR = 10;
    private static final int REQUEST_CHOOSER_VIDEO_MEDIASTORE = 20;
    private static final int REQUEST_CHOOSER_SUBTITLE_MEDIASTORE = 21;
    private static final int REQUEST_SETTINGS = 100;
    private static final int REQUEST_SYSTEM_CAPTIONS = 200;
    public static final int CONTROLLER_TIMEOUT = 3500;
    private static final String ACTION_MEDIA_CONTROL = "media_control";
    private static final String EXTRA_CONTROL_TYPE = "control_type";
    private static final int REQUEST_PLAY = 1;
    private static final int REQUEST_PAUSE = 2;
    private static final int CONTROL_TYPE_PLAY = 1;
    private static final int CONTROL_TYPE_PAUSE = 2;

    private CoordinatorLayout coordinatorLayout;
    private LinearLayout topInfoPanel;
    private LinearLayout headerButtons;
    private FrameLayout posterSlot;
    private ImageView posterView;
    private TextView posterPlaceholderView;
    private TextView posterBadgeView;
    private TextView titleView;
    private TextView videoInfoView;
    private TextView audioInfoView;
    private TextView endsAtView;
    private OutlineTextClock overlayClock;
    private OutlineTextClock headerClock;
    private ImageButton buttonOpen;
    private ImageButton buttonPlaylist;
    private ImageButton buttonQuality;
    private ImageButton buttonAudio;
    private ImageButton buttonMore;
    private ImageButton buttonSkipOffset;
    private android.app.Dialog qualityDialog;
    private android.app.Dialog playlistDialog;
    private android.app.Dialog skipOffsetDialog;
    private android.app.Dialog menuDialog;
    // While a picker panel is open the app must stay out of immersive/fullscreen, otherwise OxygenOS/ColorOS
    // applies its fullscreen back-gesture guard ("swipe again to go back") and the panel needs two swipes.
    private boolean pickerDialogOpen;
    // Media3 keeps the controller shown indefinitely while paused (it forces the auto-hide timeout to 0),
    // so reuse the same CONTROLLER_TIMEOUT + hideController() to also clear the UI on pause. A tap re-shows
    // and re-arms it, exactly like during playback (see scheduleHideControllerOnPause).
    private final Runnable hideControllerAction = () -> {
        if (player != null && !player.getPlayWhenReady() && controllerVisibleFully)
            playerView.hideController();
    };
    // Adaptive sizing source of truth (phone/tablet/TV). Computed in onCreate, recomputed on config change.
    private UiMetrics ui;
    private ImageButton buttonPiP;
    private ImageButton buttonAspectRatio;
    // Forced display aspect ratio currently applied (0 = natural video AR). Persisted via Prefs.aspectRatio.
    private float currentAspectRatio = 0f;
    private List<AspectMode> aspectModes;
    private ImageButton buttonRotation;
    private ImageButton buttonLock;
    // Swipe-to-unlock bar shown over the video while the screen is locked; the only affordance for leaving
    // the locked state (drag on touch, hold a D-pad key on TV).
    private SwipeToUnlockView swipeToUnlock;
    // Back-button guard while locked: the first Back arms this, a second Back within the window exits.
    private boolean lockBackPressedOnce;
    private ObjectAnimator emptyStatePulse;
    private ImageButton exoSettings;
    private ImageButton exoSubtitle;
    private ImageButton exoPlayPause;
    private ImageButton exoPrev;
    private ImageButton exoNext;
    private boolean episodeNavLoading;
    private ProgressBar loadingProgressBar;
    private PlayerControlView controlView;
    private CustomDefaultTimeBar timeBar;

    private boolean restoreOrientationLock;
    private boolean restorePlayState;
    private boolean restorePlayStateAllowed;
    private boolean play;
    private float subtitlesScale;
    private boolean isScrubbing;
    private boolean scrubbingNoticeable;
    private long scrubbingStart;
    public boolean frameRendered;
    private boolean alive;
    public static boolean focusPlay = false;
    private Uri nextUri;
    private static boolean isTvBox;
    public static boolean locked = false;
    private Thread nextUriThread;
    private Thread segmentFinderThread;
    public Thread frameRateSwitchThread;

    public static boolean restoreControllerTimeout = false;
    public static boolean shortControllerTimeout = false;

    final Rational rationalLimitWide = new Rational(239, 100);
    final Rational rationalLimitTall = new Rational(100, 239);

    static final String API_POSITION = "position";
    static final String API_DURATION = "duration";
    static final String API_RETURN_RESULT = "return_result";
    static final String API_SUBS = "subs";
    static final String API_SUBS_ENABLE = "subs.enable";
    static final String API_SUBS_NAME = "subs.name";
    static final String API_TITLE = "title";
    static final String API_THUMBNAIL = "thumbnail";
    static final String API_SEGMENTS = "segments";
    static final String API_HEADERS = "headers";
    static final String API_VIDEO_LIST = "video_list";
    static final String API_VIDEO_LIST_NAME = "video_list.name";
    static final String API_VIDEO_LIST_FILENAME = "video_list.filename";
    static final String API_VIDEO_LIST_THUMBNAIL = "video_list.thumbnail";
    static final String API_VIDEO_LIST_SEGMENTS = "video_list.segments";
    static final String API_VIDEO_LIST_SEASON = "video_list.season";
    static final String API_VIDEO_LIST_EPISODE = "video_list.episode";
    static final String API_VIDEO_LIST_IMDB_ID = "video_list.imdb_id";
    static final String API_VIDEO_LIST_ID = "video_list.id";
    static final String API_SEASON = "season";
    static final String API_EPISODE = "episode";
    static final String API_IMDB_ID = "imdb_id";
    static final String API_ID = "id";
    static final String API_END_BY = "end_by";
    // Manual video-quality contract (LAMPA -> player): parallel label/url arrays for the current item,
    // and per-episode variants keyed "<prefix>.$index" (mirrors the video_list.* pattern).
    static final String API_QUALITY_LEVELS = "quality_levels";
    static final String API_QUALITY_URLS = "quality_urls";
    static final String API_VIDEO_LIST_QUALITY_LEVELS = "video_list.quality_levels";
    static final String API_VIDEO_LIST_QUALITY_URLS = "video_list.quality_urls";
    boolean apiAccess;
    boolean apiAccessPartial;
    String apiTitle;
    Uri apiThumbnailUri;
    String apiSegments;
    String[] apiHeaders;
    final List<MediaItem> apiMediaItems = new ArrayList<>();
    final List<String> apiPlaylistSegments = new ArrayList<>();
    int apiPlaylistStartIndex;
    // Per-episode resume positions for non-persistent playlist sessions (in-session only). One slot per
    // playlist item aligned by index; null when there is no playlist. Kept off the player so it survives
    // an onStop/release rebuild. See onPositionDiscontinuity()/savePlayer().
    long[] apiPlaylistPositions;
    // Episode metadata received via the launch Intent (from LAMPA, com.justplus.player branch). Stored for
    // now; not consumed yet. apiSeason/apiEpisode are -1 when absent; the per-item lists hold null.
    int apiSeason = -1;
    int apiEpisode = -1;
    String apiImdbId;
    // TMDB id (LAMPA's "id" extra). Used as a fallback for online skip-segment lookup when no imdb id
    // is supplied; series vs movie is decided by whether season/episode are present.
    String apiTmdbId;
    final List<Integer> apiPlaylistSeasons = new ArrayList<>();
    final List<Integer> apiPlaylistEpisodes = new ArrayList<>();
    final List<String> apiPlaylistImdbIds = new ArrayList<>();
    final List<String> apiPlaylistTmdbIds = new ArrayList<>();
    // Manual quality selection (LAMPA quality-switching port). Per-episode label->url maps aligned by
    // index with apiMediaItems; apiSingleQuality holds the top-level map for a single (non-playlist) video.
    // Maps are empty when the sender supplied no quality variants.
    final List<LinkedHashMap<String, String>> apiPlaylistQuality = new ArrayList<>();
    LinkedHashMap<String, String> apiSingleQuality = new LinkedHashMap<>();
    // Current manual choice — in-session only, never persisted.
    int selectedVideoQualityMode = VideoQualityChoice.MODE_AUTO;
    TrackGroup selectedVideoTrackGroup;
    int selectedVideoTrackIndex = -1;
    // Sticky quality across auto-next: number of lines of the last chosen SOURCE label (0 = none).
    int stickyQualityLines;
    // Skip-segment timing offset (seconds) — in-session only, never persisted; applies to all
    // playlist items and is reset on a new media session (resetApiAccess).
    private double skipOffsetSec = 0;
    // True once any skip segment has appeared this session; keeps the offset button available
    // afterwards even on an item that itself has no segments.
    private boolean skipSeenThisSession;
    private static final double SKIP_OFFSET_MAX_SEC = 30;   // ± range of the offset slider
    private static final double SKIP_OFFSET_STEP_SEC = 0.25; // fine step (touch / ± buttons)
    // Set before a SOURCE-switch reinitialisation so the player keeps a paused state (initializePlayer
    // otherwise force-plays under apiAccess). Consumed once inside initializePlayer.
    boolean sourceSwitchKeepPaused;
    List<MediaItem.SubtitleConfiguration> apiSubs = new ArrayList<>();
    boolean intentReturnResult;
    boolean playbackFinished;

    DisplayManager displayManager;
    DisplayManager.DisplayListener displayListener;
    SubtitleFinder subtitleFinder;

    Runnable barsHider = () -> {
        if (playerView != null && !controllerVisible) {
            Utils.toggleSystemUi(PlayerActivity.this, playerView, false);
        }
    };

    static final long SKIP_POLL_INTERVAL_MS = 250;
    // Segment highlights (see CustomDefaultTimeBar): a near-opaque *_FILL band across the segment plus a
    // crisp boundary hairline in the lighter *_HIGHLIGHT colour. Three-colour timeline system — coral =
    // playback, cool steel = skip (complementary to the warm coral so it never merges over the played
    // track, and still legible over the dark unplayed track), amber = ad. High alpha keeps each band
    // reading the same over both the coral and the dark portions of the bar.
    static final int SKIP_HIGHLIGHT_COLOR = 0xFFEAF6FF;
    static final int SKIP_FILL_COLOR = 0xC77FB8D4;
    static final int AD_HIGHLIGHT_COLOR = 0xFFFFD27A;
    static final int AD_FILL_COLOR = 0xC7FFA000;
    SkipManager skipManager;
    boolean skipBuilt;
    Button buttonSkip;
    ClipDrawable skipButtonProgress;
    TextView notificationSkip;
    // Top-center pill shown while hold-to-speed (2x) is active. Non-clickable so it never intercepts the hold.
    TextView speedBoostIndicator;
    final Runnable skipNotificationHider = new Runnable() {
        @Override
        public void run() {
            hideSkipNotification();
        }
    };
    SkipSegment pendingSkip;
    // Confirm key whose ACTION_UP must be swallowed after triggering a Skip on its ACTION_DOWN (TV).
    private int skipKeyUpToConsume = 0;
    final Runnable skipRunnable = new Runnable() {
        @Override
        public void run() {
            skipTick();
            if (player != null && player.isPlaying()) {
                playerView.postDelayed(this, SKIP_POLL_INTERVAL_MS);
            }
        }
    };

    final Runnable endsAtRunnable = new Runnable() {
        @Override
        public void run() {
            updateEndsAt();
            if (controllerVisible) {
                playerView.postDelayed(this, 1000);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Rotate ASAP, before super/inflating to avoid glitches with activity launch animation
        mPrefs = new Prefs(this);
        Utils.setOrientation(this, mPrefs.orientation);

        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT == 28 && Build.MANUFACTURER.equalsIgnoreCase("xiaomi") &&
                (Build.DEVICE.equalsIgnoreCase("oneday") || Build.DEVICE.equalsIgnoreCase("once"))) {
            setContentView(R.layout.activity_player_textureview);
        } else {
            setContentView(R.layout.activity_player);
        }

        if (Build.VERSION.SDK_INT >= 31) {
            Window window = getWindow();
            if (window != null) {
                window.setDecorFitsSystemWindows(false);
                WindowInsetsController windowInsetsController = window.getInsetsController();
                if (windowInsetsController != null) {
                    // On Android 12 BEHAVIOR_DEFAULT allows system gestures without visible system bars
                    windowInsetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
                }
            }
        }

        isTvBox = Utils.isTvBox(this);
        ui = UiMetrics.of(this, isTvBox);

        if (isTvBox) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        final Intent launchIntent = getIntent();
        final String action = launchIntent.getAction();
        final String type = launchIntent.getType();

        if ("com.brouken.player.action.SHORTCUT_VIDEOS".equals(action)) {
            openFile(Utils.getMoviesFolderUri());
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String text = launchIntent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                final Uri parsedUri = Uri.parse(text);
                if (parsedUri.isAbsolute()) {
                    mPrefs.updateMedia(this, parsedUri, null);
                    focusPlay = true;
                }
            }
        } else if (launchIntent.getData() != null) {
            resetApiAccess();
            final Uri uri = launchIntent.getData();
            if (SubtitleUtils.isSubtitle(uri, type)) {
                handleSubtitles(uri);
            } else {
                Bundle bundle = launchIntent.getExtras();
                if (bundle != null) {
                    apiAccess = bundle.containsKey(API_POSITION) || bundle.containsKey(API_RETURN_RESULT)
                            || bundle.containsKey(API_SUBS) || bundle.containsKey(API_SUBS_ENABLE)
                            || bundle.containsKey(API_VIDEO_LIST) || bundle.containsKey(API_QUALITY_LEVELS);
                    if (apiAccess) {
                        mPrefs.setPersistent(false);
                    } else if (bundle.containsKey(API_TITLE)) {
                        apiAccessPartial = true;
                    }
                    apiTitle = bundle.getString(API_TITLE);
                    final String thumbnail = bundle.getString(API_THUMBNAIL);
                    if (thumbnail != null) {
                        apiThumbnailUri = Uri.parse(thumbnail);
                    }
                    apiSegments = bundle.getString(API_SEGMENTS);
                    apiHeaders = bundle.getStringArray(API_HEADERS);
                    apiSeason = bundle.getInt(API_SEASON, -1);
                    apiEpisode = bundle.getInt(API_EPISODE, -1);
                    apiImdbId = bundle.getString(API_IMDB_ID);
                    apiTmdbId = getStringOrIntExtra(bundle, API_ID);
                    // Quality variants for a single (non-playlist) video; playlists carry per-episode maps.
                    apiSingleQuality = readQualityMap(bundle, API_QUALITY_LEVELS, API_QUALITY_URLS);
                    if (bundle.containsKey(API_VIDEO_LIST)) {
                        parseApiPlaylist(bundle, uri);
                    }
                }

                mPrefs.updateMedia(this, uri, type);

                if (bundle != null) {
                    Uri defaultSub = null;
                    Parcelable[] subsEnable = bundle.getParcelableArray(API_SUBS_ENABLE);
                    if (subsEnable != null && subsEnable.length > 0) {
                        defaultSub = (Uri) subsEnable[0];
                    }

                    Parcelable[] subs = bundle.getParcelableArray(API_SUBS);
                    String[] subsName = bundle.getStringArray(API_SUBS_NAME);
                    if (subs != null && subs.length > 0) {
                        for (int i = 0; i < subs.length; i++) {
                            Uri sub = (Uri) subs[i];
                            String name = null;
                            if (subsName != null && subsName.length > i) {
                                name = subsName[i];
                            }
                            apiSubs.add(SubtitleUtils.buildSubtitle(this, sub, name, sub.equals(defaultSub)));
                        }
                    }
                }

                if (apiSubs.isEmpty()) {
                    searchSubtitles();
                }

                if (bundle != null) {
                    intentReturnResult = bundle.getBoolean(API_RETURN_RESULT);

                    if (bundle.containsKey(API_POSITION)) {
                        mPrefs.updatePosition((long) bundle.getInt(API_POSITION));
                    }
                }
            }
            focusPlay = true;
        }

        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        playerView = findViewById(R.id.video_view);
        exoPlayPause = findViewById(R.id.exo_play_pause);
        // Coral hero: the central Play/Pause sits on a brand disc (inset from the large tap target) with a
        // white glyph. Doubles as a contrast anchor on bright frames, where a bare white glyph washes out.
        // Coral hero: the central Play/Pause sits on a brand disc (inset from the large tap target) with a
        // white glyph. Doubles as a contrast anchor on bright frames, where a bare white glyph washes out.
        final GradientDrawable playDisc = new GradientDrawable();
        playDisc.setShape(GradientDrawable.OVAL);
        playDisc.setColor(brandColor());
        exoPlayPause.setBackground(new InsetDrawable((Drawable) playDisc, ui.heroInset()));
        // Hero size scales per device class (phone = 90dp, unchanged; larger on tablet/TV). Overrides the
        // Media3 style's exo_icon_size so the transport isn't tiny on a 10-foot screen.
        final ViewGroup.LayoutParams heroLp = exoPlayPause.getLayoutParams();
        heroLp.width = ui.heroBox();
        heroLp.height = ui.heroBox();
        exoPlayPause.setLayoutParams(heroLp);
        // Clip to the oval disc outline so the borderless press/focus ripple is round, not the default square
        // (view-bounds) shape — matching the episode buttons.
        exoPlayPause.setClipToOutline(true);
        exoPlayPause.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        // Replacing the button background drops the D-pad focus / touch-press highlight, so re-add it as a
        // foreground ripple on top of the disc — critical for TV navigation, harmless on touch.
        final TypedValue playHighlight = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, playHighlight, true)
                && playHighlight.resourceId != 0) {
            exoPlayPause.setForeground(ContextCompat.getDrawable(this, playHighlight.resourceId));
        }
        loadingProgressBar = findViewById(R.id.loading);
        // Keep the loading ring proportional to the hero it overlays.
        final ViewGroup.LayoutParams spinnerLp = loadingProgressBar.getLayoutParams();
        spinnerLp.width = ui.spinnerSize();
        spinnerLp.height = ui.spinnerSize();
        loadingProgressBar.setLayoutParams(spinnerLp);
        exoPrev = findViewById(R.id.exo_prev);
        exoNext = findViewById(R.id.exo_next);
        setupEpisodeNavButtons();

        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        playerView.setRepeatToggleModes(Player.REPEAT_MODE_ONE);

        playerView.setControllerHideOnTouch(false);
        playerView.setControllerAutoShow(true);

        ((DoubleTapPlayerView)playerView).setDoubleTapEnabled(false);

        timeBar = playerView.findViewById(R.id.exo_progress);
        timeBar.addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                if (player == null) {
                    return;
                }
                restorePlayState = player.isPlaying();
                if (restorePlayState) {
                    player.pause();
                }
                scrubbingNoticeable = false;
                isScrubbing = true;
                frameRendered = true;
                playerView.setControllerShowTimeoutMs(-1);
                scrubbingStart = player.getCurrentPosition();
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
                reportScrubbing(position);
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                reportScrubbing(position);
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                playerView.setCustomErrorMessage(null);
                isScrubbing = false;
                if (restorePlayState) {
                    restorePlayState = false;
                    playerView.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
                    if (player != null) {
                        player.setPlayWhenReady(true);
                    }
                }
            }
        });

        buttonOpen = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonOpen.setImageResource(R.drawable.ic_folder_open_24dp);
        buttonOpen.setId(View.generateViewId());
        buttonOpen.setContentDescription(getString(R.string.button_open));

        buttonOpen.setOnClickListener(view -> openFile(mPrefs.mediaUri));

        buttonOpen.setOnLongClickListener(view -> {
            if (!isTvBox && mPrefs.askScope) {
                askForScope(true, false);
            } else {
                loadSubtitleFile(mPrefs.mediaUri);
            }
            return true;
        });

        buttonPlaylist = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonPlaylist.setImageResource(R.drawable.ic_playlist_24dp);
        buttonPlaylist.setId(View.generateViewId());
        buttonPlaylist.setContentDescription("Playlist");
        buttonPlaylist.setVisibility(View.GONE);
        buttonPlaylist.setOnClickListener(view -> showPlaylistDialog());

        buttonQuality = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonQuality.setImageResource(R.drawable.ic_high_quality_24dp);
        buttonQuality.setImageTintList(ContextCompat.getColorStateList(this, R.color.control_icon_tint));
        buttonQuality.setId(View.generateViewId());
        buttonQuality.setContentDescription(getString(R.string.button_quality));
        buttonQuality.setVisibility(View.GONE);
        buttonQuality.setOnClickListener(view -> showQualityDialog());

        buttonAudio = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonAudio.setImageResource(R.drawable.ic_audiotrack_24dp);
        buttonAudio.setId(View.generateViewId());
        buttonAudio.setContentDescription(getString(R.string.button_audio_track));
        buttonAudio.setVisibility(View.GONE);
        buttonAudio.setOnClickListener(view -> showAudioDialog());

        buttonMore = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonMore.setImageResource(R.drawable.ic_settings_24dp);
        buttonMore.setId(View.generateViewId());
        buttonMore.setContentDescription(getString(R.string.button_more));
        buttonMore.setOnClickListener(view -> showMoreMenu());
        buttonMore.setOnLongClickListener(view -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS);
            return true;
        });

        buttonSkipOffset = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonSkipOffset.setImageResource(R.drawable.ic_skip_offset_24dp);
        buttonSkipOffset.setId(View.generateViewId());
        buttonSkipOffset.setContentDescription(getString(R.string.button_skip_offset));
        buttonSkipOffset.setVisibility(View.GONE);
        buttonSkipOffset.setOnClickListener(view -> showSkipOffsetDialog());

        if (Utils.isPiPSupported(this)) {
            // TODO: Android 12 improvements:
            // https://developer.android.com/about/versions/12/features/pip-improvements
            mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
            boolean success = updatePictureInPictureActions(R.drawable.ic_play_arrow_24dp, R.string.exo_controls_play_description, CONTROL_TYPE_PLAY, REQUEST_PLAY);

            if (success) {
                buttonPiP = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
                buttonPiP.setContentDescription(getString(R.string.button_pip));
                buttonPiP.setImageResource(R.drawable.ic_picture_in_picture_alt_24dp);

                buttonPiP.setOnClickListener(view -> enterPiP());
            }
        }

        buttonAspectRatio = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonAspectRatio.setId(Integer.MAX_VALUE - 100);
        buttonAspectRatio.setContentDescription(getString(R.string.button_crop));
        updatebuttonAspectRatioIcon();
        buttonAspectRatio.setOnClickListener(view -> {
            cycleAspectMode();
            resetHideCallbacks();
        });
        if (isTvBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            buttonAspectRatio.setOnLongClickListener(v -> {
                scaleStart();
                updatebuttonAspectRatioIcon();
                return true;
            });
        } else {
            buttonAspectRatio.setOnLongClickListener(v -> {
                showAspectModePicker();
                return true;
            });
        }
        buttonRotation = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonRotation.setContentDescription(getString(R.string.button_rotate));
        updateButtonRotation();
        buttonRotation.setOnClickListener(view -> cycleOrientation());

        buttonLock = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        buttonLock.setImageResource(R.drawable.ic_lock_24dp);
        buttonLock.setImageTintList(ContextCompat.getColorStateList(this, R.color.control_icon_tint));
        buttonLock.setId(View.generateViewId());
        buttonLock.setContentDescription(getString(R.string.button_lock));
        buttonLock.setOnClickListener(view -> playerView.toggleLock());

        final int titleViewPaddingHorizontal = ui.gridH();
        final int titleViewPaddingVertical = getResources().getDimensionPixelOffset(R.dimen.exo_styled_bottom_bar_time_padding);
        FrameLayout centerView = playerView.findViewById(R.id.exo_controls_background);

        topInfoPanel = new LinearLayout(this);
        topInfoPanel.setOrientation(LinearLayout.HORIZONTAL);
        topInfoPanel.setGravity(Gravity.TOP);
        // Soft top scrim (dark → transparent) instead of a flat opaque band, so the video breathes under the header.
        topInfoPanel.setBackgroundResource(R.drawable.scrim_top);
        topInfoPanel.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        topInfoPanel.setPadding(titleViewPaddingHorizontal, titleViewPaddingVertical, titleViewPaddingHorizontal, titleViewPaddingVertical);
        topInfoPanel.setVisibility(View.GONE);

        posterSlot = new FrameLayout(this);
        // Poster anchors the left column and is sized to roughly match the right column's two rows (time +
        // icons) so neither side leaves a void. Bumped on TV for 10-foot legibility.
        final LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, ui.posterHeight());
        slotParams.setMarginEnd(ui.dpS(16));
        slotParams.gravity = Gravity.TOP;
        posterSlot.setLayoutParams(slotParams);
        posterSlot.setBackgroundColor(0xFF333333); // bg_placeholder_card
        final int posterCornerRadius = Utils.dpToPx(4);
        posterSlot.setClipToOutline(true);
        posterSlot.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), posterCornerRadius);
            }
        });
        posterSlot.setVisibility(View.GONE);

        posterView = new ImageView(this);
        posterView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));
        posterView.setAdjustViewBounds(true);
        posterView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        posterSlot.addView(posterView);

        posterPlaceholderView = new TextView(this);
        posterPlaceholderView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        posterPlaceholderView.setMinWidth(Utils.dpToPx(54));
        posterPlaceholderView.setGravity(Gravity.CENTER);
        posterPlaceholderView.setTextColor(0x80FFFFFF); // text_tertiary
        posterPlaceholderView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textPlaceholder());
        posterPlaceholderView.setTypeface(Typeface.DEFAULT_BOLD);
        posterPlaceholderView.setVisibility(View.GONE);
        posterSlot.addView(posterPlaceholderView);

        posterBadgeView = createPosterNumberBadge();
        posterSlot.addView(posterBadgeView);

        topInfoPanel.addView(posterSlot);

        // Left column of the header grid: title (row 1) over a single combined metadata line (row 2).
        final LinearLayout infoColumn = new LinearLayout(this);
        infoColumn.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams infoColumnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoColumnParams.gravity = Gravity.TOP;
        infoColumnParams.setMarginEnd(Utils.dpToPx(16));
        infoColumn.setLayoutParams(infoColumnParams);

        titleView = new TextView(this);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textTitle());
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        infoColumn.addView(titleView);

        // Two meta lines: video (resolution · codec · HDR) and the audio track (label / codec / language).
        videoInfoView = createInfoLine(Utils.dpToPx(2));
        infoColumn.addView(videoInfoView);
        audioInfoView = createInfoLine(0);
        infoColumn.addView(audioInfoView);

        topInfoPanel.addView(infoColumn);

        // Right block of the header, mirroring the left (poster + text column): a one-line time row on top, with
        // the display-icon pill right-aligned directly beneath it — so the pill's right edge lands on the same
        // grid line as the clock and the bottom-bar pill.
        final LinearLayout headerClockColumn = new LinearLayout(this);
        headerClockColumn.setOrientation(LinearLayout.VERTICAL);
        headerClockColumn.setGravity(Gravity.END);
        final LinearLayout.LayoutParams headerClockColumnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerClockColumnParams.gravity = Gravity.TOP;
        headerClockColumn.setLayoutParams(headerClockColumnParams);

        // Time row (row 1): "until … ·" then the clock on one line. The clock is the bold, right-pinned anchor,
        // so it never jumps sideways when the dynamically-computed end time appears/updates while loading.
        final LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams timeRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeRowLp.gravity = Gravity.END;
        timeRow.setLayoutParams(timeRowLp);

        endsAtView = new TextView(this);
        endsAtView.setTextColor(0xB3FFFFFF);
        endsAtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textClock());
        endsAtView.setVisibility(View.GONE);
        timeRow.addView(endsAtView);

        headerClock = new OutlineTextClock(this);
        headerClock.setFormat12Hour("h:mm a");
        headerClock.setFormat24Hour("HH:mm");
        // Dimmed white (matches the "until …" text); pure white read as too harsh. The black outline and
        // bold weight keep it legible and as the anchor without the glare.
        headerClock.setTextColor(0xB3FFFFFF);
        headerClock.setTypeface(Typeface.DEFAULT_BOLD);
        headerClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textClock());
        final LinearLayout.LayoutParams headerClockLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerClockLp.setMarginStart(Utils.dpToPx(6));
        headerClock.setLayoutParams(headerClockLp);
        timeRow.addView(headerClock);

        headerClockColumn.addView(timeRow);

        // Display-icon pill (row 2): aspect / PiP / rotation, right-aligned directly under the clock. No negative
        // margin — the pill right-aligns to the column edge, which matches the clock and the bottom-bar pill.
        // Populated in the controls assembly; empty on TV (those controls live in the bottom bar there).
        headerButtons = new LinearLayout(this);
        headerButtons.setOrientation(LinearLayout.HORIZONTAL);
        final LinearLayout.LayoutParams headerButtonsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerButtonsParams.gravity = Gravity.END;
        headerButtonsParams.topMargin = Utils.dpToPx(4);
        headerButtons.setLayoutParams(headerButtonsParams);
        headerClockColumn.addView(headerButtons);

        // Long-press the clock to copy the full launch intent to the clipboard, for diagnostics.
        headerClockColumn.setOnLongClickListener(view -> {
            copyLaunchIntentToClipboard();
            return true;
        });

        topInfoPanel.addView(headerClockColumn);

        centerView.addView(topInfoPanel);

        // Skip button — a solid dark pill floating over the video (bottom-end), independent of the
        // controller. Modern TV focus: a coral ring + slight scale-up on focus (replacing the dated flat
        // grey selectableItemBackground wash), with the remaining-time countdown drawn as a neutral white
        // underline integrated into the pill rather than a detached bar below it.
        final int skipCornerRadius = Utils.dpToPx(8);
        buttonSkip = new Button(this);
        buttonSkip.setText(R.string.button_skip);
        buttonSkip.setAllCaps(false);
        buttonSkip.setTextColor(Color.WHITE);
        buttonSkip.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textSkip());
        buttonSkip.setTypeface(Typeface.DEFAULT_BOLD);
        buttonSkip.setMinHeight(0);
        buttonSkip.setMinimumHeight(0);
        // Extra bottom padding leaves room for the integrated countdown underline below the label.
        buttonSkip.setPadding(Utils.dpToPx(14), Utils.dpToPx(7), Utils.dpToPx(16), Utils.dpToPx(10));

        final Drawable skipIcon = ContextCompat.getDrawable(this, R.drawable.exo_styled_controls_next);
        if (skipIcon != null) {
            final int skipIconSize = Utils.dpToPx(18);
            skipIcon.setBounds(0, 0, skipIconSize, skipIconSize);
            buttonSkip.setCompoundDrawablesRelative(skipIcon, null, null, null);
            buttonSkip.setCompoundDrawablePadding(Utils.dpToPx(6));
            buttonSkip.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE));
        }

        // Solid dark pill with the neutral white countdown underline baked into its background as inset layers.
        // (A separate MATCH_PARENT underline View resolves to the full screen width inside a wrap-content
        // FrameLayout, which stretched the whole floating unit across the screen — hence layers instead.)
        final int skipRingWidth = Utils.dpToPx(2);
        final int skipBarHeight = Utils.dpToPx(3);
        final int skipBarCorner = Utils.dpToPx(2);
        final GradientDrawable skipPillFill = new GradientDrawable();
        skipPillFill.setColor(Color.argb(0xF0, 0x16, 0x16, 0x16));
        skipPillFill.setCornerRadius(skipCornerRadius);
        final GradientDrawable skipBarTrack = new GradientDrawable();
        skipBarTrack.setColor(0x33FFFFFF); // faint white groove under the countdown fill
        skipBarTrack.setCornerRadius(skipBarCorner);
        final GradientDrawable skipBarFill = new GradientDrawable();
        skipBarFill.setColor(Color.WHITE); // neutral countdown, matches the pill's white label + icon
        skipBarFill.setCornerRadius(skipBarCorner);
        skipButtonProgress = new ClipDrawable(skipBarFill, Gravity.START, ClipDrawable.HORIZONTAL);
        skipButtonProgress.setLevel(0);
        final LayerDrawable skipPillBackground = new LayerDrawable(
                new Drawable[]{skipPillFill, skipBarTrack, skipButtonProgress});
        // Pin the underline (track + draining fill) to the pill's bottom edge, inset from the corners.
        for (int layer = 1; layer <= 2; layer++) {
            skipPillBackground.setLayerGravity(layer, Gravity.BOTTOM);
            skipPillBackground.setLayerHeight(layer, skipBarHeight);
            skipPillBackground.setLayerInsetBottom(layer, Utils.dpToPx(5));
            skipPillBackground.setLayerInsetLeft(layer, skipCornerRadius);
            skipPillBackground.setLayerInsetRight(layer, skipCornerRadius);
        }
        buttonSkip.setBackground(skipPillBackground);
        buttonSkip.setOnClickListener(v -> {
            // While the screen is locked the Skip button must never act, whether tapped or
            // activated via a TV remote's confirm key (which routes through performClick()).
            if (PlayerActivity.locked) {
                return;
            }
            if (pendingSkip != null && player != null) {
                final SkipSegment segment = pendingSkip;
                segment.skipped = true;
                hideSkipButton();
                skipSeekTo(segment);
            }
        });

        // Add the pill straight to the coordinator, floating bottom-end. No wrapper view: the countdown
        // underline is baked into the button's own background, so the previous wrapping FrameLayout — which
        // stretched to full width inside the CoordinatorLayout and pinned the pill to the left edge — is gone.
        // Modern TV focus: a coral ring on the pill (state-driven stroke) plus a slight scale-up, replacing
        // the dated flat grey selectableItemBackground wash.
        buttonSkip.setOnFocusChangeListener((v, hasFocus) -> {
            skipPillFill.setStroke(hasFocus ? skipRingWidth : 0, brandColor());
            final float scale = hasFocus ? 1.06f : 1f;
            buttonSkip.animate().scaleX(scale).scaleY(scale).setDuration(150).start();
        });
        final CoordinatorLayout.LayoutParams skipButtonParams = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skipButtonParams.gravity = Gravity.BOTTOM | Gravity.END;
        skipButtonParams.setMargins(0, 0, Utils.dpToPx(24), Utils.dpToPx(96));
        buttonSkip.setLayoutParams(skipButtonParams);
        buttonSkip.setVisibility(View.GONE);
        coordinatorLayout.addView(buttonSkip);

        // Hold-to-speed (2x) indicator: the same rounded dark pill as the auto-skip notification
        // (fast-forward icon + label), floating top-centre. Non-clickable so it never intercepts the hold.
        speedBoostIndicator = new TextView(this);
        speedBoostIndicator.setText("2×");
        speedBoostIndicator.setAllCaps(false);
        speedBoostIndicator.setTextColor(Color.WHITE);
        speedBoostIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textSkip());
        speedBoostIndicator.setTypeface(Typeface.DEFAULT_BOLD);
        speedBoostIndicator.setGravity(Gravity.CENTER_VERTICAL);
        speedBoostIndicator.setPadding(Utils.dpToPx(14), Utils.dpToPx(9), Utils.dpToPx(16), Utils.dpToPx(9));
        speedBoostIndicator.setClickable(false);
        speedBoostIndicator.setFocusable(false);

        final Drawable speedBoostIcon = ContextCompat.getDrawable(this, R.drawable.exo_icon_fastforward);
        if (speedBoostIcon != null) {
            final int speedBoostIconSize = Utils.dpToPx(18);
            speedBoostIcon.setBounds(0, 0, speedBoostIconSize, speedBoostIconSize);
            speedBoostIndicator.setCompoundDrawablesRelative(speedBoostIcon, null, null, null);
            speedBoostIndicator.setCompoundDrawablePadding(Utils.dpToPx(6));
            speedBoostIndicator.setCompoundDrawableTintList(ColorStateList.valueOf(brandColor()));
        }

        final GradientDrawable speedBoostBackground = new GradientDrawable();
        speedBoostBackground.setColor(Color.argb(0xF0, 0x16, 0x16, 0x16));
        speedBoostBackground.setCornerRadius(skipCornerRadius);
        speedBoostIndicator.setBackground(speedBoostBackground);

        final CoordinatorLayout.LayoutParams speedBoostParams = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        speedBoostParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        speedBoostParams.setMargins(0, Utils.dpToPx(28), 0, 0);
        speedBoostIndicator.setLayoutParams(speedBoostParams);
        speedBoostIndicator.setVisibility(View.GONE);
        coordinatorLayout.addView(speedBoostIndicator);

        // Toast-style notification shown after an automatic skip: the same solid dark pill as the Skip
        // button (bell icon + label, no progress underline), floating top-centre. Auto-hides after 5s or
        // on any interaction (see onUserInteraction).
        notificationSkip = new TextView(this);
        notificationSkip.setText(R.string.notification_skipped);
        notificationSkip.setAllCaps(false);
        notificationSkip.setTextColor(Color.WHITE);
        notificationSkip.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textSkip());
        notificationSkip.setTypeface(Typeface.DEFAULT_BOLD);
        notificationSkip.setGravity(Gravity.CENTER_VERTICAL);
        notificationSkip.setPadding(Utils.dpToPx(14), Utils.dpToPx(9), Utils.dpToPx(16), Utils.dpToPx(9));

        final Drawable skipDoneIcon = ContextCompat.getDrawable(this, R.drawable.ic_notifications_24dp);
        if (skipDoneIcon != null) {
            final int skipDoneIconSize = Utils.dpToPx(18);
            skipDoneIcon.setBounds(0, 0, skipDoneIconSize, skipDoneIconSize);
            notificationSkip.setCompoundDrawablesRelative(skipDoneIcon, null, null, null);
            notificationSkip.setCompoundDrawablePadding(Utils.dpToPx(6));
            notificationSkip.setCompoundDrawableTintList(ColorStateList.valueOf(brandColor()));
        }

        final GradientDrawable notificationBackground = new GradientDrawable();
        notificationBackground.setColor(Color.argb(0xF0, 0x16, 0x16, 0x16));
        notificationBackground.setCornerRadius(skipCornerRadius);
        notificationSkip.setBackground(notificationBackground);

        final CoordinatorLayout.LayoutParams notificationParams = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        notificationParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        notificationParams.setMargins(0, Utils.dpToPx(28), 0, 0);
        notificationSkip.setLayoutParams(notificationParams);
        notificationSkip.setVisibility(View.GONE);
        notificationSkip.setOnClickListener(v -> hideSkipNotification());
        coordinatorLayout.addView(notificationSkip);

        // Persistent clock over the video, shown only when the controls (and thus the in-header clock) are
        // hidden and the "show clock" preference is on. It is positioned to exactly mirror the in-header
        // clock (see syncOverlayClockPosition), so toggling the controls swaps between the two clocks in the
        // same spot with no jump.
        overlayClock = new OutlineTextClock(this);
        overlayClock.setFormat12Hour("h:mm a");
        overlayClock.setFormat24Hour("HH:mm");
        // Same dimmed white as the header clock — the black outline keeps it readable over bright frames.
        overlayClock.setTextColor(0xB3FFFFFF);
        overlayClock.setTypeface(Typeface.DEFAULT_BOLD);
        // Must match the header clock size (see below) so the two line up exactly when controls toggle.
        overlayClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textClock());
        final CoordinatorLayout.LayoutParams overlayClockLp = new CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        overlayClockLp.gravity = Gravity.TOP | Gravity.START;
        overlayClock.setLayoutParams(overlayClockLp);
        overlayClock.setVisibility(View.GONE);
        coordinatorLayout.addView(overlayClock);

        // Whenever the in-header clock is (re)laid out, mirror its position onto the floating clock.
        headerClock.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> syncOverlayClockPosition());

        // Swipe-to-unlock bar, shown while the screen is locked: the lock icon is dragged to the right edge
        // to unlock. The only way out of the locked state, and touch only (the lock feature is not offered on
        // TV). Centered near the bottom.
        if (!isTvBox) {
            swipeToUnlock = new SwipeToUnlockView(this);
            swipeToUnlock.setVisibility(View.GONE);
            final CoordinatorLayout.LayoutParams swipeLp = new CoordinatorLayout.LayoutParams(
                    Utils.dpToPx(260), Utils.dpToPx(48));
            swipeLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            swipeLp.bottomMargin = Utils.dpToPx(48);
            swipeToUnlock.setLayoutParams(swipeLp);
            swipeToUnlock.setOnUnlockListener(() -> playerView.toggleLock());
            swipeToUnlock.setOnStartTouchingListener(() -> {
                if (playerView != null) {
                    playerView.removeCallbacks(swipeHider);
                }
            });
            swipeToUnlock.setOnStopTouchingListener(this::rescheduleSwipeHide);
            coordinatorLayout.addView(swipeToUnlock);
        }

        topInfoPanel.setOnLongClickListener(view -> {
            // Prevent FileUriExposedException
            if (mPrefs.mediaUri != null && ContentResolver.SCHEME_FILE.equals(mPrefs.mediaUri.getScheme())) {
                return false;
            }

            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, mPrefs.mediaUri);
            if (mPrefs.mediaType == null)
                shareIntent.setType("video/*");
            else
                shareIntent.setType(mPrefs.mediaType);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // Start without intent chooser to allow any target to be set as default
            startActivity(shareIntent);

            return true;
        });

        if (Build.VERSION.SDK_INT >= 35) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        controlView = playerView.findViewById(R.id.exo_controller);
        controlView.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            if (windowInsets != null) {
                if (Build.VERSION.SDK_INT >= 31) {
                    boolean visibleBars = windowInsets.isVisible(WindowInsets.Type.statusBars());
                    if (visibleBars && !controllerVisible) {
                        playerView.postDelayed(barsHider, 2500);
                    } else {
                        playerView.removeCallbacks(barsHider);
                    }
                }

                int insetLeft = windowInsets.getSystemWindowInsetLeft();
                int insetRight = windowInsets.getSystemWindowInsetRight();

                // Balance the horizontal insets: offset BOTH sides by the larger of the two so the header and
                // bottom-bar content stay symmetric even when only one side carries the status bar or a display
                // cutout (in landscape that side would otherwise get a much bigger margin — the lopsided look).
                // Applied as padding with no margin, so the scrim backgrounds still span the full width.
                // On TV all system insets are 0, so synthesize overscan-safe insets here — every edge-anchored
                // element (header, bottom bar, seek bar, Skip pill) keys off these, so the whole content grid
                // moves inward as a unit and stays aligned. overscanH/V are 0 on phone/tablet (no visual change).
                final int overscanV = ui.overscanV();
                final int insetH = Math.max(Math.max(insetLeft, insetRight), ui.overscanH());
                int paddingLeft = insetH;
                int marginLeft = 0;
                int paddingRight = insetH;
                int marginRight = 0;

                int bottomBarPaddingBottom = 0;
                int progressBarMarginBottom = 0;

                // Don't use exo_top (the built-in top scrim): it is a sibling of exo_controls_background and Media3
                // animates it on a different schedule, so it appears before / lingers after the header. Instead the
                // header panel's own background is extended up over the status-bar area (see topInfoPanel below) —
                // being the header itself, it can never desync from it. Keep exo_top collapsed.
                findViewById(R.id.exo_top).getLayoutParams().height = 0;

                if (Build.VERSION.SDK_INT >= 35) {
                    final int left = windowInsets.getInsets(WindowInsets.Type.navigationBars()).left;
                    final int right = windowInsets.getInsets(WindowInsets.Type.navigationBars()).right;

                    final FrameLayout exoBottomBar = findViewById(R.id.exo_bottom_bar);
                    ViewGroup.LayoutParams params = exoBottomBar.getLayoutParams();
                    params.height = getResources().getDimensionPixelSize(R.dimen.exo_styled_bottom_bar_height) + windowInsets.getSystemWindowInsetBottom() + overscanV;
                    exoBottomBar.setLayoutParams(params);

                    findViewById(R.id.exo_left).getLayoutParams().width = left;
                    findViewById(R.id.exo_right).getLayoutParams().width = right;

                    bottomBarPaddingBottom = windowInsets.getSystemWindowInsetBottom() + overscanV;
                    progressBarMarginBottom = windowInsets.getSystemWindowInsetBottom() + overscanV;
                } else {
                    // No top padding: the header panel's background (below) covers the status-bar area instead.
                    view.setPadding(0, 0, 0, windowInsets.getSystemWindowInsetBottom() + overscanV);
                }

                // Extend the header's background up over the status-bar area (top margin -> 0, top inset moved into
                // the top padding). The content position is unchanged (padding pushes it down by the same amount the
                // margin used to), but the panel now paints the status-bar strip, in perfect sync with the header.
                Utils.setViewParams(topInfoPanel, paddingLeft + titleViewPaddingHorizontal, windowInsets.getSystemWindowInsetTop() + overscanV + Utils.dpToPx(4), paddingRight + titleViewPaddingHorizontal, titleViewPaddingVertical,
                        marginLeft, 0, marginRight, 0);


                Utils.setViewParams(findViewById(R.id.exo_bottom_bar), paddingLeft, 0, paddingRight, bottomBarPaddingBottom,
                        marginLeft, 0, marginRight, 0);

                Utils.setViewParams(findViewById(R.id.exo_progress), insetH, 0, insetH, 0,
                        0, 0, 0, getResources().getDimensionPixelSize(R.dimen.exo_styled_progress_margin_bottom) + progressBarMarginBottom);

                // Keep the Skip pill above the seek bar and clear of the nav-bar inset. It floats on the
                // full-screen coordinator (not the controller), so a fixed bottom offset overlapped the
                // progress bar on tablets — derive it from the seek bar's own geometry (its top sits at
                // insetBottom + progress margin + progress layout height above the screen bottom).
                if (buttonSkip != null) {
                    final CoordinatorLayout.LayoutParams skipLp = (CoordinatorLayout.LayoutParams) buttonSkip.getLayoutParams();
                    // Float a small, deliberate gap above the bottom control bar (progress + time/pills) — not
                    // flush against it, and not the huge gap the full progress touch-target height produced.
                    skipLp.bottomMargin = windowInsets.getSystemWindowInsetBottom() + overscanV
                            + getResources().getDimensionPixelSize(R.dimen.exo_styled_progress_margin_bottom)
                            + ui.dpS(24);
                    // Align the floating Skip button's right edge to the shared content grid (same as the pills
                    // and the progress bar), instead of a fixed 24dp + insetRight that overshoots in landscape.
                    skipLp.rightMargin = insetH + ui.gridH();
                    buttonSkip.setLayoutParams(skipLp);
                }

                Utils.setViewMargins(findViewById(R.id.exo_error_message), 0, windowInsets.getSystemWindowInsetTop() / 2, 0, getResources().getDimensionPixelSize(R.dimen.exo_error_message_margin_bottom) + windowInsets.getSystemWindowInsetBottom() / 2);

                windowInsets.consumeSystemWindowInsets();
            }
            return windowInsets;
        });
        timeBar.setAdMarkerColor(Color.argb(0x00, 0xFF, 0xFF, 0xFF));
        timeBar.setPlayedAdMarkerColor(Color.argb(0x98, 0xFF, 0xFF, 0xFF));
        // Brand the timeline: coral played portion + coral scrubber (the surfaces the user actually touches).
        timeBar.setPlayedColor(brandColor());
        timeBar.setScrubberColor(brandColor());

        try {
            trackNameProvider = new CustomDefaultTrackNameProvider(getResources());
            trackNameProvider.setTrackNames(resolvedTrackNames);
            final Field field = PlayerControlView.class.getDeclaredField("trackNameProvider");
            field.setAccessible(true);
            field.set(controlView, trackNameProvider);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        findViewById(R.id.delete).setOnClickListener(view -> askDeleteMedia());

        findViewById(R.id.next).setOnClickListener(view -> {
            if (!isTvBox && mPrefs.askScope) {
                askForScope(false, true);
            } else {
                skipToNext();
            }
        });

        exoPlayPause.setOnClickListener(view -> dispatchPlayPause());

        // Prevent double tap actions in controller
        findViewById(R.id.exo_bottom_bar).setOnTouchListener((v, event) -> true);
        //titleView.setOnTouchListener((v, event) -> true);

        playerListener = new PlayerListener();

        mBrightnessControl = new BrightnessControl(this);
        if (mPrefs.brightness >= 0) {
            mBrightnessControl.currentBrightnessLevel = mPrefs.brightness;
            mBrightnessControl.setScreenBrightness(mBrightnessControl.levelToBrightness(mBrightnessControl.currentBrightnessLevel));
        }
        playerView.setBrightnessControl(mBrightnessControl);

        final LinearLayout exoBasicControls = playerView.findViewById(R.id.exo_basic_controls);
        exoSubtitle = exoBasicControls.findViewById(R.id.exo_subtitle);
        exoBasicControls.removeView(exoSubtitle);
        // Managed like the audio/quality buttons: hidden until the media actually has subtitle tracks,
        // so it never shows greyed-out while loading. Re-asserted after Media3's own updates (see onEvents).
        exoSubtitle.setVisibility(View.GONE);
        exoSubtitle.setImageTintList(ContextCompat.getColorStateList(this, R.color.control_icon_tint));

        exoSettings = exoBasicControls.findViewById(R.id.exo_settings);
        exoBasicControls.removeView(exoSettings);
        final ImageButton exoRepeat = exoBasicControls.findViewById(R.id.exo_repeat_toggle);
        exoBasicControls.removeView(exoRepeat);
        //exoBasicControls.setVisibility(View.GONE);

        // Open our native subtitle panel instead of Media3's built-in track popup.
        exoSubtitle.setOnClickListener(v -> showSubtitleDialog());

        exoSubtitle.setOnLongClickListener(v -> {
            enableRotation();
            safelyStartActivityForResult(new Intent(Settings.ACTION_CAPTIONING_SETTINGS), REQUEST_SYSTEM_CAPTIONS);
            return true;
        });

        updateButtons(false);

        final HorizontalScrollView horizontalScrollView = (HorizontalScrollView) getLayoutInflater().inflate(R.layout.controls, null);
        final LinearLayout controls = horizontalScrollView.findViewById(R.id.controls);

        // Multimedia pickers (subtitle / audio / quality / playlist), each shown when relevant, live in the
        // bottom bar on every device.
        controls.addView(exoSubtitle);
        controls.addView(buttonAudio);
        controls.addView(buttonQuality);
        controls.addView(buttonPlaylist);
        if (mPrefs.repeatToggle) {
            controls.addView(exoRepeat);
        }

        // Display / screen controls: beside the header clock on touch; in the bottom bar on TV so the remote
        // keeps a single left/right focus zone.
        final LinearLayout displayParent = isTvBox ? controls : headerButtons;
        displayParent.addView(buttonAspectRatio);
        if (Utils.isPiPSupported(this) && buttonPiP != null) {
            displayParent.addView(buttonPiP);
        }
        if (!isTvBox) {
            displayParent.addView(buttonRotation);
        }
        // "More" (overflow) always lives at the end of the bottom bar.
        controls.addView(buttonMore);

        // One uniform button box across both clusters so the header pill and the bottom pill match in height,
        // size and inter-button gap.
        styleClusterButton(exoSubtitle);
        styleClusterButton(buttonAudio);
        styleClusterButton(buttonQuality);
        styleClusterButton(buttonPlaylist);
        if (mPrefs.repeatToggle) {
            styleClusterButton(exoRepeat);
        }
        styleClusterButton(buttonMore);
        styleClusterButton(buttonAspectRatio);
        if (buttonPiP != null) {
            styleClusterButton(buttonPiP);
        }
        if (!isTvBox) {
            styleClusterButton(buttonRotation);
            // Group the header display icons into a chrome pill so they read as one designed control, not loose glyphs.
            applyControlPill(headerButtons);
        }
        // Group the bottom-right pickers (subtitle / audio / HD / playlist / settings) into a matching pill.
        applyControlPill(controls);

        // Inset the bottom pill to the shared 14dp content grid so its right edge lines up with the header
        // pill / clock and stays inside the progress bar, instead of running to the screen edge.
        final LinearLayout.LayoutParams horizontalScrollViewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        horizontalScrollViewLp.gravity = Gravity.CENTER_VERTICAL;
        horizontalScrollViewLp.setMarginEnd(ui.gridH());
        exoBasicControls.addView(horizontalScrollView, horizontalScrollViewLp);

        // Lock sits isolated at the far-left of the bottom bar — prepended into the time row, away from the
        // display cluster (MX-style) so it is no longer adjacent to the rotation button. Touch only (the lock
        // feature is not offered on TV).
        if (!isTvBox) {
            final View exoTime = findViewById(R.id.exo_time);
            if (exoTime instanceof LinearLayout) {
                // Match the right-hand controls: same 40dp box + chrome pill, so the lock reads as part of the
                // same control language instead of a lone heavy glyph. The time text stays bare, to its right.
                styleClusterButton(buttonLock);
                final GradientDrawable lockPill = new GradientDrawable();
                lockPill.setColor(ContextCompat.getColor(this, R.color.ui_controls_background));
                lockPill.setCornerRadius(ui.pillCorner());
                buttonLock.setBackground(lockPill);
                buttonLock.setClipToOutline(true);
                final LinearLayout.LayoutParams lockLp = (LinearLayout.LayoutParams) buttonLock.getLayoutParams();
                lockLp.setMarginEnd(ui.lockMarginEnd());
                buttonLock.setLayoutParams(lockLp);
                ((LinearLayout) exoTime).addView(buttonLock, 0);

                // exo_basic_controls (the right-hand cluster, holding a full-width HorizontalScrollView) is
                // laid out after exo_time, so it sits on top of it and its empty left area swallows taps on
                // the lock (a scroll view consumes touches for its own drag detection). Bring exo_time to the
                // front so the lock wins its taps. The cluster's buttons sit to the right, clear of exo_time
                // (which is ~494px wide and non-clickable outside the lock), so they and scrolling still work.
                exoTime.bringToFront();
            }
        }

        if (Build.VERSION.SDK_INT > 23) {
            horizontalScrollView.setOnScrollChangeListener((view, i, i1, i2, i3) -> resetHideCallbacks());
        }

        playerView.setControllerVisibilityListener(new PlayerView.ControllerVisibilityListener() {
            @Override
            public void onVisibilityChanged(int visibility) {
                controllerVisible = visibility == View.VISIBLE;
                controllerVisibleFully = playerView.isControllerFullyVisible();

                if (controllerVisible) {
                    updateMediaInfo();
                    startEndsAtUpdates();
                    playerView.post(PlayerActivity.this::updateSubtitleButton);
                } else {
                    stopEndsAtUpdates();
                }
                updateOverlayClock();
                scheduleHideControllerOnPause();

                if (PlayerActivity.restoreControllerTimeout) {
                    restoreControllerTimeout = false;
                    if (player == null || !player.isPlaying()) {
                        playerView.setControllerShowTimeoutMs(-1);
                    } else {
                        playerView.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
                    }
                }

                // https://developer.android.com/training/system-ui/immersive
                // While a picker panel is open keep the nav/gesture bar visible (avoids OxygenOS's two-swipe
                // back-gesture guard) but keep the status bar hidden for a clean top — see applyPickerBars.
                if (pickerDialogOpen) {
                    applyPickerBars();
                } else {
                    Utils.toggleSystemUi(PlayerActivity.this, playerView, visibility == View.VISIBLE);
                }
                if (visibility == View.VISIBLE && !isEmptyStateVisible()) {
                    // Because when using dpad controls, focus resets to first item in bottom controls bar
                    findViewById(R.id.exo_play_pause).requestFocus();
                }

                if (controllerVisible && playerView.isControllerFullyVisible()) {
                    if (errorToShow != null) {
                        showError(errorToShow);
                        errorToShow = null;
                    }
                }
            }
        });

        youTubeOverlay = findViewById(R.id.youtube_overlay);
        youTubeOverlay.performListener(new YouTubeOverlay.PerformListener() {
            @Override
            public void onAnimationStart() {
                youTubeOverlay.setAlpha(1.0f);
                youTubeOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd() {
                youTubeOverlay.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                youTubeOverlay.setVisibility(View.GONE);
                                youTubeOverlay.setAlpha(1.0f);
                            }
                        });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (useMediaStore()) {
                Utils.scanMediaStorage(this);
            }
        }

        maybeCheckForUpdate(launchIntent);
    }

    // Silent, non-intrusive self-update check. Only the sideloaded universal build self-updates
    // (BuildConfig.ENABLE_UPDATE); the check runs only on an idle launch (no media intent), is
    // throttled to once a day, and the "update available" dialog is shown only while nothing is
    // playing — it must never cover a video the user just launched.
    private static final long UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;

    private void maybeCheckForUpdate(final Intent launchIntent) {
        if (!BuildConfig.ENABLE_UPDATE || !mPrefs.autoUpdate) {
            return;
        }
        final String action = launchIntent.getAction();
        final boolean launchedIdle = launchIntent.getData() == null
                && !Intent.ACTION_SEND.equals(action)
                && !"com.brouken.player.action.SHORTCUT_VIDEOS".equals(action);
        if (!launchedIdle) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - mPrefs.updateLastCheck < UPDATE_CHECK_INTERVAL_MS) {
            return;
        }
        mPrefs.setUpdateLastCheck(now);
        Updater.find(info -> runOnUiThread(() -> {
            if (isFinishing() || info == null) {
                return;
            }
            if (info.versionCode == mPrefs.updateSkippedVersionCode) {
                return;
            }
            if (haveMedia && player != null && player.isPlaying()) {
                return;
            }
            UpdateUi.showAvailableDialog(PlayerActivity.this, info,
                    () -> mPrefs.setUpdateSkippedVersionCode(info.versionCode));
        }));
    }

    @Override
    public void onStart() {
        super.onStart();
        alive = true;
        if (!(isTvBox && Build.VERSION.SDK_INT >= 31)) {
            updateSubtitleStyle(this);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            playerView.removeCallbacks(barsHider);
            Utils.toggleSystemUi(this, playerView, true);
        }
        initializePlayer();
        updateButtonRotation();
    }

    @Override
    public void onResume() {
        super.onResume();
        restorePlayStateAllowed = true;
        if (isTvBox && Build.VERSION.SDK_INT >= 31) {
            updateSubtitleStyle(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        alive = false;
        // Stop the empty-state pulse while backgrounded (it's an infinite animator); showEmptyState restarts
        // it cleanly on return. Avoids ticking the Choreographer with no media loaded and the activity stopped.
        stopEmptyStatePulse();
        if (Build.VERSION.SDK_INT >= 31) {
            playerView.removeCallbacks(barsHider);
        }
        playerView.setCustomErrorMessage(null);
        releasePlayer(false);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        // While locked, swallow the first Back (re-showing the unlock bar with a hint) and only exit if Back
        // is pressed again within the window — so a stray Back can't drop out of a locked video.
        if (locked && !lockBackPressedOnce) {
            lockBackPressedOnce = true;
            showSwipeToUnlock();
            Utils.showText(playerView, getString(R.string.press_back_again), 2000);
            if (playerView != null) {
                playerView.postDelayed(() -> lockBackPressedOnce = false, 2000);
            }
            return;
        }
        restorePlayStateAllowed = false;
        super.onBackPressed();
    }

    @Override
    public void finish() {
        if (intentReturnResult) {
            Intent intent = new Intent("com.mxtech.intent.result.VIEW");
            // Report which item finished so the launcher can attribute the position to the
            // correct playlist entry (and mark preceding ones watched), not just the launched one.
            if (player != null) {
                final MediaItem currentMediaItem = player.getCurrentMediaItem();
                if (currentMediaItem != null && currentMediaItem.localConfiguration != null) {
                    intent.setData(currentMediaItem.localConfiguration.uri);
                }
            }
            intent.putExtra(API_END_BY, playbackFinished ? "playback_completion" : "user");
            if (!playbackFinished) {
                if (player != null) {
                    long duration = player.getDuration();
                    if (duration != C.TIME_UNSET) {
                        intent.putExtra(API_DURATION, (int) player.getDuration());
                    }
                    if (player.isCurrentMediaItemSeekable()) {
                        if (mPrefs.persistentMode) {
                            intent.putExtra(API_POSITION, (int) mPrefs.nonPersitentPosition);
                        } else {
                            intent.putExtra(API_POSITION, (int) player.getCurrentPosition());
                        }
                    }
                }
            }
            setResult(Activity.RESULT_OK, intent);
        }

        super.finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            final String action = intent.getAction();
            final String type = intent.getType();
            final Uri uri = intent.getData();

            if (Intent.ACTION_VIEW.equals(action) && uri != null) {
                if (SubtitleUtils.isSubtitle(uri, type)) {
                    handleSubtitles(uri);
                } else {
                    mPrefs.updateMedia(this, uri, type);
                    searchSubtitles();
                }
                focusPlay = true;
                initializePlayer();
            } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (text != null) {
                    final Uri parsedUri = Uri.parse(text);
                    if (parsedUri.isAbsolute()) {
                        mPrefs.updateMedia(this, parsedUri, null);
                        focusPlay = true;
                        initializePlayer();
                    }
                }
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                if (player == null)
                    break;
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    player.pause();
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    player.play();
                } else if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                Utils.adjustVolume(this, mAudioManager, playerView, keyCode == KeyEvent.KEYCODE_VOLUME_UP, event.getRepeatCount() == 0, true);
                return true;
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                if (player == null)
                    break;
                if (!controllerVisibleFully) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (!controllerVisibleFully || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                    if (player == null)
                        break;
                    playerView.removeCallbacks(playerView.textClearRunnable);
                    long pos = player.getCurrentPosition();
                    if (playerView.keySeekStart == -1) {
                        playerView.keySeekStart = pos;
                    }
                    long seekTo = pos - 10_000;
                    if (seekTo < 0)
                        seekTo = 0;
                    player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                    player.seekTo(seekTo);
                    final String message = Utils.formatMilisSign(seekTo - playerView.keySeekStart) + "\n" + Utils.formatMilis(seekTo);
                    playerView.setCustomErrorMessage(message);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (!controllerVisibleFully || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                    if (player == null)
                        break;
                    playerView.removeCallbacks(playerView.textClearRunnable);
                    long pos = player.getCurrentPosition();
                    if (playerView.keySeekStart == -1) {
                        playerView.keySeekStart = pos;
                    }
                    long seekTo = pos + 10_000;
                    long seekMax = player.getDuration();
                    if (seekMax != C.TIME_UNSET && seekTo > seekMax)
                        seekTo = seekMax;
                    PlayerActivity.player.setSeekParameters(SeekParameters.NEXT_SYNC);
                    player.seekTo(seekTo);
                    final String message = Utils.formatMilisSign(seekTo - playerView.keySeekStart) + "\n" + Utils.formatMilis(seekTo);
                    playerView.setCustomErrorMessage(message);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (isTvBox) {
                    if (controllerVisible && player != null && player.isPlaying()) {
                        playerView.hideController();
                        return true;
                    } else {
                        onBackPressed();
                    }
                }
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                return super.onKeyDown(keyCode, event);
            default:
                if (!controllerVisibleFully) {
                    playerView.showController();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                playerView.postDelayed(playerView.textClearRunnable, CustomPlayerView.MESSAGE_TIMEOUT_KEY);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (!isScrubbing) {
                    playerView.postDelayed(playerView.textClearRunnable, 1000);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // While locked, swallow every key at the earliest point — before the view hierarchy,
        // onKeyDown, and the window's default (MediaSession-backed) volume handling — so hardware
        // volume/media/seek keys can't act. BACK is excluded so the normal lock-aware exit path
        // still works. Re-show the unlock hint on the first press, mirroring the touch tap().
        if (locked && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                showSwipeToUnlock();
            }
            return true;
        }

        if (isScaling) {
            final int keyCode = event.getKeyCode();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        scale(true);
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        scale(false);
                        break;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        break;
                    default:
                        if (isScaleStarting) {
                            isScaleStarting = false;
                        } else {
                            scaleEnd();
                        }
                }
            }
            return true;
        }

        // TV: while the floating Skip button is showing (controller hidden), OK/Enter must trigger the
        // skip. Keys are handled here (see below) rather than through view-focus dispatch, and the
        // button does not reliably hold focus when it appears, so key off its visibility rather than
        // focus. Route the confirm key straight to the button on ACTION_DOWN and swallow the paired
        // ACTION_UP — otherwise, once the skip hides the button and the controller appears, that
        // trailing key-up lands on the newly focused play/pause button and pauses playback.
        if (isTvBox && isSkipConfirmKey(event.getKeyCode())) {
            if (event.getAction() == KeyEvent.ACTION_DOWN
                    && !locked
                    && !controllerVisibleFully
                    && buttonSkip != null
                    && buttonSkip.getVisibility() == View.VISIBLE) {
                buttonSkip.performClick();
                skipKeyUpToConsume = event.getKeyCode();
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP
                    && skipKeyUpToConsume == event.getKeyCode()) {
                skipKeyUpToConsume = 0;
                return true;
            }
        }

        if (isTvBox && !controllerVisibleFully) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                onKeyDown(event.getKeyCode(), event);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                onKeyUp(event.getKeyCode(), event);
            }
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (locked) {
            return true;
        }
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:
                    final float value = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    Utils.adjustVolume(this, mAudioManager, playerView, value > 0.0f, Math.abs(value) > 1.0f, true);
                    return true;
            }
        } else if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {
            // TODO: This somehow works, but it would use better filtering
            float value = event.getAxisValue(MotionEvent.AXIS_RZ);
            for (int i = 0; i < event.getHistorySize(); i++) {
                float historical = event.getHistoricalAxisValue(MotionEvent.AXIS_RZ, i);
                if (Math.abs(historical) > value) {
                    value = historical;
                }
            }
            if (Math.abs(value) == 1.0f) {
                Utils.adjustVolume(this, mAudioManager, playerView, value < 0, true, true);
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        if (isInPictureInPictureMode) {
            // On Android TV it is required to hide controller in this PIP change callback
            playerView.hideController();
            setSubtitleTextSizePiP();
            playerView.setScale(1.f);
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null || !ACTION_MEDIA_CONTROL.equals(intent.getAction()) || player == null) {
                        return;
                    }

                    switch (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        case CONTROL_TYPE_PLAY:
                            player.play();
                            break;
                        case CONTROL_TYPE_PAUSE:
                            player.pause();
                            break;
                    }
                }
            };
            ContextCompat.registerReceiver(this, mReceiver, new IntentFilter(ACTION_MEDIA_CONTROL), ContextCompat.RECEIVER_EXPORTED);
        } else {
            setSubtitleTextSize();
            if (mPrefs.aspectRatio > 0) {
                playerView.applyAspectMode(mPrefs.resizeMode, mPrefs.aspectRatio);
            } else if (mPrefs.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView.setScale(mPrefs.scale);
            }
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
            playerView.setControllerAutoShow(true);
            if (player != null) {
                if (player.isPlaying())
                    Utils.toggleSystemUi(this, playerView, false);
                else
                    playerView.showController();
            }
        }
    }

    void resetApiAccess() {
        apiAccess = false;
        apiAccessPartial = false;
        apiTitle = null;
        apiThumbnailUri = null;
        apiSegments = null;
        apiHeaders = null;
        apiMediaItems.clear();
        apiPlaylistSegments.clear();
        apiPlaylistStartIndex = 0;
        apiPlaylistPositions = null;
        resolvedMediaTypes.clear();
        apiSeason = -1;
        apiEpisode = -1;
        apiImdbId = null;
        apiTmdbId = null;
        apiPlaylistSeasons.clear();
        apiPlaylistEpisodes.clear();
        apiPlaylistImdbIds.clear();
        apiPlaylistTmdbIds.clear();
        apiPlaylistQuality.clear();
        apiSingleQuality = new LinkedHashMap<>();
        selectedVideoQualityMode = VideoQualityChoice.MODE_AUTO;
        selectedVideoTrackGroup = null;
        selectedVideoTrackIndex = -1;
        stickyQualityLines = 0;
        apiSubs.clear();
        mPrefs.setPersistent(true);
        if (skipManager != null) {
            skipManager.clear();
        }
        skipBuilt = false;
        cancelSegmentFinder();
        hideSkipButton();
        // Skip offset is session-scoped: a new media session resets it and hides its control.
        skipOffsetSec = 0;
        skipSeenThisSession = false;
        if (skipOffsetDialog != null && skipOffsetDialog.isShowing()) {
            skipOffsetDialog.dismiss();
        }
        updateSkipOffsetButton();
        if (timeBar != null) {
            timeBar.clearSkipHighlights();
        }
    }

    // Skip segments (intro/ad) received via the launch Intent — see com.brouken.player.skip.

    private void setupSkipSource() {
        if (skipManager == null) {
            skipManager = new SkipManager();
        }
        skipBuilt = false;
        // Do not hide the auto-skip notification here: when a skip lands at the very end of an item, the
        // next item auto-advances through onMediaItemTransition -> setupSkipSource almost immediately, and
        // hiding here would cut the notification short. Its own 3s timer governs it, so it rides across the
        // transition ("carry-through") and disappears on schedule.
        final String json = currentSegmentsJson();
        skipManager.setSource(json != null && !json.isEmpty() ? new IntentSegmentsSource(json) : null);
        // Source (re)set → the manager holds no segments until rebuildSkip() runs against the new
        // duration. Drop any highlights from the previous item right now so switching episodes never
        // leaves stale timecodes on the bar; the new segments (intent or online) repaint on rebuild.
        if (timeBar != null) {
            timeBar.clearSkipHighlights();
        }
    }

    private String currentSegmentsJson() {
        if (player != null && !apiPlaylistSegments.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            if (index >= 0 && index < apiPlaylistSegments.size()) {
                return apiPlaylistSegments.get(index);
            }
            return null;
        }
        return apiSegments;
    }

    private void rebuildSkip() {
        if (skipManager == null) {
            return;
        }
        double durationSec = 0;
        if (player != null) {
            final long durationMs = player.getDuration();
            if (durationMs != C.TIME_UNSET && durationMs > 0) {
                durationSec = durationMs / 1000.0;
            }
        }
        skipManager.setOffsetSec(skipOffsetSec);
        skipManager.rebuild(durationSec);
        updateSkipHighlights();
        if (skipManager.hasSegments()) {
            skipSeenThisSession = true;
        }
        updateSkipOffsetButton();
    }

    /** The offset button is shown once any skip segment exists — now or earlier this session. */
    private void updateSkipOffsetButton() {
        if (buttonSkipOffset == null) {
            return;
        }
        final boolean show = mPrefs.skipEnabled && skipManager != null
                && (skipManager.hasSegments() || skipSeenThisSession);
        buttonSkipOffset.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private String formatSkipOffset(double sec) {
        if (Math.abs(sec) < 0.001) {
            return "0 s";
        }
        String s = String.format(java.util.Locale.US, "%+.2f", sec);
        if (s.indexOf('.') >= 0) { // trim trailing zeros: "+2.50" -> "+2.5", "+3.00" -> "+3"
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') {
                end--;
            }
            if (end > 0 && s.charAt(end - 1) == '.') {
                end--;
            }
            s = s.substring(0, end);
        }
        return s + " s";
    }

    /** Apply a new session skip offset and re-derive the segments (moves timeline highlights live). */
    private void applySkipOffset(double sec) {
        skipOffsetSec = sec;
        rebuildSkip();
    }

    // Session-only skip-offset panel: an end-docked translucent panel matching the quality/playlist
    // dialogs — a large centred value readout, a coral-tinted SeekBar (touch = fine 0.25s drag; TV =
    // D-pad 0.5s steps) flanked by borderless −/+ icon buttons, and a subtle Reset pill.
    private void showSkipOffsetDialog() {
        if (player == null) {
            return;
        }
        final int accent = brandColor();    // coral, matches the skip timeline highlight
        final int trackBg = 0x33FFFFFF;
        final int progressMax = (int) Math.round(2 * SKIP_OFFSET_MAX_SEC / SKIP_OFFSET_STEP_SEC);
        final int mid = progressMax / 2;

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        final TextView header = new TextView(this);
        header.setText(getString(R.string.skip_offset_title));
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textTitle());
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, Utils.dpToPx(14));
        root.addView(header);

        final View divider = new View(this);
        final LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(1));
        divider.setLayoutParams(dividerLp);
        divider.setBackgroundColor(0x1AFFFFFF);
        root.addView(divider);

        root.addView(makeVerticalSpacer());

        final TextView value = new TextView(this);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textValue());
        value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, 0, 0, Utils.dpToPx(20));
        root.addView(value);

        final android.widget.SeekBar seekBar = new android.widget.SeekBar(this);
        seekBar.setMax(progressMax);
        seekBar.setKeyProgressIncrement(2); // D-pad step = 0.5 s
        seekBar.setProgress((int) Math.round(skipOffsetSec / SKIP_OFFSET_STEP_SEC) + mid);
        seekBar.setFocusable(true);
        seekBar.setSplitTrack(false);
        seekBar.setProgressTintList(ColorStateList.valueOf(accent));
        seekBar.setThumbTintList(ColorStateList.valueOf(accent));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(trackBg));

        final ImageButton minus = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        minus.setImageResource(R.drawable.ic_remove_24dp);
        minus.setContentDescription("-");
        final ImageButton plus = new ImageButton(this, null, 0, R.style.ExoStyledControls_Button_Bottom);
        plus.setImageResource(R.drawable.ic_add_24dp);
        plus.setContentDescription("+");

        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        seekLp.leftMargin = Utils.dpToPx(6);
        seekLp.rightMargin = Utils.dpToPx(6);
        seekBar.setLayoutParams(seekLp);
        row.addView(minus);
        row.addView(seekBar);
        row.addView(plus);
        root.addView(row);

        root.addView(makeVerticalSpacer());

        final TextView reset = new TextView(this);
        reset.setText(getString(R.string.skip_offset_reset));
        reset.setTextColor(Color.WHITE);
        reset.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textAction());
        reset.setGravity(Gravity.CENTER);
        reset.setClickable(true);
        reset.setFocusable(true);
        reset.setPadding(Utils.dpToPx(28), Utils.dpToPx(11), Utils.dpToPx(28), Utils.dpToPx(11));
        final GradientDrawable resetContent = new GradientDrawable();
        resetContent.setCornerRadius(Utils.dpToPx(22));
        resetContent.setColor(0x1AFFFFFF);
        final GradientDrawable resetMask = new GradientDrawable();
        resetMask.setCornerRadius(Utils.dpToPx(22));
        resetMask.setColor(Color.WHITE);
        reset.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), resetContent, resetMask));
        final LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resetLp.gravity = Gravity.CENTER_HORIZONTAL;
        reset.setLayoutParams(resetLp);
        root.addView(reset);

        // Reflects the current value into the readout (coral when non-zero, white at rest).
        final Runnable render = () -> {
            value.setText(formatSkipOffset(skipOffsetSec));
            value.setTextColor(Math.abs(skipOffsetSec) < 0.001 ? Color.WHITE : accent);
        };
        render.run();

        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    applySkipOffset((progress - mid) * SKIP_OFFSET_STEP_SEC);
                    render.run();
                }
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar sb) {
            }
        });
        minus.setOnClickListener(v -> {
            final int p = Math.max(0, seekBar.getProgress() - 1);
            seekBar.setProgress(p);
            applySkipOffset((p - mid) * SKIP_OFFSET_STEP_SEC);
            render.run();
        });
        plus.setOnClickListener(v -> {
            final int p = Math.min(progressMax, seekBar.getProgress() + 1);
            seekBar.setProgress(p);
            applySkipOffset((p - mid) * SKIP_OFFSET_STEP_SEC);
            render.run();
        });
        reset.setOnClickListener(v -> {
            seekBar.setProgress(mid);
            applySkipOffset(0);
            render.run();
        });

        int padTop = 0;
        int padBottom = 0;
        final WindowInsets rootInsets = coordinatorLayout.getRootWindowInsets();
        if (rootInsets != null) {
            // Status bar is hidden while a picker is open (applyPickerBars), so its height is only breathing
            // room. In portrait the status-bar height reads well; landscape is much shorter (and its status-bar
            // inset can include the camera cutout), where that same height looks oversized — use a compact
            // fixed inset there. Pad the bottom for the nav/gesture bar. dp keeps it density/resolution-adaptive.
            final boolean landscape = getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            final int landscapeTop = ui.pickerTopPadLand();
            if (Build.VERSION.SDK_INT >= 30) {
                padTop = landscape ? landscapeTop : rootInsets.getInsets(WindowInsets.Type.statusBars()).top;
                padBottom = rootInsets.getInsets(WindowInsets.Type.navigationBars()).bottom + ui.overscanV();
            } else {
                padTop = landscape ? landscapeTop : rootInsets.getSystemWindowInsetTop();
                padBottom = rootInsets.getSystemWindowInsetBottom() + ui.overscanV();
            }
        }
        final int hPad = Utils.dpToPx(24) + ui.overscanH();
        root.setPadding(hPad, padTop + Utils.dpToPx(20), hPad, padBottom + Utils.dpToPx(24));

        if (skipOffsetDialog != null) {
            skipOffsetDialog.dismiss();
        }
        skipOffsetDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        skipOffsetDialog.setContentView(root);
        skipOffsetDialog.setCanceledOnTouchOutside(true);
        final Window window = skipOffsetDialog.getWindow();
        if (window != null) {
            // Deliberately NOT fullscreen/edge-to-edge: a fullscreen dialog window makes OxygenOS treat the
            // panel as immersive and apply its two-swipe back-gesture guard. A plain window closes on one back.
            window.setLayout(ui.pickerWidthPx(getResources().getConfiguration()), ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.END);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xF0141414));
        }
        showPickerDialog(skipOffsetDialog);
        seekBar.post(seekBar::requestFocus);
    }

    private View makeVerticalSpacer() {
        final View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return spacer;
    }

    // Online skip-segment lookup (FIND_INTO.MD): when the current item has no intent-provided segments,
    // fetch them by imdb/season/episode and feed the result through the same SkipManager path.

    private void maybeFetchSegmentsOnline() {
        if (player == null || skipManager == null) {
            return;
        }
        if (!mPrefs.skipEnabled || !mPrefs.skipFetchOnline || skipManager.hasSegments()) {
            return;
        }
        final String imdbId = currentImdbId();
        final String tmdbId = currentTmdbId();
        if ((imdbId == null || imdbId.isEmpty()) && (tmdbId == null || tmdbId.isEmpty())) {
            return;
        }
        final long durationMs = player.getDuration();
        final double durationSec = (durationMs != C.TIME_UNSET && durationMs > 0) ? durationMs / 1000.0 : 0;
        final int targetIndex = player.getCurrentMediaItemIndex();

        cancelSegmentFinder();
        segmentFinderThread = SegmentFinder.find(imdbId, tmdbId, currentSeason(), currentEpisode(), durationSec,
                segments -> runOnUiThread(() -> onSegmentsFetched(targetIndex, segments)));
    }

    private void onSegmentsFetched(int targetIndex, java.util.List<SkipSegment> segments) {
        if (player == null || skipManager == null || segments == null || segments.isEmpty()) {
            return;
        }
        // Ignore if the media item changed since the fetch started, or intent segments have appeared.
        if (player.getCurrentMediaItemIndex() != targetIndex || skipManager.hasSegments()) {
            return;
        }
        skipManager.setSource(new NetworkSegmentsSource(segments));
        rebuildSkip();
    }

    private void cancelSegmentFinder() {
        if (segmentFinderThread != null) {
            segmentFinderThread.interrupt();
            segmentFinderThread = null;
        }
    }

    private String currentImdbId() {
        if (player != null && !apiPlaylistImdbIds.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            return index >= 0 && index < apiPlaylistImdbIds.size() ? apiPlaylistImdbIds.get(index) : null;
        }
        return apiImdbId;
    }

    private String currentTmdbId() {
        if (player != null && !apiPlaylistTmdbIds.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            return index >= 0 && index < apiPlaylistTmdbIds.size() ? apiPlaylistTmdbIds.get(index) : null;
        }
        return apiTmdbId;
    }

    private int currentSeason() {
        if (player != null && !apiPlaylistSeasons.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            if (index >= 0 && index < apiPlaylistSeasons.size()) {
                final Integer season = apiPlaylistSeasons.get(index);
                return season != null ? season : -1;
            }
            return -1;
        }
        return apiSeason;
    }

    private int currentEpisode() {
        if (player != null && !apiPlaylistEpisodes.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            if (index >= 0 && index < apiPlaylistEpisodes.size()) {
                final Integer episode = apiPlaylistEpisodes.get(index);
                return episode != null ? episode : -1;
            }
            return -1;
        }
        return apiEpisode;
    }

    /** Brand accent color from {@code @color/brand}; pass an alpha (0x00..0xFF) for a translucent variant. */
    private int brandColor() {
        return ContextCompat.getColor(this, R.color.brand);
    }

    private int brandColor(int alpha) {
        return (alpha << 24) | (brandColor() & 0x00FFFFFF);
    }

    /** Brand accent at reduced brightness (same hue/saturation) for large selected-row fills, where full
     *  brand is too intense on the eyes. Scaling RGB proportionally lowers only the value, unlike alpha
     *  blending which desaturates the color against the dark panel. */
    private int brandColorDim() {
        final int c = brandColor();
        final float f = 0.72f; // tweakable: lower = darker
        return Color.rgb(Math.round(Color.red(c) * f),
                Math.round(Color.green(c) * f),
                Math.round(Color.blue(c) * f));
    }

    // Show a picker panel (audio/subtitle/playlist/quality/skip). OxygenOS/ColorOS applies a fullscreen
    // back-gesture guard (the "swipe again to go back" toast → two swipes) while the app is immersive, i.e.
    // system bars hidden. Our pickers call hideController(), which hides the bars, so keep the bars visible
    // while a panel is open and restore immersive on dismiss — this lets the back gesture close the panel in
    // one swipe (the reference lampaua build never goes immersive for its pickers).
    private void showPickerDialog(final android.app.Dialog dialog) {
        // Hide the controls for a clean panel. Keep the navigation/gesture bar visible (so OxygenOS doesn't
        // apply its fullscreen back-gesture guard) but hide the status bar (clean top, no strip over the
        // panel). Restore immersive when the panel dismisses.
        pickerDialogOpen = true;
        playerView.hideController();
        applyPickerBars();
        dialog.setOnDismissListener(d -> {
            pickerDialogOpen = false;
            Utils.toggleSystemUi(PlayerActivity.this, playerView, controllerVisibleFully);
        });
        dialog.show();
    }

    // Bar state while a picker panel is open: navigation/gesture bar shown (avoids OxygenOS's fullscreen
    // back-gesture guard, which keys on hidden nav gestures), status bar hidden (clean top edge).
    private void applyPickerBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            final WindowInsetsController c = getWindow() != null ? getWindow().getInsetsController() : null;
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars());
                c.show(WindowInsets.Type.navigationBars());
            }
        } else {
            playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /** Group a control cluster into a rounded, semi-transparent chrome pill (matches the Skip button chrome). */
    private void applyControlPill(ViewGroup cluster) {
        final GradientDrawable pill = new GradientDrawable();
        pill.setColor(ContextCompat.getColor(this, R.color.ui_controls_background));
        pill.setCornerRadius(ui.pillCorner());
        cluster.setBackground(pill);
        cluster.setClipToOutline(true);
        final int padH = ui.pillPadH();
        cluster.setPadding(padH, cluster.getPaddingTop(), padH, cluster.getPaddingBottom());
    }

    private void updateSkipHighlights() {
        if (timeBar == null) {
            return;
        }
        final java.util.List<SkipSegment> segments = skipManager != null ? skipManager.getSegments() : null;
        final long durationMs = player != null ? player.getDuration() : C.TIME_UNSET;
        if (segments == null || segments.isEmpty() || durationMs == C.TIME_UNSET || durationMs <= 0 || !mPrefs.skipEnabled) {
            timeBar.clearSkipHighlights();
            return;
        }
        final int count = segments.size();
        final long[] starts = new long[count];
        final long[] ends = new long[count];
        final int[] colors = new int[count];
        final int[] fillColors = new int[count];
        for (int i = 0; i < count; i++) {
            final SkipSegment segment = segments.get(i);
            final boolean ad = segment.type == SkipSegment.Type.AD;
            starts[i] = segment.startMs();
            ends[i] = segment.endMs();
            colors[i] = ad ? AD_HIGHLIGHT_COLOR : SKIP_HIGHLIGHT_COLOR;
            fillColors[i] = ad ? AD_FILL_COLOR : SKIP_FILL_COLOR;
        }
        timeBar.setSkipHighlights(starts, ends, colors, fillColors, durationMs);
    }

    private void skipTick() {
        if (player == null || skipManager == null || !mPrefs.skipEnabled) {
            hideSkipButton();
            return;
        }
        final double posSec = player.getCurrentPosition() / 1000.0;
        final SkipSegment segment = skipManager.activeSegment(posSec);
        if (segment == null) {
            hideSkipButton();
            return;
        }
        // Ad segments are always skipped silently. Skip segments follow a per-position preference:
        // end credits use skipModeCredits, everything else (intro/recap) uses skipMode.
        final boolean auto;
        if (segment.type == SkipSegment.Type.AD) {
            auto = true;
        } else if (segment.credits) {
            auto = Prefs.SKIP_MODE_AUTO.equals(mPrefs.skipModeCredits);
        } else {
            auto = Prefs.SKIP_MODE_AUTO.equals(mPrefs.skipMode);
        }
        if (auto) {
            segment.skipped = true;
            hideSkipButton();
            skipSeekTo(segment);
            showSkipNotification();
        } else {
            updateSkipButtonProgress(segment);
            showSkipButton(segment);
        }
    }

    /** Sizes the coral fill to the fraction of the segment still remaining (1 at start, 0 at the end). */
    private void updateSkipButtonProgress(SkipSegment segment) {
        if (skipButtonProgress == null || player == null || segment == null)
            return;
        final long totalMs = segment.endMs() - segment.startMs();
        if (totalMs <= 0) {
            skipButtonProgress.setLevel(0);
            return;
        }
        final long remainingMs = segment.endMs() - player.getCurrentPosition();
        final double fraction = Math.max(0, Math.min(1, remainingMs / (double) totalMs));
        skipButtonProgress.setLevel((int) Math.round(fraction * 10000));
    }

    private void skipSeekTo(SkipSegment segment) {
        if (player == null)
            return;
        // A segment reaching the file end maps its end to the duration, so skipping it lands on the
        // very last frame. seekTo(duration) parks there — playback stalls, paused, without advancing —
        // so it must move to the next episode like a natural end-of-media instead. Credits that stop
        // short of the end (a post-credits scene/teaser follows) and the last item, with no next
        // episode, fall through to an exact seek so that trailing content still plays.
        if (segment.reachesEnd && player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
            return;
        }
        // Exact seek so playback resumes precisely at the segment end, not at an earlier keyframe.
        player.setSeekParameters(SeekParameters.EXACT);
        player.seekTo(segment.endMs());
    }

    // OK/Enter-style keys that activate the focused Skip button on a TV remote / gamepad.
    private static boolean isSkipConfirmKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_START:
                return true;
            default:
                return false;
        }
    }

    // Called from CustomPlayerView.toggleLock() whenever the touch lock changes. When "hide skip
    // controls while locked" is on, drop the Skip button and auto-skip notification immediately;
    // showSkipButton/showSkipNotification keep them suppressed until unlock, after which the next
    // skip poll restores the button if a segment is still active.
    void onLockChanged() {
        if (locked) {
            lockScreen();
        } else {
            unlockScreen();
        }
        if (locked && mPrefs != null && mPrefs.skipHideWhenLocked) {
            hideSkipButton();
            hideSkipNotification();
        }
    }

    // Entering the lock: pin the current orientation (restored on unlock), arm the swipe bar and reset the
    // Back guard. The controller is already hidden by CustomPlayerView.toggleLock().
    private void lockScreen() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        lockBackPressedOnce = false;
        showSwipeToUnlock();
    }

    // Leaving the lock: hide the bar and restore the user's orientation preference.
    private void unlockScreen() {
        hideSwipeToUnlock();
        lockBackPressedOnce = false;
        if (mPrefs != null) {
            Utils.setOrientation(this, mPrefs.orientation);
        }
    }

    // Some paths flip `locked` directly (new media, playback stopped) without going through onLockChanged;
    // the lock does not persist, so undo its UI (bar + orientation pin) here too.
    private void clearLockUi() {
        hideSwipeToUnlock();
        lockBackPressedOnce = false;
        if (mPrefs != null) {
            Utils.setOrientation(this, mPrefs.orientation);
        }
    }

    private void showSkipButton(SkipSegment segment) {
        // When configured, keep the Skip button hidden while the screen is locked.
        if (locked && mPrefs.skipHideWhenLocked) {
            hideSkipButton();
            return;
        }
        pendingSkip = segment;
        if (buttonSkip != null && buttonSkip.getVisibility() != View.VISIBLE) {
            buttonSkip.setVisibility(View.VISIBLE);
            if (isTvBox) {
                buttonSkip.requestFocus();
            }
        }
    }

    private void hideSkipButton() {
        pendingSkip = null;
        if (buttonSkip != null) {
            if (isTvBox && buttonSkip.hasFocus() && playerView != null) {
                playerView.requestFocus();
            }
            buttonSkip.setVisibility(View.GONE);
        }
    }

    private void showSkipNotification() {
        if (notificationSkip == null) {
            return;
        }
        // When configured, suppress the auto-skip notification while the screen is locked.
        if (locked && mPrefs.skipHideWhenLocked) {
            return;
        }
        notificationSkip.setVisibility(View.VISIBLE);
        playerView.removeCallbacks(skipNotificationHider);
        playerView.postDelayed(skipNotificationHider, 3000);
    }

    private void hideSkipNotification() {
        if (notificationSkip == null) {
            return;
        }
        if (playerView != null) {
            playerView.removeCallbacks(skipNotificationHider);
        }
        if (notificationSkip.getVisibility() != View.GONE) {
            notificationSkip.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // Any touch or key dispatched to the player dismisses the auto-skip notification.
        hideSkipNotification();
    }

    private void startSkipPolling() {
        if (playerView == null) {
            return;
        }
        playerView.removeCallbacks(skipRunnable);
        playerView.post(skipRunnable);
    }

    private void stopSkipPolling() {
        if (playerView != null) {
            playerView.removeCallbacks(skipRunnable);
        }
    }

    private void parseApiPlaylist(Bundle bundle, Uri dataUri) {
        final Parcelable[] parcelableList = bundle.getParcelableArray(API_VIDEO_LIST);
        final String[] stringList = parcelableList == null ? getSmartStringArray(bundle, API_VIDEO_LIST) : null;
        final int size = parcelableList != null ? parcelableList.length
                : (stringList != null ? stringList.length : 0);
        if (size == 0) {
            return;
        }
        final String[] names = getSmartStringArray(bundle, API_VIDEO_LIST_NAME);
        final String[] filenames = getSmartStringArray(bundle, API_VIDEO_LIST_FILENAME);
        final String[] posters = getSmartStringArray(bundle, API_VIDEO_LIST_THUMBNAIL);
        final String[] segments = getSmartStringArray(bundle, API_VIDEO_LIST_SEGMENTS);
        final String[] seasons = getSmartStringArray(bundle, API_VIDEO_LIST_SEASON);
        final String[] episodes = getSmartStringArray(bundle, API_VIDEO_LIST_EPISODE);
        final String[] imdbIds = getSmartStringArray(bundle, API_VIDEO_LIST_IMDB_ID);
        final String[] tmdbIds = getSmartStringArray(bundle, API_VIDEO_LIST_ID);

        apiMediaItems.clear();
        apiPlaylistSegments.clear();
        apiPlaylistSeasons.clear();
        apiPlaylistEpisodes.clear();
        apiPlaylistImdbIds.clear();
        apiPlaylistTmdbIds.clear();
        apiPlaylistQuality.clear();
        apiPlaylistStartIndex = 0;

        for (int i = 0; i < size; i++) {
            Uri uri = null;
            if (parcelableList != null) {
                if (parcelableList[i] instanceof Uri) {
                    uri = (Uri) parcelableList[i];
                }
            } else if (stringList[i] != null) {
                uri = Uri.parse(stringList[i]);
            }
            if (uri == null) {
                continue;
            }

            String title = names != null && i < names.length ? names[i] : null;
            if (title == null || title.isEmpty()) {
                title = filenames != null && i < filenames.length ? filenames[i] : null;
            }
            if (title == null || title.isEmpty()) {
                title = uri.getLastPathSegment();
            }

            Uri poster = null;
            if (posters != null && i < posters.length && posters[i] != null && !posters[i].isEmpty()) {
                poster = Uri.parse(posters[i]);
            }

            final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title);
            if (poster != null) {
                metadataBuilder.setArtworkUri(poster);
            }

            if (dataUri != null && uri.equals(dataUri)) {
                apiPlaylistStartIndex = apiMediaItems.size();
            }
            apiMediaItems.add(new MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(metadataBuilder.build())
                    .build());
            // Keep segments aligned by index with apiMediaItems (null when absent)
            apiPlaylistSegments.add(segments != null && i < segments.length ? segments[i] : null);
            // Episode metadata, aligned by index (null when absent). Stored, not yet used.
            apiPlaylistSeasons.add(parseIntOrNull(seasons, i));
            apiPlaylistEpisodes.add(parseIntOrNull(episodes, i));
            apiPlaylistImdbIds.add(imdbIds != null && i < imdbIds.length
                    && imdbIds[i] != null && !imdbIds[i].isEmpty() ? imdbIds[i] : null);
            apiPlaylistTmdbIds.add(tmdbIds != null && i < tmdbIds.length
                    && tmdbIds[i] != null && !tmdbIds[i].isEmpty() ? tmdbIds[i] : null);
            // Per-episode quality variants, aligned by index (empty map when absent).
            apiPlaylistQuality.add(readQualityMap(bundle,
                    API_VIDEO_LIST_QUALITY_LEVELS + "." + i, API_VIDEO_LIST_QUALITY_URLS + "." + i));
        }

        // One resume slot per episode, unset until the episode has actually been played.
        apiPlaylistPositions = new long[apiMediaItems.size()];
        for (int i = 0; i < apiPlaylistPositions.length; i++) {
            apiPlaylistPositions[i] = C.TIME_UNSET;
        }
    }

    // Reads two parallel extras (labels + urls) into an insertion-ordered label->url map, sorted from
    // highest resolution to lowest and truncated to the shorter of the two arrays. Returns an empty map
    // when either side is missing, so callers never deal with null.
    private static LinkedHashMap<String, String> readQualityMap(Bundle bundle, String levelsKey, String urlsKey) {
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        final String[] levels = getSmartStringArray(bundle, levelsKey);
        final String[] urls = getSmartQualityUrls(bundle, urlsKey);
        if (levels == null || urls == null) {
            return map;
        }
        final int count = Math.min(levels.length, urls.length);
        final ArrayList<Integer> order = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            order.add(i);
        }
        Collections.sort(order, (a, b) -> Integer.compare(qualityNumber(levels[b]), qualityNumber(levels[a])));
        for (int i : order) {
            final String label = levels[i];
            final String url = urls[i];
            if (label != null && !label.trim().isEmpty() && url != null && !url.trim().isEmpty()) {
                map.put(label, url);
            }
        }
        return map;
    }

    // Quality URLs may arrive as a String[]/ArrayList<String> or, per the LAMPA contract, as a Uri[]
    // (Parcelable[]). Normalise all of these to a String[].
    private static String[] getSmartQualityUrls(Bundle bundle, String key) {
        final String[] strings = getSmartStringArray(bundle, key);
        if (strings != null) {
            return strings;
        }
        final Parcelable[] parcelables = bundle.getParcelableArray(key);
        if (parcelables != null) {
            final String[] result = new String[parcelables.length];
            for (int i = 0; i < parcelables.length; i++) {
                result[i] = parcelables[i] == null ? null : parcelables[i].toString();
            }
            return result;
        }
        return null;
    }

    // Reads an extra that senders may pass as either a String or a numeric (e.g. TMDB "id"), returning
    // its string form, or null when absent/empty.
    private static String getStringOrIntExtra(Bundle bundle, String key) {
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }
        final Object value = bundle.get(key);
        if (value == null) {
            return null;
        }
        final String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer parseIntOrNull(String[] array, int i) {
        if (array == null || i >= array.length || array[i] == null || array[i].isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(array[i].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String[] getSmartStringArray(Bundle bundle, String key) {
        final String[] array = bundle.getStringArray(key);
        if (array != null) {
            return array;
        }
        final ArrayList<String> list = bundle.getStringArrayList(key);
        if (list != null) {
            return list.toArray(new String[0]);
        }
        final CharSequence[] charSequences = bundle.getCharSequenceArray(key);
        if (charSequences != null) {
            final String[] result = new String[charSequences.length];
            for (int i = 0; i < charSequences.length; i++) {
                result[i] = charSequences[i] == null ? null : charSequences[i].toString();
            }
            return result;
        }
        return null;
    }

    void updateTopInfo() {
        if (player == null) {
            return;
        }
        final MediaItem item = player.getCurrentMediaItem();
        final MediaMetadata metadata = item != null ? item.mediaMetadata : null;

        CharSequence title = metadata != null ? metadata.title : null;
        if (title == null || title.length() == 0) {
            title = Utils.getFileName(this, mPrefs.mediaUri);
        }
        titleView.setText(title);

        final Uri artworkUri = metadata != null ? metadata.artworkUri : null;
        updatePoster(artworkUri, player.getCurrentMediaItemIndex(), player.getMediaItemCount());

        final boolean hasPlaylist = player.getMediaItemCount() > 1;
        if (buttonPlaylist != null) {
            buttonPlaylist.setVisibility(hasPlaylist ? View.VISIBLE : View.GONE);
        }
        updateQualityButton();
        updateAudioButton();
        // Show prev/next episode arrows (Media3 built-in, flanking play/pause) only for playlists
        playerView.setShowNextButton(hasPlaylist);
        playerView.setShowPreviousButton(hasPlaylist);

        topInfoPanel.setVisibility(View.VISIBLE);
        updateMediaInfo();
        updateEndsAt();
    }

    private TextView createInfoLine(int topMargin) {
        final TextView view = new TextView(this);
        view.setTextColor(0x99FFFFFF); // text_secondary
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textInfo());
        view.setMaxLines(1);
        view.setEllipsize(TextUtils.TruncateAt.END);
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        view.setLayoutParams(lp);
        view.setVisibility(View.GONE);
        return view;
    }

    void updateMediaInfo() {
        if (player == null) {
            return;
        }
        final Format video = player.getVideoFormat();
        setInfoLine(videoInfoView, buildVideoInfo(video));
        setInfoLine(audioInfoView, buildAudioInfo(getSelectedAudioFormat()));
    }

    private static void setInfoLine(TextView view, String text) {
        if (view == null) {
            return;
        }
        if (text != null && !text.isEmpty()) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private static String buildVideoInfo(Format video) {
        if (video == null) {
            return null;
        }
        final StringBuilder b = new StringBuilder();
        appendField(b, resolutionClass(video.width, video.height));
        appendField(b, codecName(video));
        appendField(b, hdrName(video.colorInfo));
        return b.toString();
    }

    // Coarse resolution label (4K / 1080p / …) instead of raw pixel dimensions, to keep the header tidy.
    private static String resolutionClass(int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        final int longSide = Math.max(width, height);
        final int shortSide = Math.min(width, height);
        if (longSide >= 3840 || shortSide >= 2160) return "4K";
        if (longSide >= 2560 || shortSide >= 1440) return "1440p";
        if (longSide >= 1920 || shortSide >= 1080) return "1080p";
        if (longSide >= 1280 || shortSide >= 720) return "720p";
        if (longSide >= 640 || shortSide >= 480) return "480p";
        return shortSide + "p";
    }

    private String buildAudioInfo(Format audio) {
        if (audio == null) {
            return null;
        }
        // Same shape as the track list: <label or container name or language> [<codec> <channels> <bitrate>k] (<lang>)
        final String language = languageDisplayName(audio.language);
        // Rich release label: Media3's Format.label first, then the name read from the container.
        String metaName = audio.label;
        if ((metaName == null || metaName.isEmpty()) && audio.id != null) {
            metaName = resolvedTrackNames.get(audio.id);
        }
        final String title = (metaName != null && !metaName.isEmpty()) ? metaName : language;
        final StringBuilder b = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            b.append(title);
        }
        final String tech = CustomDefaultTrackNameProvider.techInfo(audio);
        if (!tech.isEmpty()) {
            if (b.length() > 0) b.append(' ');
            b.append('[').append(tech).append(']');
        }
        // If we led with a rich name, still surface the language after it.
        if (metaName != null && !metaName.isEmpty() && language != null) {
            if (b.length() > 0) b.append(' ');
            b.append('(').append(language).append(')');
        }
        return b.toString();
    }

    /** Called on the UI thread once the container parser has recovered track names. */
    private void onContainerMetadata(java.util.List<TrackMetadata> tracks) {
        containerTracks.clear();
        containerTracks.addAll(tracks);
        resolveTrackNames();
        updateMediaInfo();
    }

    /**
     * Maps container track names onto the player's current tracks by {@code Format.id} (the tkhd
     * trackId / MKV TrackNumber), falling back to order within each type, and stores the result in
     * {@link #resolvedTrackNames} (shared live with {@link #trackNameProvider}).
     */
    private void resolveTrackNames() {
        resolvedTrackNames.clear();
        if (player == null || containerTracks.isEmpty()) {
            return;
        }
        resolveNamesForType(C.TRACK_TYPE_AUDIO, TrackMetadata.Type.AUDIO);
        resolveNamesForType(C.TRACK_TYPE_TEXT, TrackMetadata.Type.SUBTITLE);
    }

    private void resolveNamesForType(int trackType, TrackMetadata.Type metaType) {
        final java.util.List<TrackMetadata> ordered = new java.util.ArrayList<>();
        for (TrackMetadata t : containerTracks) {
            if (t.type == metaType) {
                ordered.add(t);
            }
        }
        java.util.Collections.sort(ordered, (a, b) -> Integer.compare(a.trackId, b.trackId));

        int counter = 0;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != trackType) {
                continue;
            }
            for (int i = 0; i < group.length; i++) {
                final Format format = group.getMediaTrackGroup().getFormat(i);
                String name = null;
                // 1. Match by trackId (Format.id == tkhd trackId / MKV TrackNumber).
                if (format.id != null) {
                    final Integer id = tryParseInt(format.id);
                    if (id != null) {
                        for (TrackMetadata t : containerTracks) {
                            if (t.trackId == id && t.name != null && !t.name.isEmpty()) {
                                name = t.name;
                                break;
                            }
                        }
                    }
                }
                // 2. Fall back to order within this type.
                if (name == null && counter < ordered.size()) {
                    final String byOrder = ordered.get(counter).name;
                    if (byOrder != null && !byOrder.isEmpty()) {
                        name = byOrder;
                    }
                }
                if (name != null && format.id != null) {
                    resolvedTrackNames.put(format.id, name);
                }
                counter++;
            }
        }
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String codecName(Format format) {
        final String codec = CustomDefaultTrackNameProvider.formatNameFromMime(format.sampleMimeType);
        return codec != null ? codec : CustomDefaultTrackNameProvider.formatNameFromMime(format.codecs);
    }

    private static void appendField(StringBuilder builder, String field) {
        if (field != null && !field.isEmpty()) {
            if (builder.length() > 0) builder.append(" · ");
            builder.append(field);
        }
    }

    private static String hdrName(ColorInfo colorInfo) {
        if (colorInfo == null) {
            return null;
        }
        switch (colorInfo.colorTransfer) {
            case C.COLOR_TRANSFER_ST2084:
                return "HDR10";
            case C.COLOR_TRANSFER_HLG:
                return "HLG";
            default:
                return null;
        }
    }

    private Format getSelectedAudioFormat() {
        if (player == null) {
            return null;
        }
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() == C.TRACK_TYPE_AUDIO && group.isSelected()) {
                for (int i = 0; i < group.length; i++) {
                    if (group.isTrackSelected(i)) {
                        return group.getMediaTrackGroup().getFormat(i);
                    }
                }
                return group.getMediaTrackGroup().getFormat(0);
            }
        }
        return null;
    }

    private static String languageDisplayName(final String language) {
        if (language == null || language.isEmpty() || "und".equals(language)) {
            return null;
        }
        try {
            final String name = new Locale(language).getDisplayLanguage();
            if (name != null && !name.isEmpty() && !name.equalsIgnoreCase(language)) {
                return name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
            }
        } catch (Exception ignored) {
        }
        return language;
    }

    void updateEndsAt() {
        if (player == null || endsAtView == null) {
            return;
        }
        final long duration = player.getDuration();
        if (!controllerVisible || duration == C.TIME_UNSET || duration <= 0) {
            endsAtView.setVisibility(View.GONE);
            return;
        }
        final long remaining = Math.max(0, duration - player.getCurrentPosition());
        float speed = player.getPlaybackParameters().speed;
        if (speed <= 0) {
            speed = 1f;
        }
        final long endMs = System.currentTimeMillis() + (long) (remaining / speed);
        final String time = DateFormat.getTimeFormat(this).format(new Date(endMs));
        endsAtView.setText(getString(R.string.time_ends_at_inline, time));
        endsAtView.setVisibility(View.VISIBLE);
    }

    // With "show clock" on, a single floating clock stays lit at all times, positioned over the header's
    // clock slot (which is kept laid-out but invisible via alpha). Toggling the controls therefore no longer
    // swaps between two clocks and makes it blink. With the preference off, only the in-header clock is used.
    void updateOverlayClock() {
        if (overlayClock == null) {
            return;
        }
        final boolean show = mPrefs.showClock;
        if (headerClock != null) {
            headerClock.setAlpha(show ? 0f : 1f);
        }
        if (show) {
            syncOverlayClockPosition();
        }
        overlayClock.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Mirror the floating clock onto the in-header clock's on-screen position so switching between the two
    // (as the controls hide/show) is seamless. Skips while the header clock isn't laid out, keeping the last
    // known position rather than snapping to the top-left corner.
    private void syncOverlayClockPosition() {
        if (overlayClock == null || headerClock == null || coordinatorLayout == null
                || headerClock.getWidth() == 0) {
            return;
        }
        final int[] clockLoc = new int[2];
        final int[] rootLoc = new int[2];
        headerClock.getLocationInWindow(clockLoc);
        coordinatorLayout.getLocationInWindow(rootLoc);
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) overlayClock.getLayoutParams();
        final int left = clockLoc[0] - rootLoc[0];
        final int top = clockLoc[1] - rootLoc[1];
        if (lp.leftMargin != left || lp.topMargin != top) {
            lp.leftMargin = left;
            lp.topMargin = top;
            overlayClock.setLayoutParams(lp);
        }
    }

    private final Runnable swipeHider = this::hideSwipeToUnlock;

    // Reveal the swipe-to-unlock bar and auto-hide it after the standard long-touch timeout. While the user
    // is actively dragging (onStartTouching) the auto-hide is cancelled and rescheduled on release.
    void showSwipeToUnlock() {
        if (swipeToUnlock == null) {
            return;
        }
        swipeToUnlock.setVisibility(View.VISIBLE);
        rescheduleSwipeHide();
    }

    private void rescheduleSwipeHide() {
        if (playerView != null) {
            playerView.removeCallbacks(swipeHider);
            playerView.postDelayed(swipeHider, CustomPlayerView.MESSAGE_TIMEOUT_LONG);
        }
    }

    void hideSwipeToUnlock() {
        if (swipeToUnlock == null) {
            return;
        }
        if (playerView != null) {
            playerView.removeCallbacks(swipeHider);
        }
        swipeToUnlock.setVisibility(View.GONE);
    }

    private void copyLaunchIntentToClipboard() {
        final String report = Utils.buildIntentReport(getIntent());
        final android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("intent", report));
            android.widget.Toast.makeText(this, R.string.intent_copied, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void startEndsAtUpdates() {
        if (playerView == null) {
            return;
        }
        playerView.removeCallbacks(endsAtRunnable);
        playerView.post(endsAtRunnable);
    }

    private void stopEndsAtUpdates() {
        if (playerView != null) {
            playerView.removeCallbacks(endsAtRunnable);
        }
        if (endsAtView != null) {
            endsAtView.setVisibility(View.GONE);
        }
    }

    // Small episode-number chip, inset from the poster's top-start corner so its rounded corners don't
    // clash with the poster's rounded clip. Reused by the header poster and the playlist rows.
    private TextView createPosterNumberBadge() {
        final TextView badge = new TextView(this);
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.setMargins(Utils.dpToPx(3), Utils.dpToPx(3), 0, 0);
        badge.setLayoutParams(lp);
        final GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC000000);
        bg.setCornerRadius(Utils.dpToPx(3));
        badge.setBackground(bg);
        badge.setGravity(Gravity.CENTER);
        badge.setMinWidth(Utils.dpToPx(18));
        badge.setPadding(Utils.dpToPx(5), 0, Utils.dpToPx(5), Utils.dpToPx(1));
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textBadge());
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setVisibility(View.GONE);
        return badge;
    }

    private void updatePoster(final Uri uri, final int index, final int count) {
        final boolean isPlaylist = count > 1;
        final String number = String.valueOf(index + 1);
        posterBadgeView.setText(number);
        posterPlaceholderView.setText(number);

        if (uri != null) {
            posterSlot.setVisibility(View.VISIBLE);
            posterView.setVisibility(View.VISIBLE);
            posterPlaceholderView.setVisibility(View.GONE);
            posterBadgeView.setVisibility(isPlaylist ? View.VISIBLE : View.GONE);
            Glide.with(this)
                    .load(uri)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            showPosterFallback(isPlaylist);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(posterView);
        } else {
            showPosterFallback(isPlaylist);
        }
    }

    private void showPosterFallback(final boolean isPlaylist) {
        if (isPlaylist) {
            posterSlot.setVisibility(View.VISIBLE);
            posterView.setVisibility(View.GONE);
            posterBadgeView.setVisibility(View.GONE);
            posterPlaceholderView.setVisibility(View.VISIBLE);
        } else {
            posterSlot.setVisibility(View.GONE);
        }
    }

    private void showPlaylistDialog() {
        if (player == null || player.getMediaItemCount() <= 1) {
            return;
        }
        final int count = player.getMediaItemCount();
        final int current = player.getCurrentMediaItemIndex();
        final int radius = Utils.dpToPx(4);
        final View[] currentRow = new View[1];

        final LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        final int listPad = Utils.dpToPx(10);
        listLayout.setPadding(listPad, listPad, listPad, listPad);

        final TextView header = new TextView(this);
        header.setText(getString(R.string.playlist));
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textTitle());
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10));
        listLayout.addView(header);

        final View divider = new View(this);
        final LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(1));
        dividerLp.bottomMargin = Utils.dpToPx(4);
        divider.setLayoutParams(dividerLp);
        divider.setBackgroundColor(0x1AFFFFFF);
        listLayout.addView(divider);

        for (int i = 0; i < count; i++) {
            final int index = i;
            final MediaItem item = player.getMediaItemAt(i);
            final MediaMetadata md = item.mediaMetadata;
            CharSequence title = md != null ? md.title : null;
            if (title == null || title.length() == 0) {
                title = "Video " + (i + 1);
            }
            final Uri artwork = md != null ? md.artworkUri : null;
            final boolean isCurrent = i == current;

            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dpToPx(8), Utils.dpToPx(7), Utils.dpToPx(10), Utils.dpToPx(7));
            row.setClickable(true);
            row.setFocusable(true);
            row.setMinimumHeight(ui.rowMinHeight());
            // Rounded row: subtle fill for the current item, plus a rounded ripple for touch/D-pad focus.
            final GradientDrawable rowContent = new GradientDrawable();
            rowContent.setCornerRadius(Utils.dpToPx(8));
            rowContent.setColor(isCurrent ? brandColorDim() : Color.TRANSPARENT);
            final GradientDrawable rowMask = new GradientDrawable();
            rowMask.setCornerRadius(Utils.dpToPx(8));
            rowMask.setColor(Color.WHITE);
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), rowContent, rowMask));
            if (isCurrent) {
                currentRow[0] = row;
            }

            final FrameLayout box = new FrameLayout(this);
            final LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, Utils.dpToPx(56));
            boxLp.setMarginEnd(Utils.dpToPx(12));
            boxLp.gravity = Gravity.CENTER_VERTICAL;
            box.setLayoutParams(boxLp);
            box.setMinimumWidth(Utils.dpToPx(40));
            box.setBackgroundColor(0xFF2A2A2A);
            box.setClipToOutline(true);
            box.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });

            final ImageView poster = new ImageView(this);
            poster.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT));
            poster.setAdjustViewBounds(true);
            poster.setScaleType(ImageView.ScaleType.FIT_CENTER);
            box.addView(poster);

            if (artwork != null) {
                Glide.with(this).load(artwork).into(poster);
                final TextView numberChip = createPosterNumberBadge();
                numberChip.setText(String.valueOf(i + 1));
                numberChip.setVisibility(View.VISIBLE);
                box.addView(numberChip);
            } else {
                poster.setVisibility(View.GONE);
                final TextView number = new TextView(this);
                number.setText(String.valueOf(i + 1));
                number.setTypeface(Typeface.DEFAULT_BOLD);
                number.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                number.setGravity(Gravity.CENTER);
                number.setMinWidth(Utils.dpToPx(40));
                number.setTextColor(0x99FFFFFF);
                number.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textListNumber());
                box.addView(number);
            }
            row.addView(box);

            final TextView titleText = new TextView(this);
            titleText.setText(title);
            titleText.setTextColor(isCurrent ? 0xFFFFFFFF : 0xFFDDDDDD);
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textList());
            titleText.setMaxLines(2);
            titleText.setEllipsize(TextUtils.TruncateAt.END);
            if (isCurrent) {
                titleText.setTypeface(Typeface.DEFAULT_BOLD);
            }
            final LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            titleLp.gravity = Gravity.CENTER_VERTICAL;
            titleText.setLayoutParams(titleLp);
            row.addView(titleText);

            row.setOnClickListener(v -> {
                if (player != null) {
                    player.seekToDefaultPosition(index);
                    player.setPlayWhenReady(true);
                }
                if (playlistDialog != null) {
                    playlistDialog.dismiss();
                }
            });

            listLayout.addView(row);
        }

        final android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(listLayout);
        // The dialog spans the full height behind the status/navigation bars, so pad the content clear of
        // them. Use a FIXED inset captured now (the status bar is visible while the controls — and thus this
        // dialog — are shown): a dynamic inset listener would drop to 0 when hideController() flips the
        // activity to immersive flags, making the list visibly "jump" up under the still-visible status bar.
        int padTop = 0;
        int padBottom = 0;
        final WindowInsets rootInsets = coordinatorLayout.getRootWindowInsets();
        if (rootInsets != null) {
            // Status bar is hidden while a picker is open (applyPickerBars), so its height is only breathing
            // room. In portrait the status-bar height reads well; landscape is much shorter (and its status-bar
            // inset can include the camera cutout), where that same height looks oversized — use a compact
            // fixed inset there. Pad the bottom for the nav/gesture bar. dp keeps it density/resolution-adaptive.
            final boolean landscape = getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            final int landscapeTop = ui.pickerTopPadLand();
            if (Build.VERSION.SDK_INT >= 30) {
                padTop = landscape ? landscapeTop : rootInsets.getInsets(WindowInsets.Type.statusBars()).top;
                padBottom = rootInsets.getInsets(WindowInsets.Type.navigationBars()).bottom + ui.overscanV();
            } else {
                padTop = landscape ? landscapeTop : rootInsets.getSystemWindowInsetTop();
                padBottom = rootInsets.getSystemWindowInsetBottom() + ui.overscanV();
            }
        }
        scrollView.setPadding(ui.overscanH(), padTop, ui.overscanH(), padBottom);

        if (playlistDialog != null) {
            playlistDialog.dismiss();
        }
        playlistDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        playlistDialog.setContentView(scrollView);
        playlistDialog.setCanceledOnTouchOutside(true);
        final Window window = playlistDialog.getWindow();
        if (window != null) {
            // Deliberately NOT fullscreen/edge-to-edge: a fullscreen dialog window makes OxygenOS treat the
            // panel as immersive and apply its two-swipe back-gesture guard. A plain window closes on one back.
            window.setLayout(ui.pickerWidthPx(getResources().getConfiguration()), ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.END);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xF0141414));
        }
        // Hide the player's overlay (header + bottom controls) so only the playlist panel is shown.
        showPickerDialog(playlistDialog);
        if (currentRow[0] != null) {
            currentRow[0].post(() -> currentRow[0].requestFocus());
        }
    }

    // ---- Manual video quality (LAMPA quality-switching port) --------------------------------------

    // Quality map for the item currently playing: per-episode for a playlist, or the single-video map.
    private LinkedHashMap<String, String> currentQualityMap() {
        if (player != null && !apiPlaylistQuality.isEmpty()) {
            final int index = player.getCurrentMediaItemIndex();
            if (index >= 0 && index < apiPlaylistQuality.size()) {
                return apiPlaylistQuality.get(index);
            }
        }
        return apiSingleQuality;
    }

    // URI of the item currently loaded in the player (or the persisted media URI as a fallback).
    private Uri currentPlayingUri() {
        if (player != null) {
            final MediaItem item = player.getCurrentMediaItem();
            if (item != null && item.localConfiguration != null) {
                return item.localConfiguration.uri;
            }
        }
        return mPrefs.mediaUri;
    }

    // Builds the list shown in the quality menu. Auto/Maximum and per-rendition entries are added ONLY
    // when the stream offers a real in-stream choice (>= 2 selectable video renditions); a single
    // progressive track degrades to a plain list of SOURCE (separate-URL) variants.
    private ArrayList<VideoQualityChoice> buildQualityChoices() {
        final ArrayList<VideoQualityChoice> choices = new ArrayList<>();
        if (player == null) {
            return choices;
        }

        final HashMap<Integer, VideoQualityChoice> renditions = new HashMap<>();
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int index = 0; index < group.length; index++) {
                if (!group.isTrackSupported(index)) continue;
                Format format = group.getTrackFormat(index);
                int longSide = Math.max(format.width, format.height);
                int shortSide = Math.min(format.width, format.height);
                if (longSide <= 0) continue;
                VideoQualityChoice previous = renditions.get(longSide);
                if (previous == null || format.bitrate > previous.bitrate) {
                    String codec = shortCodec(format.sampleMimeType);
                    String dimensions = shortSide > 0
                            ? longSide + " × " + shortSide : String.valueOf(longSide);
                    String details = codec == null ? dimensions : dimensions + "  •  " + codec;
                    String bitrate = format.bitrate > 0
                            ? getString(R.string.quality_bitrate, format.bitrate / 1_000_000f) : "";
                    renditions.put(longSide, VideoQualityChoice.track(
                            longSide + "p", details, bitrate,
                            group.getMediaTrackGroup(), index, format.bitrate));
                }
            }
        }
        if (renditions.size() >= 2) {
            choices.add(VideoQualityChoice.auto());
            choices.add(VideoQualityChoice.maximum());
            ArrayList<Integer> longSides = new ArrayList<>(renditions.keySet());
            Collections.sort(longSides, Collections.reverseOrder());
            for (Integer longSide : longSides) choices.add(renditions.get(longSide));
        }

        final LinkedHashMap<String, String> quality = currentQualityMap();
        if (quality != null) {
            for (Map.Entry<String, String> entry : quality.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    choices.add(VideoQualityChoice.source(entry.getKey(), entry.getValue()));
                }
            }
        }
        return choices;
    }

    // Shows the quality button whenever there is more than one thing to choose between.
    private void updateQualityButton() {
        if (buttonQuality == null) {
            return;
        }
        final boolean show = player != null && buildQualityChoices().size() >= 2;
        buttonQuality.setVisibility(show ? View.VISIBLE : View.GONE);
        // Light the HD icon coral when a specific quality is pinned (anything other than Auto).
        buttonQuality.setSelected(selectedVideoQualityMode != VideoQualityChoice.MODE_AUTO);
    }

    private String qualityChoiceTitle(VideoQualityChoice choice) {
        switch (choice.mode) {
            case VideoQualityChoice.MODE_AUTO:
                return getString(R.string.quality_auto);
            case VideoQualityChoice.MODE_MAXIMUM:
                return getString(R.string.quality_maximum);
            default:
                return choice.label;
        }
    }

    private String qualityChoiceSubtitle(VideoQualityChoice choice) {
        switch (choice.mode) {
            case VideoQualityChoice.MODE_AUTO:
                return getString(R.string.quality_auto_description);
            case VideoQualityChoice.MODE_MAXIMUM:
                return getString(R.string.quality_maximum_badge);
            default:
                return choice.details;
        }
    }

    // Quality menu in JAPP's native style (modelled on showPlaylistDialog): a full-height translucent
    // panel docked to the end edge, with the current choice ticked and reachable by remote.
    private void showQualityDialog() {
        if (player == null) {
            Toast.makeText(this, R.string.quality_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        final ArrayList<VideoQualityChoice> choices = buildQualityChoices();
        if (choices.size() < 2) {
            Toast.makeText(this, R.string.quality_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        final int selected = selectedQualityIndex(choices);
        final View[] currentRow = new View[1];

        final LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        final int listPad = Utils.dpToPx(10);
        listLayout.setPadding(listPad, listPad, listPad, listPad);

        final TextView header = new TextView(this);
        header.setText(getString(R.string.quality_title));
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textTitle());
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10));
        listLayout.addView(header);

        final View divider = new View(this);
        final LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(1));
        dividerLp.bottomMargin = Utils.dpToPx(4);
        divider.setLayoutParams(dividerLp);
        divider.setBackgroundColor(0x1AFFFFFF);
        listLayout.addView(divider);

        for (int i = 0; i < choices.size(); i++) {
            final VideoQualityChoice choice = choices.get(i);
            final boolean isCurrent = i == selected;

            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dpToPx(12), Utils.dpToPx(10), Utils.dpToPx(12), Utils.dpToPx(10));
            row.setClickable(true);
            row.setFocusable(true);
            row.setMinimumHeight(ui.rowMinHeight());
            final GradientDrawable rowContent = new GradientDrawable();
            rowContent.setCornerRadius(Utils.dpToPx(8));
            rowContent.setColor(isCurrent ? brandColorDim() : Color.TRANSPARENT);
            final GradientDrawable rowMask = new GradientDrawable();
            rowMask.setCornerRadius(Utils.dpToPx(8));
            rowMask.setColor(Color.WHITE);
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), rowContent, rowMask));
            if (isCurrent) {
                currentRow[0] = row;
            }

            final LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            final LinearLayout.LayoutParams blockLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            blockLp.gravity = Gravity.CENTER_VERTICAL;
            textBlock.setLayoutParams(blockLp);

            final TextView title = new TextView(this);
            title.setText(qualityChoiceTitle(choice));
            title.setTextColor(isCurrent ? 0xFFFFFFFF : 0xFFDDDDDD);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textBody());
            title.setSingleLine(true);
            if (isCurrent) {
                title.setTypeface(Typeface.DEFAULT_BOLD);
            }
            textBlock.addView(title);

            final String subtitle = qualityChoiceSubtitle(choice);
            if (subtitle != null && !subtitle.isEmpty()) {
                final TextView details = new TextView(this);
                details.setText(subtitle);
                details.setTextColor(0x99FFFFFF);
                details.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textCaption());
                details.setSingleLine(true);
                textBlock.addView(details);
            }
            row.addView(textBlock);

            if (choice.bitrateText != null && !choice.bitrateText.isEmpty()) {
                final TextView bitrate = new TextView(this);
                bitrate.setText(choice.bitrateText);
                bitrate.setTextColor(0x99FFFFFF);
                bitrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textCaption());
                bitrate.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                bitrate.setSingleLine(true);
                final LinearLayout.LayoutParams bitrateLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bitrateLp.setMarginEnd(Utils.dpToPx(10));
                bitrateLp.gravity = Gravity.CENTER_VERTICAL;
                bitrate.setLayoutParams(bitrateLp);
                row.addView(bitrate);
            }

            row.setOnClickListener(v -> {
                applyVideoQuality(choice);
                if (qualityDialog != null) {
                    qualityDialog.dismiss();
                }
            });
            listLayout.addView(row);
        }

        final android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(listLayout);
        int padTop = 0;
        int padBottom = 0;
        final WindowInsets rootInsets = coordinatorLayout.getRootWindowInsets();
        if (rootInsets != null) {
            // Status bar is hidden while a picker is open (applyPickerBars), so its height is only breathing
            // room. In portrait the status-bar height reads well; landscape is much shorter (and its status-bar
            // inset can include the camera cutout), where that same height looks oversized — use a compact
            // fixed inset there. Pad the bottom for the nav/gesture bar. dp keeps it density/resolution-adaptive.
            final boolean landscape = getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            final int landscapeTop = ui.pickerTopPadLand();
            if (Build.VERSION.SDK_INT >= 30) {
                padTop = landscape ? landscapeTop : rootInsets.getInsets(WindowInsets.Type.statusBars()).top;
                padBottom = rootInsets.getInsets(WindowInsets.Type.navigationBars()).bottom + ui.overscanV();
            } else {
                padTop = landscape ? landscapeTop : rootInsets.getSystemWindowInsetTop();
                padBottom = rootInsets.getSystemWindowInsetBottom() + ui.overscanV();
            }
        }
        scrollView.setPadding(ui.overscanH(), padTop, ui.overscanH(), padBottom);

        if (qualityDialog != null) {
            qualityDialog.dismiss();
        }
        qualityDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        qualityDialog.setContentView(scrollView);
        qualityDialog.setCanceledOnTouchOutside(true);
        final Window window = qualityDialog.getWindow();
        if (window != null) {
            // Deliberately NOT fullscreen/edge-to-edge: a fullscreen dialog window makes OxygenOS treat the
            // panel as immersive and apply its two-swipe back-gesture guard. A plain window closes on one back.
            window.setLayout(ui.pickerWidthPx(getResources().getConfiguration()), ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.END);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xF0141414));
        }
        showPickerDialog(qualityDialog);
        if (currentRow[0] != null) {
            currentRow[0].post(() -> currentRow[0].requestFocus());
        }
    }

    private void applyVideoQuality(VideoQualityChoice choice) {
        if (player == null || choice == null) {
            return;
        }
        if (choice.mode == VideoQualityChoice.MODE_SOURCE) {
            if (choice.sourceUrl == null || choice.sourceUrl.trim().isEmpty()) {
                return;
            }
            final Uri target = Uri.parse(choice.sourceUrl);
            if (target.equals(currentPlayingUri())) {
                return; // re-selecting the current URL is a no-op
            }
            selectedVideoQualityMode = VideoQualityChoice.MODE_SOURCE;
            selectedVideoTrackGroup = null;
            selectedVideoTrackIndex = -1;
            stickyQualityLines = qualityNumber(choice.label);
            switchSource(target, Math.max(0, player.getCurrentPosition()), player.getPlayWhenReady());
            return;
        }

        selectedVideoQualityMode = choice.mode;
        selectedVideoTrackGroup = choice.group;
        selectedVideoTrackIndex = choice.trackIndex;
        // A manual in-stream choice clears any sticky SOURCE preference for following episodes.
        stickyQualityLines = 0;
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setForceHighestSupportedBitrate(choice.mode == VideoQualityChoice.MODE_MAXIMUM);
        if (choice.mode == VideoQualityChoice.MODE_TRACK && choice.group != null) {
            builder.setOverrideForType(new TrackSelectionOverride(
                    choice.group, Collections.singletonList(choice.trackIndex)));
        }
        player.setTrackSelectionParameters(builder.build());
    }

    // A row in the native side-panel menus (audio / speed / more).
    private static class MenuItem {
        final int iconRes;
        final CharSequence title;
        final CharSequence subtitle;
        final boolean checked;
        final Runnable action;

        MenuItem(CharSequence title, CharSequence subtitle, boolean checked, Runnable action) {
            this(0, title, subtitle, checked, action);
        }

        MenuItem(int iconRes, CharSequence title, CharSequence subtitle, boolean checked, Runnable action) {
            this.iconRes = iconRes;
            this.title = title;
            this.subtitle = subtitle;
            this.checked = checked;
            this.action = action;
        }
    }

    // Full-height translucent panel docked to the end edge, matching the quality/playlist menus.
    private void showSideMenu(CharSequence menuTitle, List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        final View[] currentRow = new View[1];

        final LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        final int listPad = Utils.dpToPx(10);
        listLayout.setPadding(listPad, listPad, listPad, listPad);

        final TextView header = new TextView(this);
        header.setText(menuTitle);
        header.setTextColor(Color.WHITE);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textTitle());
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10), Utils.dpToPx(10));
        listLayout.addView(header);

        final View divider = new View(this);
        final LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(1));
        dividerLp.bottomMargin = Utils.dpToPx(4);
        divider.setLayoutParams(dividerLp);
        divider.setBackgroundColor(0x1AFFFFFF);
        listLayout.addView(divider);

        for (final MenuItem item : items) {
            final boolean isCurrent = item.checked;

            final LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dpToPx(12), Utils.dpToPx(10), Utils.dpToPx(12), Utils.dpToPx(10));
            row.setClickable(true);
            row.setFocusable(true);
            row.setMinimumHeight(ui.rowMinHeight());
            final GradientDrawable rowContent = new GradientDrawable();
            rowContent.setCornerRadius(Utils.dpToPx(8));
            rowContent.setColor(isCurrent ? brandColorDim() : Color.TRANSPARENT);
            final GradientDrawable rowMask = new GradientDrawable();
            rowMask.setCornerRadius(Utils.dpToPx(8));
            rowMask.setColor(Color.WHITE);
            row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), rowContent, rowMask));
            if (isCurrent) {
                currentRow[0] = row;
            }

            if (item.iconRes != 0) {
                final ImageView icon = new ImageView(this);
                icon.setImageResource(item.iconRes);
                icon.setImageTintList(ColorStateList.valueOf(isCurrent ? 0xFFFFFFFF : 0xFFDDDDDD));
                final int iconSize = Utils.dpToPx(22);
                final LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconLp.setMarginEnd(Utils.dpToPx(16));
                icon.setLayoutParams(iconLp);
                row.addView(icon);
            }

            final LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            final LinearLayout.LayoutParams blockLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            blockLp.gravity = Gravity.CENTER_VERTICAL;
            textBlock.setLayoutParams(blockLp);

            final TextView title = new TextView(this);
            title.setText(item.title);
            title.setTextColor(isCurrent ? 0xFFFFFFFF : 0xFFDDDDDD);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textBody());
            title.setSingleLine(true);
            if (isCurrent) {
                title.setTypeface(Typeface.DEFAULT_BOLD);
            }
            textBlock.addView(title);

            if (item.subtitle != null && item.subtitle.length() > 0) {
                final TextView details = new TextView(this);
                details.setText(item.subtitle);
                details.setTextColor(0x99FFFFFF);
                details.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.textCaption());
                details.setSingleLine(true);
                textBlock.addView(details);
            }
            row.addView(textBlock);

            row.setOnClickListener(v -> {
                if (menuDialog != null) {
                    menuDialog.dismiss();
                }
                if (item.action != null) {
                    item.action.run();
                }
            });
            listLayout.addView(row);
        }

        final android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(listLayout);
        int padTop = 0;
        int padBottom = 0;
        final WindowInsets rootInsets = coordinatorLayout.getRootWindowInsets();
        if (rootInsets != null) {
            // Status bar is hidden while a picker is open (applyPickerBars), so its height is only breathing
            // room. In portrait the status-bar height reads well; landscape is much shorter (and its status-bar
            // inset can include the camera cutout), where that same height looks oversized — use a compact
            // fixed inset there. Pad the bottom for the nav/gesture bar. dp keeps it density/resolution-adaptive.
            final boolean landscape = getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            final int landscapeTop = ui.pickerTopPadLand();
            if (Build.VERSION.SDK_INT >= 30) {
                padTop = landscape ? landscapeTop : rootInsets.getInsets(WindowInsets.Type.statusBars()).top;
                padBottom = rootInsets.getInsets(WindowInsets.Type.navigationBars()).bottom + ui.overscanV();
            } else {
                padTop = landscape ? landscapeTop : rootInsets.getSystemWindowInsetTop();
                padBottom = rootInsets.getSystemWindowInsetBottom() + ui.overscanV();
            }
        }
        scrollView.setPadding(ui.overscanH(), padTop, ui.overscanH(), padBottom);

        if (menuDialog != null) {
            menuDialog.dismiss();
        }
        menuDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        menuDialog.setContentView(scrollView);
        menuDialog.setCanceledOnTouchOutside(true);
        final Window window = menuDialog.getWindow();
        if (window != null) {
            // Deliberately NOT fullscreen/edge-to-edge: a fullscreen dialog window makes OxygenOS treat the
            // panel as immersive and apply its two-swipe back-gesture guard. A plain window closes on one back.
            window.setLayout(ui.pickerWidthPx(getResources().getConfiguration()), ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.END);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xF0141414));
        }
        showPickerDialog(menuDialog);
        if (currentRow[0] != null) {
            currentRow[0].post(() -> currentRow[0].requestFocus());
        }
    }

    private static class AudioChoice {
        final String label;
        final TrackGroup group;
        final int trackIndex;
        final boolean selected;

        AudioChoice(String label, TrackGroup group, int trackIndex, boolean selected) {
            this.label = label;
            this.group = group;
            this.trackIndex = trackIndex;
            this.selected = selected;
        }
    }

    private ArrayList<AudioChoice> buildAudioChoices() {
        final ArrayList<AudioChoice> choices = new ArrayList<>();
        if (player == null) {
            return choices;
        }
        int number = 0;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_AUDIO) {
                continue;
            }
            final TrackGroup trackGroup = group.getMediaTrackGroup();
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) {
                    continue;
                }
                final Format format = trackGroup.getFormat(i);
                number++;
                // Same descriptor as the header meta line, so the picker matches what is shown up top.
                String label = buildAudioInfo(format);
                if (label == null || label.isEmpty()) {
                    label = getString(R.string.audio_track_number, number);
                }
                choices.add(new AudioChoice(label, trackGroup, i, group.isTrackSelected(i)));
            }
        }
        return choices;
    }

    // Shows the audio button only when there is more than one audio track to pick from.
    private void updateAudioButton() {
        if (buttonAudio == null) {
            return;
        }
        final boolean show = player != null && buildAudioChoices().size() >= 2;
        buttonAudio.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Media3 keeps the subtitle button visible-but-disabled while loading; we instead hide it entirely
    // until the media actually exposes subtitle tracks, matching the audio/quality buttons.
    private void updateSubtitleButton() {
        if (exoSubtitle == null) {
            return;
        }
        boolean hasSubtitles = false;
        boolean textSelected = false;
        if (player != null) {
            for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
                if (group.getType() == C.TRACK_TYPE_TEXT) {
                    hasSubtitles = true;
                    if (group.isSelected()) {
                        textSelected = true;
                        break;
                    }
                }
            }
        }
        exoSubtitle.setVisibility(hasSubtitles ? View.VISIBLE : View.GONE);
        // Light the CC icon coral while a subtitle track is actually showing.
        exoSubtitle.setSelected(textSelected);
    }

    private void applyAudio(AudioChoice choice) {
        if (player == null || choice == null || choice.group == null) {
            return;
        }
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(new TrackSelectionOverride(
                        choice.group, Collections.singletonList(choice.trackIndex)))
                .build());
    }

    private void showAudioDialog() {
        final ArrayList<AudioChoice> choices = buildAudioChoices();
        if (choices.size() < 2) {
            return;
        }
        final List<MenuItem> items = new ArrayList<>();
        for (final AudioChoice choice : choices) {
            items.add(new MenuItem(choice.label, null, choice.selected,
                    () -> applyAudio(choice)));
        }
        showSideMenu(getString(R.string.audio_title), items);
    }

    private String buildSubtitleInfo(Format text) {
        final String language = languageDisplayName(text.language);
        String name = text.label;
        if ((name == null || name.isEmpty()) && text.id != null) {
            name = resolvedTrackNames.get(text.id);
        }
        final StringBuilder b = new StringBuilder();
        final String title = (name != null && !name.isEmpty()) ? name : language;
        if (title != null && !title.isEmpty()) {
            b.append(title);
        }
        // If we led with a rich name, still surface the language after it.
        if (name != null && !name.isEmpty() && language != null) {
            b.append(' ').append('(').append(language).append(')');
        }
        return b.toString();
    }

    // Subtitle picker in the same native side panel as audio/quality (replaces the Media3 built-in popup).
    private void showSubtitleDialog() {
        if (player == null) {
            return;
        }
        final boolean textEnabled = player.getCurrentTracks().isTypeSelected(C.TRACK_TYPE_TEXT);
        final List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem(getString(R.string.subtitle_off), null, !textEnabled, this::disableSubtitles));
        int number = 0;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT) {
                continue;
            }
            final TrackGroup trackGroup = group.getMediaTrackGroup();
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSupported(i)) {
                    continue;
                }
                final Format format = trackGroup.getFormat(i);
                number++;
                String label = buildSubtitleInfo(format);
                if (label == null || label.isEmpty()) {
                    label = getString(R.string.audio_track_number, number);
                }
                final int index = i;
                items.add(new MenuItem(label, null, textEnabled && group.isTrackSelected(i),
                        () -> applySubtitle(trackGroup, index)));
            }
        }
        showSideMenu(getString(R.string.subtitle_title), items);
    }

    private void disableSubtitles() {
        if (player == null) {
            return;
        }
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build());
    }

    private void applySubtitle(TrackGroup group, int index) {
        if (player == null || group == null) {
            return;
        }
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(new TrackSelectionOverride(group, Collections.singletonList(index)))
                .build());
    }

    private static final float[] SPEED_PRESETS =
            {0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f};

    private String formatSpeed(float speed) {
        if (Math.abs(speed - 1f) < 0.001f) {
            return getString(R.string.speed_normal);
        }
        final String number = speed == Math.rint(speed)
                ? String.valueOf((int) speed)
                : String.valueOf(speed);
        return number + "×";
    }

    private void showSpeedDialog() {
        if (player == null) {
            return;
        }
        final float current = player.getPlaybackParameters().speed;
        final List<MenuItem> items = new ArrayList<>();
        for (final float speed : SPEED_PRESETS) {
            items.add(new MenuItem(formatSpeed(speed), null,
                    Math.abs(current - speed) < 0.001f, () -> applySpeed(speed)));
        }
        showSideMenu(getString(R.string.speed_title), items);
    }

    private void applySpeed(float speed) {
        if (player == null) {
            return;
        }
        player.setPlaybackSpeed(speed);
        mPrefs.speed = speed;
        updateEndsAt();
    }

    private void cycleOrientation() {
        mPrefs.orientation = Utils.getNextOrientation(mPrefs.orientation);
        Utils.setOrientation(PlayerActivity.this, mPrefs.orientation);
        updateButtonRotation();
        Utils.showText(playerView, getString(mPrefs.orientation.description), 2500);
        resetHideCallbacks();
    }

    // Uniform box for every button that lives inside a control pill (header display cluster + bottom pickers),
    // so both pills share one height, one button size and one inter-button gap. 40dp box, 8dp padding keeps
    // the glyph at the standard 24dp.
    private void styleClusterButton(final ImageButton button) {
        if (button == null) {
            return;
        }
        final int pad = ui.clusterPad();
        button.setPadding(pad, pad, pad, pad);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ui.clusterBox(), ui.clusterBox());
        params.gravity = Gravity.CENTER_VERTICAL;
        button.setLayoutParams(params);
    }

    // Overflow menu: everything used rarely or once per session lives here so the main row stays calm.
    private void showMoreMenu() {
        final List<MenuItem> items = new ArrayList<>();
        if (player != null) {
            items.add(new MenuItem(R.drawable.ic_speed_24dp, getString(R.string.speed_title),
                    formatSpeed(player.getPlaybackParameters().speed), false, this::showSpeedDialog));
        }
        if (buttonSkipOffset != null && buttonSkipOffset.getVisibility() == View.VISIBLE) {
            items.add(new MenuItem(R.drawable.ic_skip_offset_24dp, getString(R.string.button_skip_offset), null, false, this::showSkipOffsetDialog));
        }
        items.add(new MenuItem(R.drawable.ic_folder_open_24dp, getString(R.string.button_open), null, false, () -> openFile(mPrefs.mediaUri)));
        // "More" → the full app settings screen (long-pressing the gear opens it directly, too).
        items.add(new MenuItem(R.drawable.ic_settings_24dp, getString(R.string.button_more), null, false, () ->
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS)));
        showSideMenu(getString(R.string.pref_title), items);
    }

    // Replaces the current item's URL with a separate-URL quality variant and reinitialises the player,
    // preserving the playback position in-session. Under apiAccess Prefs is non-persistent, so the
    // position is held in memory (not keyed by URI) and never written to disk.
    private void switchSource(Uri target, long positionMs, boolean resume) {
        if (player == null || target == null) {
            return;
        }
        final int index = player.getCurrentMediaItemIndex();
        if (!apiMediaItems.isEmpty() && index >= 0 && index < apiMediaItems.size()) {
            final MediaItem old = apiMediaItems.get(index);
            apiMediaItems.set(index, old.buildUpon().setUri(target).build());
            apiPlaylistStartIndex = index;
        } else {
            mPrefs.mediaUri = target;
        }
        mPrefs.updatePosition(positionMs);
        sourceSwitchKeepPaused = !resume;
        restorePlayState = resume;
        initializePlayer();
    }

    // Re-applies a remembered SOURCE quality (by number of lines) to the item now playing — used after
    // an auto-next so a chosen quality carries across episodes. Falls back to the base URL when the new
    // episode has no matching label.
    private void applyStickyQuality() {
        if (player == null || stickyQualityLines <= 0) {
            return;
        }
        final LinkedHashMap<String, String> quality = currentQualityMap();
        if (quality == null || quality.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : quality.entrySet()) {
            if (qualityNumber(entry.getKey()) != stickyQualityLines) {
                continue;
            }
            final String url = entry.getValue();
            if (url == null || url.trim().isEmpty()) {
                return;
            }
            final Uri target = Uri.parse(url);
            if (target.equals(currentPlayingUri())) {
                return; // already playing the sticky quality
            }
            selectedVideoQualityMode = VideoQualityChoice.MODE_SOURCE;
            switchSource(target, 0, player.getPlayWhenReady());
            return;
        }
    }

    private int selectedQualityIndex(List<VideoQualityChoice> choices) {
        if (selectedVideoQualityMode != VideoQualityChoice.MODE_SOURCE) {
            for (int i = 0; i < choices.size(); i++) {
                final VideoQualityChoice choice = choices.get(i);
                if (choice.mode == VideoQualityChoice.MODE_TRACK
                        && selectedVideoQualityMode == VideoQualityChoice.MODE_TRACK
                        && choice.group == selectedVideoTrackGroup
                        && choice.trackIndex == selectedVideoTrackIndex) {
                    return i;
                }
                if ((choice.mode == VideoQualityChoice.MODE_AUTO
                        || choice.mode == VideoQualityChoice.MODE_MAXIMUM)
                        && choice.mode == selectedVideoQualityMode) {
                    return i;
                }
            }
        }
        final Uri current = currentPlayingUri();
        if (current != null) {
            for (int i = 0; i < choices.size(); i++) {
                final VideoQualityChoice choice = choices.get(i);
                if (choice.mode == VideoQualityChoice.MODE_SOURCE && choice.sourceUrl != null
                        && choice.sourceUrl.equals(current.toString())) {
                    return i;
                }
            }
        }
        return 0;
    }

    // Number of scan lines a quality label denotes ("1080p" -> 1080, "4K"/"UHD" -> 2160, else 0).
    private static int qualityNumber(String label) {
        if (label == null) {
            return 0;
        }
        String normalized = label.toLowerCase(Locale.US);
        if (normalized.contains("4k") || normalized.contains("uhd")) {
            return 2160;
        }
        String digits = label.replaceAll("[^0-9]", "");
        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String shortCodec(String mimeType) {
        if (mimeType == null) return null;
        if (mimeType.contains("avc")) return "H.264";
        if (mimeType.contains("hevc")) return "H.265";
        if (mimeType.contains("av01")) return "AV1";
        if (mimeType.contains("vp9")) return "VP9";
        if (mimeType.contains("eac3")) return "E-AC3";
        if (mimeType.contains("ac3")) return "AC3";
        if (mimeType.contains("aac") || mimeType.contains("mp4a")) return "AAC";
        return mimeType.substring(mimeType.lastIndexOf('/') + 1).toUpperCase(Locale.US);
    }

    private static final class VideoQualityChoice {
        static final int MODE_AUTO = 0;
        static final int MODE_MAXIMUM = 1;
        static final int MODE_TRACK = 2;
        static final int MODE_SOURCE = 3;
        final String label;
        final String details;
        final String bitrateText;
        final int mode;
        final TrackGroup group;
        final int trackIndex;
        final int bitrate;
        final String sourceUrl;

        private VideoQualityChoice(String label, String details, String bitrateText,
                                   int mode, TrackGroup group, int trackIndex,
                                   int bitrate, String sourceUrl) {
            this.label = label;
            this.details = details;
            this.bitrateText = bitrateText;
            this.mode = mode;
            this.group = group;
            this.trackIndex = trackIndex;
            this.bitrate = bitrate;
            this.sourceUrl = sourceUrl;
        }

        static VideoQualityChoice auto() {
            return new VideoQualityChoice("", "", "", MODE_AUTO, null, -1, -1, null);
        }

        static VideoQualityChoice maximum() {
            return new VideoQualityChoice("", "", "", MODE_MAXIMUM, null, -1, -1, null);
        }

        static VideoQualityChoice track(String label, String details, String bitrateText,
                                        TrackGroup group, int index, int bitrate) {
            return new VideoQualityChoice(label, details, bitrateText,
                    MODE_TRACK, group, index, bitrate, null);
        }

        static VideoQualityChoice source(String label, String url) {
            return new VideoQualityChoice(label, "", "", MODE_SOURCE, null, -1, -1, url);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (restoreOrientationLock) {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                restoreOrientationLock = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (resultCode == RESULT_OK && alive) {
            releasePlayer();
        }

        if (requestCode == REQUEST_CHOOSER_VIDEO || requestCode == REQUEST_CHOOSER_VIDEO_MEDIASTORE) {
            if (resultCode == RESULT_OK) {
                resetApiAccess();
                restorePlayState = false;

                final Uri uri = data.getData();

                if (requestCode == REQUEST_CHOOSER_VIDEO) {
                    boolean uriAlreadyTaken = false;

                    // https://commonsware.com/blog/2020/06/13/count-your-saf-uri-permission-grants.html
                    final ContentResolver contentResolver = getContentResolver();
                    for (UriPermission persistedUri : contentResolver.getPersistedUriPermissions()) {
                        if (persistedUri.getUri().equals(mPrefs.scopeUri)) {
                            continue;
                        } else if (persistedUri.getUri().equals(uri)) {
                            uriAlreadyTaken = true;
                        } else {
                            try {
                                contentResolver.releasePersistableUriPermission(persistedUri.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!uriAlreadyTaken && uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }
                }

                mPrefs.setPersistent(true);
                mPrefs.updateMedia(this, uri, data.getType());

                if (requestCode == REQUEST_CHOOSER_VIDEO) {
                    searchSubtitles();
                }
            }
        } else if (requestCode == REQUEST_CHOOSER_SUBTITLE || requestCode == REQUEST_CHOOSER_SUBTITLE_MEDIASTORE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();

                if (requestCode == REQUEST_CHOOSER_SUBTITLE) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }

                handleSubtitles(uri);
            }
        } else if (requestCode == REQUEST_CHOOSER_SCOPE_DIR) {
            if (resultCode == RESULT_OK) {
                final Uri uri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mPrefs.updateScope(uri);
                    mPrefs.markScopeAsked();
                    searchSubtitles();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            mPrefs.loadUserPreferences();
            updateSubtitleStyle(this);
            updateOverlayClock();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        // Init here because onStart won't follow when app was only paused when file chooser was shown
        // (for example pop-up file chooser on tablets)
        if (resultCode == RESULT_OK && alive) {
            initializePlayer();
        }
    }

    private void handleSubtitles(Uri uri) {
        // Convert subtitles to UTF-8 if necessary
        SubtitleUtils.clearCache(this);
        uri = Utils.convertToUTF(this, uri);
        mPrefs.updateSubtitle(uri);
    }

    // Whether the current media is a Matroska container, detected from the resolved MIME type or the
    // URI extension. Extensionless streams that never reveal a matroska type are not matched.
    private boolean isMatroskaMedia() {
        if (MimeTypes.VIDEO_MATROSKA.equals(mPrefs.mediaType)) {
            return true;
        }
        if (mPrefs.mediaUri == null) {
            return false;
        }
        final String path = mPrefs.mediaUri.getPath();
        // Case-insensitive ".mkv" suffix test without allocating a lower-cased copy of the path.
        return path != null && path.regionMatches(true, path.length() - 4, ".mkv", 0, 4);
    }

    public void initializePlayer() {
        boolean isNetworkUri = Utils.isSupportedNetworkUri(mPrefs.mediaUri);
        haveMedia = mPrefs.mediaUri != null;

        // Unless this is the stuck-playback recovery rebuild (which must keep forceHevcForDolbyVision),
        // clear the one-shot Dolby Vision recovery state so a normal open plays DV through its regular
        // path and a future stall on any item can recover again.
        if (pendingStuckRecovery) {
            pendingStuckRecovery = false;
        } else {
            forceHevcForDolbyVision = false;
            stuckRecoveryAttemptedUri = null;
        }

        // Fresh media — drop any container track names so the tap re-parses for this item.
        containerTracks.clear();
        resolvedTrackNames.clear();

        if (player != null) {
            player.removeListener(playerListener);
            player.clearMediaItems();
            player.release();
            player = null;
        }

        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true));
        if (mPrefs.tunneling) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setTunnelingEnabled(true)
            );
        }
        switch (mPrefs.languageAudio) {
            case Prefs.TRACK_DEFAULT:
                break;
            case Prefs.TRACK_DEVICE:
                trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setPreferredAudioLanguages(Utils.getDeviceLanguages())
                );
                break;
            default:
                trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setPreferredAudioLanguages(mPrefs.languageAudio)
                );
        }
        final CaptioningManager captioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        if (!captioningManager.isEnabled()) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            );
        }
        Locale locale = captioningManager.getLocale();
        if (locale != null) {
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setPreferredTextLanguage(locale.getISO3Language())
            );
        }
        // https://github.com/google/ExoPlayer/issues/8571
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
                .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE);
        // On TV boxes the platform MediaCodec decoder for the heavy codecs common in MKV remuxes
        // (DTS/EAC3/TrueHD) can wedge during init on the playback thread — no exception is thrown, so
        // the load never reaches a ready state (JPP-1005). Instead of blanket-forcing software audio
        // (which also kills Atmos/passthrough on every TV), hide only those codecs from the platform
        // decoder via the combined MediaCodecSelector below. MediaCodecAudioRenderer consults the sink
        // first, so where the receiver advertises passthrough the compressed bitstream still goes out
        // (Atmos preserved); where it does not, the track falls through to the ffmpeg software
        // renderer. Only MKV audio on a TV is affected; video keeps the user's decoder priority.
        final boolean blockHeavyMkvAudio = isTvBox && isMatroskaMedia();
        DefaultRenderersFactory baseRenderersFactory = blockHeavyMkvAudio
                ? new DefaultRenderersFactory(this) {
                    @Override
                    protected void buildAudioRenderers(Context context, int extensionRendererMode,
                                                       MediaCodecSelector mediaCodecSelector,
                                                       boolean enableDecoderFallback, AudioSink audioSink,
                                                       Handler eventHandler,
                                                       AudioRendererEventListener eventListener,
                                                       ArrayList<Renderer> out) {
                        // Ensure the ffmpeg audio renderer exists as a fallback even when the user
                        // chose "device decoders only"; keep it behind the platform renderer (ON, not
                        // PREFER) so passthrough still wins when available. An explicit PREFER stands.
                        super.buildAudioRenderers(context,
                                extensionRendererMode == EXTENSION_RENDERER_MODE_OFF
                                        ? EXTENSION_RENDERER_MODE_ON : extensionRendererMode,
                                mediaCodecSelector, enableDecoderFallback, audioSink, eventHandler,
                                eventListener, out);
                    }
                }
                : new DefaultRenderersFactory(this);
        @SuppressLint("WrongConstant") DefaultRenderersFactory renderersFactory = baseRenderersFactory
                .setExtensionRendererMode(mPrefs.decoderPriority)
                .setMapDV7ToHevc(mPrefs.mapDV7ToHevc);
        if (forceHevcForDolbyVision || blockHeavyMkvAudio) {
            // One combined codec selector for two independent needs:
            // - blockHeavyMkvAudio: no platform decoder for the heavy MKV audio codecs, so they
            //   passthrough (if the sink supports it) or fall back to ffmpeg (see above).
            // - forceHevcForDolbyVision: route a Dolby Vision track to the plain HEVC decoder (its base
            //   layer is HEVC), bypassing a device DV decoder that wedged. Picture stays HDR10.
            renderersFactory.setMediaCodecSelector((mimeType, requiresSecureDecoder, requiresTunnelingDecoder) -> {
                if (blockHeavyMkvAudio && HEAVY_MKV_AUDIO_MIMES.contains(mimeType)) {
                    return Collections.emptyList();
                }
                return MediaCodecSelector.DEFAULT.getDecoderInfos(
                        forceHevcForDolbyVision && MimeTypes.VIDEO_DOLBY_VISION.equals(mimeType)
                                ? MimeTypes.VIDEO_H265 : mimeType,
                        requiresSecureDecoder, requiresTunnelingDecoder);
            });
        }

        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(this, renderersFactory)
                .setTrackSelector(trackSelector);

        // Build the upstream data source factory (content://, file://, http(s)), applying any
        // launch-intent HTTP headers/User-Agent, then wrap it so we can tap the byte stream and
        // read rich track names straight from the container (see TrackNameParsingDataSource).
        androidx.media3.datasource.DataSource.Factory upstreamFactory = new DefaultDataSource.Factory(this);

        if (haveMedia && isNetworkUri && mPrefs.mediaUri.getScheme().toLowerCase().startsWith("http")) {
            HashMap<String, String> headers = new HashMap<>();
            String userAgent = null;

            // Headers supplied by the launching app as a flat [name, value, name, value, ...] array
            // (MX Player / Lampa convention). Some CDNs require a specific User-Agent to authorize.
            if (apiHeaders != null) {
                for (int i = 0; i + 1 < apiHeaders.length; i += 2) {
                    final String name = apiHeaders[i];
                    final String value = apiHeaders[i + 1];
                    if (name == null || value == null) {
                        continue;
                    }
                    if ("User-Agent".equalsIgnoreCase(name)) {
                        userAgent = value;
                    } else {
                        headers.put(name, value);
                    }
                }
            }

            String userInfo = mPrefs.mediaUri.getUserInfo();
            if (userInfo != null && userInfo.length() > 0 && userInfo.contains(":")) {
                headers.put("Authorization", "Basic " + Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
            }

            if (!headers.isEmpty() || userAgent != null) {
                DefaultHttpDataSource.Factory defaultHttpDataSourceFactory = new DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true);
                if (userAgent != null) {
                    defaultHttpDataSourceFactory.setUserAgent(userAgent);
                }
                if (!headers.isEmpty()) {
                    defaultHttpDataSourceFactory.setDefaultRequestProperties(headers);
                }
                upstreamFactory = new DefaultDataSource.Factory(this, defaultHttpDataSourceFactory);
            }
        }

        final androidx.media3.datasource.DataSource.Factory dataSourceFactory = new TrackNameParsingDataSource.Factory(upstreamFactory, trackNameListener);
        playerBuilder.setMediaSourceFactory(
                new DefaultMediaSourceFactory(this, extractorsFactory).setDataSourceFactory(dataSourceFactory));

        player = playerBuilder.build();

        if (!mPrefs.allowSystemFrameRate) {
            // Stop ExoPlayer from voting Surface.setFrameRate() on start/pause/seek. On many TV
            // panels a refresh-rate switch (even a "seamless" one) re-syncs the panel and flickers.
            player.setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF);
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        player.setAudioAttributes(audioAttributes, true);

        if (mPrefs.skipSilence) {
            player.setSkipSilenceEnabled(true);
        }

        youTubeOverlay.player(player);
        playerView.setPlayer(player);

        if (mediaSession != null) {
            mediaSession.release();
        }

        if (player.canAdvertiseSession()) {
            try {
                mediaSession = new MediaSession.Builder(this, player).build();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        playerView.setControllerShowTimeoutMs(-1);

        locked = false;
        clearLockUi();

        if (haveMedia) {
            hideEmptyState();
            if (isNetworkUri) {
                timeBar.setBufferedColor(DefaultTimeBar.DEFAULT_BUFFERED_COLOR);
            } else {
                // https://github.com/google/ExoPlayer/issues/5765
                timeBar.setBufferedColor(0x33FFFFFF);
            }

            playerView.setResizeMode(mPrefs.resizeMode);
            currentAspectRatio = mPrefs.aspectRatio;
            if (mPrefs.aspectRatio > 0) {
                playerView.applyAspectMode(mPrefs.resizeMode, mPrefs.aspectRatio);
            } else if (mPrefs.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                playerView.setScale(mPrefs.scale);
            } else {
                playerView.setScale(1.f);
            }
            updatebuttonAspectRatioIcon();

            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                    .setUri(mPrefs.mediaUri)
                    .setMimeType(mPrefs.mediaType);
            String title;
            if (apiTitle != null) {
                title = apiTitle;
            } else {
                title = Utils.getFileName(PlayerActivity.this, mPrefs.mediaUri);
            }
            if (title != null) {
                final MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                        .setTitle(title)
                        .setDisplayTitle(title)
                        .setArtworkUri(apiThumbnailUri)
                        .build();
                mediaItemBuilder.setMediaMetadata(mediaMetadata);
            }
            if (apiAccess && apiSubs.size() > 0) {
                mediaItemBuilder.setSubtitleConfigurations(apiSubs);
            } else if (mPrefs.subtitleUri != null && Utils.fileExists(this, mPrefs.subtitleUri)) {
                MediaItem.SubtitleConfiguration subtitle = SubtitleUtils.buildSubtitle(this, mPrefs.subtitleUri, null, true);
                mediaItemBuilder.setSubtitleConfigurations(Collections.singletonList(subtitle));
            }
            if (!apiMediaItems.isEmpty()) {
                player.setMediaItems(new ArrayList<>(apiMediaItems), apiPlaylistStartIndex, mPrefs.getPosition());
            } else {
                player.setMediaItem(mediaItemBuilder.build(), mPrefs.getPosition());
            }

            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.release();
                }
                loudnessEnhancer = new LoudnessEnhancer(player.getAudioSessionId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            notifyAudioSessionUpdate(true);

            videoLoading = true;

            updateLoading(true);

            // A SOURCE quality-switch that was paused must stay paused after reinitialisation; otherwise
            // apiAccess would force auto-play. Consume the one-shot flag here.
            final boolean keepPaused = sourceSwitchKeepPaused;
            sourceSwitchKeepPaused = false;
            if ((mPrefs.getPosition() == 0L || apiAccess || apiAccessPartial) && !keepPaused) {
                play = true;
            }

            updateTopInfo();

            setupSkipSource();

            updateButtons(true);

            ((DoubleTapPlayerView)playerView).setDoubleTapEnabled(true);

            if (!apiAccess) {
                if (nextUriThread != null) {
                    nextUriThread.interrupt();
                }
                nextUri = null;
                nextUriThread = new Thread(() -> {
                    Uri uri = findNext();
                    if (!Thread.currentThread().isInterrupted()) {
                        nextUri = uri;
                    }
                });
                nextUriThread.start();
            }

            player.setHandleAudioBecomingNoisy(!isTvBox);
//            mediaSession.setActive(true);
        } else {
            playerView.showController();
            showEmptyState();
        }

        player.addListener(playerListener);
        player.prepare();

        if (restorePlayState) {
            restorePlayState = false;
            playerView.showController();
            playerView.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
            player.setPlayWhenReady(true);
        }
    }

    private void savePlayer() {
        if (player != null) {
            mPrefs.updateBrightness(mBrightnessControl.currentBrightnessLevel);
            mPrefs.updateOrientation();

            if (haveMedia) {
                // Prevent overwriting temporarily inaccessible media position
                if (player.isCurrentMediaItemSeekable()) {
                    mPrefs.updatePosition(player.getCurrentPosition());
                    rememberEpisodePosition(player.getCurrentMediaItemIndex(), player.getCurrentPosition());
                }
                mPrefs.updateMeta(getSelectedTrack(C.TRACK_TYPE_AUDIO),
                        getSelectedTrack(C.TRACK_TYPE_TEXT),
                        playerView.getResizeMode(),
                        playerView.getVideoSurfaceView().getScaleX(),
                        currentAspectRatio,
                        player.getPlaybackParameters().speed);
            }
        }
    }

    private void rememberEpisodePosition(final int index, final long position) {
        if (apiPlaylistPositions != null && index >= 0 && index < apiPlaylistPositions.length) {
            apiPlaylistPositions[index] = position;
        }
    }

    private boolean isEmptyStateVisible() {
        final View overlay = findViewById(R.id.empty_state);
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    private boolean isReducedMotion() {
        return Settings.Global.getFloat(getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
    }

    // Branded empty state shown while there is no clip to play: an animated brand-mark reveal
    // and a single "Open video" call to action (the only entry point to a file when nothing is loaded).
    private void showEmptyState() {
        final View overlay = findViewById(R.id.empty_state);
        if (overlay == null) {
            return;
        }
        final View mark = findViewById(R.id.empty_state_mark);
        final TextView title = findViewById(R.id.empty_state_title);
        final TextView subtitle = findViewById(R.id.empty_state_subtitle);
        final View open = findViewById(R.id.empty_state_open);

        open.setOnClickListener(v -> openFile(mPrefs.mediaUri));
        stopEmptyStatePulse();

        // TV is viewed from across the room; scale the phone-tuned sizes up, matching the
        // isTvBox sizing used elsewhere (poster, clock, skip button).
        if (isTvBox) {
            setViewSize(mark, 140);
            setViewSize(findViewById(R.id.empty_state_mark_icon), 68);
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            setViewSize(findViewById(R.id.empty_state_open_icon), 28);
            ((TextView) findViewById(R.id.empty_state_open_label))
                    .setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            final int padV = Utils.dpToPx(18);
            open.setPadding(Utils.dpToPx(30), padV, Utils.dpToPx(32), padV);
            open.setMinimumHeight(Utils.dpToPx(64));
        } else if (ui.deviceClass != UiMetrics.DeviceClass.PHONE) {
            // Tablet: scale the phone XML defaults by the device-class factor (phone keeps the XML sizes).
            setViewSize(mark, ui.dpS(96));
            setViewSize(findViewById(R.id.empty_state_mark_icon), ui.dpS(46));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.sp(22));
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.sp(14));
            setViewSize(findViewById(R.id.empty_state_open_icon), ui.dpS(20));
            ((TextView) findViewById(R.id.empty_state_open_label))
                    .setTextSize(TypedValue.COMPLEX_UNIT_SP, ui.sp(16));
        }

        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();

        final View[] items = {mark, title, subtitle, open};

        if (isReducedMotion()) {
            for (View v : items) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
                v.setTranslationY(0f);
            }
            open.requestFocus();
            return;
        }

        final float density = getResources().getDisplayMetrics().density;
        final PathInterpolator easeOutExpo = new PathInterpolator(0.16f, 1f, 0.3f, 1f);

        mark.setAlpha(0f);
        mark.setScaleX(0.85f);
        mark.setScaleY(0.85f);
        title.setAlpha(0f);
        title.setTranslationY(12 * density);
        subtitle.setAlpha(0f);
        subtitle.setTranslationY(12 * density);
        open.setAlpha(0f);
        open.setTranslationY(16 * density);

        mark.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(0).setDuration(450).setInterpolator(easeOutExpo).start();
        title.animate().alpha(1f).translationY(0f)
                .setStartDelay(120).setDuration(350).setInterpolator(easeOutExpo).start();
        subtitle.animate().alpha(1f).translationY(0f)
                .setStartDelay(200).setDuration(300).setInterpolator(easeOutExpo).start();
        open.animate().alpha(1f).translationY(0f)
                .setStartDelay(260).setDuration(350).setInterpolator(easeOutExpo)
                .withEndAction(() -> {
                    open.requestFocus();
                    startEmptyStatePulse(open);
                }).start();
    }

    private void setViewSize(View view, int dp) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.width = Utils.dpToPx(dp);
        lp.height = Utils.dpToPx(dp);
        view.setLayoutParams(lp);
    }

    private void hideEmptyState() {
        stopEmptyStatePulse();
        final View overlay = findViewById(R.id.empty_state);
        if (overlay != null && overlay.getVisibility() != View.GONE) {
            overlay.setVisibility(View.GONE);
        }
    }

    private void startEmptyStatePulse(View view) {
        emptyStatePulse = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.04f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.04f));
        emptyStatePulse.setDuration(1400);
        emptyStatePulse.setRepeatCount(ValueAnimator.INFINITE);
        emptyStatePulse.setRepeatMode(ValueAnimator.REVERSE);
        emptyStatePulse.setInterpolator(new AccelerateDecelerateInterpolator());
        emptyStatePulse.start();
    }

    private void stopEmptyStatePulse() {
        if (emptyStatePulse != null) {
            emptyStatePulse.cancel();
            emptyStatePulse = null;
        }
    }

    private void cancelLoadWatchdog() {
        if (playerView != null) {
            playerView.removeCallbacks(loadTimeoutRunnable);
        }
    }

    private void reportVideoLoadTimeout() {
        if (player == null) {
            return;
        }
        // A silent stall (buffering never reached READY). Not sent to Sentry — it is usually an
        // upstream/network condition, not an app bug — just surface a friendly message with its code.
        updateLoading(false);
        showSnack(getString(R.string.error_playback_timeout), null);
    }

    public void releasePlayer() {
        releasePlayer(true);
    }

    public void releasePlayer(boolean save) {
        cancelLoadWatchdog();
        if (save) {
            savePlayer();
        }

        if (player != null) {
            notifyAudioSessionUpdate(false);

//            mediaSession.setActive(false);
            if (mediaSession != null) {
                mediaSession.release();
            }

            if (player.isPlaying() && restorePlayStateAllowed) {
                restorePlayState = true;
            }
            player.removeListener(playerListener);
            player.clearMediaItems();
            player.release();
            player = null;
        }
        stopSkipPolling();
        cancelSegmentFinder();
        hideSkipButton();
        hideSkipNotification();
        skipBuilt = false;
        if (timeBar != null) {
            timeBar.clearSkipHighlights();
        }
        stopEndsAtUpdates();
        if (overlayClock != null) {
            overlayClock.setVisibility(View.GONE);
        }
        setEpisodeNavLoading(false);
        Glide.with(getApplicationContext()).clear(posterView);
        posterSlot.setVisibility(View.GONE);
        topInfoPanel.setVisibility(View.GONE);
        if (playlistDialog != null) {
            playlistDialog.dismiss();
            playlistDialog = null;
        }
        if (qualityDialog != null) {
            qualityDialog.dismiss();
            qualityDialog = null;
        }
        if (skipOffsetDialog != null) {
            skipOffsetDialog.dismiss();
            skipOffsetDialog = null;
        }
        if (menuDialog != null) {
            menuDialog.dismiss();
            menuDialog = null;
        }
        if (buttonPlaylist != null) {
            buttonPlaylist.setVisibility(View.GONE);
        }
        if (buttonQuality != null) {
            buttonQuality.setVisibility(View.GONE);
        }
        updateButtons(false);
    }

    private class PlayerListener implements Player.Listener {
        @Override
        public void onVideoSizeChanged(VideoSize videoSize) {
            // Media3 resets the content-frame AR to the video's natural AR on every size change (e.g. a
            // mid-stream video-track switch), silently dropping a forced ratio. Reassert it after that
            // update (posted, so it wins).
            // Skip while ZOOM is active: that means free pinch-zoom has taken over, and reasserting would
            // fight it (adaptive streams fire this on every resolution switch).
            if (currentAspectRatio > 0 && playerView != null
                    && playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                playerView.post(() ->
                        playerView.applyAspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, currentAspectRatio));
            }
        }

        @Override
        public void onAudioSessionIdChanged(int audioSessionId) {
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.release();
                }
                loudnessEnhancer = new LoudnessEnhancer(audioSessionId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            notifyAudioSessionUpdate(true);
        }

        @Override
        public void onPositionDiscontinuity(Player.PositionInfo oldPosition,
                                            Player.PositionInfo newPosition, int reason) {
            if (apiPlaylistPositions == null || player == null) {
                return;
            }
            final int oldIndex = oldPosition.mediaItemIndex;
            final int newIndex = newPosition.mediaItemIndex;
            // Leaving an episode (auto-advance or manual jump): remember where we left it.
            if (oldIndex != newIndex) {
                rememberEpisodePosition(oldIndex, oldPosition.positionMs);
            }
            // Manually jumping back to an already-watched episode: resume where we left it. Auto-advance
            // (gapless) keeps starting the next episode from the beginning, as it should. The follow-up
            // seek lands with oldIndex == newIndex, so it neither loops nor overwrites the saved slot.
            if (reason == Player.DISCONTINUITY_REASON_SEEK && oldIndex != newIndex
                    && newIndex >= 0 && newIndex < apiPlaylistPositions.length) {
                final long saved = apiPlaylistPositions[newIndex];
                if (saved != C.TIME_UNSET && saved > 0 && newPosition.positionMs < 1000) {
                    player.seekTo(newIndex, saved);
                }
            }
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            // Keep the playlist start index (and mediaUri) tracking the item that is actually
            // playing. They would otherwise stay frozen at the session's initial episode, so a
            // rebuild after onStop (setMediaItems uses apiPlaylistStartIndex) would restart the
            // first episode while applying the current episode's saved position.
            if (!apiMediaItems.isEmpty() && player != null) {
                final int idx = player.getCurrentMediaItemIndex();
                if (idx >= 0 && idx < apiMediaItems.size()) {
                    apiPlaylistStartIndex = idx;
                    final MediaItem current = apiMediaItems.get(idx);
                    if (current.localConfiguration != null) {
                        mPrefs.mediaUri = current.localConfiguration.uri;
                    }
                }
            }
            updateTopInfo();
            hideSkipButton();
            cancelSegmentFinder();
            setupSkipSource();
        }

        @Override
        public void onEvents(Player player, Player.Events events) {
            // Media3 re-shows the subtitle button (greyed, disabled) on its own control updates while
            // loading. Re-assert our "hidden until subtitle tracks exist" rule afterwards (deferred so it wins),
            // but only on events that can actually touch the controls — not on every frequent event dispatch.
            if (playerView != null && events.containsAny(
                    Player.EVENT_TRACKS_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                playerView.post(PlayerActivity.this::updateSubtitleButton);
            }

            // A gapless playlist auto-advance can move to the next item while staying STATE_READY, so
            // onPlaybackStateChanged never re-fires and rebuildSkip() is never called for the new item.
            // onMediaItemTransition has already reset skipBuilt via setupSkipSource(); once the new item's
            // duration is known, build its highlights exactly once. The skipBuilt guard shares the work with
            // the STATE_READY path so there is no double build.
            if (!skipBuilt && player.getPlaybackState() == Player.STATE_READY) {
                final long duration = player.getDuration();
                if (duration != C.TIME_UNSET && duration > 0) {
                    rebuildSkip();
                    skipBuilt = true;
                    maybeFetchSegmentsOnline();
                }
            }
        }

        @Override
        public void onTracksChanged(Tracks tracks) {
            // Tracks are now known — (re)map any container names onto them, then refresh the header.
            resolveTrackNames();
            updateMediaInfo();
            // In-stream renditions are known only now, so the quality button's visibility can change.
            updateQualityButton();
            updateAudioButton();
            if (playerView != null) {
                playerView.post(PlayerActivity.this::updateSubtitleButton);
            }
            // Apply a sticky quality choice to a freshly auto-advanced episode once its variants are known.
            // Posted so the reinitialisation never runs while listeners are being dispatched.
            if (playerView != null) {
                playerView.post(PlayerActivity.this::applyStickyQuality);
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            playerView.setKeepScreenOn(isPlaying);

            if (Utils.isPiPSupported(PlayerActivity.this)) {
                if (isPlaying) {
                    updatePictureInPictureActions(R.drawable.ic_pause_24dp, R.string.exo_controls_pause_description, CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
                } else {
                    updatePictureInPictureActions(R.drawable.ic_play_arrow_24dp, R.string.exo_controls_play_description, CONTROL_TYPE_PLAY, REQUEST_PLAY);
                }
            }

            if (!isScrubbing) {
                if (isPlaying) {
                    if (shortControllerTimeout) {
                        playerView.setControllerShowTimeoutMs(CONTROLLER_TIMEOUT / 3);
                        shortControllerTimeout = false;
                        restoreControllerTimeout = true;
                    } else {
                        playerView.setControllerShowTimeoutMs(CONTROLLER_TIMEOUT);
                    }
                } else {
                    playerView.setControllerShowTimeoutMs(-1);
                }
            }

            if (!isPlaying && PlayerActivity.locked) {
                PlayerActivity.locked = false;
                clearLockUi();
            }

            if (isPlaying) {
                startSkipPolling();
            } else {
                stopSkipPolling();
            }

            // Pausing while the controller is already visible doesn't change its visibility, so arm the
            // pause auto-hide here too; resuming cancels it (guarded inside scheduleHideControllerOnPause).
            scheduleHideControllerOnPause();
        }

        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onPlaybackStateChanged(int state) {
            boolean isNearEnd = false;
            final long duration = player.getDuration();
            if (duration != C.TIME_UNSET) {
                final long position = player.getCurrentPosition();
                if (position + 4000 >= duration) {
                    isNearEnd = true;
                }
            }
            setEndControlsVisible(haveMedia && (state == Player.STATE_ENDED || isNearEnd));

            if (state == Player.STATE_READY) {
                frameRendered = true;
                cancelLoadWatchdog();
                // Loaded successfully — clear any pending resolver-handshake flag from a prior attempt.
                resolverNotReadyUri = null;

                // Ready — hide the spinner and re-enable the episode arrows. Done unconditionally (not only on
                // the initial open) so episode switches, which don't set videoLoading, are also cleared.
                updateLoading(false);
                setEpisodeNavLoading(false);

                if (!skipBuilt) {
                    rebuildSkip();
                    skipBuilt = true;
                    maybeFetchSegmentsOnline();
                }

                updateMediaInfo();

                if (videoLoading) {
                    videoLoading = false;

                    if (mPrefs.orientation == Utils.Orientation.UNSPECIFIED) {
                        mPrefs.orientation = Utils.getNextOrientation(mPrefs.orientation);
                        Utils.setOrientation(PlayerActivity.this, mPrefs.orientation);
                    }

                    final Format format = player.getVideoFormat();

                    if (format != null) {
                        if (!isTvBox && mPrefs.orientation == Utils.Orientation.VIDEO) {
                            if (Utils.isPortrait(format)) {
                                PlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                            } else {
                                PlayerActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                            }
                            updateButtonRotation();
                        }

                        updateSubtitleViewMargin(format);
                    }

                    if (duration != C.TIME_UNSET && duration > TimeUnit.MINUTES.toMillis(20)) {
                        timeBar.setKeyTimeIncrement(TimeUnit.MINUTES.toMillis(1));
                    } else {
                        timeBar.setKeyCountIncrement(20);
                    }

                    boolean switched = false;
                    if (mPrefs.frameRateMatching) {
                        if (play) {
                            if (displayManager == null) {
                                displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
                            }
                            if (displayListener == null) {
                                displayListener = new DisplayManager.DisplayListener() {
                                    @Override
                                    public void onDisplayAdded(int displayId) {

                                    }

                                    @Override
                                    public void onDisplayRemoved(int displayId) {

                                    }

                                    @Override
                                    public void onDisplayChanged(int displayId) {
                                        if (play) {
                                            play = false;
                                            displayManager.unregisterDisplayListener(this);
                                            if (player != null) {
                                                player.play();
                                            }
                                            if (playerView != null) {
                                                playerView.hideController();
                                            }
                                        }
                                    }
                                };
                            }
                            displayManager.registerDisplayListener(displayListener, null);
                        }
                        switched = Utils.switchFrameRate(PlayerActivity.this, mPrefs.mediaUri, play);
                    }
                    if (!switched) {
                        if (displayManager != null) {
                            displayManager.unregisterDisplayListener(displayListener);
                        }
                        if (play) {
                            play = false;
                            player.play();
                            playerView.hideController();
                        }
                    }

                    if (mPrefs.speed <= 0.99f || mPrefs.speed >= 1.01f) {
                        player.setPlaybackSpeed(mPrefs.speed);
                    }
                    if (!apiAccess) {
                        setSelectedTracks(mPrefs.subtitleTrackId, mPrefs.audioTrackId);
                    }
                }
            } else if (state == Player.STATE_BUFFERING) {
                // Buffering (e.g. switching episodes) — show the spinner in place of play and disable the arrows.
                updateLoading(true);
                setEpisodeNavLoading(true);
                // (Re)arm the watchdog: if this buffering never resolves to STATE_READY, it is a stuck load.
                playerView.removeCallbacks(loadTimeoutRunnable);
                playerView.postDelayed(loadTimeoutRunnable, VIDEO_LOAD_TIMEOUT_MS);
            } else if (state == Player.STATE_ENDED) {
                cancelLoadWatchdog();
                playbackFinished = true;
                if (apiAccess) {
                    finish();
                }
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            updateLoading(false);
            cancelLoadWatchdog();
            // The Lampac stream resolver returned its handshake ({"rch":…}) instead of media: it
            // resolves the real stream by running client-side code over a WebSocket, which this player
            // does not implement, so the link can never be obtained here. Show a friendly message and
            // stop — this is an unsupported upstream flow, not an app bug, so it is not reported to Sentry.
            if (isResolverNotReadyForCurrentItem()) {
                resolverNotReadyUri = null;
                showSnack(getString(R.string.error_stream_not_ready), null);
                releasePlayer(false);
                return;
            }
            // An extensionless streaming URL (e.g. a resolver that returns HLS) gets guessed as a
            // progressive source and then fails to parse. Re-prepare it as the manifest type the
            // real response revealed before treating the source error as fatal.
            if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                    && recoverFromContainerError()) {
                return;
            }
            // A mid-playback stall: Media3's StuckPlayerDetector reports "no progress" as a
            // TYPE_UNEXPECTED error with ERROR_CODE_TIMEOUT. Not an app bug — the device decoder wedged
            // (commonly a Dolby Vision stream its DV decoder can't handle). Try a one-shot recovery that
            // re-decodes DV as plain HEVC; if not applicable / already tried, show a friendly message.
            if (error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT) {
                if (recoverFromStuckPlayback()) {
                    return;
                }
                showErrorScreen(errorSummary(error), "Stall class: device_decoder\n\n" + errorReport(error));
                // Report as device/firmware telemetry (tagged), not an app bug.
                io.sentry.Sentry.captureException(error, scope -> {
                    scope.setTag("player.error_code", error.getErrorCodeName());
                    scope.setTag("player.stall_class", "device_decoder");
                    final MediaItem item = player != null ? player.getCurrentMediaItem() : null;
                    final Uri uri = item != null && item.localConfiguration != null
                            ? item.localConfiguration.uri : mPrefs.mediaUri;
                    if (Utils.isSupportedNetworkUri(uri)) {
                        scope.setExtra("media_uri", Utils.uriToReportString(uri));
                    }
                });
                releasePlayer(false);
                return;
            }
            // Enrich via the per-capture ScopeCallback overload (not withScope) so the tag/extra land
            // on exactly this event.
            io.sentry.Sentry.captureException(error, scope -> {
                scope.setTag("player.error_code", error.getErrorCodeName());
                final MediaItem item = player != null ? player.getCurrentMediaItem() : null;
                final Uri uri = item != null && item.localConfiguration != null
                        ? item.localConfiguration.uri : mPrefs.mediaUri;
                if (Utils.isSupportedNetworkUri(uri)) {
                    scope.setExtra("media_uri", Utils.uriToReportString(uri));
                }
            });
            if (error instanceof ExoPlaybackException) {
                final ExoPlaybackException exoPlaybackException = (ExoPlaybackException) error;
                if (exoPlaybackException.type == ExoPlaybackException.TYPE_SOURCE) {
                    // A source error is fatal — surface it (message + code) before releasing, since
                    // after teardown the deferred controller-visible path can no longer fire.
                    showError(exoPlaybackException);
                    releasePlayer(false);
                    return;
                }
                if (controllerVisible && controllerVisibleFully) {
                    showError(exoPlaybackException);
                } else {
                    errorToShow = exoPlaybackException;
                }
            } else {
                // Any other playback error — surface a general message + code instead of failing
                // silently (it was already reported to Sentry above).
                showErrorScreen(errorSummary(error), errorReport(error));
            }
        }
    }

    // Re-prepare the current network item as the streaming manifest type discovered from its HTTP
    // response (or HLS as the common fallback), so an extensionless resolver URL that actually
    // serves HLS/DASH plays instead of dying on a progressive-parse error. Rebuilding the whole
    // timeline forces DefaultMediaSourceFactory to re-instantiate the source with the new type
    // (buildUpon()/replace won't, since the URI is unchanged). Guarded so a genuinely unsupported
    // stream fails once rather than looping.
    private boolean recoverFromContainerError() {
        if (player == null) {
            return false;
        }
        final int index = player.getCurrentMediaItemIndex();
        final int count = player.getMediaItemCount();
        if (index < 0 || index >= count) {
            return false;
        }
        final MediaItem currentItem = player.getMediaItemAt(index);
        if (currentItem.localConfiguration == null) {
            return false;
        }
        final Uri uri = currentItem.localConfiguration.uri;
        if (!Utils.isSupportedNetworkUri(uri)) {
            return false;
        }
        String targetMime = resolvedMediaTypes.get(uri.toString());
        if (targetMime == null) {
            targetMime = MimeTypes.APPLICATION_M3U8;
        }
        if (targetMime.equals(currentItem.localConfiguration.mimeType)) {
            return false;
        }

        final long position = player.getCurrentPosition();
        final List<MediaItem> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final MediaItem item = player.getMediaItemAt(i);
            items.add(i == index ? item.buildUpon().setMimeType(targetMime).build() : item);
        }
        player.setMediaItems(items, index, position);
        player.prepare();
        player.play();
        return true;
    }

    // One-shot recovery from a mid-playback stall (ERROR_CODE_TIMEOUT) on a Dolby Vision track: rebuild
    // the player forcing the DV track through the plain HEVC decoder (bypassing a device DV decoder that
    // wedged). The renderers factory / codec selector can only be set at construction, so a full rebuild
    // is required; position is preserved via savePlayer() and restorePlayState resumes playback. Guarded
    // per URI so a stream that stalls again after the switch fails once instead of looping. Returns true
    // when a recovery rebuild was scheduled.
    private boolean recoverFromStuckPlayback() {
        if (player == null) {
            return false;
        }
        final MediaItem item = player.getCurrentMediaItem();
        if (item == null || item.localConfiguration == null) {
            return false;
        }
        final Format videoFormat = player.getVideoFormat();
        if (videoFormat == null || !MimeTypes.VIDEO_DOLBY_VISION.equals(videoFormat.sampleMimeType)) {
            return false;
        }
        final String uri = item.localConfiguration.uri.toString();
        if (uri.equals(stuckRecoveryAttemptedUri)) {
            return false;
        }
        stuckRecoveryAttemptedUri = uri;
        forceHevcForDolbyVision = true;
        pendingStuckRecovery = true;
        restorePlayState = true;
        // Rebuild on the next loop, after this onPlayerError callback returns, so the player is not
        // released while its own listener is executing.
        playerView.post(() -> {
            releasePlayer();
            initializePlayer();
        });
        return true;
    }

    // True when the just-failed load was a Lampac resolver handshake for the item that is playing now.
    private boolean isResolverNotReadyForCurrentItem() {
        if (resolverNotReadyUri == null || player == null) {
            return false;
        }
        final MediaItem item = player.getCurrentMediaItem();
        return item != null && item.localConfiguration != null
                && resolverNotReadyUri.equals(item.localConfiguration.uri.toString());
    }

    private void enableRotation() {
        try {
            if (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0) {
                Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                restoreOrientationLock = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean useMediaStore() {
        final int targetSdkVersion = getApplicationContext().getApplicationInfo().targetSdkVersion;
        return (isTvBox && Build.VERSION.SDK_INT >= 30 && targetSdkVersion >= 30 && mPrefs.fileAccess.equals("auto")) || mPrefs.fileAccess.equals("mediastore");
    }

    private void openFile(Uri pickerInitialUri) {
        if (useMediaStore()) {
            Intent intent = new Intent(this, MediaStoreChooserActivity.class);
            startActivityForResult(intent, REQUEST_CHOOSER_VIDEO_MEDIASTORE);
        } else if ((isTvBox && mPrefs.fileAccess.equals("auto")) || mPrefs.fileAccess.equals("legacy")) {
            Utils.alternativeChooser(this, pickerInitialUri, true);
        } else {
            enableRotation();

            if (pickerInitialUri == null || Utils.isSupportedNetworkUri(pickerInitialUri)) {
                pickerInitialUri = Utils.getMoviesFolderUri();
            }

            final Intent intent = createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT, pickerInitialUri);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, Utils.supportedMimeTypesVideo);

            if (Build.VERSION.SDK_INT < 30) {
                final ComponentName systemComponentName = Utils.getSystemComponent(this, intent);
                if (systemComponentName != null) {
                    intent.setComponent(systemComponentName);
                }
            }

            safelyStartActivityForResult(intent, REQUEST_CHOOSER_VIDEO);
        }
    }

    private void loadSubtitleFile(Uri pickerInitialUri) {
        Toast.makeText(PlayerActivity.this, R.string.open_subtitles, Toast.LENGTH_SHORT).show();
        final int targetSdkVersion = getApplicationContext().getApplicationInfo().targetSdkVersion;
        if ((isTvBox && Build.VERSION.SDK_INT >= 30 && targetSdkVersion >= 30 && mPrefs.fileAccess.equals("auto")) || mPrefs.fileAccess.equals("mediastore")) {
            Intent intent = new Intent(this, MediaStoreChooserActivity.class);
            intent.putExtra(MediaStoreChooserActivity.SUBTITLES, true);
            startActivityForResult(intent, REQUEST_CHOOSER_SUBTITLE_MEDIASTORE);
        } else if ((isTvBox && mPrefs.fileAccess.equals("auto")) || mPrefs.fileAccess.equals("legacy")) {
            Utils.alternativeChooser(this, pickerInitialUri, false);
        } else {
            enableRotation();

            final Intent intent = createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT, pickerInitialUri);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            final String[] supportedMimeTypes = {
                    MimeTypes.APPLICATION_SUBRIP,
                    MimeTypes.TEXT_SSA,
                    MimeTypes.TEXT_VTT,
                    MimeTypes.APPLICATION_TTML,
                    "text/*",
                    "application/octet-stream"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes);

            if (Build.VERSION.SDK_INT < 30) {
                final ComponentName systemComponentName = Utils.getSystemComponent(this, intent);
                if (systemComponentName != null) {
                    intent.setComponent(systemComponentName);
                }
            }

            safelyStartActivityForResult(intent, REQUEST_CHOOSER_SUBTITLE);
        }
    }

    private void requestDirectoryAccess() {
        enableRotation();
        final Intent intent = createBaseFileIntent(Intent.ACTION_OPEN_DOCUMENT_TREE, Utils.getMoviesFolderUri());
        safelyStartActivityForResult(intent, REQUEST_CHOOSER_SCOPE_DIR);
    }

    private Intent createBaseFileIntent(final String action, final Uri initialUri) {
        final Intent intent = new Intent(action);

        // http://stackoverflow.com/a/31334967/1615876
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);

        if (Build.VERSION.SDK_INT >= 26 && initialUri != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }

        return intent;
    }

    void safelyStartActivityForResult(final Intent intent, final int code) {
        if (intent.resolveActivity(getPackageManager()) == null)
            showSnack(getText(R.string.error_files_missing).toString(), intent.toString());
        else
            startActivityForResult(intent, code);
    }

    private TrackGroup getTrackGroupFromFormatId(int trackType, String id) {
        if ((id == null && trackType == C.TRACK_TYPE_AUDIO ) || player == null) {
            return null;
        }
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() == trackType) {
                final TrackGroup trackGroup = group.getMediaTrackGroup();
                final Format format = trackGroup.getFormat(0);
                if (Objects.equals(id, format.id)) {
                    return trackGroup;
                }
            }
        }
        return null;
    }

    public void setSelectedTracks(final String subtitleId, final String audioId) {
        if ("#none".equals(subtitleId)) {
            if (trackSelector == null) {
                return;
            }
            trackSelector.setParameters(trackSelector.buildUponParameters().setDisabledTextTrackSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED));
        }

        TrackGroup subtitleGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_TEXT, subtitleId);
        TrackGroup audioGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_AUDIO, audioId);

        TrackSelectionParameters.Builder overridesBuilder = new TrackSelectionParameters.Builder(this);
        TrackSelectionOverride trackSelectionOverride = null;
        final List<Integer> tracks = new ArrayList<>(); tracks.add(0);
        if (subtitleGroup != null) {
            trackSelectionOverride = new TrackSelectionOverride(subtitleGroup, tracks);
            overridesBuilder.addOverride(trackSelectionOverride);
        }
        if (audioGroup != null) {
            trackSelectionOverride = new TrackSelectionOverride(audioGroup, tracks);
            overridesBuilder.addOverride(trackSelectionOverride);
        }

        if (player != null) {
            TrackSelectionParameters.Builder trackSelectionParametersBuilder = player.getTrackSelectionParameters().buildUpon();
            if (trackSelectionOverride != null) {
                trackSelectionParametersBuilder.setOverrideForType(trackSelectionOverride);
            }
            player.setTrackSelectionParameters(trackSelectionParametersBuilder.build());
        }
    }

    private boolean hasOverrideType(final int trackType) {
        TrackSelectionParameters trackSelectionParameters = player.getTrackSelectionParameters();
        for (TrackSelectionOverride override : trackSelectionParameters.overrides.values()) {
            if (override.getType() == trackType)
                return true;
        }
        return false;
    }

    public String getSelectedTrack(final int trackType) {
        if (player == null) {
            return null;
        }
        Tracks tracks = player.getCurrentTracks();

        // Disabled (e.g. selected subtitle "None" - different than default)
        if (!tracks.isTypeSelected(trackType)) {
            return "#none";
        }

        // Audio track set to "Auto"
        if (trackType == C.TRACK_TYPE_AUDIO) {
            if (!hasOverrideType(C.TRACK_TYPE_AUDIO)) {
                return null;
            }
        }

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.isSelected() && group.getType() == trackType) {
                Format format = group.getMediaTrackGroup().getFormat(0);
                return format.id;
            }
        }

        return null;
    }

    void setSubtitleTextSize() {
        setSubtitleTextSize(getResources().getConfiguration().orientation);
    }

    void setSubtitleTextSize(final int orientation) {
        // Tweak text size as fraction size doesn't work well in portrait
        final SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) {
            final float size;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale;
            } else {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                float ratio = ((float)metrics.heightPixels / (float)metrics.widthPixels);
                if (ratio < 1)
                    ratio = 1 / ratio;
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale / ratio;
            }

            subtitleView.setFractionalTextSize(size);
        }
    }

    void updateSubtitleViewMargin() {
        if (player == null) {
            return;
        }

        updateSubtitleViewMargin(player.getVideoFormat());
    }

    // Set margins to fix PGS aspect as subtitle view is outside of content frame
    void updateSubtitleViewMargin(Format format) {
        if (format == null) {
            return;
        }

        final Rational aspectVideo = Utils.getRational(format);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        final Rational aspectDisplay = new Rational(metrics.widthPixels, metrics.heightPixels);

        int marginHorizontal = 0;
        int marginVertical = 0;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (aspectDisplay.floatValue() > aspectVideo.floatValue()) {
                // Left & right bars
                int videoWidth = metrics.heightPixels / aspectVideo.getDenominator() * aspectVideo.getNumerator();
                marginHorizontal = (metrics.widthPixels - videoWidth) / 2;
            }
        }

        Utils.setViewParams(playerView.getSubtitleView(), 0, 0, 0, 0,
                marginHorizontal, marginVertical, marginHorizontal, marginVertical);
    }

    void setSubtitleTextSizePiP() {
        final SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null)
            subtitleView.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 2);
    }

    @TargetApi(26)
    boolean updatePictureInPictureActions(final int iconId, final int resTitle, final int controlType, final int requestCode) {
        try {
            final ArrayList<RemoteAction> actions = new ArrayList<>();
            final PendingIntent intent = PendingIntent.getBroadcast(PlayerActivity.this, requestCode,
                    new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType), PendingIntent.FLAG_IMMUTABLE);
            final Icon icon = Icon.createWithResource(PlayerActivity.this, iconId);
            final String title = getString(resTitle);
            actions.add(new RemoteAction(icon, title, title, intent));
            ((PictureInPictureParams.Builder) mPictureInPictureParamsBuilder).setActions(actions);
            setPictureInPictureParams(((PictureInPictureParams.Builder) mPictureInPictureParamsBuilder).build());
            return true;
        } catch (IllegalStateException e) {
            // On Samsung devices with Talkback active:
            // Caused by: java.lang.IllegalStateException: setPictureInPictureParams: Device doesn't support picture-in-picture mode.
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean isInPip() {
        if (!Utils.isPiPSupported(this))
            return false;
        return isInPictureInPictureMode();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!isInPip()) {
            setSubtitleTextSize(newConfig.orientation);
        }
        updateSubtitleViewMargin();

        updateButtonRotation();

        // Recompute adaptive metrics on resize/fold/rotation (manifest opts out of recreate for these, so
        // playback isn't interrupted). Re-run the inset pass (grid/overscan) and drop any open picker — it was
        // sized for the old width/orientation and is rebuilt fresh on next open. Density/fontScale changes are
        // NOT in configChanges, so those recreate the activity and re-apply everything via onCreate.
        final UiMetrics next = UiMetrics.of(this, isTvBox);
        if (!next.sameClassAndWidth(ui)) {
            ui = next;
            dismissOpenPickers();
            if (controlView != null) {
                controlView.requestApplyInsets();
            }
        }
    }

    private void dismissOpenPickers() {
        final android.app.Dialog[] pickers = { qualityDialog, playlistDialog, skipOffsetDialog, menuDialog };
        for (final android.app.Dialog d : pickers) {
            if (d != null && d.isShowing()) {
                d.dismiss();
            }
        }
    }

    void showError(ExoPlaybackException error) {
        // A fatal playback error: go straight to the full error screen (friendly explanation + report),
        // rather than a dismissible toast the user has to expand.
        showErrorScreen(errorSummary(error), errorReport(error));
    }

    // Short, human-facing text for the error screen's panel: the stable ExoPlayer error-code name, the
    // underlying error text, and the network URL that was being played (sanitised). No internal codes.
    private String errorSummary(PlaybackException error) {
        final StringBuilder sb = new StringBuilder(error.getErrorCodeName());
        final String message = ErrorActivity.rootMessage(error);
        if (message != null) {
            sb.append('\n').append(message);
        }
        final Uri uri = currentMediaUri();
        if (Utils.isSupportedNetworkUri(uri)) {
            sb.append("\n\n").append(Utils.uriToReportString(uri));
        }
        return sb.toString();
    }

    // Full diagnostic report — the same detail sent to Sentry: error-code name, the sanitised media
    // URI (for network media), and the complete stack trace with its "Caused by" chain.
    private String errorReport(PlaybackException error) {
        final StringBuilder sb = new StringBuilder("Error code: ").append(error.getErrorCodeName());
        final Uri uri = currentMediaUri();
        if (Utils.isSupportedNetworkUri(uri)) {
            sb.append("\nMedia: ").append(Utils.uriToReportString(uri));
        }
        return sb.append("\n\n").append(ErrorActivity.stackTrace(error)).toString();
    }

    private Uri currentMediaUri() {
        final MediaItem item = player != null ? player.getCurrentMediaItem() : null;
        return item != null && item.localConfiguration != null
                ? item.localConfiguration.uri : mPrefs.mediaUri;
    }

    void showSnack(final String textPrimary, final String textSecondary) {
        // On TV the Snackbar action button is not reachable with the D-pad, so the "Details" affordance
        // would be lost. Present the error as an AlertDialog instead — its buttons are D-pad focusable.
        if (isTvBox) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(textPrimary);
            builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
            if (textSecondary != null) {
                builder.setNeutralButton(R.string.error_details, (dialogInterface, i) -> showErrorScreen(textSecondary, textSecondary));
            }
            builder.show();
            return;
        }
        snackbar = Snackbar.make(coordinatorLayout, textPrimary, Snackbar.LENGTH_LONG);
        if (textSecondary != null) {
            snackbar.setAction(R.string.error_details, v -> showErrorScreen(textSecondary, textSecondary));
        }
        snackbar.setAnchorView(R.id.exo_bottom_bar);
        snackbar.show();
    }

    private void showErrorScreen(final String summary, final String report) {
        startActivity(new Intent(this, ErrorActivity.class)
                .putExtra(ErrorActivity.EXTRA_SUMMARY, summary)
                .putExtra(ErrorActivity.EXTRA_REPORT, report));
    }

    void reportScrubbing(long position) {
        final long diff = position - scrubbingStart;
        if (Math.abs(diff) > 1000) {
            scrubbingNoticeable = true;
        }
        if (scrubbingNoticeable) {
            playerView.clearIcon();
            playerView.setCustomErrorMessage(Utils.formatMilisSign(diff));
        }
        if (frameRendered) {
            frameRendered = false;
            if (player != null) {
                player.seekTo(position);
            }
        }
    }

    void updateSubtitleStyle(final Context context) {
        final CaptioningManager captioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        final SubtitleView subtitleView = playerView.getSubtitleView();
        final boolean isTablet = Utils.isTablet(context);
        subtitlesScale = SubtitleUtils.normalizeFontScale(captioningManager.getFontScale(), isTvBox || isTablet);
        if (subtitleView != null) {
            final CaptioningManager.CaptionStyle userStyle = captioningManager.getUserStyle();
            final CaptionStyleCompat userStyleCompat = CaptionStyleCompat.createFromCaptionStyle(userStyle);
            final CaptionStyleCompat captionStyle = new CaptionStyleCompat(
                    userStyle.hasForegroundColor() ? userStyleCompat.foregroundColor : Color.WHITE,
                    userStyle.hasBackgroundColor() ? userStyleCompat.backgroundColor : Color.TRANSPARENT,
                    userStyle.hasWindowColor() ? userStyleCompat.windowColor : Color.TRANSPARENT,
                    userStyle.hasEdgeType() ? userStyleCompat.edgeType : CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    userStyle.hasEdgeColor() ? userStyleCompat.edgeColor : Color.BLACK,
                    Typeface.create(userStyleCompat.typeface != null ? userStyleCompat.typeface : Typeface.DEFAULT,
                            mPrefs.subtitleStyleBold ? Typeface.BOLD : Typeface.NORMAL));
            subtitleView.setStyle(captionStyle);
            subtitleView.setApplyEmbeddedStyles(mPrefs.subtitleStyleEmbedded);
            subtitleView.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION * 2f / 3f);
        }
        setSubtitleTextSize();
    }

    void searchSubtitles() {
        if (mPrefs.mediaUri == null)
            return;

        if (Utils.isSupportedNetworkUri(mPrefs.mediaUri) && Utils.isProgressiveContainerUri(mPrefs.mediaUri)) {
            SubtitleUtils.clearCache(this);
            if (SubtitleFinder.isUriCompatible(mPrefs.mediaUri)) {
                subtitleFinder = new SubtitleFinder(PlayerActivity.this, mPrefs.mediaUri);
                subtitleFinder.start();
            }
            return;
        }

        if (mPrefs.scopeUri != null || isTvBox) {
            DocumentFile video = null;
            File videoRaw = null;
            final String scheme = mPrefs.mediaUri.getScheme();

            if (mPrefs.scopeUri != null) {
                if ("com.android.externalstorage.documents".equals(mPrefs.mediaUri.getHost()) ||
                        "org.courville.nova.provider".equals(mPrefs.mediaUri.getHost())) {
                    // Fast search based on path in uri
                    video = SubtitleUtils.findUriInScope(this, mPrefs.scopeUri, mPrefs.mediaUri);
                } else {
                    // Slow search based on matching metadata, no path in uri
                    // Provider "com.android.providers.media.documents" when using "Videos" tab in file picker
                    DocumentFile fileScope = DocumentFile.fromTreeUri(this, mPrefs.scopeUri);
                    DocumentFile fileMedia = DocumentFile.fromSingleUri(this, mPrefs.mediaUri);
                    video = SubtitleUtils.findDocInScope(fileScope, fileMedia);
                }
            } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                videoRaw = new File(mPrefs.mediaUri.getSchemeSpecificPart());
                video = DocumentFile.fromFile(videoRaw);
            }

            if (video != null) {
                DocumentFile subtitle = null;
                if (mPrefs.scopeUri != null) {
                    subtitle = SubtitleUtils.findSubtitle(video);
                } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                    File parentRaw = videoRaw.getParentFile();
                    DocumentFile dir = DocumentFile.fromFile(parentRaw);
                    subtitle = SubtitleUtils.findSubtitle(video, dir);
                }

                if (subtitle != null) {
                    handleSubtitles(subtitle.getUri());
                }
            }
        }
    }

    Uri findNext() {
        // TODO: Unify with searchSubtitles()
        if (mPrefs.scopeUri != null || isTvBox) {
            DocumentFile video = null;
            File videoRaw = null;

            if (!isTvBox && mPrefs.scopeUri != null) {
                if ("com.android.externalstorage.documents".equals(mPrefs.mediaUri.getHost())) {
                    // Fast search based on path in uri
                    video = SubtitleUtils.findUriInScope(this, mPrefs.scopeUri, mPrefs.mediaUri);
                } else {
                    // Slow search based on matching metadata, no path in uri
                    // Provider "com.android.providers.media.documents" when using "Videos" tab in file picker
                    DocumentFile fileScope = DocumentFile.fromTreeUri(this, mPrefs.scopeUri);
                    DocumentFile fileMedia = DocumentFile.fromSingleUri(this, mPrefs.mediaUri);
                    video = SubtitleUtils.findDocInScope(fileScope, fileMedia);
                }
            } else if (isTvBox) {
                videoRaw = new File(mPrefs.mediaUri.getSchemeSpecificPart());
                video = DocumentFile.fromFile(videoRaw);
            }

            if (video != null) {
                DocumentFile next;
                if (!isTvBox) {
                    next = SubtitleUtils.findNext(video);
                } else {
                    File parentRaw = videoRaw.getParentFile();
                    DocumentFile dir = DocumentFile.fromFile(parentRaw);
                    next = SubtitleUtils.findNext(video, dir);
                }
                if (next != null) {
                    return next.getUri();
                }
            }
        }
        return null;
    }

    void askForScope(boolean loadSubtitlesOnCancel, boolean skipToNextOnCancel) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
        builder.setMessage(String.format(getString(R.string.request_scope), getString(R.string.app_name)));
        builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> requestDirectoryAccess()
        );
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            mPrefs.markScopeAsked();
            if (loadSubtitlesOnCancel) {
                loadSubtitleFile(mPrefs.mediaUri);
            }
            if (skipToNextOnCancel) {
                nextUri = findNext();
                if (nextUri != null) {
                    skipToNext();
                }
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    void resetHideCallbacks() {
        if (haveMedia && player != null && player.isPlaying()) {
            // Keep controller UI visible - alternative to resetHideCallbacks()
            playerView.setControllerShowTimeoutMs(PlayerActivity.CONTROLLER_TIMEOUT);
        }
    }

    private void updateLoading(final boolean enableLoading) {
        if (enableLoading) {
            // INVISIBLE (not GONE): keep the 90dp slot so the row doesn't resize while the spinner shows over it.
            exoPlayPause.setVisibility(View.INVISIBLE);
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
            exoPlayPause.setVisibility(View.VISIBLE);
            if (focusPlay) {
                focusPlay = false;
                exoPlayPause.requestFocus();
            }
        }
    }

    // Shrink the built-in prev/next episode arrows (Media3 defaults render them as large as play/pause) and
    // take over their click handling so we can gate them while a video is loading. Media3 keeps updating the
    // buttons' enabled state on player events, so an OnClickListener guard — not setEnabled — is the reliable gate.
    private void setupEpisodeNavButtons() {
        // The prev/next episode arrows are a clear secondary tier below the coral Play/Pause hero: their 46dp
        // disc is ~0.66 of the hero's 70dp, so Play reads as primary rather than a near-peer. Both use the same
        // filled-white skip glyphs so they read as a symmetric pair (the default Media3 src drawables are
        // mismatched — only the "next" one carries a gradient halo).
        final int size = ui.episodeDisc();
        final int padding = ui.episodeDiscPad();
        final int margin = ui.episodeDiscMargin();
        if (exoPrev != null) {
            exoPrev.setImageResource(R.drawable.ic_skip_previous);
        }
        if (exoNext != null) {
            exoNext.setImageResource(R.drawable.ic_skip_next);
        }
        setupEpisodeNavButton(exoPrev, size, padding, margin);
        setupEpisodeNavButton(exoNext, size, padding, margin);
        if (exoPrev != null) {
            exoPrev.setOnClickListener(v -> {
                if (!episodeNavLoading && player != null && player.hasPreviousMediaItem()) {
                    player.seekToPrevious();
                    resetHideCallbacks();
                }
            });
        }
        if (exoNext != null) {
            exoNext.setOnClickListener(v -> {
                if (!episodeNavLoading && player != null && player.hasNextMediaItem()) {
                    player.seekToNext();
                    resetHideCallbacks();
                }
            });
        }
        // Media3's PlayerControlView re-enables these arrows in updateNavigation()/updateAll() — on player
        // events AND every time the controller is shown — based on command availability, which keeps "prev"
        // enabled on the first item and "next" on the last. Enforcing our state via a posted re-assert lands
        // one frame late (visible enable→disable flicker on load). Correcting it in a pre-draw pass instead
        // fixes it before the frame is drawn, so Media3's transient enable is never rendered.
        if (playerView != null) {
            playerView.getViewTreeObserver().addOnPreDrawListener(() -> {
                updateEpisodeNavButtons();
                return true;
            });
        }
    }

    private void setupEpisodeNavButton(final ImageButton button, final int size, final int padding, final int margin) {
        if (button == null) {
            return;
        }
        final ViewGroup.LayoutParams lp = button.getLayoutParams();
        lp.width = size;
        lp.height = size;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMarginStart(margin);
            ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(margin);
        }
        button.setLayoutParams(lp);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        // Neutral chrome disc echoing the coral Play/Pause hero: same pill fill (@color/ui_controls_background),
        // circular to suit the round glyphs. The 12dp icon padding leaves a ring matching the hero's proportion.
        final GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(ContextCompat.getColor(this, R.color.ui_controls_background));
        button.setBackground(disc);
        button.setClipToOutline(true);
        // Replacing the background drops the D-pad focus / touch-press highlight, so re-add it as a foreground
        // ripple on top of the disc — critical for TV navigation, harmless on touch.
        final TypedValue highlight = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, highlight, true)
                && highlight.resourceId != 0) {
            button.setForeground(ContextCompat.getDrawable(this, highlight.resourceId));
        }
    }

    // Grey out and disable the prev/next episode arrows while a video is loading, using the same disabled
    // styling (enabled state + opacity) as the other control buttons via Utils.setButtonEnabled.
    private void setEpisodeNavLoading(final boolean loading) {
        episodeNavLoading = loading;
        updateEpisodeNavButtons();
    }

    // Enable each episode arrow only when that direction exists in the playlist: "prev" is disabled on the
    // first item, "next" on the last. Both are additionally disabled while a switch is loading. Idempotent
    // (only writes on an actual change) so the per-draw enforcer below can call it every frame cheaply.
    private void updateEpisodeNavButtons() {
        final boolean canPrev = !episodeNavLoading && player != null && player.hasPreviousMediaItem();
        final boolean canNext = !episodeNavLoading && player != null && player.hasNextMediaItem();
        if (exoPrev != null && exoPrev.isEnabled() != canPrev) {
            Utils.setButtonEnabled(this, exoPrev, canPrev);
        }
        if (exoNext != null && exoNext.isEnabled() != canNext) {
            Utils.setButtonEnabled(this, exoNext, canNext);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onUserLeaveHint() {
        if (mPrefs!= null && mPrefs.autoPiP && player != null && player.isPlaying() && Utils.isPiPSupported(this))
            enterPiP();
        else
            super.onUserLeaveHint();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPiP() {
        final AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (AppOpsManager.MODE_ALLOWED != appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), getPackageName())) {
            final Intent intent = new Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.fromParts("package", getPackageName(), null));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
            return;
        }

        if (player == null) {
            return;
        }

        playerView.setControllerAutoShow(false);
        playerView.hideController();

        final Format format = player.getVideoFormat();

        if (format != null) {
            // https://github.com/google/ExoPlayer/issues/8611
            // TODO: Test/disable on Android 11+
            final View videoSurfaceView = playerView.getVideoSurfaceView();
            if (videoSurfaceView instanceof SurfaceView) {
                ((SurfaceView)videoSurfaceView).getHolder().setFixedSize(format.width, format.height);
            }

            Rational rational = Utils.getRational(format);
            if (Build.VERSION.SDK_INT >= 33 &&
                    getPackageManager().hasSystemFeature(FEATURE_EXPANDED_PICTURE_IN_PICTURE) &&
                    (rational.floatValue() > rationalLimitWide.floatValue() || rational.floatValue() < rationalLimitTall.floatValue())) {
                ((PictureInPictureParams.Builder)mPictureInPictureParamsBuilder).setExpandedAspectRatio(rational);
            }
            if (rational.floatValue() > rationalLimitWide.floatValue())
                rational = rationalLimitWide;
            else if (rational.floatValue() < rationalLimitTall.floatValue())
                rational = rationalLimitTall;

            ((PictureInPictureParams.Builder)mPictureInPictureParamsBuilder).setAspectRatio(rational);
        }
        enterPictureInPictureMode(((PictureInPictureParams.Builder)mPictureInPictureParamsBuilder).build());
    }

    void setEndControlsVisible(boolean visible) {
        final boolean hasPlaylist = player != null && player.getMediaItemCount() > 1;
        final int deleteVisible = (visible && haveMedia && Utils.isDeletable(this, mPrefs.mediaUri)) ? View.VISIBLE : View.INVISIBLE;
        final int nextVisible = (visible && haveMedia && !hasPlaylist && (nextUri != null || (mPrefs.askScope && !isTvBox))) ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.delete).setVisibility(deleteVisible);
        findViewById(R.id.next).setVisibility(nextVisible);
    }

    void askDeleteMedia() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
        builder.setMessage(getString(R.string.delete_query));
        builder.setPositiveButton(R.string.delete_confirmation, (dialogInterface, i) -> {
            releasePlayer();
            deleteMedia();
            if (nextUri == null) {
                haveMedia = false;
                setEndControlsVisible(false);
                playerView.setControllerShowTimeoutMs(-1);
                showEmptyState();
            } else {
                skipToNext();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {});
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    void deleteMedia() {
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(mPrefs.mediaUri.getScheme())) {
                DocumentsContract.deleteDocument(getContentResolver(), mPrefs.mediaUri);
            } else if (ContentResolver.SCHEME_FILE.equals(mPrefs.mediaUri.getScheme())) {
                final File file = new File(mPrefs.mediaUri.getSchemeSpecificPart());
                if (file.canWrite()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dispatchPlayPause() {
        if (player == null)
            return;

        @Player.State int state = player.getPlaybackState();
        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED || !player.getPlayWhenReady()) {
            shortControllerTimeout = true;
            androidx.media3.common.util.Util.handlePlayButtonAction(player);
        } else {
            androidx.media3.common.util.Util.handlePauseButtonAction(player);
        }
    }

    void skipToNext() {
        if (nextUri != null) {
            releasePlayer();
            mPrefs.updateMedia(this, nextUri, null);
            searchSubtitles();
            initializePlayer();
        }
    }

    void notifyAudioSessionUpdate(final boolean active) {
        final Intent intent = new Intent(active ? AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
                : AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        if (active) {
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE);
        }
        try {
            sendBroadcast(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    void updateButtons(final boolean enable) {
        if (buttonPiP != null) {
            Utils.setButtonEnabled(this, buttonPiP, enable);
        }
        Utils.setButtonEnabled(this, buttonAspectRatio, enable);
        if (isTvBox) {
            Utils.setButtonEnabled(this, exoSettings, true);
        } else {
            Utils.setButtonEnabled(this, exoSettings, enable);
        }
    }

    private void scaleStart() {
        isScaling = true;
        if (playerView.getResizeMode() != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        }
        scaleFactor = playerView.getVideoSurfaceView().getScaleX();
        playerView.removeCallbacks(playerView.textClearRunnable);
        playerView.clearIcon();
        playerView.setCustomErrorMessage((int)(scaleFactor * 100) + "%");
        playerView.hideController();
        isScaleStarting = true;
    }

    private void scale(boolean up) {
        if (up) {
            scaleFactor += 0.01;
        } else {
            scaleFactor -= 0.01;
        }
        scaleFactor = Utils.normalizeScaleFactor(scaleFactor, playerView.getScaleFit());
        playerView.setScale(scaleFactor);
        playerView.setCustomErrorMessage((int)(scaleFactor * 100) + "%");
    }

    private void scaleEnd() {
        isScaling = false;
        playerView.postDelayed(playerView.textClearRunnable, 200);
        if (player != null && !player.isPlaying()) {
            playerView.showController();
        }
        if (Math.abs(playerView.getScaleFit() - scaleFactor) < 0.01 / 2) {
            playerView.setScale(1.f);
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }
        updatebuttonAspectRatioIcon();
    }

    // A scale mode = a Media3 resize mode plus an optional forced display aspect ratio (ratio 0 = natural).
    // Indices 0..2 (Fit/Crop/Fill) are the tap cycle; the rest (forced ratios) are picker-only, like VLC.
    private static final class AspectMode {
        final int resizeMode;
        final float ratio;
        final String label;
        AspectMode(int resizeMode, float ratio, String label) {
            this.resizeMode = resizeMode;
            this.ratio = ratio;
            this.label = label;
        }
    }

    private List<AspectMode> getAspectModes() {
        if (aspectModes == null) {
            aspectModes = new ArrayList<>();
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 0f, getString(R.string.video_resize_fit)));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, 0f, getString(R.string.video_resize_crop)));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FILL, 0f, getString(R.string.video_resize_fill)));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 16f / 9f, "16:9"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 4f / 3f, "4:3"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 16f / 10f, "16:10"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 2f / 1f, "2:1"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 2.35f, "2.35:1"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 2.39f, "2.39:1"));
            aspectModes.add(new AspectMode(AspectRatioFrameLayout.RESIZE_MODE_FIT, 5f / 4f, "5:4"));
        }
        return aspectModes;
    }

    private void applyAspectMode(int index) {
        final AspectMode mode = getAspectModes().get(index);
        currentAspectRatio = mode.ratio;
        playerView.applyAspectMode(mode.resizeMode, mode.ratio);
        Utils.showText(playerView, mode.label);
        updatebuttonAspectRatioIcon();
    }

    // Tap: cycle Fit → Crop → Fill → 16:9 → 4:3. From any other picker-only ratio, a tap returns to Fit.
    private static final int ASPECT_CYCLE_COUNT = 5;

    private void cycleAspectMode() {
        final List<AspectMode> modes = getAspectModes();
        int current = -1;
        for (int i = 0; i < ASPECT_CYCLE_COUNT; i++) {
            if (isCurrentAspectMode(modes.get(i))) {
                current = i;
                break;
            }
        }
        applyAspectMode((current + 1) % ASPECT_CYCLE_COUNT);
    }

    private void showAspectModePicker() {
        final List<AspectMode> modes = getAspectModes();
        final String[] labels = new String[modes.size()];
        int checked = -1;
        for (int i = 0; i < modes.size(); i++) {
            labels[i] = modes.get(i).label;
            if (isCurrentAspectMode(modes.get(i)))
                checked = i;
        }
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.button_crop)
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    applyAspectMode(which);
                    d.dismiss();
                })
                .create();
        showPickerDialog(dialog);
    }

    private boolean isCurrentAspectMode(AspectMode mode) {
        if (mode.ratio > 0)
            return Math.abs(mode.ratio - currentAspectRatio) < 0.001f;
        return currentAspectRatio == 0 && playerView.getResizeMode() == mode.resizeMode;
    }

    public void setSpeedBoostIndicatorVisible(boolean visible) {
        if (speedBoostIndicator != null)
            speedBoostIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    // Arms the pause auto-hide when the controller is fully visible and playback is paused (ready, not
    // scrubbing/locked/in a picker). Called from the visibility listener and on play/pause transitions.
    private void scheduleHideControllerOnPause() {
        if (playerView == null)
            return;
        playerView.removeCallbacks(hideControllerAction);
        if (controllerVisibleFully && haveMedia && player != null
                && player.getPlaybackState() == Player.STATE_READY && !player.getPlayWhenReady()
                && !locked && !pickerDialogOpen && !isScrubbing) {
            playerView.postDelayed(hideControllerAction, CONTROLLER_TIMEOUT);
        }
    }

    private void updatebuttonAspectRatioIcon() {
        if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
            buttonAspectRatio.setImageResource(R.drawable.ic_fit_screen_24dp);
        } else {
            buttonAspectRatio.setImageResource(R.drawable.ic_aspect_ratio_24dp);
        }
    }

    private void updateButtonRotation() {
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean auto = false;
        try {
            auto = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (mPrefs.orientation == Utils.Orientation.VIDEO) {
            if (auto) {
                buttonRotation.setImageResource(R.drawable.ic_screen_lock_rotation_24dp);
            } else if (portrait) {
                buttonRotation.setImageResource(R.drawable.ic_screen_lock_portrait_24dp);
            } else {
                buttonRotation.setImageResource(R.drawable.ic_screen_lock_landscape_24dp);
            }
        } else {
            if (auto) {
                buttonRotation.setImageResource(R.drawable.ic_screen_rotation_24dp);
            } else if (portrait) {
                buttonRotation.setImageResource(R.drawable.ic_screen_portrait_24dp);
            } else {
                buttonRotation.setImageResource(R.drawable.ic_screen_landscape_24dp);
            }
        }
    }
}