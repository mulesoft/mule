/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.mule.module.pgp.i18n.PGPMessages.noFileKeyFound;
import static org.mule.module.pgp.i18n.PGPMessages.noSecretKeyDefined;
import static org.mule.module.pgp.util.BouncyCastleUtil.KEY_FINGERPRINT_CALCULATOR;
import static org.mule.module.pgp.util.ValidatorUtil.validateNotNull;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.exception.MissingPGPKeyException;
import org.mule.util.IOUtils;
import org.mule.util.SecurityUtils;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;


public class PGPKeyRingImpl implements PGPKeyRing, Initialisable
{

    protected static final Log logger = LogFactory.getLog(PGPKeyRingImpl.class);

    private String publicKeyRingFileName;

    private HashMap<String, PGPPublicKey> principalsKeyBundleMap;

    private String secretKeyRingFileName;

    private String secretAliasId;

    private PGPSecretKey secretKey;

    private String secretPassPhrase;

    private PGPSecretKeyRingCollection secretKeys;

    private boolean readSecretKey ;

    public void initialise() throws InitialisationException
    {
        try
        {
            if (!SecurityUtils.isFipsSecurityModel())
            {
                java.security.Security.addProvider(new BouncyCastleProvider());
            }

            principalsKeyBundleMap = new HashMap<String, PGPPublicKey>();

            readPublicKeyRing();
        }
        catch (MissingPGPKeyException exception)
        {
            throw new InitialisationException(exception, this);
        }
        catch (Exception e)
        {
            logger.error("Error in Initialisation: " + e.getMessage(), e);
            throw new InitialisationException(CoreMessages.failedToCreate("PGPKeyRingImpl"), e, this);
        }
    }

    private void readPublicKeyRing() throws Exception
    {
        InputStream inputStream = IOUtils.getResourceAsStream(getPublicKeyRingFileName(), getClass());
        validateNotNull(inputStream, noFileKeyFound(getPublicKeyRingFileName()));
        PGPPublicKeyRingCollection collection = new PGPPublicKeyRingCollection(inputStream, KEY_FINGERPRINT_CALCULATOR);
        inputStream.close();

        Iterator keyRingsIterator = collection.getKeyRings();
        while (keyRingsIterator.hasNext())
        {
            PGPPublicKeyRing ring = (PGPPublicKeyRing) keyRingsIterator.next();
            String userID = "";
            Iterator publicKeysIterator = ring.getPublicKeys();
            while (publicKeysIterator.hasNext())
            {
                PGPPublicKey publicKey = (PGPPublicKey) publicKeysIterator.next();
                Iterator userIDs = publicKey.getUserIDs();
                if (userIDs.hasNext())
                {
                    userID = (String) userIDs.next();
                }
                principalsKeyBundleMap.put(userID, publicKey);
            }
        }
    }

    private void readPrivateKeyBundle() throws Exception
    {
        if (getSecretKeyRingFileName() == null)
        {
            throw new MissingPGPKeyException(noSecretKeyDefined());
        }

        InputStream secretKeyInputStream = IOUtils.getResourceAsStream(getSecretKeyRingFileName(), getClass());
        validateNotNull(secretKeyInputStream, noFileKeyFound(getSecretKeyRingFileName()));
        secretKeys = new PGPSecretKeyRingCollection(secretKeyInputStream, KEY_FINGERPRINT_CALCULATOR);
        secretKeyInputStream.close();
        String secretAliasId = getSecretAliasId();
        if (secretAliasId != null)
        {
            secretKey = secretKeys.getSecretKey(parseSecretAliasId(secretAliasId));
        }
        readSecretKey = true;
    }

    public String getSecretKeyRingFileName()
    {
        return secretKeyRingFileName;
    }

    public void setSecretKeyRingFileName(String value)
    {
        this.secretKeyRingFileName = value;
    }

    public String getSecretAliasId()
    {
        return secretAliasId;
    }

    public void setSecretAliasId(String value)
    {
        this.secretAliasId = value;
    }

    public String getSecretPassphrase()
    {
        return secretPassPhrase;
    }

    public void setSecretPassphrase(String value)
    {
        this.secretPassPhrase = value;
    }

    public PGPSecretKey getSecretKey() throws Exception
    {
        if (! readSecretKey)
        {
            readPrivateKeyBundle();
        }

        return secretKey;
    }

    /**
     * @param secretAliasId: the keyID to use for decryption. It must be a hexadecimal value.
     * @return the same keyID but as a decimal value.
     */
    private Long parseSecretAliasId(String secretAliasId)
    {
        return new BigInteger(secretAliasId, 16).longValue();
    }

    public PGPSecretKeyRingCollection getSecretKeys ()
    {
        return secretKeys;
    }

    public String getPublicKeyRingFileName()
    {
        return publicKeyRingFileName;
    }

    public void setPublicKeyRingFileName(String value)
    {
        this.publicKeyRingFileName = value;
    }

    public PGPPublicKey getPublicKey(String principalId)
    {
        return principalsKeyBundleMap.get(principalId);
    }
}
