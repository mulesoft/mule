/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.Validate;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;

public class DecryptStreamTransformer implements StreamTransformer
{
    private static final long offset = 1 << 24;

    private InputStream toBeDecrypted;
    private PGPPublicKey publicKey;
    private PGPSecretKey secretKey;
    private String password;
    private Provider provider;

    private InputStream uncStream;
    private InputStream compressedStream;
    private InputStream clearStream;
    private long bytesWrote;

    public DecryptStreamTransformer(InputStream toBeDecrypted,
                                     PGPPublicKey publicKey,
                                     PGPSecretKey secretKey,
                                     String password,
                                     Provider provider) throws IOException
    {
        Validate.notNull(toBeDecrypted, "The toBeDecrypted should not be null");
        Validate.notNull(publicKey, "The publicKey should not be null");
        Validate.notNull(secretKey, "The secretKey should not be null");
        Validate.notNull(password, "The password should not be null");
        Validate.notNull(provider, "The security provider can't be null");

        this.toBeDecrypted = toBeDecrypted;
        this.publicKey = publicKey;
        this.secretKey = secretKey;
        this.password = password;
        this.bytesWrote = 0;
        this.provider = provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(OutputStream out) throws Exception
    {
        InputStream decodedInputStream = PGPUtil.getDecoderStream(this.toBeDecrypted);
        PGPObjectFactory pgpF = new PGPObjectFactory(decodedInputStream);
        Object o = pgpF.nextObject();

        if (o == null)
        {
            throw new IllegalArgumentException("Invalid PGP message");
        }

        // the first object might be a PGP marker packet.
        PGPEncryptedDataList enc;
        if (o instanceof PGPEncryptedDataList)
        {
            enc = (PGPEncryptedDataList) o;

        }
        else
        {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        // This loop looks like it is ready for multiple encrypted
        // objects, but really only one is expected.
        Iterator<?> it = enc.getEncryptedDataObjects();
        PGPPublicKeyEncryptedData pbe = null;
        PGPPrivateKey privateKey = null;
        while (privateKey == null && it.hasNext())
        {
            pbe = (PGPPublicKeyEncryptedData) it.next();
            privateKey = getPrivateKey(pbe.getKeyID(), this.password);
            if (privateKey == null)
            {
                throw new IllegalArgumentException("Failed to find private key with ID " + pbe.getKeyID());
            }
        }

        clearStream = pbe.getDataStream(privateKey, provider);
        PGPObjectFactory plainFact = new PGPObjectFactory(clearStream);

        o = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        if (o instanceof PGPOnePassSignatureList)
        {
            PGPOnePassSignatureList list = (PGPOnePassSignatureList) o;
            signature = list.get(0);
            signature.initVerify(this.publicKey, provider);
            // TODO verify signature
            // signature.verify(null);
            o = plainFact.nextObject();
        }

        compressedStream = null;
        if (o instanceof PGPCompressedData)
        {
            PGPCompressedData cData = (PGPCompressedData) o;
            compressedStream = new BufferedInputStream(cData.getDataStream());
            PGPObjectFactory pgpFact = new PGPObjectFactory(compressedStream);
            Object streamData = pgpFact.nextObject();
            o = streamData;
        }

        if (o instanceof PGPLiteralData)
        {
            PGPLiteralData ld = (PGPLiteralData) o;
            uncStream = ld.getInputStream();
        }
        else
        {
            throw new PGPException("input is not PGPLiteralData - type unknown.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean write(OutputStream out, AtomicLong bytesRequested) throws Exception
    {
        int len = 0;
        byte[] buf = new byte[1 << 16];
        boolean wroteSomething = false;

        while (bytesRequested.get() + offset > bytesWrote && (len = uncStream.read(buf)) > 0)
        {
            out.write(buf, 0, len);
            bytesWrote = bytesWrote + len;
            wroteSomething = true;
        }

        if (wroteSomething && len <= 0)
        {
            uncStream.close();
            if (compressedStream != null)
            {
                compressedStream.close();
            }
            clearStream.close();
            return true;
        }

        return false;
    }

    private PGPPrivateKey getPrivateKey(long keyID, String pass) throws PGPException, NoSuchProviderException
    {
        PGPSecretKey pgpSecKey = this.secretKey;
        if (pgpSecKey == null)
        {
            return null;
        }
        else
        {
            return pgpSecKey.extractPrivateKey(pass.toCharArray(), provider);
        }
    }
}
