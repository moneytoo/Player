package com.brouken.player.encrypt;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by MIRSAAB on 10/11/2017.
 */

public class VideoFetchUtils {
    static final /* synthetic */ boolean $assertionsDisabled = (!VideoFetchUtils.class.desiredAssertionStatus());
    Context context;
    private final String[] VIDEO_COLUMNS = new String[]{"_id", "_display_name", "title", "date_added", "duration", "resolution", "_size", "_data", "mime_type"};
    static Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    static String VIDEO_ID = MediaStore.Video.Media._ID;
    static String VIDEO_NAME = MediaStore.Video.Media.DISPLAY_NAME;
    static String VIDEO_PATH = MediaStore.Video.Media.DATA;
    static String VIDEO_SIZE = MediaStore.Video.Media.SIZE;
    static String VIDEO_DURATION = MediaStore.Video.Media.DURATION;
    static String VIDEO_RESOLUTION = MediaStore.Video.Media.RESOLUTION;
    //----------------------------------------------------
    private static final String[] PROJECTION_VIDEO_URI = {
            VIDEO_ID, VIDEO_NAME, VIDEO_PATH, VIDEO_SIZE, VIDEO_DURATION, VIDEO_RESOLUTION
    };

    //  private static final String SEPARATOR_RESOLUTION = "x";

    public static String VIDEO_EXT = "youtu_mp4_mpeg_mts_m2ts_ts_avi_mov_vob_m3u8";

    public VideoFetchUtils(Context context) {
        this.context = context;
         }


    private static long getIDfromUri(Context context, Uri fileUri) {
        if (fileUri == null) {
            return 0;
        }
        Cursor cursor = context.getContentResolver().query(fileUri, new String[]{"_id"}, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            return 0;
        }
        int fileId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
        cursor.close();
        return (long) fileId;
    }

    @Nullable
    public static NyVideo buildNyVideo(@NonNull Cursor cursor) {
        NyVideo video = new NyVideo();
        Info Info = new Info();

        final String[] columnNames = cursor.getColumnNames();
        for (int i = 0; i < columnNames.length; i++)
            switch (columnNames[i]) {
                case MediaStore.Video.Media._ID:
                    Info.id = cursor.getLong(i);
                    break;
                case MediaStore.Video.Media.DISPLAY_NAME:
                    final String name = cursor.getString(i);
                    if (name != null) {
                        video.setName(name);
                    }
                    break;
                case MediaStore.Video.Media.DATA:
                    final String path = cursor.getString(i);
                    if (path == null) return null;
                    File file = new File(path);
                    if (!file.exists()) return null;
                    video.setPath(path);
                    //  if (video.getName().isEmpty()) {
                    video.setName(NyFileUtil.getFileNameWithoutExtFromPath(path));
                    // }
                case MediaStore.Video.Media.SIZE:
                    Info.size = cursor.getLong(i);
                    break;
                case MediaStore.Video.Media.DURATION:
                    Info.duration = (int) cursor.getLong(i);
                    break;
                case MediaStore.Video.Media.RESOLUTION:
                    final String resolution = cursor.getString(i);
                    if (resolution != null) {

                        final int infix = resolution.indexOf("x");
                        if (infix > 0) {
                            Info.width = Integer.parseInt(resolution.substring(0, infix));
                            Info.height = Integer.parseInt(resolution.substring(infix + 1));
                        }
                    }
                    break;
            }

     /*  if (video.getDuration() <= 0 || video.getWidth() <= 0 || video.getHeight() <= 0) {
            if (invalidateVideoDurationAndResolution(video)) {
                updateNyVideo(video);
            }
          //  else {
          //      return null;
          //  }
        }*/
        video.setInfo(Info);
        return video;
    }


