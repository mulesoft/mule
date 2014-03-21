/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.Validate;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;

public class EncryptStreamTransformer implements StreamTransformer
{
    private static final long offset = 1 << 24;

    private InputStream toBeEncrypted;
    private PGPPublicKey publicKey;
    private Provider provider;

    private OutputStream pgpOutputStream;
    private OutputStream compressedEncryptedOutputStream;
    private OutputStream encryptedOutputStream;
    private OutputStream armoredOut;
    private long bytesWrote;

    public EncryptStreamTransformer(InputStream toBeEncrypted, PGPPublicKey publicKey, Provider provider) throws IOException
    {
        Validate.notNull(toBeEncrypted, "The toBeEncrypted should not be null");
        Validate.notNull(publicKey, "The publicKey should not be null");

        this.toBeEncrypted = toBeEncrypted;
        this.publicKey = publicKey;
        this.bytesWrote = 0;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(OutputStream out) throws Exception
    {
        armoredOut = new ArmoredOutputStream(out);
        PGPEncryptedDataGenerator encrDataGen = new PGPEncryptedDataGenerator(PGPEncryptedData.CAST5, false,
            new SecureRandom(), provider);
        encrDataGen.addMethod(this.publicKey);
        encryptedOutputStream = encrDataGen.open(armoredOut, new byte[1 << 16]);

        PGPCompressedDataGenerator comprDataGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
        compressedEncryptedOutputStream = comprDataGen.open(encryptedOutputStream);

        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
        pgpOutputStream = lData.open(compressedEncryptedOutputStream, PGPLiteralData.BINARY, "stream",
            new Date(), new byte[1 << 16]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean write(OutputStream out, AtomicLong bytesRequested) throws Exception
    {
        int len = 0;
        byte[] buf = new byte[1 << 16];
        boolean wroteSomething = false;

        while (bytesRequested.get() + offset > bytesWrote && (len = this.toBeEncrypted.read(buf)) > 0)
        {
            pgpOutputStream.write(buf, 0, len);
            bytesWrote = bytesWrote + len;
            wroteSomething = true;
        }

        if (wroteSomething && len <= 0)
        {
            pgpOutputStream.close();
            compressedEncryptedOutputStream.close();
            encryptedOutputStream.close();
            armoredOut.close();
            toBeEncrypted.close();
            return true;
        }

        return false;
    }
}
