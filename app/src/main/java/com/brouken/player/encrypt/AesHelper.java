package com.brouken.player.encrypt;


import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created by wangye on 16-10-19.
 */

public class AesHelper {

    private static final int BLOCK_SIZE = 16;
    private static final int AES_BLOCK_SIZE = 16;

    private AesHelper() {
    }

    public static final void jumpToOffset(final Cipher c,
                                          final Key aesKey, final IvParameterSpec iv, final long offset) {
        if (!c.getAlgorithm().toUpperCase().startsWith("AES/CTR")) {
            throw new IllegalArgumentException(
                    "Invalid algorithm, only AES/CTR mode supported");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Invalid offset");
        }

        final int skip = (int) (offset % AES_BLOCK_SIZE);
        final IvParameterSpec calculatedIVForOffset = calculateIVForOffset(iv,
                offset - skip);
        try {
            c.init(Cipher.ENCRYPT_MODE, aesKey, calculatedIVForOffset);
            final byte[] skipBuffer = new byte[skip];
            c.update(skipBuffer, 0, skip, skipBuffer);
            Arrays.fill(skipBuffer, (byte) 0);
        } catch (ShortBufferException | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IvParameterSpec calculateIVForOffset(final IvParameterSpec iv,
                                                        final long blockOffset) {
        final BigInteger ivBI = new BigInteger(1, iv.getIV());
        final BigInteger ivForOffsetBI = ivBI.add(BigInteger.valueOf(blockOffset
                / AES_BLOCK_SIZE));

        final byte[] ivForOffsetBA = ivForOffsetBI.toByteArray();
        final IvParameterSpec ivForOffset;
        if (ivForOffsetBA.length >= AES_BLOCK_SIZE) {
            ivForOffset = new IvParameterSpec(ivForOffsetBA, ivForOffsetBA.length - AES_BLOCK_SIZE,
                    AES_BLOCK_SIZE);
        } else {
            final byte[] ivForOffsetBASized = new byte[AES_BLOCK_SIZE];
            System.arraycopy(ivForOffsetBA, 0, ivForOffsetBASized, AES_BLOCK_SIZE
                    - ivForOffsetBA.length, ivForOffsetBA.length);
            ivForOffset = new IvParameterSpec(ivForOffsetBASized);
        }

        return ivForOffset;
    }
}
