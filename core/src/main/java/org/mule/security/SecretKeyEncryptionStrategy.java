/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringMessageUtils;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecretKey based encryption using JCE. Users must specify a key as an array of
 * bytes. This can be set directly on the strategy or a keyFactory can be specified.
 * A keyFactory is an implementation of {@link SecretKeyFactory} and must return a
 * byte array. The default algorthm used by this strategy is Blowfish, but users can
 * specify any valid algorithm supported by JCE.
 * 
 * @see SecretKeyFactory
 */
public class SecretKeyEncryptionStrategy extends AbstractJCEEncryptionStrategy
{

    public static final String DEFAULT_ALGORITHM = "Blowfish";

    private byte[] key;
    private SecretKeyFactory keyFactory;

    public SecretKeyEncryptionStrategy()
    {
        algorithm = DEFAULT_ALGORITHM;
    }

    public void initialise() throws InitialisationException
    {
        if (key == null)
        {
            if (keyFactory == null)
            {
                throw new InitialisationException(CoreMessages.objectIsNull("Key / KeyFactory"), this);
            }
            else
            {
                try
                {
                    key = keyFactory.getKey();
                }
                catch (Exception e)
                {
                    throw new InitialisationException(e, this);
                }
            }
        }
        super.initialise();
    }

    protected KeySpec createKeySpec()
    {
        return new SecretKeySpec(key, algorithm);
    }

    protected AlgorithmParameterSpec createAlgorithmParameterSpec()
    {
        return null;
    }

    public void setKey(byte[] rawKey)
    {
        this.key = rawKey;
    }

    public void setKey(String rawKey)
    {
        this.key = StringMessageUtils.getBytes(rawKey);
    }

    public SecretKeyFactory getKeyFactory()
    {
        return keyFactory;
    }

    public void setKeyFactory(SecretKeyFactory keyFactory)
    {
        this.keyFactory = keyFactory;
    }

    protected SecretKey getSecretKey() throws GeneralSecurityException
    {
        return KeyGenerator.getInstance(algorithm).generateKey();
    }

}
