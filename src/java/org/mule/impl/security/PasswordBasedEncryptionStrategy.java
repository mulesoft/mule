/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.InitialisationException;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.security.CryptoFailureException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * <code>PasswordBasedEncryptionStrategy</code> uses password-based encryption to
 * encrypt and decrypt data.  Developers can set the salt, iternationCount
 * password and algorythm on this stragetgy, but on the password in plain text
 * is required.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class PasswordBasedEncryptionStrategy implements UMOEncryptionStrategy
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(PasswordBasedEncryptionStrategy.class);

    private PBEKeySpec pbeKeySpec;
    private PBEParameterSpec pbeParamSpec;
    private SecretKeyFactory keyFac;
    private SecretKey pbeKey;
    private Cipher encryptCipher;
    private Cipher decryptCipher;

    private String algorithm = "PBEWithMD5AndDES";
    private byte[] salt = null; //new byte[]{
        //(byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
        //(byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99};

    private int iterationCount = 20;
    private char[] password;


    public void initialise() throws InitialisationException
    {
        if(algorithm==null) {
            throw new InitialisationException("PBE algorithm cannot be null");
        } else {
            logger.debug("Using encryption algorithm: " + algorithm);
        }
        if(salt==null) {
            salt = new byte[]{(byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
        (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99};
            //throw new InitialisationException("PBE salt cannot be null");
        }

        if(password==null) {
            throw new InitialisationException("PBE password cannot be null");
        }
        pbeParamSpec = new PBEParameterSpec(salt, iterationCount);
        pbeKeySpec = new PBEKeySpec(password);
        try
        {
            keyFac = SecretKeyFactory.getInstance(getAlgorithm());
            pbeKey = keyFac.generateSecret(pbeKeySpec);
            // Create PBE Cipher
            encryptCipher = Cipher.getInstance(getAlgorithm());
            encryptCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

            decryptCipher = Cipher.getInstance(getAlgorithm());
            decryptCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        } catch (Exception e)
        {
            throw new InitialisationException("Failed to create PBE encryption ciphers: " + e.getMessage(), e);
        }
    }

    public byte[] encrypt(byte[] data) throws CryptoFailureException
    {
        try
        {
            return encryptCipher.doFinal(data);
        } catch (Exception e)
        {
            throw new CryptoFailureException(e.getMessage(), e, this);
        }
    }

    public byte[] decrypt(byte[] data) throws CryptoFailureException
    {
        try
        {
            return decryptCipher.doFinal(data);
        } catch (Exception e)
        {
            throw new CryptoFailureException(e.getMessage(), e, this);
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

    public byte[] getSalt()
    {
        return salt;
    }

    public void setSalt(byte[] salt)
    {
        this.salt = salt;
    }

    public void setSalt(String salt)
    {
        this.salt = salt.getBytes();
    }

    public int getIterationCount()
    {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount)
    {
        this.iterationCount = iterationCount;
    }

    public char[] getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password.toCharArray();
    }

    public void setPassword(char[] password)
    {
        this.password = password;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Algorithm=").append(algorithm);
//        buf.append("Salt length=").append(salt.length);
//        buf.append("Iterations=").append(iterationCount);
        return buf.toString();
    }
}
