package com.brouken.player.encrypt;

import static com.brouken.player.encrypt.Constants.ENCRYPT_SKIP;
import static com.brouken.player.encrypt.NyFileUtil.getPath;

import android.content.Context;
import android.net.Uri;
import android.util.Log;


import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


//https://github.com/google/ExoPlayer/issues/2467

public final class EncryptedDataSource implements DataSource {

    private TransferListener mTransferListener;
    private StreamingCipherInputStream mInputStream;
    private Uri mUri;
    private long mBytesRemaining;
    private boolean mOpened;
    private final Cipher mCipher;
    private final SecretKeySpec mSecretKeySpec;
    private final IvParameterSpec mIvParameterSpec;
    private DataSpec mDataSpec;
    private final Context mContext;
    private String passWord;
    private long filesize = 0L;


    public EncryptedDataSource(Context context, Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec, TransferListener listener) {
        mCipher = cipher;
        mSecretKeySpec = secretKeySpec;
        mIvParameterSpec = ivParameterSpec;
        mTransferListener = listener;
        mContext = context;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        mTransferListener = transferListener;
    }

    @Override
    public long open(DataSpec dataSpec) throws EncryptedFileDataSourceException {
        // if we're open, we shouldn't need to open again, fast-fail

        if (mOpened) {
            return mBytesRemaining;
        }

        mDataSpec = dataSpec;
        mUri = dataSpec.uri;
        //1 work for smb
        //  String url = NyFileUtil.getPath(mContext, mUri);
        //2- work for local share of encrypted file
        String url = getPath(mContext, mUri);
        if (url.contains("%")) {
            try {
                url = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        passWord = EncryptUtil.getPasswordFromFileName(url);
        long extraSkip = (Integer.parseInt(passWord) > 5) ? ENCRYPT_SKIP : 0L;

        // put all our throwable work in a single block, wrap the error in a custom Exception
        try {
            File file = new File(url);
            if (file.isFile() && file.exists()) {
                filesize = file.length();
                FileInputStream fileInputStream = new FileInputStream(file);
                mInputStream = new StreamingCipherInputStream(fileInputStream, mCipher, mSecretKeySpec, mIvParameterSpec);
            } else {
                //Log.e("EncryptedDataSource", "mUri.getPath(): " + mUri.getPath());
                InputStream inputStream = mContext.getContentResolver().openInputStream(mUri);
                mInputStream = new StreamingCipherInputStream(inputStream, mCipher, mSecretKeySpec, mIvParameterSpec);
            }
            // }

            //  TODO most important
            mInputStream.forceSkip(dataSpec.position + extraSkip);
            //
            computeBytesRemaining(dataSpec);
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        }
        // if we made it this far, we're open
        mOpened = true;
        // notify
        if (mTransferListener != null) {
            mTransferListener.onTransferStart(this, dataSpec, true);
        }
        // report
        return mBytesRemaining;
    }

  /*  private void setupInputStream(File file) throws FileNotFoundException {
        //  File encryptedFile = new File(Objects.requireNonNull(mUri.getPath()));
        FileInputStream fileInputStream = new FileInputStream(file);
        mInputStream = new StreamingCipherInputStream(fileInputStream, mCipher, mSecretKeySpec, mIvParameterSpec);
    }*/

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws EncryptedFileDataSourceException {
        // fast-fail if there's 0 quantity requested or we think we've already processed everything
        if (readLength == 0) {
            return 0;
        } else if (mBytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        }

        // constrain the read length and try to read from the cipher input stream
        int bytesToRead = getBytesToRead(readLength);
        int bytesRead;
        try {
            bytesRead = mInputStream.read(buffer, offset, bytesToRead);
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        }
        // if we get a -1 that means we failed to read - we're either going to EOF error or broadcast EOF
        if (bytesRead == -1) {
            if (mBytesRemaining != C.LENGTH_UNSET) {
                throw new EncryptedFileDataSourceException(new EOFException());
            }
            return C.RESULT_END_OF_INPUT;
        }
        // we can't decrement bytes remaining if it's just a flag representation (as opposed to a mutable numeric quantity)
        if (mBytesRemaining != C.LENGTH_UNSET) {
            mBytesRemaining -= bytesRead;
        }
        // notify
        if (mTransferListener != null) {
            mTransferListener.onBytesTransferred(this, mDataSpec, true, bytesRead);
        }
        // report
        return bytesRead;
    }

    private int getBytesToRead(int bytesToRead) {
        if (mBytesRemaining == C.LENGTH_UNSET) {
            return bytesToRead;
        }
        return (int) Math.min(mBytesRemaining, bytesToRead);
    }

    @Override
    public Uri getUri() {
        return mUri;
    }

    @Override
    public void close() throws EncryptedFileDataSourceException {
        mUri = null;
        try {
            if (mInputStream != null) {
                mInputStream.close();
            }
        } catch (IOException e) {
            throw new EncryptedFileDataSourceException(e);
        } finally {
            mInputStream = null;
            if (mOpened) {
                mOpened = false;
                if (mTransferListener != null) {
                    mTransferListener.onTransferEnd(this, mDataSpec, true);
                }
            }
        }
    }

    public static final class EncryptedFileDataSourceException extends IOException {
        public EncryptedFileDataSourceException(IOException cause) {
            super(cause);
        }
    }


    private void computeBytesRemaining(DataSpec dataSpec) throws IOException {
        //  Log.e("EncryptedDataSource", "mInputStream.available()  " + mInputStream.available());
        //  Log.e("EncryptedDataSource", "dataSpec.length  " + dataSpec.length);
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else {
            //TODO to make it play encrypted file exceed 2gb
            if (filesize > 0L) mBytesRemaining = filesize;
            else mBytesRemaining = mInputStream.available();
            //  if (mBytesRemaining == Integer.MAX_VALUE) {
            //    mBytesRemaining = C.LENGTH_UNSET;
            //  }
        }

    }

}