    @Nullable
    public static NyVideo queryNyVideoByPath(Context context, @Nullable String path) {
        if (path == null) return null;
      //  path = escapedSqlComparisionString(path);
        ContentResolver mContentResolver = context.getContentResolver();
        Cursor cursor = mContentResolver.query(VIDEO_URI, PROJECTION_VIDEO_URI,
                VIDEO_PATH + "='" + path + "' COLLATE NOCASE", null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return VideoFetchUtils.buildNyVideo(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

  /*  private static String escapedSqlComparisionString(String string) {
//        return string.replaceAll("'", "''");
      /  return StringsKt.replace(string, "'", "''", false);
    }*/

   /*
    public List<Folder> fetchAllFoldersFromVideolist(ArrayList<NyVideo> allVideosInLocal) {
        List<Folder> folderList = new ArrayList();
        for (NyVideo video : allVideosInLocal) {
            String parentFolder = new File(video.getPath()).getParent();
            String videoFolderName = new File(parentFolder).getName();
            Folder aFolder = new Folder();
            aFolder.setName(videoFolderName);
            aFolder.setPath(parentFolder);
            aFolder.videosPP();
            //--  aFolder.sizePP(video.getSize());
            if (folderList.contains(aFolder)) {
                ((Folder) folderList.get(folderList.indexOf(aFolder))).videosPP();
                //--     ((Folder) folderList.get(folderList.indexOf(aFolder))).sizePP(video.getSize());
            } else {
                folderList.add(aFolder);
            }
        }
        return folderList;
    }*/

    public ArrayList<NyVideo> fetchAllVideosFromMediaStore() {
        ArrayList<NyVideo> allVideosInLocal = new ArrayList<NyVideo>();
        Cursor videoCursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, this.VIDEO_COLUMNS, null, null, "date_added DESC");
        if (videoCursor != null) {
            allVideosInLocal = buildVideoListFromCursor(context, videoCursor);
            videoCursor.close();
        }
        return allVideosInLocal;
    }

    public static ArrayList<NyVideo> buildVideoListFromCursor(Context context, Cursor videoCursor) {
        ArrayList<NyVideo> videoList = new ArrayList();
        while (videoCursor.moveToNext()) {
            NyVideo video = buildNyVideo(videoCursor);
            if (video != null) videoList.add(video);
        }
        return videoList;
    }


    private static final ArrayList<String> allDirectories = new ArrayList<String>();
    private static final ArrayList<NyVideo> allNyVideos = new ArrayList<NyVideo>();

    private static final ArrayList<String> videoDirectories = new ArrayList<String>();

    private static void fetchVideosInAllDirectories(String path) {

        File tempfile = new File(path);
        File[] files = tempfile.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    videoDirectories.add(file.getAbsolutePath());
                    Log.e("directory Fetch ", file.getName());
                    fetchVideosInAllDirectories(file.getAbsolutePath());
                } else {
                    String ext = NyFileUtil.getFileExtension(file.getName());
                    if (ext!=null && VIDEO_EXT.contains(ext)) {
                        NyVideo nyVideo = new NyVideo();
                        nyVideo.setName(file.getName());
                        nyVideo.setPath(file.getAbsolutePath());
                        allNyVideos.add(nyVideo);
                        // Log.e("video Fetch ",file.getName());
                    }
                }

            }
        }
    }

    public static List<NyVideo> fetchVideosInOneDirectory(String path) {
        List<NyVideo> allNyVideos = new ArrayList<NyVideo>();
        String folder = new File(path).getParent();
        File tempfile = new File(folder);
        File[] files = tempfile.listFiles();
        if (files != null) {
            int i = 0;
          //  position = 0;
         //   String fileName;
            for (File file : files) {
           //     fileName = file.getName();
                String lExt = NyFileUtil.getFileExtension(file.getName().toLowerCase());
                if (lExt != null && VIDEO_EXT.contains(lExt)){
                    NyVideo nyVideo = new NyVideo();
                    nyVideo.setName(file.getName());
                    nyVideo.setPath(file.getAbsolutePath());
                    allNyVideos.add(nyVideo);
                 //   if (path.contains(fileName)) position = i;
                 //   i++;
                }
            }
        }
        return allNyVideos;
    }



    public static ArrayList<NyVideo> scanVideosInAllDirectories(String path) {
        allNyVideos.clear();
        fetchVideosInAllDirectories(path);
        return allNyVideos;
    }


    public static void GetFilesInAllDirectories(String path) {

        File tempfile = new File(path);
        File[] files = tempfile.listFiles();

        if (files != null) {
            for (File checkFile : files) {
                if (checkFile.isDirectory()) {
                    allDirectories.add(checkFile.getAbsolutePath());
                    GetFilesInAllDirectories(checkFile.getAbsolutePath());
                }
            }
        }
    }


}