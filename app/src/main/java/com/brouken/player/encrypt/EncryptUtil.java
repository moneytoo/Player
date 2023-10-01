package com.brouken.player.encrypt;


import static com.brouken.player.encrypt.Constants.AES_ALGORITHM;
import static com.brouken.player.encrypt.Constants.CTR_TRANSFORMATION;
import static com.brouken.player.encrypt.Constants.DOWNLOAD_BROADCAST;
import static com.brouken.player.encrypt.Constants.DOWNLOAD_FULL_NAME;
import static com.brouken.player.encrypt.Constants.DOWNLOAD_URL;
import static com.brouken.player.encrypt.Constants.ENCRYPT_SKIP;
import static com.brouken.player.encrypt.NyFileUtil.convertFileToString;
import static com.brouken.player.encrypt.NyFileUtil.getFileExtension;
import static com.brouken.player.encrypt.NyFileUtil.getFileNameWithoutExtFromPath;
import static com.brouken.player.encrypt.NyFileUtil.getTypeFromName;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {

 /*   public static HybridFile ConstructEncryptTarget(HybridFile targetFile, String password) {
        OpenMode mode = targetFile.getMode();
        String tarDir = targetFile.getPath();  //assume that targetFile is a directory
        if (!targetFile.isDirectory(App.getInstance()))
            tarDir = targetFile.getParent(App.getInstance());
        String newName = getSecureFileName(targetFile.getSimpleName(), password);
        return new HybridFile(mode, tarDir, newName, false);
    }

    public static HybridFile ConstructEncryptTarget(String filename, String password) {
        SecureSharedPreferences sp = new SecureSharedPreferences(App.getInstance(), "General");
        int modeI = sp.getInt(KEY_MODE, 1);
        OpenMode mode = getOpenMode(modeI);
        String tarDir = sp.getString(DEFAULT_ENCRYPTION, NyFileUtil.getVideoDir());
        Log.e("Encrypt Utility", "tarDir" + tarDir);

        filename = getSecureFileName(filename, password);
        return new HybridFile(mode, tarDir, filename, false);
    }

    public static void startBatchEncryption(Context context, HybridFileParcelable[] files, OpenMode mode) {
        ArrayList<HybridFileParcelable> sourceFiles = new ArrayList<>(Arrays.asList(files));
        Intent intent = new Intent(context, nyEncryptService.class);
        intent.putParcelableArrayListExtra(nyEncryptService.TAG_COPY_SOURCES, sourceFiles);
        if (mode == null)
            mode = ((MainActivity) context).getCurrentMainFragment().getMainFragmentViewModel().getOpenMode();
        intent.putExtra(nyEncryptService.TAG_COPY_OPEN_MODE, mode);
        ServiceWatcherUtil.runService(context, intent);
    }


    public static String getDefaultPassword() {
        String password = null;
        SecureSharedPreferences sp = new SecureSharedPreferences(App.getInstance(), "General");
        int encrypted = sp.getInt(KEY_CODE, 1);

        if (encrypted == 0) {
            password = sp.getString(KEY_PW, "nanyangtaiji1234");
        } else {
            if (sp.getBoolean(KEY_OVERHEAD, false)) encrypted = encrypted + 5;
            password = Integer.toString(encrypted);
        }
        return password;
    }*/

    //-------------------------------------------------------------//
    public static class CTRnoPadding {
        public SecretKeySpec secretKeySpec;
        public IvParameterSpec ivParameterSpec;
        public Cipher cipher;
    }

    //Level 0  video specific password
    public static Cipher CipherFromPath(String path) {
        if (!path.contains("_NY")) return null;
        else return LevelCipherPackage(encryptLevelFromFileName(path)).cipher;
    }


    public static Cipher LevelCipherOnly(int encryptLevel) {
        return LevelCipherPackage(encryptLevel).cipher;
    }

    //Level 0  video specific password
    public static Cipher LevelCipherOnly(String password) {
        return LevelCipherPackage(password).cipher;
    }

    public static CTRnoPadding LevelCipherPackage(String password) {
        CTRnoPadding mCTR = new CTRnoPadding();
        byte[] key = null;
        byte[] iv = null;

        if (password.equals("1") || password.equals("6")) {
            key = "nanyangtaiji1234".getBytes();
            iv = "1958061019621212".getBytes();
        } else if (password.equals("2") || password.equals("7")) {
            key = "1958061019621212".getBytes();
            iv = "nanyangtaiji1234".getBytes();
        } else if (password.equals("3") || password.equals("8")) {
            key = "1962121219580610".getBytes();
            iv = "nanyangtaiji1234".getBytes();
        } else if (password.equals("4") || password.equals("9")) {
            key = "1234nanyangtaiji".getBytes();
            iv = "1958061019621212".getBytes();
        } else if (password.equals("5")) {
            key = "taiji1234nanyang".getBytes();
            iv = "1962121219580610".getBytes();
        } else if (password.equals("0") || password.equals("P")) {
            //  SharedPreferences sp=activity.getSharedPreferences(ENCRYPTION_SETTINGS, Context.MODE_PRIVATE);
            //  password = sp.getString(KEY_PW, "nanyangtaiji1234");
            //TODO to be update (2022-4-20)
            password = "nanyangtaiji1234";
           // Log.e("EncryptUtil password2 =", password);
            key = password.getBytes();
            iv = password.getBytes();
        } else if (password.length() > 1) {
            if (password.length() < 16) password = password + "nanyantaiji1234";
            password = password.trim().substring(0, 16);
            //   Log.e("EncryptUtil password3 =", password);
            key = password.getBytes();
            iv = password.getBytes();
        }

        mCTR.secretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        mCTR.ivParameterSpec = new IvParameterSpec(iv);
        try {
            mCTR.cipher = Cipher.getInstance(CTR_TRANSFORMATION);
            mCTR.cipher.init(Cipher.ENCRYPT_MODE, mCTR.secretKeySpec, mCTR.ivParameterSpec);
        } catch (Exception e) {
            Log.e("EncryptUtil", " LevelCipherPackage mCES" + e.toString());
            // e.printStackTrace();
        }

        return mCTR;
    }


    private static CTRnoPadding LevelCipherPackage(int encryptLevel) {
        // if (encryptLevel==0)
        return LevelCipherPackage(String.valueOf(encryptLevel));
    }

    public static CTRnoPadding CipherPackageFromPath(String mUrl) {
        return LevelCipherPackage(getPasswordFromFileName(mUrl));
    }


    public Cipher CTRNoppadingCipher(String KeyS, String ivS) {
        byte[] key = KeyS.getBytes();
        byte[] iv = ivS.getBytes();
        Cipher mCipher;
        SecretKeySpec mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        IvParameterSpec mIvParameterSpec = new IvParameterSpec(iv);
        try {
            mCipher = Cipher.getInstance(CTR_TRANSFORMATION);
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
            return mCipher;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static int encryptLevelFromFileName(String mUrl) {
        int tempI = -1;

        if (mUrl.contains("_NY")) {
            tempI = Character.getNumericValue(mUrl.charAt(mUrl.lastIndexOf('Y') + 1));
        }
        if (mUrl.contains(".HW")) {
            tempI = Character.getNumericValue(mUrl.charAt(mUrl.lastIndexOf('W') + 1));
        }
        //  Toast.makeText(this, "Encryption:" + tempI, Toast.LENGTH_LONG).show();
        return (tempI > 5) ? tempI - 5 : tempI;
    }

    public static int encryptLevelFromPassword(String password) {
        int level = -1;

        if (password == null || password == "N") return -1;

        if (password.equals("1") || password.equals("6")) {
            level = 1;
        } else if (password.equals("2") || password.equals("7")) {
            level = 2;
        } else if (password.equals("3") || password.equals("8")) {
            level = 3;
        } else if (password.equals("4") || password.equals("9")) {
            level = 4;
        } else if (password.equals("5")) {
            level = 5;
        } else if (password.length() > 1 || password.equals("0") || password.equals("P")) {
            //  Log.i("this-----------------------",password);
            level = 0;
        }

        return level;
    }

    public static String getPasswordFromFileName(String url) {
        String password = null;
        if (url.contains("_NY")) {
            password = url.substring(url.lastIndexOf("_NY") + 3, url.lastIndexOf("_NY") + 4);
        } else if (url.contains(".HW")) {
            password = url.substring(url.lastIndexOf(".HW") + 3, url.lastIndexOf(".HW") + 4);
        }
        return password;
    }

    public static String getSecureFileName(String path, String passWord) {
        String onlyFileName = getFileNameWithoutExtFromPath(path);

        String nyx = "_NY" + passWord;
        if (passWord.length() > 1) nyx = "_NY0";
        if (onlyFileName.contains(nyx)) {
            //decrypted filename to the origin
            onlyFileName = onlyFileName.replace(nyx, "");  //reset
        } else
            // encrypted filename
            onlyFileName = onlyFileName + nyx;

        String ext = getFileExtension(path);
        return onlyFileName + "." + ext;
    }


    //---------------------------------------


    public static void defaultEncryptFiles(Context context, final List<String> fileList,
                                           String passWord, final ManipulateCallback<String> callback) {
        int size = fileList.size();
        boolean isLast = false;
        String filePath;
        for (int i = 0; i < size; i++) {
            filePath = fileList.get(i);
            if (i == size - 1) isLast = true;
            defaultEncryptFile(context, filePath, passWord, callback, isLast);
        }
    }


    public static void defaultEncryptFile(final Context context, final String filePath, String
            passWord, final ManipulateCallback<String> callback, boolean isLast) {
        defaultEncryptFile(context, new File(filePath), passWord);
        if (isLast && callback != null) callback.manipulateCallBack("Encrypt");
    }


    public static boolean defaultEncryptFile(Context context, File infile, String passWord) {
        String infilePath = infile.getAbsolutePath();
        String parentPath = infile.getParent();
        String ext = getFileExtension(infilePath);
        String newName = getFileNameWithoutExtFromPath(infilePath) + "_NY0." + ext;
        // Toast.makeText(context, newName, Toast.LENGTH_LONG).show();
        File outfile = new File(parentPath, newName);
        Cipher cipher = LevelCipherOnly(passWord);

        return StandardEncryptFile(context, infile, outfile, cipher);
    }

    public static boolean StandardEncryptFile(Context context, File infile, File
            outfile, String passWord) {
        return StandardEncryptFile(context, infile, outfile, LevelCipherOnly(passWord));
    }

    public static boolean StandardEncryptFile(Context context, File infile, File
            outfile, Cipher cipher) {
        long total = 0;
        long fileLength;
        byte[] IgnoredBuffer;
        byte[] buffer;
        InputStream inputStream;
          NormalProgressDialog
               .showLoading(context, infile.getName() + " is encrypting!", false);

        try {
            inputStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            FileOutputStream fileOutputStream = new FileOutputStream(outfile);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);
            buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                //   onProgressChanged(fileLength, total, "Encrypted");
            }
            inputStream.close();
            cipherOutputStream.close();

            if (outfile.exists() && outfile.length() != 0) {
                String mimeType = getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, outfile);
                }
                NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            Log.e("EncetptUtil", e.toString());
        }
        return false;
    }

    //just merge a file with a encryptedFile
    public static boolean MergedWithEncryptedFile(Context context, File infile, File
            outfile, Cipher cipher, File mergeF) {

        int bytesRead;
        try {
            long total = 0;
            byte[] buffer = new byte[1024];

            InputStream sourceStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            FileOutputStream fileOutputStream = new FileOutputStream(outfile);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);

            if (mergeF != null) {
                InputStream mergeStream = context.getContentResolver().openInputStream(Uri.fromFile(mergeF));
                while ((bytesRead = mergeStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    total += bytesRead;           //in terms of MB
                    //   onProgressChanged(fileLength, total, "Encrypted");
                }
                mergeStream.close();
            }

            while ((bytesRead = sourceStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                //   onProgressChanged(fileLength, total, "Encrypted");
            }
            sourceStream.close();
            cipherOutputStream.close();

            if (outfile.exists() && outfile.length() != 0) {
                String mimeType = getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, outfile);
                }
                NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean EncryptMergedFile(Context context, File infile, File outfile, String
            passWord, File mergeF) {
        return EncryptMergedFile(context, infile, outfile, LevelCipherOnly(passWord), mergeF);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean EncryptMergedFile(Context context, File infile, File outfile, Cipher
            cipher, File mergeF) {

        int bytesRead;
        try {
            long total = 0;
            byte[] buffer = new byte[1024];

            //  Log.e("EncryptMergedFile","infile "+infile.length());
            //  Log.e("EncryptMergedFile","mergeF "+mergeF.length());
            InputStream sourceStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            FileOutputStream fos = new FileOutputStream(outfile);
            //  DataOutputStream dos=null;
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fos, cipher);
            if (mergeF != null && mergeF.exists() && mergeF.canRead() && mergeF.length() > 0) {
                //  int extraLength=(int) mergeF.length()+4; //for bytes for integer
                //  Log.e("EncryptMergedFile","extraLength "+extraLength);
                //   fos.write(extraLength);
                //  dos = new DataOutputStream(fos);
                //  dos.write(extraLength);
                InputStream mergeStream = context.getContentResolver().openInputStream(Uri.fromFile(mergeF));
                while ((bytesRead = mergeStream.read(buffer)) != -1) {
                    cipherOutputStream.write(buffer, 0, bytesRead);
                    total += bytesRead;           //in terms of MB
                }
                mergeStream.close();
            } else {
                //write random 2MB string
                Random rd = new Random();
                byte[] arr = new byte[(int) ENCRYPT_SKIP];
                rd.nextBytes(arr);
                cipherOutputStream.write(arr);
            }
            while ((bytesRead = sourceStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
            }
            sourceStream.close();
            cipherOutputStream.close();
            //  dos.close();
            if (outfile.exists() && outfile.length() != 0) {
                //  Log.e("EncryptMergedFile","output "+outfile.length());
                String mimeType = getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, outfile);
                }
                 NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static boolean DecryptMergedFile(Context context, File infile, File
            outfile, CTRnoPadding ces, File mergeF) {
        int bytesRead;
        byte[] buffer = new byte[1024];

        try {
            InputStream sourceStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            CipherInputStream cipherinputputStream = new CipherInputStream(sourceStream, ces.cipher);
            FileOutputStream fos = new FileOutputStream(outfile);
            if (mergeF != null) {
                FileOutputStream fom = new FileOutputStream(mergeF);
                byte[] bufferM = new byte[(int) ENCRYPT_SKIP];
                cipherinputputStream.read(bufferM);
                fom.write(bufferM, 0, (int) ENCRYPT_SKIP);
                fom.close();
                Log.e("DecryptMergedFile", "head " + mergeF.length());
            } else {
                sourceStream.skip(ENCRYPT_SKIP);
                AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, ENCRYPT_SKIP);
            }

            while ((bytesRead = cipherinputputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            sourceStream.close();
            cipherinputputStream.close();
            fos.close();
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }

        if (outfile.exists() && outfile.length() != 0) {
            Log.e("DecryptMergedFile", "output " + outfile.length());
            String mimeType = getTypeFromName(infile.getName());
            if (mimeType.contains("video")) {
                registerEncryptedVideo(context, outfile);
            }
            return true;
        }
        return false;
    }

    public static byte[] EncryptFileToBuffer(Context context, String filePath) {
        InputStream inputStream;
        String passWord = getPasswordFromFileName(filePath);
        File infile = new File(filePath);
        byte[] buffer = new byte[(int) infile.length()];
        try {
            inputStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            CTRnoPadding ces = EncryptUtil.LevelCipherPackage(passWord);
            if (Integer.parseInt(passWord) > 5) {
                inputStream.skip(ENCRYPT_SKIP);
                AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, ENCRYPT_SKIP);
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, ces.cipher);
            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(data)) != -1) {
                bufferStream.write(data, 0, bytesRead);
            }
            inputStream.close();
            cipherInputStream.close();
            bufferStream.flush();
            buffer = bufferStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("EncryptFileToBuffer", "buffer " + buffer.length);
        return buffer;
    }


    private static final int headSkip = 50 * 1024;

   /* public static boolean AdvancedEncryptFile(Context context, File infile, File outfile, String
            passWord, boolean addBlock) {
        long total = 0;
        long fileLength;
        byte[] IgnoredBuffer;
        byte[] buffer;
        InputStream inputStream;
        //delay the other operation
        //   NormalProgressDialog
        //          .showLoading(context, infile.getName() + " is encrypting!", true);
        try {
            //  Toast.makeText(getContext(), "password: "+sp.getString(KEY_PW, "nanyangtaiji1234"), Toast.LENGTH_LONG).show();
            Cipher mCipher = LevelCipherOnly(passWord);

            inputStream = new FileInputStream(infile);
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, mCipher);
            fileLength = inputStream.available();
            FileOutputStream fileOutputStream = new FileOutputStream(outfile);
            int bytesRead;
            if (addBlock) {
                InputStream clone = new FileInputStream(infile);
                IgnoredBuffer = new byte[2 * 1024 * 1024];
                bytesRead = clone.read(IgnoredBuffer);
                fileOutputStream.write(IgnoredBuffer, 0, bytesRead);
                clone.close();
                //first mb does not encrypt
            }

            if (fileLength == 0) {
                buffer = new byte[10 * 1024 * 1024];
            } else if (fileLength > 1024 * 1024) {
                buffer = new byte[1024 * 1024];
            } else buffer = new byte[1024];


            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                //   onProgressChanged(fileLength, total, "Encrypted");
            }

            fileOutputStream.close();
            cipherInputStream.close();
            inputStream.close();


            if (outfile.exists() && outfile.length() != 0) {

                String mimeType = NyFileUtil.getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, infile, outfile);
                }
                NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }*/

    public static boolean AdvancedEncryptFile(Context context, File infile, File
            outfile, String passWord) {
        return AdvancedEncryptFile(context, infile, outfile, passWord, 0);
    }

  /*  public static boolean AdvancedEncryptFile(Context context, File infile, File
            outfile, String passWord, int byteSkipped, ProgressHandler progressHandler) {
        long total = 0;
        long fileLength;
        byte[] IgnoredBuffer;
        byte[] buffer;
        InputStream inputStream;
        //   NormalProgressDialog
        //    .showLoading(context, infile.getName() + " is encrypting!", false);
        try {
            inputStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            assert inputStream != null;
            fileLength = inputStream.available();
            CTRnoPadding ces = EncryptUtil.LevelCipherPackage(passWord);
            if (passWord.length() == 1 && Integer.parseInt(passWord) > 5) {
                // Log.e(TAG, "passWord "+ passWord);
                inputStream.skip(ENCRYPT_SKIP);
                AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, ENCRYPT_SKIP);
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, ces.cipher);

            if (fileLength == 0) {
                buffer = new byte[10 * 1024 * 1024];
            } else if (fileLength > 1024 * 1024) {
                buffer = new byte[1024 * 1024];
            } else buffer = new byte[1024];

            int bytesRead;
            FileOutputStream fileOutputStream = new FileOutputStream(outfile, false);
            if (byteSkipped > 0) {
                IgnoredBuffer = new byte[byteSkipped];
                bytesRead = cipherInputStream.read(IgnoredBuffer);
                fileOutputStream.write(IgnoredBuffer, 0, bytesRead);
                //first mb does not encrypt
            }

            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                //   onProgressChanged(fileLength, total, "Encrypted");
            }
            inputStream.close();
            cipherInputStream.close();
            fileOutputStream.close();

            if (outfile.exists() && outfile.length() != 0) {
                // Log.e("EncryptUtil", "AdvancedEncryptFile   " + outfile.getPath());
                String mimeType = getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, outfile);
                }
               // NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            Log.e("EncryptUtil", "AdvancedEncryptFile fail  " + e.toString());
            //  e.printStackTrace();
        }
        return false;
    }*/


    public static boolean AdvancedEncryptFile(Context context, File infile, File
            outfile, String passWord, int byteSkipped) {
        long total = 0;
        long fileLength;
        byte[] IgnoredBuffer;
        byte[] buffer;
        InputStream inputStream;
          NormalProgressDialog
         .showLoading(context, infile.getName() + " is encrypting!", false);
        try {
            inputStream = context.getContentResolver().openInputStream(Uri.fromFile(infile));
            assert inputStream != null;
            fileLength = inputStream.available();
            CTRnoPadding ces = EncryptUtil.LevelCipherPackage(passWord);
            if (passWord.length() == 1 && Integer.parseInt(passWord) > 5) {
                // Log.e(TAG, "passWord "+ passWord);
                inputStream.skip(ENCRYPT_SKIP);
                AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, ENCRYPT_SKIP);
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, ces.cipher);

            if (fileLength == 0) {
                buffer = new byte[10 * 1024 * 1024];
            } else if (fileLength > 1024 * 1024) {
                buffer = new byte[1024 * 1024];
            } else buffer = new byte[4 * 1024];

            int bytesRead;
            FileOutputStream fileOutputStream = new FileOutputStream(outfile, false);
            if (byteSkipped > 0) {
                IgnoredBuffer = new byte[byteSkipped];
                bytesRead = cipherInputStream.read(IgnoredBuffer);
                fileOutputStream.write(IgnoredBuffer, 0, bytesRead);
                //first mb does not encrypt
            }

            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
                //   onProgressChanged(fileLength, total, "Encrypted");
            }
            inputStream.close();
            cipherInputStream.close();
            fileOutputStream.close();

            if (outfile.exists() && outfile.length() != 0) {
                // Log.e("EncryptUtil", "AdvancedEncryptFile   " + outfile.getPath());
                String mimeType = getTypeFromName(infile.getName());
                if (mimeType.contains("video")) {
                    registerEncryptedVideo(context, outfile);
                }
                NormalProgressDialog.stopLoading();
                return true;
            }
        } catch (Exception e) {
            Log.e("EncryptUtil", "AdvancedEncryptFile fail  " + e.toString());
            //  e.printStackTrace();
        }
        return false;
    }

    public static boolean DecryptExtractFirstPart(Context context, int length, File infile, File
            outfile, Cipher cipher) {

        return DecryptExtractFirstPartFromLocal(context.getContentResolver(), length, infile, outfile, cipher);

    }

    public static boolean DecryptExtractFirstPart(Context context, int length, String
            inPath, File outfile, Cipher cipher) {

        if (NyFileUtil.isOnline(inPath))
            return DecryptExtractFirstPartFromOnline(context, length, inPath, outfile, cipher);
        else
            return DecryptExtractFirstPartFromLocal(context.getContentResolver(), length, new File(inPath), outfile, cipher);

    }


    public static boolean DecryptExtractFirstPartFromLocal(ContentResolver contentResolver,
                                                           int length, File infile, File outfile, Cipher cipher) {

        try {
            InputStream inputStream = contentResolver.openInputStream(Uri.fromFile(infile));
            FileOutputStream fileOutputStream = new FileOutputStream(outfile);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);
            byte[] buffer = new byte[length];
            int bytesRead = inputStream.read(buffer);
            cipherOutputStream.write(buffer, 0, bytesRead);
            inputStream.close();
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean DecryptExtractFirstPartFromOnline(Context context, int length, String
            inPath, File outfile, Cipher cipher) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        CipherOutputStream cipherOutputStream = null;
        try {
            URL url = new URL(inPath);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            inputStream = conn.getInputStream();
            fileOutputStream = new FileOutputStream(outfile);
            cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);
            byte[] buffer = new byte[length];
            int bytesRead = inputStream.read(buffer);
            cipherOutputStream.write(buffer, 0, bytesRead);
            inputStream.close();
            fileOutputStream.close();
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e("EncryptUtil", "DecryptExtractFirstPartFromOnline Exception:" + e.toString());
            e.printStackTrace();
        }
        return false;
    }


    public static boolean EncryptFileHead(Context context, int length, String infilePath) {
        File infile = new File(infilePath);
        String passWord = getPasswordFromFileName(infile.getName());
        return EncryptFileHead(context, length, infile, passWord);
    }

    public static boolean EncryptFileHead(Context context, int length, File infile) {
        String passWord = getPasswordFromFileName(infile.getName());
        return EncryptFileHead(context, length, infile, passWord);
    }

    public static boolean EncryptFileHead(ContentResolver contentResolve, int length, File
            infile, String passWord) {
        if (passWord == null) passWord = "1";
        Cipher cipher = LevelCipherOnly(passWord);
        File temp = new File(NyFileUtil.getVideoDir(), "temp");
        if (DecryptExtractFirstPartFromLocal(contentResolve, length, infile, temp, cipher)) {
            try {
                byte[] buffer = new byte[length];
                RandomAccessFile filehead = new RandomAccessFile(infile, "rw");
                InputStream inputStream = contentResolve.openInputStream(Uri.fromFile(temp));
                inputStream.read(buffer);
                filehead.write(buffer);
                inputStream.close();
                filehead.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Toast.makeText(context, "Head error", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private static boolean EncryptFileHead(Context context, int length, File infile, String
            passWord) {
        return EncryptFileHead(context.getContentResolver(), length, infile, passWord);
    }


    /*
      new Thread(new Runnable() {
        @Override
        public void run() {
            onEncryptDocuments(activity, encryptDocs);
        }
    }).start();*/

    public static void DecryptNYFile(Context context, String inPath, String outPath) {
        AdvancedEncryptFile(context, new File(inPath), new File(outPath), getPasswordFromFileName(inPath), 0);
    }


    public static boolean EncryptUrlToFilePath(Context context, String inUrl, String
            outUrl, Cipher cipher) {
        return EncryptUriToFilePath(context, Uri.parse(inUrl), outUrl, cipher);
    }

    public static boolean EncryptUriToFilePath(Context context, Uri inUri, String
            outUrl, Cipher cipher) {
        long total = 0;
        long fileLength;

        InputStream inputStream;
        try {
            byte[] buffer;
            inputStream = context.getContentResolver().openInputStream(inUri);
            FileOutputStream fileOutputStream = new FileOutputStream(outUrl);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, cipher);
            buffer = new byte[1024];
            int bytesRead;
            assert inputStream != null;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
                total += bytesRead;           //in terms of MB
            }
            inputStream.close();
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e("EncryptUtil", "EncryptUriToFilePath Exception:" + e.toString());
        }
        return false;
    }

    //-------------------------------moving from FileViewerUtils-----------
    public static byte[] DecryptedBufferFromUri(Activity activity, Uri uri) {
        byte[] buffer;
        String passWord = "N";
        Cipher cipher = null;
        String filePath = NyFileUtil.getPath(activity, uri);
        if (filePath.contains("_NY")) {
            passWord = getPasswordFromFileName(filePath);
            cipher = LevelCipherOnly(passWord);
        }
        try {
            InputStream is = null;
            if (passWord.equals("N"))
                is = activity.getContentResolver().openInputStream(uri);
            else
                is = new CipherInputStream(activity.getContentResolver().openInputStream(uri), cipher);

            int len;
            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            while ((len = is.read(data, 0, data.length)) != -1) {
                bufferStream.write(data, 0, len);
            }
            bufferStream.flush();
            buffer = bufferStream.toByteArray();
            is.close();
        } catch (IOException e) {
            return null;
        }
        return buffer;
    }


    public static byte[] DecryptedBufferFromOnline(Context context, String filePath) {
        InputStream inputStream = null;
        try {
            URL url = new URL(filePath);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            inputStream = conn.getInputStream();
            if (filePath.contains("_NY")) {
                String passWord = EncryptUtil.getPasswordFromFileName(filePath);
                CTRnoPadding ces = EncryptUtil.LevelCipherPackage(passWord);
                //THE FOLLOWING are REDUNDANT
              /*  if (Integer.parseInt(passWord) > 5) {
                    //   Log.e(TAG, "passWord "+ passWord);
                    inputStream.skip(DEFAULTSKIP);
                    AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, DEFAULTSKIP);
                }*/
                inputStream = new CipherInputStream(inputStream, ces.cipher);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(inputStream.available());
            int n = 0;
            byte[] data = new byte[1024 * 1024];
            while (-1 != (n = inputStream.read(data))) {
                buffer.write(data, 0, n);
                //  Log.e("EncryptUtil", "inputStream.read n:  " + n);
            }
            inputStream.close();
            return buffer.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean DecryptedFileFromOnline(Context context, String filePath, String
            outUrl) {
        InputStream inputStream = null;
        try {
            URL url = new URL(filePath);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            inputStream = conn.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(outUrl);
            if (filePath.contains("_NY")) {
                String passWord = EncryptUtil.getPasswordFromFileName(filePath);
                CTRnoPadding ces = EncryptUtil.LevelCipherPackage(passWord);
                if (Integer.parseInt(passWord) > 5) {
                    //   Log.e(TAG, "passWord "+ passWord);
                    inputStream.skip(ENCRYPT_SKIP);
                    AesHelper.jumpToOffset(ces.cipher, ces.secretKeySpec, ces.ivParameterSpec, ENCRYPT_SKIP);
                }
                inputStream = new CipherInputStream(inputStream, ces.cipher);
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            assert inputStream != null;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e("EncryptUtil", "DecryptedFileFromOnline Exception:" + e.toString());
        }
        return false;
    }

    //-------------------------------------------------------------------------

    public static String decrypt(String data, String pwd) {

        try {
            SecretKeySpec key = generatekey(pwd);
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedVal = Base64.decode(data, Base64.DEFAULT);
            byte[] decVal = c.doFinal(decodedVal);
            return new String(decVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String encrypt(String data, String pwd) {

        try {
            //  String data = XorEncrypt(indata);
            SecretKeySpec key = generatekey(pwd);
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(data.getBytes());
            return Base64.encodeToString(encVal, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //TODO
    public static String DbDecrypt(String data, String pwd) {

        try {

            byte[] decodedVal = Base64.decode(data, Base64.DEFAULT);
            SecretKeySpec key = generatekey(pwd);
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decVal = c.doFinal(decodedVal);
            String info = Base64.encodeToString(decVal, Base64.DEFAULT);
            return XorDecrypt(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String DbEncrypt(String indata, String pwd) {

        try {
            String data1 = XorEncrypt(indata);
            String data = Base64.encodeToString(data1.getBytes(), Base64.DEFAULT);
            SecretKeySpec key = generatekey(pwd);
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(data.getBytes());
            String encryptedVal = Base64.encodeToString(encVal, Base64.DEFAULT);
            return encryptedVal;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String XorEncrypt(String info) {
        if (info == null) {
            return null;
        }
        byte[] bytes = info.getBytes();
        int len = bytes.length;
        int key = 0x12;
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) (bytes[i] ^ key);
            key = bytes[i];
        }
        return new String(bytes);
    }

    public static String XorDecrypt(String info) {
        if (info == null) {
            return null;
        }
        byte[] bytes = info.getBytes();
        int len = bytes.length;
        int key = 0x12;
        for (int i = len - 1; i > 0; i--) {
            bytes[i] = (byte) (bytes[i] ^ bytes[i - 1]);
        }
        bytes[0] = (byte) (bytes[0] ^ key);
        return new String(bytes);
    }

    private static SecretKeySpec generatekey(String pwd) throws Exception {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[0];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bytes = pwd.getBytes(StandardCharsets.UTF_8);
            }
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest();
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //------------------------------------------------------------------------------
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


    public static String StrToEncryptStr(String inStr, int encryptLevel) {
        if (encryptLevel == -1) return inStr;
        Cipher cipher = LevelCipherOnly(encryptLevel);
        return StrToEncryptStr(inStr, cipher);
    }

    public static String StrToEncryptStr(String inStr, String passWord) {
        if (passWord == "N") return inStr;
        Cipher cipher = LevelCipherOnly(passWord);
        return StrToEncryptStr(inStr, cipher);
    }

    public static String StrToEncryptStr(String inStr, Cipher cipher) {
        String outStr = null;
        try {
            byte[] encrypted = cipher.doFinal(inStr.getBytes());
            outStr = Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outStr;
    }

    //-----------------unused---------------------

    public static String StrToEncryptStr(String inStr, byte[] key, byte[] iv) {

        String outStr = null;
        SecretKeySpec mSecretKeySpec = new SecretKeySpec(key, AES_ALGORITHM);
        IvParameterSpec mIvParameterSpec = new IvParameterSpec(iv);
        try {
            Cipher mCipher = Cipher.getInstance(CTR_TRANSFORMATION);
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);
            byte[] encrypted = mCipher.doFinal(inStr.getBytes());
            outStr = Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outStr;
    }

    public static String StrToEncryptStr(String inStr, String KeyS, String ivS) {
        return StrToEncryptStr(inStr, KeyS.getBytes(), ivS.getBytes());
    }

    //-------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String FileToEncryptStr(String path, String KeyS, String ivS) {
        String input = null;
        try {
            input = convertFileToString(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return StrToEncryptStr(input, KeyS, ivS);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String convertNyFileToStr(String path) {
        String passWord = "N";
        if (path.contains("_NY")) {
            passWord = getPasswordFromFileName(path);
        }
        return FileToEncryptStr(path, passWord);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String FileToEncryptStr(String path, String passWord) {
        String input = null;
        try {
            input = convertFileToString(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (passWord.equals("N")) return input;
        else return StrToEncryptStr(input, passWord);
    }


    public static void saveStringToEncryptedFile(String inStr, String filePath, String passWord) {
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(StrToEncryptStr(inStr, passWord));
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveStringToNyFile(String inStr, String path) {
        String passWord = "N";
        if (path.contains("_NY")) {
            passWord = getPasswordFromFileName(path);
        }
        saveStringToEncryptedFile(inStr, path, passWord);
    }


    public static void shareStr(Context context, String info) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("text", info));
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, info);
        context.startActivity(Intent.createChooser(shareIntent, "Request register code"));
    }

//--------------------
    /*
    private static LocalSingleHttpServer mServer = null;

    public static String httpReroute(Context context, String mUrl) {

        try {
            if (mServer != null) {
                mServer.stop();
                mServer = null;
            }
            // Libmedia requirement
            Licensing.allow(context);
            Log.e("EncryptUtil", "Licensing.allow-----"+context.toString());
            // optional
          //  Licensing.setDeveloperMode(true);
            mServer = new LocalSingleHttpServer();
            Log.e("EncryptUtil", "Password-----"+getPasswordFromFileName(mUrl));
            final Cipher mCipher = EncryptUtil.LevelCipherOnly(getPasswordFromFileName(mUrl));

            if (mCipher != null) {  // null means a clear video ; no need to set a decryption processing
                mServer.setCipher(mCipher);
                mServer.start();
                return mServer.getURL(mUrl);
            } else return mUrl;
        } catch (Exception e) {
            Toast.makeText(context, "Server failure", Toast.LENGTH_SHORT).show();
            return mUrl;
        }

    }*/


    public interface ManipulateCallback<T> {
        void manipulateCallBack(T ret);

    }


    public static void AnnouncedAndRegister(Context context, File infile) {
        String fileName = infile.getName();
        String filePath = infile.getAbsolutePath();

        //--- register--------------
        String mimeType = getTypeFromName(infile.getName());
        if (mimeType.contains("video") || fileName.toLowerCase().endsWith("mts")) {
            //updateMediaStore(context, outfile.getPath());
            ContentResolver mContentResolver = context.getContentResolver();
            ContentValues values = new ContentValues(3);
            values.put(MediaStore.Video.Media.DISPLAY_NAME, infile.getName());
            values.put(MediaStore.Video.Media.DATA, infile.getPath());
            values.put(MediaStore.Video.Media.SIZE, infile.length());
            //   mContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            mContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

            // NyVideo nyVideo = new NyVideo(fileName, filePath);
            // appendVideoToList(nyVideo, "1 NewDownload");
        }

        Intent intent = new Intent();
        // 设置Intent的Action属性
        intent.setAction(DOWNLOAD_BROADCAST);
        intent.putExtra(DOWNLOAD_URL, filePath);
        intent.putExtra(DOWNLOAD_FULL_NAME, fileName);
        // 发送广播
        context.sendBroadcast(intent);
        // if (!filePath.endsWith(".lst") && !filePath.endsWith(".json") && !filePath.endsWith(".tmp"))
        //    Toast.makeText(context, fileName + context.getResources().getString(R.string.downloadTo) + infile.getParent(), Toast.LENGTH_LONG).show();
    }

    public static void AnnouncedAndRegister(Context context, String videoPath) {
        File infile = new File(videoPath);
        if (infile.exists() && infile.length() > 0) AnnouncedAndRegister(context, infile);
    }


    //---------------------------------

    public static void CreateFixedLengthFile(long fixedLength, String filePath) throws
            IOException {
        byte[] buf = new byte[1024];
        FileOutputStream fos = new FileOutputStream(filePath);
        long m = fixedLength / buf.length;
        for (long i = 0; i < m; i++) {
            fos.write(buf, 0, buf.length);
        }
        fos.write(buf, 0, (int) (fixedLength % buf.length));
        fos.close();
    }

    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {

        return ByteBuffer.allocate(byte1.length + byte2.length)
                .put(byte1)
                .put(byte2)
                .array();

    }

    public static void splitByteArray(byte[] input) {

        ByteBuffer bb = ByteBuffer.wrap(input);

        byte[] cipher = new byte[8];
        byte[] nonce = new byte[4];
        byte[] extra = new byte[2];
        bb.get(cipher, 0, cipher.length);
        bb.get(nonce, 0, nonce.length);
        bb.get(extra, 0, extra.length);

    }

    //https://www.hudatutorials.com/java/basics/java-arrays/java-byte-array
    //https://www.javacodeexamples.com/create-string-specified-number-of-characters-example/865

    public static String createString(int stringLength, char ch) {

        //create char array of specified length
        char[] charArray = new char[stringLength];

        //fill all elements with the specified char '*'
        Arrays.fill(charArray, ch);

        //create string from char array and return
        return new String(charArray);
    }


    //https://helloacm.com/convert-utf-8-char-array-to-raw-byte-array-in-java/
    //Given a UTF-8 Char Array, we can use the following Java Function to Convert to Raw Byte Array.
    // Each UTF-8 Character has 3 types: 3 bytes, 2 bytes or 1 byte depending on the first byte range.
    public static byte[] char2Byte(char[] a) {
        int len = 0;
        // obtain the length of the byte array
        for (char c : a) {
            if (c > 0x7FF) {
                len += 3;
            } else if (c > 0x7F) {
                len += 2;
            } else {
                len++;
            }
        }
        // fill the byte array with UTF-8 characters
        byte[] result = new byte[len];
        int i = 0;
        for (char c : a) {
            if (c > 0x7FF) {
                result[i++] = (byte) (((c >> 12) & 0x0F) | 0xE0);
                result[i++] = (byte) (((c >> 6) & 0x3F) | 0x80);
                result[i++] = (byte) ((c & 0x3F) | 0x80);
            } else if (c > 127) {
                result[i++] = (byte) (((c >> 6) & 0x1F) | 0xC0);
                result[i++] = (byte) ((c & 0x3F) | 0x80);
            } else {
                result[i++] = (byte) (c & 0x7F);
            }
        }
        return result;
    }

    //https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String randomAlphanumericString(int stringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(stringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static int getInt(File inFile) {
        int count = 0;

        FileReader fr = null;
        BufferedReader br = null;
        PrintWriter writer = null;

        try {
           /* File f = new File("FileCounter.initial");
            if (!inFile.exists()) {
                inFile.createNewFile();
                writer = new PrintWriter(new FileWriter(inFile));
                writer.print(0);
            }
            if (writer != null) writer.close();*/

            fr = new FileReader(inFile);
            br = new BufferedReader(fr);
            String initial = br.readLine();
            count = Integer.parseInt(initial);
        } catch (Exception e) {
            //  if (writer != null) writer.close();
        }

        if (br != null) {
            try {
                br.close();
            } catch (Exception e) {
            }
        }
        return count;
    }

    public static void saveInt(int count, File outF) throws IOException {
        FileWriter fw = null;
        PrintWriter pw = null;
        fw = new FileWriter(outF);
        pw = new PrintWriter(fw);
        pw.print(count);
        pw.close();
    }

    public static void mergeFiles(File[] files, File mergedF3) {
        FileWriter fstream = null;
        BufferedWriter out = null;

        try {
            fstream = new FileWriter(mergedF3, true);
            out = new BufferedWriter(fstream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (File f : files) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(f);
                BufferedReader in = new BufferedReader(new InputStreamReader(fis));

                String aLine;
                while ((aLine = in.readLine()) != null) {
                    out.write(aLine);
                    out.newLine();
                }

                in.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        System.out.print("\nTwo files are succesfully merged into the third file.");

        try {
            out.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    //--------------------------------------------------//
    private final String[] VIDEO_COLUMNS = new String[]{"_id", "_display_name", "title", "date_added", "duration", "resolution", "_size", "_data", "mime_type"};
    static Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    static String VIDEO_ID = MediaStore.Video.Media._ID;
    static String VIDEO_NAME = MediaStore.Video.Media.DISPLAY_NAME;
    static String VIDEO_PATH = MediaStore.Video.Media.DATA;
    static String VIDEO_SIZE = MediaStore.Video.Media.SIZE;
    static String VIDEO_DURATION = MediaStore.Video.Media.DURATION;
    static String VIDEO_RESOLUTION = MediaStore.Video.Media.RESOLUTION;
    private static final String SEPARATOR_RESOLUTION = "x";
    //----------------------------------------------------
    private static final String[] PROJECTION_VIDEO_URI = {
            VIDEO_ID, VIDEO_NAME, VIDEO_PATH, VIDEO_SIZE, VIDEO_DURATION, VIDEO_RESOLUTION
    };

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


   public static boolean insertNyVideo(Context context, @NonNull NyVideo video) {
        ContentResolver mContentResolver = context.getContentResolver();

        ContentValues values = new ContentValues(5);
        values.put(VIDEO_NAME, video.getName());
        values.put(VIDEO_PATH, video.getPath());
        values.put(VIDEO_SIZE, video.info.size);
        values.put(VIDEO_DURATION, video.info.duration);
        values.put(VIDEO_RESOLUTION, video.info.width + SEPARATOR_RESOLUTION + video.info.height);

        if (mContentResolver.insert(VIDEO_URI, values) == null) {
            return false;
        }
        values.clear();
        values.put(VIDEO_ID, video.info.id);
        return true;
    }
        //TODO 2022-4-21
    public static void registerEncryptedVideo(Context context, File outfile) {
        NyVideo nyVideo = VideoFetchUtils.queryNyVideoByPath(context, outfile.getPath());
        if (nyVideo != null) {
            nyVideo.setPath(outfile.getPath());
            nyVideo.setName(outfile.getName());
            nyVideo.info.size = outfile.length();
            insertNyVideo(context, nyVideo);
        }
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

}