package com.brouken.player;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.accessibility.CaptioningManager;

import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.SubtitleView;

import com.brouken.player.osd.subtitle.SubtitleEdgeType;

import java.io.File;
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
        final String path = uri.getPath().toLowerCase();

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
            final String fileName = file.getName();
            if (fileName != null && fileName.startsWith("."))
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
        if (dir == null) {
            return null;
        }

        try {
            DocumentFile[] list = dir.listFiles();
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
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isVideoFile(DocumentFile file) {
        return file.isFile() && file.getType().startsWith("video/");
    }

    public static boolean isSubtitleFile(DocumentFile file) {
        if (!file.isFile())
            return false;
        final String name = file.getName().toLowerCase();
        return name.endsWith(".srt") || name.endsWith(".ssa") || name.endsWith(".ass")
                || name.endsWith(".vtt") || name.endsWith(".ttml");
    }

    public static boolean isSubtitle(Uri uri, String mimeType) {
        if (mimeType != null) {
            for (String mime : Utils.supportedMimeTypesSubtitle) {
                if (mimeType.equals(mime)) {
                    return true;
                }
            }
            if (mimeType.equals("text/plain") || mimeType.equals("text/x-ssa") || mimeType.equals("application/octet-stream") ||
                    mimeType.equals("application/ass") || mimeType.equals("application/ssa") || mimeType.equals("application/vtt")) {
                return true;
            }
        }
        if (uri != null) {
            if (Utils.isSupportedNetworkUri(uri)) {
                String path = uri.getPath();
                if (path != null) {
                    path = path.toLowerCase();
                    for (String extension : Utils.supportedExtensionsSubtitle) {
                        if (path.endsWith("." + extension)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

    public static MediaItem.SubtitleConfiguration buildSubtitle(Context context, Uri uri, String subtitleName, boolean selected) {
        final String subtitleMime = SubtitleUtils.getSubtitleMime(uri);
        final String subtitleLanguage = SubtitleUtils.getSubtitleLanguage(uri);
        if (subtitleLanguage == null && subtitleName == null)
            subtitleName = Utils.getFileName(context, uri);

        MediaItem.SubtitleConfiguration.Builder subtitleConfigurationBuilder = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(subtitleMime)
                .setLanguage(subtitleLanguage)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .setLabel(subtitleName);
        if (selected) {
            subtitleConfigurationBuilder.setSelectionFlags(C.SELECTION_FLAG_DEFAULT);
        }
        return subtitleConfigurationBuilder.build();
    }

    /*public static float normalizeFontScale(float fontScale, boolean small) {
        // https://bbc.github.io/subtitle-guidelines/#Presentation-font-size
        float newScale;
        // ¯\_(ツ)_/¯
        if (fontScale > 1.01f) {
            if (fontScale >= 1.99f) {
                // 2.0
                newScale = (small ? 1.15f : 1.2f);
            } else {
                // 1.5
                newScale = (small ? 1.0f : 1.1f);
            }
        } else if (fontScale < 0.99f) {
            if (fontScale <= 0.26f) {
                // 0.25
                newScale = (small ? 0.65f : 0.8f);
            } else {
                // 0.5
                newScale = (small ? 0.75f : 0.9f);
            }
        } else {
            newScale = (small ? 0.85f : 1.0f);
        }
        return newScale;
    }*/

    public static void updateFractionalTextSize(SubtitleView subtitleView, CaptioningManager captioningManager, Prefs prefs) {
        float fontScale = captioningManager.getFontScale();
        int subtitleSize = prefs.subtitleSize;
        float fractionalTextSize = (SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * fontScale) + (subtitleSize * 0.001f);
        subtitleView.setFractionalTextSize(fractionalTextSize);
    }

    public static @CaptionStyleCompat.EdgeType int getSubtitleEdgeType(SubtitleEdgeType subtitleEdgeType, CaptioningManager.CaptionStyle captionStyle) {
        switch (subtitleEdgeType) {
            case Default:
                return captionStyle.hasEdgeType() ? captionStyle.edgeType : CaptionStyleCompat.EDGE_TYPE_OUTLINE;
            case None:
                return CaptionStyleCompat.EDGE_TYPE_NONE;
            case Outline:
                return CaptionStyleCompat.EDGE_TYPE_OUTLINE;
            case DropShadow:
                return CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW;
            case Raised:
                return CaptionStyleCompat.EDGE_TYPE_RAISED;
            case Depressed:
                return CaptionStyleCompat.EDGE_TYPE_DEPRESSED;
            default:
                throw new IllegalArgumentException("Unknown subtitleEdgeType: " + subtitleEdgeType);
        }
    }
}
