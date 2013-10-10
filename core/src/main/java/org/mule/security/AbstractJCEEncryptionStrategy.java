/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JCE based encryption strategy. It also provides base64 encoding of
 * encrypted/decrypted data by setting the base64encoding attribute.
 */
public abstract class AbstractJCEEncryptionStrategy extends AbstractNamedEncryptionStrategy
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected KeySpec keySpec;
    protected SecretKey secretKey;
    protected Cipher encryptCipher;
    protected Cipher decryptCipher;

    protected String algorithm = null;

    protected boolean base64Encoding = true;

    public void initialise() throws InitialisationException
    {
        if (algorithm == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("Algorithm"), this);
        }
        else
        {
            logger.debug("Using encryption algorithm: " + algorithm);
        }

        keySpec = createKeySpec();

        try
        {
            secretKey = getSecretKey();
            // Create Ciphers
            encryptCipher = Cipher.getInstance(getAlgorithm());
            decryptCipher = Cipher.getInstance(getAlgorithm());

            AlgorithmParameterSpec paramSpec = createAlgorithmParameterSpec();
            if (paramSpec != null)
            {
                encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
            }
            else
            {
                encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToCreate("encryption ciphers"),
                e, this);
        }
    }

    protected abstract SecretKey getSecretKey() throws GeneralSecurityException;

    public InputStream encrypt(InputStream data, Object info) throws CryptoFailureException {
        try
        {
            return new ByteArrayInputStream(this.encrypt(IOUtils.toByteArray(data), info));
        }
        catch (IOException e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    public InputStream decrypt(InputStream data, Object info) throws CryptoFailureException {
        try
        {
            return new ByteArrayInputStream(this.decrypt(IOUtils.toByteArray(data), info));
        }
        catch (IOException e)
        {
            throw new CryptoFailureException(this, e);
        }
    }
    
    public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException
    {
        try
        {
            byte[] buf = encryptCipher.doFinal(data);
            if (base64Encoding)
            {
                return Base64.encodeBytes(buf).getBytes();
            }
            else
            {
                return buf;
            }
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException
    {
        try
        {
            byte[] dec = data;
            if (base64Encoding)
            {
                dec = Base64.decode(new String(data));
            }
            return decryptCipher.doFinal(dec);
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public void setAlgorithm(String algorithm)
    {
        this.algorithm = algorithm;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Algorithm=").append(algorithm);
        return buf.toString();
    }

    public boolean isBase64Encoding()
    {
        return base64Encoding;
    }

    public void setBase64Encoding(boolean base64Encoding)
    {
        this.base64Encoding = base64Encoding;
    }

    protected abstract KeySpec createKeySpec();

    protected abstract AlgorithmParameterSpec createAlgorithmParameterSpec();

}
