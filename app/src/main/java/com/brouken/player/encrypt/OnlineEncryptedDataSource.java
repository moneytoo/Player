package com.brouken.player.encrypt;

import static com.brouken.player.encrypt.Constants.ENCRYPT_SKIP;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.util.Assertions;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.TransferListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OnlineEncryptedDataSource implements HttpDataSource {

   /* static {
        ExoPlayerLibraryInfo.registerModule("google.exo.okhttp");
    }*/

    private static final AtomicReference<byte[]> skipBufferReference = new AtomicReference<>();

    @NonNull
    private final Call.Factory callFactory;
    @NonNull
    private final HttpDataSource.RequestProperties requestProperties;

    @Nullable
    private final String userAgent;
    @Nullable
    private final Predicate<String> contentTypePredicate;
    @Nullable
    private TransferListener mTransferListener;
    @Nullable
    private final CacheControl cacheControl;
    @Nullable
    private final HttpDataSource.RequestProperties defaultRequestProperties;

    private DataSpec dataSpec;
    private Response response;
    private InputStream responseByteStream;
    private boolean opened;

    private long bytesToSkip;
    private long mBytesRemaining;

    private long bytesSkipped;
    private long bytesRead;


    private final Cipher mCipher;
    private final SecretKeySpec mSecretKeySpec;
    private final IvParameterSpec mIvParameterSpec;

    private OnlineStreamingCipherInputStream mInputStream;
    private String passWord;


    public OnlineEncryptedDataSource(
            Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec,
            @NonNull Call.Factory callFactory,
            @Nullable String userAgent,
            @Nullable Predicate<String> contentTypePredicate) {
        this(cipher, secretKeySpec, ivParameterSpec, callFactory, userAgent, contentTypePredicate, null);
    }

    public OnlineEncryptedDataSource(
            Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec,
            @NonNull Call.Factory callFactory, @Nullable String userAgent,
            @Nullable Predicate<String> contentTypePredicate,
            @Nullable TransferListener listener) {
        this(cipher, secretKeySpec, ivParameterSpec, callFactory, userAgent, contentTypePredicate, listener, null, null);
    }

    public OnlineEncryptedDataSource(
            Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec,
            @NonNull Call.Factory callFactory, @Nullable String userAgent,
            @Nullable Predicate<String> contentTypePredicate,
            @Nullable TransferListener listener,
            @Nullable CacheControl cacheControl, @Nullable HttpDataSource.RequestProperties defaultRequestProperties) {
        this.mCipher = cipher;
        this.mSecretKeySpec = secretKeySpec;
        this.mIvParameterSpec = ivParameterSpec;
        this.callFactory = Assertions.checkNotNull(callFactory);
        this.userAgent = userAgent;
        this.contentTypePredicate = contentTypePredicate;
        this.mTransferListener = listener;
        this.cacheControl = cacheControl;
        this.defaultRequestProperties = defaultRequestProperties;
        this.requestProperties = new RequestProperties();
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        mTransferListener = transferListener;
    }

    @Override
    public Uri getUri() {
        return response == null ? null : Uri.parse(response.request().url().toString());
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return response == null ? null : response.headers().toMultimap();
    }

    @Override
    public void setRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        requestProperties.set(name, value);
    }

    @Override
    public void clearRequestProperty(String name) {
        Assertions.checkNotNull(name);
        requestProperties.remove(name);
    }

    @Override
    public void clearAllRequestProperties() {
        requestProperties.clear();
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        this.dataSpec = dataSpec;
        this.bytesRead = 0;
        this.bytesSkipped = 0;
        long extraSkip=0;

        Request request = makeRequest(dataSpec);
        try {
            response = callFactory.newCall(request).execute();
            String mUrl = response.request().url().toString();
            if (mUrl.contains("%")) {
                try {
                    mUrl = URLDecoder.decode(mUrl, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
          //  Log.e("OnlineEncryptedDataSource", "mUrl: " + mUrl);
            passWord = EncryptUtil.getPasswordFromFileName(mUrl);
          //  Log.e("OnlineEncryptedDataSource", "passWord: " + passWord);
            extraSkip = (Integer.parseInt(passWord) > 5) ? ENCRYPT_SKIP : 0L;
            mInputStream = new OnlineStreamingCipherInputStream(response.body().byteStream(), mCipher, mSecretKeySpec, mIvParameterSpec);
            mInputStream.forceSkip(dataSpec.position + extraSkip);
            int responseCode = response.code();
            computeBytesRemaining(dataSpec,responseCode, extraSkip );
        } catch (IOException e) {
            e.printStackTrace();
        }


 /*
        // Check for a valid response code.
        if (!response.isSuccessful()) {
            Map<String, List<String>> headers = response.headers().toMultimap();
            closeConnectionQuietly();
            InvalidResponseCodeException exception = new InvalidResponseCodeException(
                    responseCode, headers, dataSpec);
            if (responseCode == 416) {
                exception.initCause(new DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE));
            }
            try {
                throw exception;
            } catch (InvalidResponseCodeException e) {
                e.printStackTrace();
            }
        }

        // Check for a valid content type.
        MediaType mediaType = response.body().contentType();
        String contentType = mediaType != null ? mediaType.toString() : null;
        if (contentTypePredicate != null && !contentTypePredicate.evaluate(contentType)) {
            closeConnectionQuietly();
            try {
                throw new InvalidContentTypeException(contentType, dataSpec);
            } catch (InvalidContentTypeException e) {
                e.printStackTrace();
            }
        }

        bytesToSkip = responseCode == 200 && dataSpec.position != 0 ? dataSpec.position : 0;
        // Determine the length of the data to be read, after skipping.
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else {
            long contentLength = response.body().contentLength()-extraSkip;
            mBytesRemaining = contentLength != -1 ? (contentLength - bytesToSkip) : C.LENGTH_UNSET;
        }
        */
        opened = true;
        if (mTransferListener != null) {
            mTransferListener.onTransferStart(this, dataSpec, true);
        }
        return mBytesRemaining;
    }

    private void computeBytesRemaining(DataSpec dataSpec, int responseCode, long extraSkip) throws IOException {
        bytesToSkip = responseCode == 200 && dataSpec.position != 0 ? dataSpec.position : 0;
        // Determine the length of the data to be read, after skipping.
        if (dataSpec.length != C.LENGTH_UNSET) {
            mBytesRemaining = dataSpec.length;
        } else {
            long contentLength = response.body().contentLength()-extraSkip;
            mBytesRemaining = contentLength != -1 ? (contentLength - bytesToSkip) : C.LENGTH_UNSET;
        }
      //  Log.e("OnlineEncryptedDataSource", "mBytesRemaining  " + mBytesRemaining);
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        try {
            if (readLength == 0) {
                return 0;
            }
            //    Log.e("OnlineEncrypted", String.valueOf(mInputStream.available()));
            if (mBytesRemaining != C.LENGTH_UNSET) {
                long bytesRemaining = mBytesRemaining - bytesRead;
                if (bytesRemaining == 0) {
                    return C.RESULT_END_OF_INPUT;
                }
                readLength = (int) Math.min(readLength, bytesRemaining);
            }

            int read = mInputStream.read(buffer, offset, readLength);
            if (read == -1) {
                if (mBytesRemaining != C.LENGTH_UNSET) {
                    // End of stream reached having not read sufficient data.
                    throw new EOFException();
                }
                return C.RESULT_END_OF_INPUT;
            }

            bytesRead += read;
            if (mTransferListener != null) {
                mTransferListener.onBytesTransferred(this, dataSpec, true, (int) bytesRead);
            }
            return read;
        } catch (IOException e) {
            throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_READ);
        }
    }

    @Override
    public void close() {
        if (opened) {
            opened = false;
            if (mTransferListener != null) {
                mTransferListener.onTransferEnd(this, dataSpec, true);
            }
            closeConnectionQuietly();
        }
    }


    private Request makeRequest(DataSpec dataSpec) {
        long position = dataSpec.position;
        long length = dataSpec.length;
        boolean allowGzip = dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP);

        HttpUrl url = HttpUrl.parse(dataSpec.uri.toString());
        Request.Builder builder = new Request.Builder().url(url);
        if (cacheControl != null) {
            builder.cacheControl(cacheControl);
        }
        if (defaultRequestProperties != null) {
            for (Map.Entry<String, String> property : defaultRequestProperties.getSnapshot().entrySet()) {
                builder.header(property.getKey(), property.getValue());
            }
        }
        for (Map.Entry<String, String> property : requestProperties.getSnapshot().entrySet()) {
            builder.header(property.getKey(), property.getValue());
        }
        if (!(position == 0 && length == C.LENGTH_UNSET)) {
            String rangeRequest = "bytes=" + position + "-";
            if (length != C.LENGTH_UNSET) {
                rangeRequest += (position + length - 1);
            }
            builder.addHeader("Range", rangeRequest);
        }
        if (userAgent != null) {
            builder.addHeader("User-Agent", userAgent);
        }

        if (!allowGzip) {
            builder.addHeader("Accept-Encoding", "identity");
        }
        if (dataSpec.httpBody != null) {
            builder.post(RequestBody.create(null, dataSpec.httpBody));
        }
        return builder.build();
    }


    private void closeConnectionQuietly() {
        response.body().close();
        response = null;
        mInputStream = null;
        responseByteStream = null;
    }

    public static final class OnlineEncryptedFileDataSourceException extends IOException {
        public OnlineEncryptedFileDataSourceException(IOException cause) {
            super(cause);
        }
    }


    public static class OnlineStreamingCipherInputStream extends CipherInputStream {

        private static final int AES_BLOCK_SIZE = 16;

        private final InputStream mUpstream;
        private final Cipher mCipher;
        private final SecretKeySpec mSecretKeySpec;
        private final IvParameterSpec mIvParameterSpec;

        public OnlineStreamingCipherInputStream(InputStream inputStream, Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec) {
            super(inputStream, cipher);
            mUpstream = inputStream;
            mCipher = cipher;
            mSecretKeySpec = secretKeySpec;
            mIvParameterSpec = ivParameterSpec;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return super.read(b, off, len);
        }

        public long forceSkip(long bytesToSkip) throws IOException {
           // Log.e("mUpstream", String.valueOf(bytesToSkip));
            long skipped = mUpstream.skip(bytesToSkip);
            try {
                int skip = (int) (bytesToSkip % AES_BLOCK_SIZE);
                long blockOffset = bytesToSkip - skip;
                long numberOfBlocks = blockOffset / AES_BLOCK_SIZE;
                // from here to the next inline comment, i don't understand
                BigInteger ivForOffsetAsBigInteger = new BigInteger(1, mIvParameterSpec.getIV()).add(BigInteger.valueOf(numberOfBlocks));
                byte[] ivForOffsetByteArray = ivForOffsetAsBigInteger.toByteArray();
                IvParameterSpec computedIvParameterSpecForOffset;
                if (ivForOffsetByteArray.length < AES_BLOCK_SIZE) {
                    byte[] resizedIvForOffsetByteArray = new byte[AES_BLOCK_SIZE];
                    System.arraycopy(ivForOffsetByteArray, 0, resizedIvForOffsetByteArray, AES_BLOCK_SIZE - ivForOffsetByteArray.length, ivForOffsetByteArray.length);
                    computedIvParameterSpecForOffset = new IvParameterSpec(resizedIvForOffsetByteArray);
                } else {
                    computedIvParameterSpecForOffset = new IvParameterSpec(ivForOffsetByteArray, ivForOffsetByteArray.length - AES_BLOCK_SIZE, AES_BLOCK_SIZE);
                }
                mCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, computedIvParameterSpecForOffset);
                byte[] skipBuffer = new byte[skip];
                // i get that we need to update, but i don't get how we're able to take the shortcut from here to the previous comment
                mCipher.update(skipBuffer, 0, skip, skipBuffer);
                Arrays.fill(skipBuffer, (byte) 0);
            } catch (Exception e) {
                return 0;
            }
            return skipped;
        }

        // We need to return the available bytes from the upstream.
        // In this implementation we're front loading it, but it's possible the value might change during the lifetime
        // of this instance, and reference to the stream should be retained and queried for available bytes instead
        @Override
        public int available() throws IOException {
           // Log.e("available", String.valueOf(mUpstream.available()));
            return mUpstream.available();
        }

    }

    //----------------------
    @Override
    public int getResponseCode() {
        return 1;

    }

}
