/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractNamedEncryptionStrategy;

import java.io.InputStream;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;

public class KeyBasedEncryptionStrategy extends AbstractNamedEncryptionStrategy
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(KeyBasedEncryptionStrategy.class);

    private PGPKeyRing keyManager;
    private CredentialsAccessor credentialsAccessor;
    private boolean checkKeyExpirity = false;

    public void initialise() throws InitialisationException
    {
        try
        {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToCreate("KeyBasedEncryptionStrategy"), e,
                this);
        }
    }

    public InputStream encrypt(InputStream data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            PGPCryptInfo pgpCryptInfo = this.safeGetCryptInfo(cryptInfo);
            PGPPublicKey publicKey = pgpCryptInfo.getPublicKey();
            StreamTransformer transformer = new EncryptStreamTransformer(data, publicKey);
            return new LazyTransformedInputStream(new TransformContinuouslyPolicy(), transformer);
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    public InputStream decrypt(InputStream data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            PGPCryptInfo pgpCryptInfo = this.safeGetCryptInfo(cryptInfo);
            PGPPublicKey publicKey = pgpCryptInfo.getPublicKey();
            StreamTransformer transformer = new DecryptStreamTransformer(data, publicKey,
                this.keyManager.getSecretKey(), this.keyManager.getSecretPassphrase());
            return new LazyTransformedInputStream(new TransformContinuouslyPolicy(), transformer);
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    private PGPCryptInfo safeGetCryptInfo(Object cryptInfo)
    {
        if (cryptInfo == null)
        {
            MuleEvent event = RequestContext.getEvent();
            PGPPublicKey publicKey = keyManager.getPublicKey((String) this.getCredentialsAccessor().getCredentials(event));
            this.checkKeyExpirity(publicKey);
            return new PGPCryptInfo(publicKey, false);
        }
        else
        {
            PGPCryptInfo info = (PGPCryptInfo) cryptInfo;
            this.checkKeyExpirity(info.getPublicKey());
            return info;
        }
    }

    private void checkKeyExpirity(PGPPublicKey publicKey)
    {
        if (this.isCheckKeyExpirity() && publicKey.getValidDays() != 0)
        {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(publicKey.getCreationTime());
            calendar.add(Calendar.DATE, publicKey.getValidDays());

            if (!calendar.getTime().after(Calendar.getInstance().getTime()))
            {
                throw new InvalidPublicKeyException(PGPMessages.pgpPublicKeyExpired());
            }
        }
    }

    public PGPKeyRing getKeyManager()
    {
        return keyManager;
    }

    public void setKeyManager(PGPKeyRing keyManager)
    {
        this.keyManager = keyManager;
    }

    public CredentialsAccessor getCredentialsAccessor()
    {
        return credentialsAccessor;
    }

    public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor)
    {
        this.credentialsAccessor = credentialsAccessor;
    }

    public boolean isCheckKeyExpirity()
    {
        return checkKeyExpirity;
    }

    public void setCheckKeyExpirity(boolean checkKeyExpirity)
    {
        this.checkKeyExpirity = checkKeyExpirity;
    }
}
