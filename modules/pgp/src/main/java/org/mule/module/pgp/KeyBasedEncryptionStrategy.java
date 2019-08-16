/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.mule.module.pgp.i18n.PGPMessages.noSecretPassPhrase;
import static org.mule.module.pgp.util.ValidatorUtil.validateNotNull;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.module.pgp.config.PGPOutputMode;
import org.mule.module.pgp.exception.MissingPGPKeyException;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractNamedEncryptionStrategy;
import org.mule.util.SecurityUtils;

import java.io.InputStream;
import java.security.Provider;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

public class KeyBasedEncryptionStrategy extends AbstractNamedEncryptionStrategy
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(KeyBasedEncryptionStrategy.class);

    private PGPKeyRing keyManager;
    private CredentialsAccessor credentialsAccessor;
    private boolean checkKeyExpirity = false;
    private Provider provider;
    private String encryptionAlgorithm;
    private int encryptionAlgorithmId;
    private PGPOutputMode pgpOutputMode;
    private String fileName;
    
    public void initialise() throws InitialisationException
    {
        if (!SecurityUtils.isFipsSecurityModel())
        {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
        provider = SecurityUtils.getDefaultSecurityProvider();

        if (encryptionAlgorithm == null)
        {
            encryptionAlgorithm = EncryptionAlgorithm.AES_256.toString();
        }

        try
        {
            encryptionAlgorithmId = EncryptionAlgorithm.valueOf(encryptionAlgorithm).getNumericId();
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException("Could not initialise encryption strategy: invalid algorithm " + encryptionAlgorithm, e);
        }
    }

    public InputStream encrypt(InputStream data, Object cryptInfo) throws CryptoFailureException
    {
        try 
        {
            PGPCryptInfo pgpCryptInfo = this.safeGetCryptInfo(cryptInfo);
            PGPPublicKey publicKey = pgpCryptInfo.getPublicKey();
            return new EncryptStreamTransformer(publicKey, provider, encryptionAlgorithmId, pgpOutputMode, fileName).process(data);
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
            PGPSecretKey secretKey = this.keyManager.getConfiguredSecretKey();
            String secretPassPhrase = this.keyManager.getSecretPassphrase();
            PGPSecretKeyRingCollection secretKeys =  this.keyManager.getSecretKeys();
            PGPPublicKeyRingCollection publicKeys = this.keyManager.getPublicKeys();

            if (secretPassPhrase == null)
            {
                throw new CryptoFailureException(noSecretPassPhrase(), this);
            }

            return new DecryptStreamTransformer(secretKey, secretKeys, publicKeys, secretPassPhrase).process(data);
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    private PGPCryptInfo safeGetCryptInfo(Object cryptInfo) throws MissingPGPKeyException
    {
        if (cryptInfo == null)
        {
            MuleEvent event = RequestContext.getEvent();
            String principalId = (String) this.getCredentialsAccessor().getCredentials(event);
            PGPPublicKey publicKey = keyManager.getPublicKey(principalId);
            validateNotNull(publicKey, PGPMessages.noPublicKeyForPrincipal(principalId));
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

    public void setPgpOutputMode(PGPOutputMode pgpOutputMode)
    {
        this.pgpOutputMode = pgpOutputMode;
    }

    public boolean isCheckKeyExpirity()
    {
        return checkKeyExpirity;
    }

    public void setCheckKeyExpirity(boolean checkKeyExpirity)
    {
        this.checkKeyExpirity = checkKeyExpirity;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm)
    {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
}
