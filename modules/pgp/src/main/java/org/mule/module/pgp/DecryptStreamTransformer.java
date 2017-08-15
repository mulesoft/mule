/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static org.mule.module.pgp.util.BouncyCastleUtil.KEY_FINGERPRINT_CALCULATOR;
import static org.mule.module.pgp.util.BouncyCastleUtil.PBE_SECRET_KEY_DECRYPTOR_BUILDER;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;


public class DecryptStreamTransformer implements StreamTransformer
{
    public static final String INVALID_KEY_ERROR_MESSAGE = "User selected private key ID %s (through secretAliasId) but message was encrypted for key ID %s";

    public static final String INVALID_PGP_MESSAGE_ERROR = "Invalid PGP message";

    public static final String INVALID_PASS_PHRASE_ERROR_MESSAGE = "PassPhrase '%s' is invalid for the private key with id '%s'";

    private static final String CHECKSUM_MESSAGE = "checksum mismatch";
    private PGPSecretKeyRingCollection secretKeys;
    private PGPSecretKey secretKey;
    private String password;
    private final boolean configuredSecretKey ;
    private InputStream compressedStream;
    private InputStream clearStream;

    public DecryptStreamTransformer(PGPSecretKey secretKey,
                                    PGPSecretKeyRingCollection secretKeys,
                                    String password) throws IOException
    {
        Validate.notNull(password, "The password should not be null");
        this.configuredSecretKey = secretKey != null;
        this.secretKey = secretKey;
        this.secretKeys = secretKeys;
        this.password = password;
    }


    public InputStream process (InputStream toBeDecrypted) throws Exception
    {
        InputStream decodedInputStream = PGPUtil.getDecoderStream(toBeDecrypted);

        PGPObjectFactory pgpF = new PGPObjectFactory(decodedInputStream, KEY_FINGERPRINT_CALCULATOR);
        Object pgpObject = pgpF.nextObject();

        if (pgpObject == null)
        {
            throw new PGPException(INVALID_PGP_MESSAGE_ERROR);
        }

        // the first object might be a PGP marker packet.
        PGPEncryptedDataList enc;
        if (pgpObject instanceof PGPEncryptedDataList)
        {
            enc = (PGPEncryptedDataList) pgpObject;

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
                throw new PGPException("Failed to find private key with ID " + pbe.getKeyID());
            }
        }

        clearStream = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey));
        PGPObjectFactory pgpObjectFactory = new PGPObjectFactory(clearStream, KEY_FINGERPRINT_CALCULATOR);

        pgpObject = pgpObjectFactory.nextObject();

        while (!(pgpObject instanceof PGPLiteralData))
        {
            if (pgpObject instanceof PGPOnePassSignatureList)
            {
                // TODO MULE-8386: Add support for PGP signature verification
                pgpObject = pgpObjectFactory.nextObject();
            }
            else if (pgpObject instanceof PGPCompressedData)
            {
                PGPCompressedData cData = (PGPCompressedData) pgpObject;
                compressedStream = new BufferedInputStream(cData.getDataStream());
                pgpObjectFactory = new PGPObjectFactory(compressedStream, KEY_FINGERPRINT_CALCULATOR);
                pgpObject = pgpObjectFactory.nextObject();
            }
            else
            {
                throw new PGPException("input is not PGPLiteralData - type unknown.");
            }
        }

        PGPLiteralData pgpLiteralData = (PGPLiteralData) pgpObject;
        return pgpLiteralData.getInputStream();
    }


    private PGPPrivateKey getPrivateKey(long keyID, String passPhrase) throws PGPException, NoSuchProviderException
    {
        PGPSecretKey pgpSecKey;
        PGPPrivateKey pgpPrivateKey;

        if(configuredSecretKey)
        {
            pgpSecKey = this.secretKey;
        }
        else
        {
            pgpSecKey = secretKeys.getSecretKey(keyID);
        }

        if (configuredSecretKey && pgpSecKey.getKeyID() != keyID)
        {
            throw new PGPException(createInvalidKeyErrorMessage(pgpSecKey.getKeyID(), keyID));
        }

        try
        {
            pgpPrivateKey = pgpSecKey.extractPrivateKey(PBE_SECRET_KEY_DECRYPTOR_BUILDER.build(passPhrase.toCharArray()));
            return pgpPrivateKey;
        }
        catch (PGPException e)
        {
            throw wrapWrongPassPhraseException(e, passPhrase, pgpSecKey.getKeyID());
        }

    }

    private String createInvalidKeyErrorMessage(Long configuredKeyId, Long validKeyId)
    {
        return format(INVALID_KEY_ERROR_MESSAGE, toHexString(configuredKeyId).toUpperCase(), toHexString(validKeyId).toUpperCase());
    }

    private PGPException wrapWrongPassPhraseException (PGPException pgpException, String invalidPassPhrase, Long keyId)
    {
        if(pgpException.getMessage().contains(CHECKSUM_MESSAGE))
        {
            return  new PGPException(createInvalidPassPhraseErrorMessage(invalidPassPhrase, keyId));
        }
        else
        {
            return pgpException;
        }
    }

    private String createInvalidPassPhraseErrorMessage (String invalidPassPhrase, long keyId)
    {
        return format(INVALID_PASS_PHRASE_ERROR_MESSAGE, invalidPassPhrase, toHexString(keyId).toUpperCase());
    }
}
