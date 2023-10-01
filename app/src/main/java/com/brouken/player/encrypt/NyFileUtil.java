package com.brouken.player.encrypt;

import static android.content.Context.CLIPBOARD_SERVICE;

import static com.brouken.player.encrypt.Constants.MAIN_PASSWORD;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NyFileUtil {
    private static final String TAG = "mFileUtil";
    public static final String VIDEO_EXT = "webm_mov_mp4_mpg_mpeg_mts_m2ts_avi_vob_m3u8_flv_mpa_mkv";
    public static final String SPECIAL_EXT = "mts_m2ts_avi";
    public static final String IMAGE_EXT = "jpg_cr2_rw2_tif_gif_bmp_tiff_png_pic_pct_mdi";
    public static final String AUDIO_EXT = "mp3_aac_mka_adp_au_m2a_m3a_oga_snd_m4a";
    public static final String DOCUMENT_EXT = "txt_doc_docx__pdf_ppt_pptx_xls_xlsx_epub";
    public static final String MUPDF_EXT = "pdf_epub_png_jpg_bmp_tiff_gif_svg_cbz_cbr_xps";
    public static final String MEDIA_EXT = VIDEO_EXT + IMAGE_EXT + AUDIO_EXT;

    /*
            MIME_TYPES.put("cgm", "image/cgm");
        MIME_TYPES.put("btif", "image/prs.btif");
        MIME_TYPES.put("dwg", "image/vnd.dwg");
        MIME_TYPES.put("dxf", "image/vnd.dxf");
        MIME_TYPES.put("fbs", "image/vnd.fastbidsheet");
        MIME_TYPES.put("fpx", "image/vnd.fpx");
        MIME_TYPES.put("fst", "image/vnd.fst");
        MIME_TYPES.put("mdi", "image/vnd.ms-mdi");
        MIME_TYPES.put("npx", "image/vnd.net-fpx");
        MIME_TYPES.put("xif", "image/vnd.xiff");
        MIME_TYPES.put("pct", "image/x-pict");
        MIME_TYPES.put("pic", "image/x-pict");

        MIME_TYPES.put("adp", "audio/adpcm");
        MIME_TYPES.put("au", "audio/basic");
        MIME_TYPES.put("snd", "audio/basic");
        MIME_TYPES.put("m2a", "audio/mpeg");
        MIME_TYPES.put("m3a", "audio/mpeg");
        MIME_TYPES.put("oga", "audio/ogg");
        MIME_TYPES.put("spx", "audio/ogg");
        MIME_TYPES.put("aac", "audio/x-aac");
        MIME_TYPES.put("mka", "audio/x-matroska");

        MIME_TYPES.put("jpgv", "video/jpeg");
        MIME_TYPES.put("jpgm", "video/jpm");
        MIME_TYPES.put("jpm", "video/jpm");
        MIME_TYPES.put("mj2", "video/mj2");
        MIME_TYPES.put("mjp2", "video/mj2");
        MIME_TYPES.put("mpa", "video/mpeg");
        MIME_TYPES.put("ogv", "video/ogg");
        MIME_TYPES.put("flv", "video/x-flv");
        MIME_TYPES.put("mkv", "video/x-matroska");
     */

    public static String StringAfter(String str, String substr){
        return str.substring(str.indexOf(substr) + substr.length());
    }

    public static String removespace(String inStr) {
        return inStr.replaceAll(" ", "");
    }

    public static String getTypeFromName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public static boolean isVideo(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (VIDEO_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isSpecialMedia(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (SPECIAL_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isVideoFile(String path) {
        try {
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return mimeType != null && mimeType.startsWith("video");
        } catch (Throwable e) {
            Log.e("ExceptionInIsVideo", "isVideoFile: " + e);
            return false;
        }
    }


    public static boolean isImage(String url) {
        if (url == null) return false;
        String lExt = getFileExtension(url);
        if (lExt != null) return (IMAGE_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isAudio(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (AUDIO_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isMedia(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (MEDIA_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isDocument(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (DOCUMENT_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean isMuPdf(String url) {
        String lExt = getFileExtension(url);
        if (lExt != null) return (MUPDF_EXT.contains(lExt.toLowerCase()));
        else return false;
    }

    public static boolean hasFile(File file) {
        return file != null
                && file.exists()
                && file.lastModified() > 0
                && file.length() > 0;
    }

    public static boolean hasFile(String url) {
        if (isOnline(url)) return false;
        File file = new File(url);
        return hasFile(file);
    }


    public static File setOutputFile(String fileName) {
        int encryptLevel = EncryptUtil.encryptLevelFromFileName(fileName);
        return setOutputFile(fileName, encryptLevel);
    }

    public static File setOutputFile(String fileName, int encryptLevel) {
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(fileName);
        String ext = NyFileUtil.getFileExtension(fileName);
        if ((ext == null || ext.length() == 1)) ext = "mp4";//will be default to mp4 if none
        //-------------------------
      /*  if (encryptLevel == -1) encryptLevel = 1;   //default with encryptlevel 1
        //-----------------above line is critical
        if (fileName.contains(insert(encryptLevel))) {
            return new File(mFileUtil.getVideoDirectory(), onlyFileName + "." + ext);
        } else {
            return new File(mFileUtil.getVideoDirectory(), onlyFileName + insert(encryptLevel) + "." + ext);
        }*/
        if (encryptLevel == -1 || fileName.contains(insert(encryptLevel))) {
            return new File(NyFileUtil.getVideoDir(), onlyFileName + "." + ext);
        } else {
            return new File(NyFileUtil.getVideoDir(), onlyFileName + insert(encryptLevel) + "." + ext);
        }
    }

    public static File setOutputFile(String fileName, String passWord) {
        return setOutputFile(fileName, EncryptUtil.encryptLevelFromPassword(passWord));
    }

   /* public static File setEncryptOutputFile(String fileName, int encryptLevel) {
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(fileName);
        String ext = NyFileUtil.getFileExtension(fileName);//default mp4
        if (encryptLevel == -1) encryptLevel = 1;   //normal video encrypted with level 1
        String insert = "_NY" + Integer.toString(encryptLevel);
        if (fileName.contains(insert)) {
            return new File(NyFileUtil.getVideoDir(), onlyFileName + "." + ext);
        } else {
            return new File(NyFileUtil.getVideoDir(), onlyFileName + insert + "." + ext);
        }
    }*/


    private static String insert(int encryptLevel) {
        return "_NY" + encryptLevel;
    }

//------------------------------------------------------

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * input/output url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level >= 19. On older API levels it returns an empty url.
     *
     * @return input/output url that can be passed to FFmpegKit or FFprobeKit
     */
    private static String getSafParameter(final Context context, final Uri uri, final String openMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.i(TAG, String.format("getSafParameter is not supported on API Level %d", Build.VERSION.SDK_INT));
            return "";
        }
        SparseArray<ParcelFileDescriptor> pfdMap = new SparseArray<>();
        String displayName = "unknown";
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
            }
        } catch (final Throwable t) {
            Log.e(TAG, String.format("Failed to get %s column for %s.%s", DocumentsContract.Document.COLUMN_DISPLAY_NAME, uri.toString(), "Exceptions.getStackTraceString(t)"));
        }


        int fd = -1;
        try {
            ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, openMode);
            fd = parcelFileDescriptor.getFd();
            pfdMap.put(fd, parcelFileDescriptor);
        } catch (final Throwable t) {
            Log.e(TAG, String.format("Failed to obtain %s parcelFileDescriptor for %s.%s", openMode, uri.toString(), "Exceptions.getStackTraceString(t)"));
        }

        // workaround for https://issuetracker.google.com/issues/162440528: ANDROID_CREATE_DOCUMENT generating file names like "transcode.mp3 (2)"
        if (displayName.lastIndexOf('.') > 0 && displayName.lastIndexOf(' ') > displayName.lastIndexOf('.')) {
            String extension = displayName.substring(displayName.lastIndexOf('.'), displayName.lastIndexOf(' '));
            displayName += extension;
        }
        // spaces can break argument list parsing, see https://github.com/alexcohn/mobile-ffmpeg/pull/1#issuecomment-688643836
        final char NBSP = (char) 0xa0;
        return "saf:" + fd + "/" + displayName.replace(' ', NBSP);
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * input url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri     saf uri
     * @return input url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForRead(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "r");
    }

    /**
     * <p>Converts the given Structured Access Framework Uri (<code>"content:…"</code>) into an
     * output url that can be used in FFmpeg and FFprobe commands.
     *
     * <p>Requires API Level &ge; 19. On older API levels it returns an empty url.
     *
     * @param context application context
     * @param uri     saf uri
     * @return output url that can be passed to FFmpegKit or FFprobeKit
     */
    public static String getSafParameterForWrite(final Context context, final Uri uri) {
        return getSafParameter(context, uri, "w");
    }

    /**
     * Called by saf_wrapper from native library to close a parcel file descriptor.
     *
     * @param "fd" parcel file descriptor created for a saf uri
     *             <p>
     *             private static void closeParcelFileDescriptor(final int fd) {
     *             try {
     *             ParcelFileDescriptor pfd = pfdMap.get(fd);
     *             if (pfd != null) {
     *             pfd.close();
     *             pfdMap.delete(fd);
     *             }
     *             } catch (final Throwable t) {
     *             android.util.Log.e(TAG, String.format("Failed to close file descriptor: %d.%s", fd, Exceptions.getStackTraceString(t)));
     *             }
     *             }
     */

    // https://stackoverflow.com/questions/12285469/get-path-from-filedescriptor-in-java

    /* @param context 上下文
     * @param uri     待解析的 Uri
     * @return 真实路径
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String url = null;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    url = Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                url = getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                url = getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if (uri.getScheme() != null) {
            // MediaStore (and general)
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                // Return the remote address
                if (isGooglePhotosUri(uri)) url = uri.getLastPathSegment();
                else url = getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                url = uri.getPath();
            }
        } else url = uri.toString();

        if (url == null) url = uri.toString();

        if (url.contains("%")) {
            try {
                url = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToNext()) {
                String result = cursor.getString(0);
                //TODO most important
                return TextUtils.isEmpty(result) ? result : getRealPathFromURI(context, uri);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        //TODO
        return uri.toString();
    }

    public static String getRealPathFromURI(Context context, Uri contentURI) {
        String result;
        Cursor cursor = context.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

 /*   public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        //TODO
        return uri.toString();
    }*/

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isOnline(Uri uri) {
        String url = uri.toString().toLowerCase();
        return isOnline(url);
    }

    public static String RemoveRedundant(String url) {
        // remove redudant strings before http link
        if (url.contains("http")) url = url.substring(url.indexOf("http"));
        else if (url.contains("Http")) url = url.substring(url.indexOf("Http"));
        else if (url.contains("HTTP")) url = url.substring(url.indexOf("HTTP"));
        if (url.contains("复制")) url = url.substring(0, url.indexOf("复制"));
        if (url.contains("Copy")) url = url.substring(0, url.indexOf("Copy"));
        if (url.contains("或者")) url = url.substring(0, url.indexOf("或者"));
        if (url.contains("or use")) url = url.substring(0, url.indexOf("or use"));
        //  Log.e("reduction------------:", url);
        return url.trim();
    }

    public static Boolean isLocal(String fileName) {
        return !isOnline(fileName);
    }


    public static boolean isCasheable(Uri uri) {
        String url = uri.toString().toLowerCase();
        return (!url.contains("m3u8") && isOnline(uri));
    }


    public static String getFileExtension(String fileName) {
        String ext = null;
        if (fileName != null && fileName.lastIndexOf('.') != -1)
            ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        return ext;
    }

    public static String getLastSegmentFromString(String path) {
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (path.contains("/")) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }

    public static String getFileNameWithoutExtFromPath(String path) {

        path = NyFileUtil.getLastSegmentFromString(path);
        if (path.contains(".")) {
            path = path.substring(0, path.indexOf('.'));
        }
        return path;
    }


    public static String getParentPath(String path) {
        if (path.contains("/")) {
            path = path.substring(0, path.lastIndexOf('/') + 1);
        }
        // Log.e(TAG,"parent path"+ path);
        return path;
    }

    public static void DeleteAllFilesinDir(File parentDir) {
        List<String> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
            } else {
                file.delete();
            }
        }
    }


    public static void DeleteBakFilesinDir(File parentDir) {
        List<String> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
            } else if (file.getName().endsWith(".bak") ||
                    //   (file.getName().endsWith(".json") && (daysAfterFileCreation(file) > 2))){
                    file.getName().endsWith(".json")) {
                file.delete();
            }
        }
    }

    private static int daysAfterFileCreation(File file) {
        return (getCurrentJulianDay() - convertDateTOJulianDay(new Date(file.lastModified())));
    }

    private static int dayMoreinDate(Date date1, Date date2) {
        return convertDateTOJulianDay(date1) - convertDateTOJulianDay(date2);
    }

    public static int getCurrentJulianDay() {
        return convertDateTOJulianDay(new Date());
    }


    private static int convertDateTOJulianDay(Date date) {
        //  SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);

        //   String filename = formatter.format(now);
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDate();
        return (1461 * (year + 4800 + (month - 14) / 12)) / 4
                + (367 * (month - 2 - 12 * ((month - 14) / 12))) / 12
                - (3 * ((year + 4900 + (month - 14) / 12) / 100)) / 4 + day - 32075;
    }


    public static void copyFileStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.close();
    }

    public static boolean copyStreamToFile(InputStream in, File dest) {
        OutputStream os = null;
        boolean success = false;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[50 * 1024 * 1024];
            int length;
            Log.e("mFilleUtil", "transfer in process...");
            while ((length = in.read(buffer)) > 0) {
                os.write(buffer, 0, length);
                Log.e("mFilleUtil", "transfered" + length);
            }

        } catch (IOException e) {
            Log.e("tag", "Failed to copy files:", e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                Log.e("tag", "Failed to close in:", e);
            }
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                    success = true;
                }
            } catch (IOException e) {
                Log.e("tag", "Failed to close out:", e);
            }
        }
        return success;
    }


    public static void copyFile(File source, File dest) {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } catch (IOException e) {
            Log.e("tag", "Failed to copy files:", e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {
                Log.e("tag", "Failed to close in:", e);
            }
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                Log.e("tag", "Failed to close out:", e);
            }
        }
    }

    public static void copyFiletoDir(String fileInPath, String dir) {
        File fileIn = new File(fileInPath);
        String fileName = NyFileUtil.getLastSegmentFromString(fileInPath);
        File fileOut = new File(dir, fileName);
        copyFile(fileIn, fileOut);
    }


    public static String timedFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHMM", Locale.ENGLISH);
        Date now = new Date();
        String filename = formatter.format(now) + ".mp4";
        return filename;
    }

    /**
     * Deletes the specified diretory and any files and directories in it
     * recursively.
     *
     * @param dir The directory to remove.
     * @throws IOException If the directory could not be removed.
     */
    public static void deleteDir(File dir)
            throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException("Not a directory " + dir);
        }

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                boolean deleted = file.delete();
                if (!deleted) {
                    throw new IOException("Unable to delete file" + file);
                }
            }
        }

        dir.delete();
    }

    public static void cleanCache(Context context) {
        File cacheFolder = new File(getCacheFolder());
        if (cacheFolder.exists()) {
            DeleteAllFilesinDir(cacheFolder);
        }
    }


    public static AppCompatActivity getActivity(Context context) {

        if (context == null) {
            return null;
        }

        if (context instanceof AppCompatActivity) {
            return (AppCompatActivity) context;
        } else if (context instanceof ContextThemeWrapper) {
            return getActivity(((ContextThemeWrapper) context).getBaseContext());
        } else if (context instanceof AppCompatActivity) {
            return (AppCompatActivity) context;
        }
        return null;
    }

    //-----------------------------

    public static boolean ClipboardToFile(Context context, File file) {
        // get the system clipboard
        ClipboardManager clipboard = (ClipboardManager) getActivity(context).getSystemService(CLIPBOARD_SERVICE);
        String clipboarText = clipboard.getText().toString();

        if (clipboarText.isEmpty()) {
            Toast.makeText(context, "Clipboard is empty!!!", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return NyFileUtil.saveStrToFile(context, clipboarText, file);
        }
    }

    public static boolean EncryptClipboardToFile(Context context, File file) {
        // get the system clipboard
        ClipboardManager clipboard = (ClipboardManager) getActivity(context).getSystemService(CLIPBOARD_SERVICE);
        String clipboarText = clipboard.getText().toString();
        clipboarText = EncryptUtil.decrypt(clipboarText, MAIN_PASSWORD);
        if (clipboarText.isEmpty()) {
            Toast.makeText(context, "Clipboard is empty!!!", Toast.LENGTH_LONG).show();
            return false;
        } else {
            File temp = new File(NyFileUtil.getOnlineDir(), "temp");
            NyFileUtil.saveStrToFile(context, clipboarText, temp);
            if (EncryptUtil.StandardEncryptFile(context, temp, file, EncryptUtil.LevelCipherOnly(1)))
                temp.delete();
        }
        return true;
    }


    public static boolean saveStrToFile(Context context, String saveStr, File savedFile) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(savedFile, false));
            outputStreamWriter.write(saveStr);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File save failed: " + e.toString());
            return false;
        }
        return true;
    }

    public static void shareFilePath(final Context context, final String filePath) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, filePath);
        context.startActivity(Intent.createChooser(shareIntent,
                "Share text"));
    }

    public static void copy_shareFilePath(Context context, String path) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("text", path));
        Intent shareIntent = new Intent(
                Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, path);
        context.startActivity(Intent.createChooser(shareIntent, "share online link"));
    }


    public static void shareMedias(Context context, final List<String> fileList, String type) {
        int size = fileList.size();
        boolean isLast = false;
        String filePath;
        for (int i = 0; i < size; i++) {
            filePath = fileList.get(i);
            shareMedia(context, filePath, type);
        }
    }

    public static void shareDocs(Context context, final List<String> fileList) {
        int size = fileList.size();
        boolean isLast = false;
        String filePath;
        for (int i = 0; i < size; i++) {
            filePath = fileList.get(i);
            shareDoc(context, filePath);
        }
    }

    public static void shareMedia(final Context context, final String mediaPath, String type) {
        Log.e(TAG, "shareMedia mediaPath: " + mediaPath);
        MediaScannerConnection.scanFile(context, new String[]{mediaPath},

                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.e(TAG, "shareMedia uri" + uri.toString());
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType(type);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.putExtra("VSMP", "https://play.google.com/store/apps/details?id=" + context.getPackageName());
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        context.startActivity(Intent.createChooser(intent, type));

                    }
                });

    }


    public static void shareDoc(final Context context, final String selectedVideoShare) {
        MediaScannerConnection.scanFile(context, new String[]{selectedVideoShare},

                null, new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Intent shareIntent = new Intent(
                                Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra("VSMP", "https://play.google.com/store/apps/details?id=" + context.getPackageName());
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        context.startActivity(Intent.createChooser(shareIntent,
                                "Share text"));

                    }
                });

    }

