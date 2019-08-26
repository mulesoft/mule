/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static org.mule.module.pgp.util.BouncyCastleUtil.PBE_SECRET_KEY_DECRYPTOR_BUILDER;

import org.mule.api.security.CryptoFailureException;
import org.mule.config.i18n.MessageFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.util.Iterator;

import org.apache.commons.lang.Validate;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;


public class DecryptStreamTransformer implements StreamTransformer
{

    public static final String INVALID_KEY_ERROR_MESSAGE = "User selected private key ID %s (through secretAliasId) but message was encrypted for key ID %s";

    public static final String INVALID_PGP_MESSAGE_ERROR = "Invalid PGP message";

    public static final String INVALID_PASS_PHRASE_ERROR_MESSAGE = "PassPhrase '%s' is invalid for the private key with id '%s'";

    private static final String CHECKSUM_MESSAGE = "checksum mismatch";
    private final PGPPublicKeyRingCollection publicKeys;
    private PGPSecretKeyRingCollection secretKeys;
    private PGPSecretKey secretKey;
    private String password;
    private final boolean configuredSecretKey;
    private InputStream compressedStream;
    private InputStream clearStream;
    private boolean validateSignatureIfFound = false;

    public DecryptStreamTransformer(PGPSecretKey secretKey,
                                    PGPSecretKeyRingCollection secretKeys,
                                    PGPPublicKeyRingCollection publicKeys, String password) throws IOException
    {
        Validate.notNull(password, "The password should not be null");
        this.configuredSecretKey = secretKey != null;
        this.secretKey = secretKey;
        this.secretKeys = secretKeys;
        this.publicKeys = publicKeys;
        this.password = password;
    }

    public DecryptStreamTransformer setValidateSignatureIfFound(boolean validate)
    {
        this.validateSignatureIfFound = validate;
        return this;
    }


    public InputStream process(InputStream toBeDecrypted) throws Exception
    {
        InputStream decodedInputStream = PGPUtil.getDecoderStream(toBeDecrypted);

        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(decodedInputStream);
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
        JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(clearStream);

        pgpObject = pgpObjectFactory.nextObject();

        PGPOnePassSignature onePassSignature = null;

        while (!(pgpObject instanceof PGPLiteralData))
        {
            if (pgpObject instanceof PGPOnePassSignatureList)
            {
                if (validateSignatureIfFound)
                {
                    onePassSignature = ((PGPOnePassSignatureList) pgpObject).get(0);
                }
                pgpObject = pgpObjectFactory.nextObject();
            }
            else if (pgpObject instanceof PGPCompressedData)
            {
                PGPCompressedData cData = (PGPCompressedData) pgpObject;
                compressedStream = new BufferedInputStream(cData.getDataStream());
                pgpObjectFactory = new JcaPGPObjectFactory(compressedStream);
                pgpObject = pgpObjectFactory.nextObject();
            }
            else
            {
                throw new PGPException("input is not PGPLiteralData - type unknown.");
            }
        }

        InputStream literalDataStream = ((PGPLiteralData) pgpObject).getInputStream();

        if (!validateSignatureIfFound)
        {
            // Discovered signature validation not required. Return literal decrypted data stream.
            return literalDataStream;
        }
        else
        {
            // TODO: Add some debug logging
            onePassSignature.init(new BcPGPContentVerifierBuilderProvider(), publicKeys.getPublicKey(onePassSignature.getKeyID()));
            ByteArrayOutputStream temporalStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1 << 16];
            int readBytesCount;
            while ((readBytesCount = literalDataStream.read(buffer)) > 0)
            {
                // Write to backup read stream
                temporalStream.write(buffer, 0, readBytesCount);
                // Update discovered signature
                onePassSignature.update(buffer, 0, readBytesCount);
            }

            temporalStream.flush();

            PGPSignatureList signatureList = (PGPSignatureList) pgpObjectFactory.nextObject();
            PGPSignature signature = signatureList.get(0);
            PGPPublicKey signerPublicKey = publicKeys.getPublicKey(signature.getKeyID());
            signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), signerPublicKey);

            if (!onePassSignature.verify(signature))
            {
                throw new PGPException("Signature verification failed while decrypting message.");
            }
            return new ByteArrayInputStream(temporalStream.toByteArray());
        }
    }


    private PGPPrivateKey getPrivateKey(long keyID, String passPhrase) throws PGPException, NoSuchProviderException
    {
        PGPSecretKey pgpSecKey;
        PGPPrivateKey pgpPrivateKey;

        if (configuredSecretKey)
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

    private PGPException wrapWrongPassPhraseException(PGPException pgpException, String invalidPassPhrase, Long keyId)
    {
        if (pgpException.getMessage().contains(CHECKSUM_MESSAGE))
        {
            return new PGPException(createInvalidPassPhraseErrorMessage(invalidPassPhrase, keyId));
        }
        else
        {
            return pgpException;
        }
    }

    private String createInvalidPassPhraseErrorMessage(String invalidPassPhrase, long keyId)
    {
        return format(INVALID_PASS_PHRASE_ERROR_MESSAGE, invalidPassPhrase, toHexString(keyId).toUpperCase());
    }
}
