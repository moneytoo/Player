package com.brouken.player;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.google.android.exoplayer2.util.MimeTypes;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class SubtitleUtils {

    public static String getSubtitleMime(Uri uri) {
        final String path = uri.getPath();
        if (path.endsWith(".ssa") || path.endsWith(".ass")) {
            return MimeTypes.TEXT_SSA;
        } else if (path.endsWith(".vtt")) {
            return MimeTypes.TEXT_VTT;
        } else if (path.endsWith(".ttml") ||  path.endsWith(".xml") || path.endsWith(".dfxp")) {
            return MimeTypes.APPLICATION_TTML;
        } else {
            return MimeTypes.APPLICATION_SUBRIP;
        }
    }

    public static String getSubtitleLanguage(Uri uri) {
        final String path = uri.getPath();

        if (path.endsWith(".srt")) {
            int last = path.lastIndexOf(".");
            int prev = last;

            for (int i = last; i >= 0; i--) {
                prev = path.indexOf(".", i);
                if (prev != last)
                    break;
            }

            int len = last - prev;

            if (len >= 2 && len <= 6) {
                // TODO: Validate lang
                return path.substring(prev + 1, last);
            }
        }

        return null;
    }

    /*
    public static DocumentFile findUriInScope(DocumentFile documentFileTree, Uri uri) {
        for (DocumentFile file : documentFileTree.listFiles()) {
            if (file.isDirectory()) {
                final DocumentFile ret = findUriInScope(file, uri);
                if (ret != null)
                    return ret;
            } else {
                final Uri fileUri = file.getUri();
                if (fileUri.toString().equals(uri.toString())) {
                    return file;
                }
            }
        }
        return null;
    }
    */

    public static DocumentFile findUriInScope(Context context, Uri scope, Uri uri) {
        DocumentFile treeUri = DocumentFile.fromTreeUri(context, scope);
        String[] trailScope = getTrailFromUri(scope);
        String[] trailVideo = getTrailFromUri(uri);

        for (int i = 0; i < trailVideo.length; i++) {
            if (i < trailScope.length) {
                if (!trailScope[i].equals(trailVideo[i]))
                    break;
            } else {
                treeUri = treeUri.findFile(trailVideo[i]);
                if (treeUri == null)
                    break;
            }
            if (i + 1 == trailVideo.length)
                return treeUri;
        }
        return null;
    }

    public static DocumentFile findDocInScope(DocumentFile scope, DocumentFile doc) {
        if (doc == null || scope == null)
            return null;
        for (DocumentFile file : scope.listFiles()) {
            if (file.isDirectory()) {
                final DocumentFile ret = findDocInScope(file, doc);
                if (ret != null)
                    return ret;
            } else {
                //if (doc.length() == file.length() && doc.lastModified() == file.lastModified() && doc.getName().equals(file.getName())) {
                // lastModified is zero when opened from Solid Explorer
                final String docName = doc.getName();
                final String fileName = file.getName();
                if (docName == null || fileName == null) {
                    continue;
                }
                if (doc.length() == file.length() && docName.equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    public static String getTrailPathFromUri(Uri uri) {
        String path = uri.getPath();
        String[] array = path.split(":");
        if (array.length > 1) {
            return array[array.length - 1];
        } else {
            return path;
        }
    }

    public static String[] getTrailFromUri(Uri uri) {
        if ("org.courville.nova.provider".equals(uri.getHost()) && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path.startsWith("/external_files/")) {
                return path.substring("/external_files/".length()).split("/");
            }
        }
        return getTrailPathFromUri(uri).split("/");
    }

    private static String getFileBaseName(String name) {
        if (name.indexOf(".") > 0)
            return name.substring(0, name.lastIndexOf("."));
        return name;
    }

    public static DocumentFile findSubtitle(DocumentFile video) {
        DocumentFile dir = video.getParentFile();
        return findSubtitle(video, dir);
    }

    public static DocumentFile findSubtitle(DocumentFile video, DocumentFile dir) {
        String videoName = getFileBaseName(video.getName());
        int videoFiles = 0;

        if (dir == null || !dir.isDirectory())
            return null;

        List<DocumentFile> candidates = new ArrayList<>();

        for (DocumentFile file : dir.listFiles()) {
            if (file.getName().startsWith("."))
                continue;
            if (isSubtitleFile(file))
                candidates.add(file);
            if (isVideoFile(file))
                videoFiles++;
        }

        if (videoFiles == 1 && candidates.size() == 1) {
            return candidates.get(0);
        }

        if (candidates.size() >= 1) {
            for (DocumentFile candidate : candidates) {
                if (candidate.getName().startsWith(videoName + '.')) {
                    return candidate;
                }
            }
        }

        return null;
    }

    public static DocumentFile findNext(DocumentFile video) {
        DocumentFile dir = video.getParentFile();
        return findNext(video, dir);
    }

    public static DocumentFile findNext(DocumentFile video, DocumentFile dir) {
        DocumentFile list[] = dir.listFiles();
        Arrays.sort(list, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        final String videoName = video.getName();
        boolean matchFound = false;

        for (DocumentFile file : list) {
            if (file.getName().equals(videoName)) {
                matchFound = true;
            } else if (matchFound) {
                if (isVideoFile(file)) {
                    return file;
                }
            }
        }

        return null;
    }

    public static boolean isVideoFile(DocumentFile file) {
        return file.isFile() && file.getType().startsWith("video/");
    }

    public static boolean isSubtitleFile(DocumentFile file) {
        if (!file.isFile())
            return false;
        final String name = file.getName();
        return name.endsWith(".srt") || name.endsWith(".ssa") || name.endsWith(".ass")
                || name.endsWith(".vtt") || name.endsWith(".ttml");
    }

    public static void clearCache(Context context) {
        try {
            for (File file : context.getCacheDir().listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Uri convertToUTF(Context context, Uri subtitleUri) {
        try {
            final CharsetDetector detector = new CharsetDetector();
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(context.getContentResolver().openInputStream(subtitleUri));
            detector.setText(bufferedInputStream);
            final CharsetMatch charsetMatch = detector.detect();

            if (!StandardCharsets.UTF_8.displayName().equals(charsetMatch.getName())) {
                String filename = subtitleUri.getPath();
                filename = filename.substring(filename.lastIndexOf("/") + 1);
                final File file = new File(context.getCacheDir(), filename);
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    stream.write(charsetMatch.getString().getBytes());
                    subtitleUri = Uri.fromFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subtitleUri;
    }
}
