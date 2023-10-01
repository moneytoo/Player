package com.brouken.player.encrypt;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class StreamingCipherInputStream extends CipherInputStream {

    private static final int AES_BLOCK_SIZE = 16;

    private InputStream mUpstream;
    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;

    public StreamingCipherInputStream(InputStream inputStream, EncryptUtil.CTRnoPadding ces) {
        super(inputStream, ces.cipher);
        new StreamingCipherInputStream(inputStream, ces.cipher, ces.secretKeySpec, ces.ivParameterSpec);
    }

    public StreamingCipherInputStream(InputStream inputStream, Cipher cipher, SecretKeySpec secretKeySpec, IvParameterSpec ivParameterSpec) {
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
        long skipped = mUpstream.skip(bytesToSkip);
       //  Log.e("StreamingCipher", "bytesToSkip  " + bytesToSkip);
         Log.e("StreamingCipher", "mUpstream.skip(bytesToSkip)  " + skipped);

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
        return mUpstream.available();
    }

    

}
