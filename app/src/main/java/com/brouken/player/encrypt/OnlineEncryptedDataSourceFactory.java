package com.brouken.player.encrypt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.TransferListener;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.CacheControl;
import okhttp3.Call;

public class OnlineEncryptedDataSourceFactory extends HttpDataSource.BaseFactory {

    @NonNull
    private final Call.Factory callFactory;
    @Nullable
    private final String userAgent;
    @Nullable
    private final TransferListener listener;
    @Nullable
    private final CacheControl cacheControl;
    private final Cipher mCipher;
    private final SecretKeySpec mSecretKeySpec;
    private final IvParameterSpec mIvParameterSpec;

    /**
     * @param callFactory A {@link Call.Factory} (typically an {@link okhttp3.OkHttpClient}) for use
     *     by the sources created by the factory.
     * @param userAgent An optional User-Agent string.
     * @param listener An optional listener.
     */
    public OnlineEncryptedDataSourceFactory(
            Cipher cipher,
            SecretKeySpec secretKeySpec,
            IvParameterSpec ivParameterSpec,
            @NonNull Call.Factory callFactory,
            @Nullable String userAgent,
            @Nullable TransferListener listener) {
        this(cipher,secretKeySpec,ivParameterSpec,callFactory, userAgent, listener, null);
    }

    /**
     * @param callFactory A {@link Call.Factory} (typically an {@link okhttp3.OkHttpClient}) for use
     *     by the sources created by the factory.
     * @param userAgent An optional User-Agent string.
     * @param listener An optional listener.
     * @param cacheControl An optional {@link CacheControl} for setting the Cache-Control header.
     */
    public OnlineEncryptedDataSourceFactory(
            Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec,
            @NonNull Call.Factory callFactory,
            @Nullable String userAgent,
            @Nullable TransferListener listener,
            @Nullable CacheControl cacheControl) {

        this.mCipher = cipher;
        this.mSecretKeySpec = secretKeySpec;
        this.mIvParameterSpec = ivParameterSpec;
        this.callFactory = callFactory;
        this.userAgent = userAgent;
        this.listener = listener;
        this.cacheControl = cacheControl;
    }

    @Override
    protected OnlineEncryptedDataSource createDataSourceInternal(HttpDataSource.RequestProperties defaultRequestProperties) {
        return new OnlineEncryptedDataSource(mCipher,mSecretKeySpec,mIvParameterSpec,callFactory, userAgent, null, listener, cacheControl,
                defaultRequestProperties);
    }
}
