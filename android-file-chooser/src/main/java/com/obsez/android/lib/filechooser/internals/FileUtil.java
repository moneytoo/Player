package com.obsez.android.lib.filechooser.internals;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by coco on 6/7/15.
 */
public class FileUtil {


    public static String getExtension(File file) {
        if (file == null) {
            return null;
        }

        int dot = file.getName().lastIndexOf(".");
        if (dot >= 0) {
            return file.getName().substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    public static String getExtensionWithoutDot(File file) {
        String ext = getExtension(file);
        if (ext.length() == 0) {
            return ext;
        }
        return ext.substring(1);
    }

    private static final class Constants {
        final static int BYTES_IN_KILOBYTES = 1024;
        final static String BYTES = " B";
        final static String KILOBYTES = " KB";
        final static String MEGABYTES = " MB";
        final static String GIGABYTES = " GB";
    }

    public static String getReadableFileSize(long size) {
        float fileSize = 0;
        String suffix = Constants.KILOBYTES;

        if (size < Constants.BYTES_IN_KILOBYTES) {
            fileSize = size;
            suffix = Constants.BYTES;
        } else {
            fileSize = (float) size / Constants.BYTES_IN_KILOBYTES;
            if (fileSize >= Constants.BYTES_IN_KILOBYTES) {
                fileSize = fileSize / Constants.BYTES_IN_KILOBYTES;
                if (fileSize >= Constants.BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / Constants.BYTES_IN_KILOBYTES;
                    suffix = Constants.GIGABYTES;
                } else {
                    suffix = Constants.MEGABYTES;
                }
            }
        }
        return String.valueOf(new DecimalFormat("###.#").format(fileSize) + suffix);
    }

    @Nullable
    public static File getDefaultPathAsFile(Context context, boolean isRemovable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            String path = getDefaultPath(context, isRemovable);
            return new File(path);
        } else {
            return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
    }

    @NonNull
    public static String getDefaultPath(Context context, boolean isRemovable) {
        String path;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            path = getStoragePath(context, isRemovable);
        } else {
            path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        }
        return path;
    }

    @NonNull
    public static String getVolDescription(Context context, StorageVolume vol) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return getVolDescLow(context,  vol);
        } else {
            return getVolDesc24(context, vol);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    private static String getVolDesc24(Context context, StorageVolume vol) {
        return vol.getDescription(context);
    }

    @NonNull
    private static String getVolDescLow(Context context, StorageVolume vol) {
        try {
            Method getDesc = vol.getClass().getMethod("getDescription");
            Object result = getDesc.invoke(vol, context);
            return (String) result;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return vol.toString();
    }

    @NonNull
    public static List<StorageVolume> getStorageVols(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return getStorageVolsLow(context);
        } else {
            return getStorageVols24(context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    private static List<StorageVolume> getStorageVols24(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            List<StorageVolume> result = Objects.requireNonNull(storageManager).getStorageVolumes();
            return result;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @NonNull
    private static List<StorageVolume> getStorageVolsLow(Context context) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Object result = getVolumeList.invoke(storageManager);
            return (List<StorageVolume>) result;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @NonNull
    public static String getStoragePath(Context context, boolean isRemovable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return getStoragePathLow(context, isRemovable);
        } else {
            return getStoragePath24(context, isRemovable);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    public static String getStoragePath24(Context context, boolean isRemovable) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            List<StorageVolume> result = Objects.requireNonNull(storageManager).getStorageVolumes();
            for (StorageVolume vol : result) {
                Log.d("X", "  ---Object--" + vol + " | desc: " + vol.getDescription(context));
                if (isRemovable != vol.isRemovable()) {
                    continue;
                }
                Method getPath = vol.getClass().getMethod("getPath");
                String path = (String) getPath.invoke(vol);
                Log.d("X", "    ---path--" + path);
                // HACK
                if (isRemovable && result.size() > 2 && path.startsWith("/storage/"))
                    return "/storage";
                else
                    return path;
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @NonNull
    public static String getStoragePathLow(Context context, boolean isRemovable) {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovableMtd = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(storageManager);
            final int length = Array.getLength(result);
            //final int length = result.size();
            Log.d("X", "---length--" + length);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                Log.d("X", "  ---Object--" + storageVolumeElement + "i==" + i);
                String path = (String) getPath.invoke(storageVolumeElement);
                Log.d("X", "  ---path_total--" + path);
                boolean removable = (Boolean) isRemovableMtd.invoke(storageVolumeElement);
                if (isRemovable == removable) {
                    Log.d("X", "    ---path--" + path);
                    // HACK
                    if (isRemovable && length > 2 && path.startsWith("/storage/"))
                        return "/storage";
                    else
                        return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static long readSDCard(Context context, Boolean isRemovable) {
        return readSDCard(context, isRemovable, false);
    }

    public static long readSDCard(Context context, Boolean isRemovable, Boolean freeOrTotal) {
        //DecimalFormat df = new DecimalFormat("0.00");
        String path = getStoragePath(context, isRemovable);
        try {
            StatFs sf = new StatFs(path);
            long blockSize;
            long blockCount;
            long availCount;
            if (Build.VERSION.SDK_INT > 18) {
                blockSize = sf.getBlockSizeLong(); //文件存储时每一个存储块的大小为4KB
                blockCount = sf.getBlockCountLong();//存储区域的存储块的总个数
                availCount = sf.getFreeBlocksLong();//存储区域中可用的存储块的个数（剩余的存储大小）
            } else {
                blockSize = sf.getBlockSize();
                blockCount = sf.getBlockCount();
                availCount = sf.getFreeBlocks();
            }
            //Log.d("sss", "总的存储空间大小:" + blockSize * blockCount / 1073741824 + "GB" + ",剩余空间:"
            //    + availCount * blockSize / 1073741824 + "GB"
            //    + "--存储块的总个数--" + blockCount + "--一个存储块的大小--" + blockSize / 1024 + "KB");
            //return df.format((freeOrTotal ? availCount : blockCount) * blockSize / 1073741824.0);
            return (freeOrTotal ? availCount : blockCount) * blockSize;
        } catch (Exception ex) {
            Log.e("X", String.format("wrong path '%s'", path), ex);
        }
        return -1;
    }


    public static void deleteFileRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            final File[] entries = file.listFiles();
            for (final File entry : entries) {
                deleteFileRecursively(entry);
            }
        }

        if (!file.delete()) {
            throw new IOException("Couldn't delete \"" + file.getName() + "\" at \"" + file.getParent());
        }
    }

    public static String getCurrentDir() {
        return new File("").getAbsolutePath();
    }

    public static File getCurrentDirectory() {
        return new File(new File("").getAbsolutePath());
    }

    public static boolean createNewDirectory(String name) {
        return createNewDirectory(name, getCurrentDirectory());
    }

    public static boolean createNewDirectory(String name, File parent) {
        final File newDir = new File(parent, name);
        if (!newDir.exists() && newDir.mkdir()) {
            //refreshDirs();
            return true;
        }
        //Toast.makeText(_context,
        //    "Couldn't create folder " + newDir.getName() + " at " + newDir.getAbsolutePath(),
        //    Toast.LENGTH_LONG).show();
        return false;
    }

    public static class NewFolderFilter implements InputFilter {
        private final int maxLength;
        private final Pattern pattern;

        /**
         * examples:
         * a simple allow only regex pattern: "^[a-z0-9]*$" (only lower case letters and numbers)
         * a simple anything but regex pattern: "^[^0-9;#&amp;]*$" (ban numbers and '&amp;', ';', '#' characters)
         */

        public NewFolderFilter() {
            this(255, "^[^/<>|\\\\:&;#\n\r\t?*~\0-\37]*$");
        }

        public NewFolderFilter(int maxLength) {
            this(maxLength, "^[^/<>|\\\\:&;#\n\r\t?*~\0-\37]*$");
        }

        public NewFolderFilter(String pattern) {
            this(255, pattern);
        }

        public NewFolderFilter(int maxLength, String pattern) {
            this.maxLength = maxLength;
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            Matcher matcher = pattern.matcher(source);
            if (!matcher.matches()) {
                return source instanceof SpannableStringBuilder ? dest.subSequence(dstart, dend) : "";
            }

            int keep = maxLength - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    --keep;
                    if (keep == start) {
                        return "";
                    }
                }
                return source.subSequence(start, keep).toString();
            }
        }
    }
}
