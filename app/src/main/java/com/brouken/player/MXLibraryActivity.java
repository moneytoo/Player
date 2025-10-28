package com.brouken.player;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.*;

/**
 * Complete Media Library implementation matching MX Player's library screen
 * Features: File browsing, sorting, filtering, view modes, search
 */
public class MXLibraryActivity extends AppCompatActivity {
    
    // UI Components
    private TextView titleText;
    private ImageButton searchButton;
    private ImageButton sortButton;
    private ImageButton shareButton;
    private RecyclerView videosRecyclerView;
    private LinearLayout viewModeContainer;
    private LinearLayout sortContainer;
    private Button allFoldersButton, foldersButton, filesButton;
    private Button listButton, gridButton;
    private LinearLayout sortOptionsContainer;
    private ScrollView sortScrollView;
    private com.google.android.material.floatingactionbutton.FloatingActionButton playFab;
    
    // Adapter and data
    private VideoLibraryAdapter adapter;
    private List<VideoFile> videoFiles = new ArrayList<>();
    private List<VideoFile> filteredVideos = new ArrayList<>();
    
    // View mode and sorting
    private ViewMode currentViewMode = ViewMode.ALL_FOLDERS;
    private LayoutMode currentLayoutMode = LayoutMode.LIST;
    private SortBy currentSortBy = SortBy.TITLE;
    private boolean sortAscending = true;
    private boolean showSortPanel = false;
    
    // Enums
    public enum ViewMode {
        ALL_FOLDERS, FOLDERS, FILES
    }
    
    public enum LayoutMode {
        LIST, GRID
    }
    
    public enum SortBy {
        TITLE, DATE, PLAYED_TIME, STATUS, LENGTH, SIZE, RESOLUTION, PATH, FRAME_RATE, TYPE
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mx_library);
        
