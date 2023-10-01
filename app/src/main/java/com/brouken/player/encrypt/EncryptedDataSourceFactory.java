package com.brouken.player.encrypt;

import android.content.Context;


import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.TransferListener;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptedDataSourceFactory implements DataSource.Factory {

  private final Cipher mCipher;
  private final SecretKeySpec mSecretKeySpec;
  private final IvParameterSpec mIvParameterSpec;
  private final TransferListener mTransferListener;
  private final Context mContext;

  public EncryptedDataSourceFactory(Context context, Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec, TransferListener listener) {
    mCipher = cipher;
    mSecretKeySpec = secretKeySpec;
    mIvParameterSpec = ivParameterSpec;
    mTransferListener = listener;
    mContext=context;
  }

  @Override
  public EncryptedDataSource createDataSource() {
    return new EncryptedDataSource(mContext, mCipher, mSecretKeySpec, mIvParameterSpec, mTransferListener);

  }

}