//---------------------------Share Service----------------------------

    /**
     * 分享功能|分享单张图片
     *
     * @param context       上下文
     * @param activityTitle Activity的名字
     * @param msgTitle      消息标题
     * @param msgText       消息内容
     * @param imgPath       图片路径，不分享图片则传null
     */
    public static void shareMsg(Context context, String activityTitle,
                                String msgTitle, String msgText, String imgPath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        if (imgPath == null || imgPath.equals("")) {
            intent.setType("text/plain"); // 纯文本
        } else {
            File f = new File(imgPath);
            if (f != null && f.exists() && f.isFile()) {
                intent.setType("image/jpg");
                Uri u = Uri.fromFile(f);
                intent.putExtra(Intent.EXTRA_STREAM, u);
            }
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, msgTitle);
        intent.putExtra(Intent.EXTRA_TEXT, msgText);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, activityTitle));
    }

    /**
     * 分享多张照片
     *
     * @param context
     * @param list    ArrayList＜ImageUri＞
     */
    public static void sendMultipleImage(Context context,
                                         ArrayList<? extends Parcelable> list) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        intent.putExtra(Intent.EXTRA_SUBJECT, "");
        intent.putExtra(Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_TITLE, "");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "share"));
    }

    public static void sendMultipleVideos(Context context,
                                          ArrayList<? extends Parcelable> list) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("video/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        intent.putExtra(Intent.EXTRA_SUBJECT, "");
        intent.putExtra(Intent.EXTRA_TEXT, "");
        intent.putExtra(Intent.EXTRA_TITLE, "");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "share"));
    }

    /**
     * <ul>
     * <li>分享任意类型的<b style="color:red">单个</b>文件|不包含目录</li>
     * <li>[<b>经验证！可以发送任意类型的文件！！！</b>]</li>
     * <li># @author http://blog.csdn.net/yuxiaohui78/article/details/8232402</li>
     * <ul>
     *
     * @param context
     * @param filePath Uri.from(file);
     */
    public static void shareFile(Context context, String filePath) {
        // File file = new File("\sdcard\android123.cwj"); //附件文件地址

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra("subject", ""); //
        intent.putExtra("body", ""); // 正文
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath)); // 添加附件，附件为file对象
        if (filePath.endsWith(".gz")) {
            intent.setType("application/x-gzip"); // 如果是gz使用gzip的mime
        } else if (filePath.endsWith(".txt")) {
            intent.setType("text/plain"); // 纯文本则用text/plain的mime
        } else {
            intent.setType("application/octet-stream"); // 其他的均使用流当做二进制数据来发送
        }
        context.startActivity(intent); // 调用系统的mail客户端进行发送
    }

    /**
     * <ul>
     * <li>分享任意类型的<b style="color:red">多个</b>文件|不包含目录</li>
     * <li>[<b>经验证！可以发送任意类型的文件！！！</b>]</li>
     * <li># @author http://blog.csdn.net/yuxiaohui78/article/details/8232402</li>
     * <ul>
     *
     * @param context
     * @param uris    list.add(Uri.from(file));
     */
    public static void shareMultipleFiles(Context context, ArrayList<Uri> uris) {

        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(
                multiple ? Intent.ACTION_SEND_MULTIPLE
                        : Intent.ACTION_SEND);

        if (multiple) {
            intent.setType("*/*");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            Uri value = uris.get(0);
            String ext = MimeTypeMap.getFileExtensionFromUrl(value.toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mimeType == null) {
                mimeType = "*/*";
            }
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, value);
        }
        context.startActivity(Intent.createChooser(intent, "Share"));
    }


    public static String getAppDirectory(Context context) {
        File file = new File(Environment.getExternalStorageDirectory(),context.getPackageName() );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    public static File getUnlockFile(Context context) {
        return new File(getAppDirectory(context), "unlock_NY1");
    }

    public static String getBlackPath(Context context) {
        return new File(getAppDirectory(context),"black.lst").getAbsolutePath();
    }

    public static String getRecallPath(Context context) {
        return new File(getAppDirectory(context),"recall.lst").getAbsolutePath();
    }

    public static String getDownloadedDir(Context context) {
        File file = new File(getAppDirectory(context),"download");
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    //TODO
    // all dir path under NytaijiDir end with "/"
    public static String getNytaijiDir() {
        File file = new File(Environment.getExternalStorageDirectory() ,"nytaiji");
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }


    public static String getThumbDir() {
        File file = new File(getNytaijiDir() ,".thumb" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String getImageDir() {
        File file = new File(getNytaijiDir() ,"image" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String getOnlineDir() {
        File file = new File(getNytaijiDir() ,".online" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String getSavedDir() {
        File file = new File(getNytaijiDir() ,".save" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }


    public static File getDirlockListFile() {
        return new File(getNytaijiDir(), "lock");
    }


    public static File getlockDirFile() {
        return new File(getNytaijiDir(), "lock");

    }

    public static File getExcludeFile() {
        return new File(getNytaijiDir(), "exclude");
    }

    public static String getCacheFolder() {
        File file = new File(getNytaijiDir() ,".cache" );
        if (!file.exists()) {
            file.mkdirs();
            File file2 = new File(".nomedia");
        }
        return file.getAbsolutePath();
    }

    public static String getEmptyCacheFolder() {
        File file = new File(getNytaijiDir() ,".cache" );
        if (!file.exists()) {
            file.mkdirs();
        } else DeleteAllFilesinDir(file);
        File file2 = new File(file, ".nomedia");
        try {
            file2.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }


    public static String getVideoDir() {
        File file = new File(getNytaijiDir() ,"video" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }

    public static String getEditDir() {
        File file = new File(getNytaijiDir() ,"edit" );
        if (!file.exists()) file.mkdirs();
        return file.getAbsolutePath();
    }


    public static File getThumbFile(String fileName) {
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(fileName);
        return new File(NyFileUtil.getThumbDir(), onlyFileName);
    }

    public static File getEncryptThumbFile(String fileName, int encryptLevel) {
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(fileName);
        //-------------------------
        if (encryptLevel == -1) encryptLevel = 1;   //default with encryptlevel 1
        //-----------------above line is critical
        if (fileName.contains(insert(encryptLevel))) {
            return new File(NyFileUtil.getThumbDir(), onlyFileName);
        } else {
            return new File(NyFileUtil.getThumbDir(), onlyFileName + insert(encryptLevel));
        }
    }

    public static File getEncryptThumbFile(String fileName, String passWord) {
        int encryptLevel = EncryptUtil.encryptLevelFromPassword(passWord);
        return getEncryptThumbFile(fileName, encryptLevel);
    }

    public static String getEncryptThumbPng(String fileName, String passWord) {
        int encryptLevel = EncryptUtil.encryptLevelFromPassword(passWord);
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(fileName);
        //-------------------------
        if (encryptLevel == -1) encryptLevel = 1;   //default with encryptlevel 1
        //-----------------above line is critical
        if (fileName.contains(insert(encryptLevel))) {
            return onlyFileName;
        } else {
            return onlyFileName + insert(encryptLevel);
        }
    }

    public static String getThumbPng(String fileName) {
        if (fileName == null) fileName = timedFileName();
        String onlyFileName = getFileNameWithoutExtFromPath(fileName);
        return onlyFileName;
    }



  /*  public static String setDecryptFileName(String path, String passWord, boolean overhead) {
        String onlyFileName = NyFileUtil.getFileNameWithoutExtFromPath(path);
        String ext = NyFileUtil.getFileExtension(path);
        String nyx = "_NY" + passWord;
        if (passWord.length() == 16) nyx = "_NY0";
        if (onlyFileName.contains(nyx)) {
            onlyFileName = onlyFileName.replace(nyx, "");  //reset
        } else onlyFileName = onlyFileName + nyx;
        return onlyFileName + "." + ext;
    }*/

    //----------------------------------------------
    @NonNull
    public static String formatFileSize(double size) {
        // 如果字节数少于1024，则直接以B为单位，否则先除于1024
        if (size < 1024) {
            return (double) Math.round(size * 100d) / 100d + "B";
        } else {
            size = size / 1024d;
        }
        // 如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        if (size < 1024) {
            return (double) Math.round(size * 100d) / 100d + "KB";
            // #.00 表示两位小数 #.0000四位小数 以此类推…
        } else {
            size = size / 1024d;
        }
        if (size < 1024) {
            return (double) Math.round(size * 100d) / 100d + "MB";
            // %.2f %.表示 小数点前任意位数 2 表示两位小数 格式后的结果为f 表示浮点型。
        } else {
            // 否则要以GB为单位
            size = size / 1024d;
            return (double) Math.round(size * 100d) / 100d + "GB";
        }
    }

    /**
     * 判断SD卡是否已挂载
     */
    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 判断SD卡上是否有足够的空间
     */
    public static boolean hasEnoughStorageOnDisk(long size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final String storage = Environment.getExternalStorageDirectory().getPath();
            final StatFs fs = new StatFs(storage);
            final long available = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            return available >= size;
        }
        return true;
    }

    //-------------------link check

    private static final String youtube = "^((?:https?:)?\\/\\/)?((?:www|m)\\.)?((?:youtube\\.com|youtu.be))(\\/(?:[\\w\\-]+\\?v=|embed\\/|v\\/)?)([\\w\\-]+)(\\S+)?$";

    public static boolean isYoutube(String url) {
        return check(youtube, url);
    }

    public static boolean isCowShare(String url) {
        return url.contains("c-t.work");
    }

    public static boolean isWeShare(String url) {
        return url.contains("we.tl");
    }

    public static boolean isCowLink(String url) {
        return url.toLowerCase().contains("static.cowtransfer");
    }

    public static boolean isWeibo(String url) {
        return url.toLowerCase().contains("weibo.com") || url.toLowerCase().contains("weibo.cn") || url.toLowerCase().contains("video.sina.com.cn");
    }

    public static boolean isDouYin(String url) {
        return url.toLowerCase().contains("v.douyin.com");
    }

    private static boolean check(String regex, String string) {
        final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }

    /**
     * 跳转到手机默认浏览器相关网页
     *
     * @param context
     * @param url
     */
    public static void goWeb(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            //ToastUtil.showLong(context.getString(R.string.link_is_empty));
            return;
        }
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        context.startActivity(intent);
    }

    public static String codeTransfer(String link) {   //should be replaced by decodring
        link = link.replace("%3A", ":");
        link = link.replace("%2F", "/");
        link = link.replace("%3F", "?");
        link = link.replace("%26", "&");
        link = link.replace("%2C", ",");
        link = link.replace("%3D", "=");
        return link;
    }

    //----------------

    public void copyStreamToFile(InputStream inputStream, FileOutputStream fileOutputStream) {
        long total = 0;
        try {
            byte[] buffer = new byte[5 * 1024 * 1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                // Log.e(TAG, "transfered"+Long.toString(total));
                //   onProgressChanged(fileLength, total, "Encrypted");
            }
            inputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String convertFileToString(String filePath) {
        File fl = new File(filePath);
        try {
            FileInputStream fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //method to read a file
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String FileToStr(String path, Charset encoding) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            return encoding.decode(ByteBuffer.wrap(encoded)).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copyAsset(Context context, String inPath, String outPath) {
        try {
            InputStream myInput = context.getAssets().open(inPath);
            // Open the empty db as the output stream
            OutputStream myOutput = new FileOutputStream(outPath);
            // transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            // Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (IOException e) {
            Log.e("NyfileUtil copyAsset ", e.toString());
        }
    }


    private static final String SEPARATOR_RESOLUTION = "x";

    public static final String reroutedHtml = Environment.getExternalStorageDirectory() + "/r.html";

    public static boolean savePathToRerouteHtml(String httpUrl) {
        String newLine = System.getProperty("line.separator");

        //
        StringBuilder content = new StringBuilder("<!DOCTYPE html>" + newLine).append("<head>").append(newLine)//
                .append("<meta http-equiv=\"refresh\" content=\"0" + ";url='")//
                .append(httpUrl + "'\" />" + newLine)
                .append("</head>" + newLine + "</html>");
        //  String output = content.toString();
        Log.e(TAG, "savePathToRerouteHtml= " + content.toString());
        try {
            FileWriter writer = new FileWriter(reroutedHtml);
            writer.write(content.toString());
            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isOnline(String path) {
        Log.e(TAG, " sSpecialSource " + path);
        return path.toLowerCase().contains("http://")
                || path.contains("https://")
                || path.contains("asset://")
                || path.contains("ftp://")
                || path.contains("mms:")
                || path.contains("rtmp:")
                || path.contains("rtsp")
                || path.contains("smb://")
                || path.contains("gdrive:")
                || path.contains("box:")
                || path.contains("dropbox:")
                || path.contains("onedrive:");
    }
}