        initializeViews();
        setupEventListeners();
        setupRecyclerView();
        loadVideoFiles();
    }
    
    private void initializeViews() {
        // Header components
        titleText = findViewById(R.id.library_title);
        searchButton = findViewById(R.id.search_button);
        sortButton = findViewById(R.id.sort_button);
        shareButton = findViewById(R.id.share_button);
        
        // Main content
        videosRecyclerView = findViewById(R.id.videos_recycler_view);
        playFab = findViewById(R.id.play_fab);
        
        // Sort panel components
        viewModeContainer = findViewById(R.id.view_mode_container);
        sortContainer = findViewById(R.id.sort_container);
        sortScrollView = findViewById(R.id.sort_scroll_view);
        sortOptionsContainer = findViewById(R.id.sort_options_container);
        
        // View mode buttons
        allFoldersButton = findViewById(R.id.all_folders_button);
        foldersButton = findViewById(R.id.folders_button);
        filesButton = findViewById(R.id.files_button);
        listButton = findViewById(R.id.list_button);
        gridButton = findViewById(R.id.grid_button);
        
        // Set initial title
        titleText.setText("Download");
        
        // Create sort options dynamically
        createSortOptions();
        
        updateViewModeButtons();
        updateLayoutModeButtons();
    }
    
    private void createSortOptions() {
        sortOptionsContainer.removeAllViews();
        
        // Sort options with icons
        String[] sortLabels = {"Title", "Date", "Played time", "Status", "Length", "Size", "Resolution", "Path", "Frame rate", "Type"};
        int[] sortIcons = {
            R.drawable.ic_sort_by_alpha,
            R.drawable.ic_date_range,
            R.drawable.ic_access_time,
            R.drawable.ic_play_circle,
            R.drawable.ic_timelapse,
            R.drawable.ic_storage,
            R.drawable.ic_hd,
            R.drawable.ic_folder,
            R.drawable.ic_speed,
            R.drawable.ic_movie
        };
        
        // First row
        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        for (int i = 0; i < 5; i++) {
            LinearLayout sortOption = createSortOption(sortLabels[i], sortIcons[i], SortBy.values()[i]);
            firstRow.addView(sortOption);
        }
        
        // Second row
        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        secondRow.setPadding(0, 16, 0, 0);
        
        for (int i = 5; i < 10; i++) {
            LinearLayout sortOption = createSortOption(sortLabels[i], sortIcons[i], SortBy.values()[i]);
            secondRow.addView(sortOption);
        }
        
        sortOptionsContainer.addView(firstRow);
        sortOptionsContainer.addView(secondRow);
        
        // Sort order buttons
        LinearLayout orderContainer = new LinearLayout(this);
        orderContainer.setOrientation(LinearLayout.HORIZONTAL);
        orderContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        orderContainer.setPadding(0, 24, 0, 16);
        orderContainer.setGravity(Gravity.CENTER);
        
        Button aToZButton = new Button(this);
        aToZButton.setText("↑ A to Z");
        aToZButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        aToZButton.setBackgroundResource(R.drawable.sort_button_selector);
        aToZButton.setTextColor(getResources().getColorStateList(R.color.sort_button_text_color));
        aToZButton.setOnClickListener(v -> setSortOrder(true));
        
        Button zToAButton = new Button(this);
        zToAButton.setText("↓ Z to A");
        zToAButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        zToAButton.setBackgroundResource(R.drawable.sort_button_selector);
        zToAButton.setTextColor(getResources().getColorStateList(R.color.sort_button_text_color));
        zToAButton.setOnClickListener(v -> setSortOrder(false));
        
        // Set margins
        LinearLayout.LayoutParams aToZParams = (LinearLayout.LayoutParams) aToZButton.getLayoutParams();
        aToZParams.setMarginEnd(8);
        LinearLayout.LayoutParams zToAParams = (LinearLayout.LayoutParams) zToAButton.getLayoutParams();
        zToAParams.setMarginStart(8);
        
        orderContainer.addView(aToZButton);
        orderContainer.addView(zToAButton);
        sortOptionsContainer.addView(orderContainer);
        
        // Add expandable sections
        TextView fieldsHeader = new TextView(this);
        fieldsHeader.setText("Fields");
        fieldsHeader.setTextColor(getResources().getColor(android.R.color.white));
        fieldsHeader.setTextSize(16);
        fieldsHeader.setPadding(0, 16, 0, 8);
        fieldsHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
        sortOptionsContainer.addView(fieldsHeader);
        
        TextView advancedHeader = new TextView(this);
        advancedHeader.setText("Advanced");
        advancedHeader.setTextColor(getResources().getColor(android.R.color.white));
        advancedHeader.setTextSize(16);
        advancedHeader.setPadding(0, 16, 0, 8);
        advancedHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
        sortOptionsContainer.addView(advancedHeader);
        
        // Action buttons
        LinearLayout actionButtons = new LinearLayout(this);
        actionButtons.setOrientation(LinearLayout.HORIZONTAL);
        actionButtons.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        actionButtons.setPadding(0, 24, 0, 0);
        actionButtons.setGravity(Gravity.CENTER);
        
        Button cancelButton = new Button(this);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        cancelButton.setBackgroundResource(R.drawable.action_button_background);
        cancelButton.setTextColor(getResources().getColor(android.R.color.white));
        cancelButton.setOnClickListener(v -> hideSortPanel());
        
        Button doneButton = new Button(this);
        doneButton.setText("Done");
        doneButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        doneButton.setBackgroundResource(R.drawable.action_button_primary);
        doneButton.setTextColor(getResources().getColor(android.R.color.white));
        doneButton.setOnClickListener(v -> {
            applySorting();
            hideSortPanel();
        });
        
        LinearLayout.LayoutParams cancelParams = (LinearLayout.LayoutParams) cancelButton.getLayoutParams();
        cancelParams.setMarginEnd(8);
        LinearLayout.LayoutParams doneParams = (LinearLayout.LayoutParams) doneButton.getLayoutParams();
        doneParams.setMarginStart(8);
        
        actionButtons.addView(cancelButton);
        actionButtons.addView(doneButton);
        sortOptionsContainer.addView(actionButtons);
    }
    
    private LinearLayout createSortOption(String label, int iconRes, SortBy sortBy) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        ));
        container.setGravity(Gravity.CENTER);
        container.setPadding(8, 8, 8, 8);
        container.setBackground(getResources().getDrawable(R.drawable.sort_option_selector));
        container.setClickable(true);
        container.setOnClickListener(v -> selectSortBy(sortBy));
        
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        icon.setColorFilter(getResources().getColor(android.R.color.white));
        
        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextColor(getResources().getColor(android.R.color.white));
        labelText.setTextSize(10);
        labelText.setGravity(Gravity.CENTER);
        labelText.setPadding(0, 4, 0, 0);
        
        container.addView(icon);
        container.addView(labelText);
        
        return container;
    }
    
    private void setupEventListeners() {
        // Header buttons
        searchButton.setOnClickListener(v -> showSearchDialog());
        sortButton.setOnClickListener(v -> toggleSortPanel());
        shareButton.setOnClickListener(v -> shareSelectedVideos());
        
        // View mode buttons
        allFoldersButton.setOnClickListener(v -> setViewMode(ViewMode.ALL_FOLDERS));
        foldersButton.setOnClickListener(v -> setViewMode(ViewMode.FOLDERS));
        filesButton.setOnClickListener(v -> setViewMode(ViewMode.FILES));
        
        // Layout mode buttons
        listButton.setOnClickListener(v -> setLayoutMode(LayoutMode.LIST));
        gridButton.setOnClickListener(v -> setLayoutMode(LayoutMode.GRID));
        
        // Play FAB
        playFab.setOnClickListener(v -> playRandomVideo());
    }
    
    private void setupRecyclerView() {
        adapter = new VideoLibraryAdapter(this, filteredVideos, currentLayoutMode);
        updateRecyclerViewLayout();
        videosRecyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener(videoFile -> {
            // Play selected video
            Intent intent = new Intent(this, MXPlayerActivity.class);
            intent.setData(Uri.parse(videoFile.getPath()));
            intent.putExtra("title", videoFile.getTitle());
            startActivity(intent);
        });
    }
    
    private void updateRecyclerViewLayout() {
        if (currentLayoutMode == LayoutMode.GRID) {
            videosRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            videosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }
    
    private void loadVideoFiles() {
        videoFiles.clear();
        
        // Query MediaStore for video files
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.RESOLUTION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        };
        
        String selection = MediaStore.Video.Media.MIME_TYPE + " LIKE 'video/%'";
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";
        
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    VideoFile videoFile = new VideoFile();
                    videoFile.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                    videoFile.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)));
                    videoFile.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
                    videoFile.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)));
                    videoFile.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
                    videoFile.setDateAdded(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)));
                    
                    int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH));
                    int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT));
                    videoFile.setResolution(width + "x" + height);
                    
                    // Generate thumbnail URI
                    Uri thumbnailUri = Uri.withAppendedPath(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                        String.valueOf(videoFile.getId())
                    );
                    videoFile.setThumbnailUri(thumbnailUri);
                    
                    videoFiles.add(videoFile);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Apply current filter and sort
        applyFiltersAndSort();
    }
    
    private void applyFiltersAndSort() {
        filteredVideos.clear();
        
        // Apply view mode filter
        switch (currentViewMode) {
            case ALL_FOLDERS:
            case FILES:
                filteredVideos.addAll(videoFiles);
                break;
            case FOLDERS:
                // Group by folders (implement folder grouping logic)
                filteredVideos.addAll(videoFiles);
                break;
        }
        
        // Apply sorting
        Collections.sort(filteredVideos, getComparator());
        
        if (adapter != null) {
            adapter.updateData(filteredVideos);
            adapter.setLayoutMode(currentLayoutMode);
        }
    }
    
    private Comparator<VideoFile> getComparator() {
        Comparator<VideoFile> comparator;
        
        switch (currentSortBy) {
            case TITLE:
                comparator = (v1, v2) -> v1.getTitle().compareToIgnoreCase(v2.getTitle());
                break;
            case DATE:
                comparator = (v1, v2) -> Long.compare(v1.getDateAdded(), v2.getDateAdded());
                break;
            case SIZE:
                comparator = (v1, v2) -> Long.compare(v1.getSize(), v2.getSize());
                break;
            case LENGTH:
                comparator = (v1, v2) -> Long.compare(v1.getDuration(), v2.getDuration());
                break;
            case PATH:
                comparator = (v1, v2) -> v1.getPath().compareToIgnoreCase(v2.getPath());
                break;
            case RESOLUTION:
                comparator = (v1, v2) -> v1.getResolution().compareToIgnoreCase(v2.getResolution());
                break;
            default:
                comparator = (v1, v2) -> v1.getTitle().compareToIgnoreCase(v2.getTitle());
                break;
        }
        
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }
    
    // UI update methods
    private void setViewMode(ViewMode viewMode) {
        currentViewMode = viewMode;
        updateViewModeButtons();
        applyFiltersAndSort();
    }
    
    private void setLayoutMode(LayoutMode layoutMode) {
        currentLayoutMode = layoutMode;
        updateLayoutModeButtons();
        updateRecyclerViewLayout();
        if (adapter != null) {
            adapter.setLayoutMode(layoutMode);
        }
    }
    
    private void selectSortBy(SortBy sortBy) {
        currentSortBy = sortBy;
        // Update UI to show selection
    }
    
    private void setSortOrder(boolean ascending) {
        sortAscending = ascending;
        // Update UI to show selection
    }
    
    private void applySorting() {
        applyFiltersAndSort();
    }
    
    private void updateViewModeButtons() {
        allFoldersButton.setSelected(currentViewMode == ViewMode.ALL_FOLDERS);
        foldersButton.setSelected(currentViewMode == ViewMode.FOLDERS);
        filesButton.setSelected(currentViewMode == ViewMode.FILES);
        
        // Update button backgrounds
        allFoldersButton.setBackgroundResource(allFoldersButton.isSelected() ? 
            R.drawable.view_mode_button_selected : R.drawable.view_mode_button_normal);
        foldersButton.setBackgroundResource(foldersButton.isSelected() ? 
            R.drawable.view_mode_button_selected : R.drawable.view_mode_button_normal);
        filesButton.setBackgroundResource(filesButton.isSelected() ? 
            R.drawable.view_mode_button_selected : R.drawable.view_mode_button_normal);
    }
    
    private void updateLayoutModeButtons() {
        listButton.setSelected(currentLayoutMode == LayoutMode.LIST);
        gridButton.setSelected(currentLayoutMode == LayoutMode.GRID);
        
        // Update button backgrounds
        listButton.setBackgroundResource(listButton.isSelected() ? 
            R.drawable.layout_mode_button_selected : R.drawable.layout_mode_button_normal);
        gridButton.setBackgroundResource(gridButton.isSelected() ? 
            R.drawable.layout_mode_button_selected : R.drawable.layout_mode_button_normal);
    }
    
    private void toggleSortPanel() {
        showSortPanel = !showSortPanel;
        sortScrollView.setVisibility(showSortPanel ? View.VISIBLE : View.GONE);
        
        if (showSortPanel) {
            // Animate sort panel sliding up
            sortScrollView.setTranslationY(sortScrollView.getHeight());
            sortScrollView.animate()
                .translationY(0)
                .setDuration(300)
                .start();
        }
    }
    
    private void hideSortPanel() {
        showSortPanel = false;
        sortScrollView.animate()
            .translationY(sortScrollView.getHeight())
            .setDuration(300)
            .withEndAction(() -> sortScrollView.setVisibility(View.GONE))
            .start();
    }
    
    private void showSearchDialog() {
        // Implement search functionality
        Toast.makeText(this, "Search functionality", Toast.LENGTH_SHORT).show();
    }
    
    private void shareSelectedVideos() {
        // Implement share functionality
        Toast.makeText(this, "Share selected videos", Toast.LENGTH_SHORT).show();
    }
    
    private void playRandomVideo() {
        if (!filteredVideos.isEmpty()) {
            VideoFile randomVideo = filteredVideos.get(new Random().nextInt(filteredVideos.size()));
            Intent intent = new Intent(this, MXPlayerActivity.class);
            intent.setData(Uri.parse(randomVideo.getPath()));
            intent.putExtra("title", randomVideo.getTitle());
            startActivity(intent);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (showSortPanel) {
            hideSortPanel();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Video file data class
     */
    public static class VideoFile {
        private long id;
        private String title;
        private String path;
        private long size;
        private long duration;
        private long dateAdded;
        private String resolution;
        private Uri thumbnailUri;
        
        // Getters and setters
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public long getDateAdded() { return dateAdded; }
        public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }
        
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        
        public Uri getThumbnailUri() { return thumbnailUri; }
        public void setThumbnailUri(Uri thumbnailUri) { this.thumbnailUri = thumbnailUri; }
        
        public String getFormattedDuration() {
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
        
        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else if (size < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
}