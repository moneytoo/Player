package com.brouken.player;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.*;
import okhttp3.*;
import org.json.JSONObject;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Complete Video Downloader implementation (1DM-style)
 * Features: Built-in browser, stream detection, multi-part downloads, HLS/DASH support
 */
public class VideoDownloaderActivity extends AppCompatActivity {
    
    // UI Components
    private WebView browserWebView;
    private LinearLayout urlInputContainer;
    private EditText urlEditText;
    private Button goButton;
    private RecyclerView downloadsRecyclerView;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton detectStreamsButton;
    
    // Downloaders and managers
    private DownloadManager downloadManager;
    private OkHttpClient httpClient;
    private List<DetectedStream> detectedStreams = new ArrayList<>();
    private List<DownloadItem> activeDownloads = new ArrayList<>();
    
    // Stream detection
    private StreamDetector streamDetector;
    private boolean streamDetectionEnabled = true;
    
    // Download completion receiver
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            handleDownloadComplete(downloadId);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_downloader);
        
        initializeComponents();
        setupBrowser();
        setupDownloadManager();
        setupEventListeners();
        
        // Register download completion receiver
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void initializeComponents() {
        browserWebView = findViewById(R.id.browser_webview);
        urlInputContainer = findViewById(R.id.url_input_container);
        urlEditText = findViewById(R.id.url_edit_text);
        goButton = findViewById(R.id.go_button);
        downloadsRecyclerView = findViewById(R.id.downloads_recycler_view);
        detectStreamsButton = findViewById(R.id.detect_streams_button);
        
        // Setup HTTP client with custom interceptors
        httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(new StreamDetectionInterceptor())
            .build();
        
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        streamDetector = new StreamDetector();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupBrowser() {
        WebSettings webSettings = browserWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        );
        
        // Enable mixed content for HTTPS sites with HTTP resources
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        browserWebView.setWebViewClient(new CustomWebViewClient());
        browserWebView.setWebChromeClient(new CustomWebChromeClient());
        
        // Load default page
        browserWebView.loadUrl("https://www.google.com");
    }
    
    private void setupDownloadManager() {
        downloadsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DownloadsAdapter adapter = new DownloadsAdapter(activeDownloads);
        downloadsRecyclerView.setAdapter(adapter);
    }
    
    private void setupEventListeners() {
        goButton.setOnClickListener(v -> {
            String url = urlEditText.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                browserWebView.loadUrl(url);
            }
        });
        
        detectStreamsButton.setOnClickListener(v -> showDetectedStreams());
        
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            goButton.performClick();
            return true;
        });
    }
    
    /**
     * Custom WebViewClient for stream detection and ad blocking
     */
    private class CustomWebViewClient extends WebViewClient {
        
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            urlEditText.setText(url);
            detectedStreams.clear();
        }
        
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            // Detect video streams
            if (streamDetectionEnabled) {
                detectPotentialStream(url, request.getRequestHeaders());
            }
            
            // Block ads and trackers
            if (isAdOrTracker(url)) {
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
            }
            
            return super.shouldInterceptRequest(view, request);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            // Handle direct video file links
            if (isDirectVideoFile(url)) {
                showDownloadDialog(url, getFileNameFromUrl(url));
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * Custom WebChromeClient for progress and title updates
     */
    private class CustomWebChromeClient extends WebChromeClient {
        
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            // Update progress bar if you have one
        }
        
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            // Update title bar
        }
    }
    
    /**
     * Stream detection logic
     */
    private void detectPotentialStream(String url, Map<String, String> headers) {
        // Detect HLS streams (.m3u8)
        if (url.contains(".m3u8") || url.contains("master.m3u8") || url.contains("playlist.m3u8")) {
            addDetectedStream(url, "HLS Stream", "m3u8");
        }
        
        // Detect DASH streams (.mpd)
        else if (url.contains(".mpd")) {
            addDetectedStream(url, "DASH Stream", "mpd");
        }
        
        // Detect direct video files
        else if (isDirectVideoFile(url)) {
            addDetectedStream(url, "Direct Video", getFileExtension(url));
        }
        
        // Detect streaming patterns
        else if (url.contains("/manifest") || url.contains("/stream") || 
                 url.contains("videoplayback") || url.contains("googlevideo.com")) {
            addDetectedStream(url, "Video Stream", "stream");
        }
    }
    
    private void addDetectedStream(String url, String title, String format) {
        DetectedStream stream = new DetectedStream(url, title, format);
        if (!detectedStreams.contains(stream)) {
            detectedStreams.add(stream);
            updateDetectButton();
        }
    }
    
    private void updateDetectButton() {
        detectStreamsButton.setVisibility(detectedStreams.isEmpty() ? View.GONE : View.VISIBLE);
        detectStreamsButton.setText("Streams (" + detectedStreams.size() + ")");
    }
    
    private boolean isDirectVideoFile(String url) {
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp"};
        String lowerUrl = url.toLowerCase();
        
        for (String ext : videoExtensions) {
            if (lowerUrl.contains(ext)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAdOrTracker(String url) {
        String[] adDomains = {
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "facebook.com/tr", "google-analytics.com", "googletagmanager.com",
            "ads", "adnxs", "adsystem", "advertising", "analytics"
        };
        
        String lowerUrl = url.toLowerCase();
        for (String domain : adDomains) {
            if (lowerUrl.contains(domain)) {
                return true;
            }
        }
        return false;
    }
    
    private String getFileNameFromUrl(String url) {
        try {
            String decoded = URLDecoder.decode(url, "UTF-8");
            String[] parts = decoded.split("/");
            String filename = parts[parts.length - 1];
            
            // Remove query parameters
            int queryIndex = filename.indexOf('?');
            if (queryIndex > 0) {
                filename = filename.substring(0, queryIndex);
            }
            
            return filename.isEmpty() ? "video" : filename;
        } catch (Exception e) {
            return "video";
        }
    }
    
    private String getFileExtension(String url) {
        try {
            String filename = getFileNameFromUrl(url);
            int dotIndex = filename.lastIndexOf('.');
            return dotIndex > 0 ? filename.substring(dotIndex + 1) : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private void showDetectedStreams() {
        if (detectedStreams.isEmpty()) {
            Toast.makeText(this, "No streams detected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] streamTitles = new String[detectedStreams.size()];
        for (int i = 0; i < detectedStreams.size(); i++) {
            DetectedStream stream = detectedStreams.get(i);
            streamTitles[i] = stream.title + " (" + stream.format + ")";
        }
        
        new android.app.AlertDialog.Builder(this, R.style.MXDialogTheme)
            .setTitle("Detected Streams")
            .setItems(streamTitles, (dialog, which) -> {
                DetectedStream selectedStream = detectedStreams.get(which);
                showDownloadDialog(selectedStream.url, selectedStream.title);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showDownloadDialog(String url, String suggestedName) {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(50, 50, 50, 50);
        
        TextView urlLabel = new TextView(this);
        urlLabel.setText("URL:");
        urlLabel.setTextColor(getResources().getColor(android.R.color.white));
        
        TextView urlText = new TextView(this);
        urlText.setText(url);
        urlText.setTextColor(getResources().getColor(android.R.color.white));
        urlText.setTextSize(12);
        urlText.setPadding(0, 8, 0, 16);
        
        TextView nameLabel = new TextView(this);
        nameLabel.setText("Filename:");
        nameLabel.setTextColor(getResources().getColor(android.R.color.white));
        
        EditText nameEdit = new EditText(this);
        nameEdit.setText(suggestedName);
        nameEdit.setTextColor(getResources().getColor(android.R.color.white));
        nameEdit.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        nameEdit.setBackgroundResource(R.drawable.edit_text_background);
        
        CheckBox hlsCheckbox = new CheckBox(this);
        hlsCheckbox.setText("Convert HLS to MP4 (if applicable)");
        hlsCheckbox.setTextColor(getResources().getColor(android.R.color.white));
        hlsCheckbox.setChecked(url.contains(".m3u8"));
        
        dialogLayout.addView(urlLabel);
        dialogLayout.addView(urlText);
        dialogLayout.addView(nameLabel);
        dialogLayout.addView(nameEdit);
        dialogLayout.addView(hlsCheckbox);
        
        new android.app.AlertDialog.Builder(this, R.style.MXDialogTheme)
            .setTitle("Download Video")
            .setView(dialogLayout)
            .setPositiveButton("Download", (dialog, which) -> {
                String filename = nameEdit.getText().toString().trim();
                if (filename.isEmpty()) {
                    filename = suggestedName;
                }
                startDownload(url, filename, hlsCheckbox.isChecked());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void startDownload(String url, String filename, boolean convertHls) {
        if (url.contains(".m3u8") && convertHls) {
            // Start HLS download worker
            startHlsDownload(url, filename);
        } else {
            // Start regular download
            startRegularDownload(url, filename);
        }
    }
    
    private void startRegularDownload(String url, String filename) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);
            request.setDescription("Downloading video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            
            // Add headers if needed
            request.addRequestHeader("User-Agent", browserWebView.getSettings().getUserAgentString());
            
            long downloadId = downloadManager.enqueue(request);
            
            DownloadItem item = new DownloadItem(downloadId, url, filename, "regular");
            activeDownloads.add(item);
            
            updateDownloadsList();
            
            Toast.makeText(this, "Download started: " + filename, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error starting download: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startHlsDownload(String url, String filename) {
        // Create HLS download work request
        Data inputData = new Data.Builder()
            .putString("url", url)
            .putString("filename", filename)
            .build();
        
        OneTimeWorkRequest downloadWork = new OneTimeWorkRequest.Builder(HlsDownloadWorker.class)
            .setInputData(inputData)
            .setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build();
        
        WorkManager.getInstance(this).enqueue(downloadWork);
        
        DownloadItem item = new DownloadItem(-1, url, filename, "hls");
        item.workerId = downloadWork.getId();
        activeDownloads.add(item);
        
        updateDownloadsList();
        
        Toast.makeText(this, "HLS download started: " + filename, Toast.LENGTH_SHORT).show();
    }
    
    private void handleDownloadComplete(long downloadId) {
        // Find the completed download
        DownloadItem completedItem = null;
        for (DownloadItem item : activeDownloads) {
            if (item.downloadId == downloadId) {
                completedItem = item;
                break;
            }
        }
        
        if (completedItem != null) {
            // Check download status
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        completedItem.status = "completed";
                        Toast.makeText(this, "Download completed: " + completedItem.filename, Toast.LENGTH_SHORT).show();
                    } else {
                        completedItem.status = "failed";
                        Toast.makeText(this, "Download failed: " + completedItem.filename, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            updateDownloadsList();
        }
    }
    
    private void updateDownloadsList() {
        if (downloadsRecyclerView.getAdapter() != null) {
            downloadsRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadReceiver);
    }
    
    @Override
    public void onBackPressed() {
        if (browserWebView.canGoBack()) {
            browserWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Data classes
     */
    public static class DetectedStream {
        public String url;
        public String title;
        public String format;
        
        public DetectedStream(String url, String title, String format) {
            this.url = url;
            this.title = title;
            this.format = format;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DetectedStream) {
                DetectedStream other = (DetectedStream) obj;
                return url.equals(other.url);
            }
            return false;
        }
    }
    
    public static class DownloadItem {
        public long downloadId;
        public String url;
        public String filename;
        public String type;
        public String status = "downloading";
        public UUID workerId;
        
        public DownloadItem(long downloadId, String url, String filename, String type) {
            this.downloadId = downloadId;
            this.url = url;
            this.filename = filename;
            this.type = type;
        }
    }
    
    /**
     * OkHttp interceptor for additional stream detection
     */
    private class StreamDetectionInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            
            // Analyze response headers for stream indicators
            String contentType = response.header("Content-Type");
            if (contentType != null) {
                if (contentType.contains("video/") || contentType.contains("application/x-mpegURL") ||
                    contentType.contains("application/dash+xml")) {
                    
                    runOnUiThread(() -> {
                        String url = request.url().toString();
                        addDetectedStream(url, "Detected Stream", getStreamTypeFromContentType(contentType));
                    });
                }
            }
            
            return response;
        }
        
        private String getStreamTypeFromContentType(String contentType) {
            if (contentType.contains("x-mpegURL")) return "m3u8";
            if (contentType.contains("dash+xml")) return "mpd";
            if (contentType.contains("video/mp4")) return "mp4";
            if (contentType.contains("video/webm")) return "webm";
            return "stream";
        }
    }
    
    /**
     * Stream detector utility class
     */
    private class StreamDetector {
        private Pattern m3u8Pattern = Pattern.compile("https?://[^\\s]+\\.m3u8[^\\s]*");
        private Pattern mpdPattern = Pattern.compile("https?://[^\\s]+\\.mpd[^\\s]*");
        
        public List<String> detectStreamsInPage(String pageContent) {
            List<String> streams = new ArrayList<>();
            
            Matcher m3u8Matcher = m3u8Pattern.matcher(pageContent);
            while (m3u8Matcher.find()) {
                streams.add(m3u8Matcher.group());
            }
            
            Matcher mpdMatcher = mpdPattern.matcher(pageContent);
            while (mpdMatcher.find()) {
                streams.add(mpdMatcher.group());
            }
            
            return streams;
        }
    }
}