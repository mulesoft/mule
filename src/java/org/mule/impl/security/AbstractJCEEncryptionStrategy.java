/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.impl.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

/**
 * A JCE based encryption strategy. It also provides base64 encoding of
 * encrypted/decrypted data by setting the base64encoding attribute
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractJCEEncryptionStrategy implements UMOEncryptionStrategy
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
        if (algorithm == null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "Algorithm"), this);
        } else {
            logger.debug("Using encryption algorithm: " + algorithm);
        }

        keySpec = createKeySpec();
        try {
            secretKey = getSecretKey();
            // Create Ciphers
            encryptCipher = Cipher.getInstance(getAlgorithm());
            decryptCipher = Cipher.getInstance(getAlgorithm());

            AlgorithmParameterSpec paramSpec = createAlgorithmParameterSpec();
            if(paramSpec!=null) {
                encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
            } else {
                encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "encryption ciphers"),
                                              e,
                                              this);
        }
    }

    protected abstract SecretKey getSecretKey() throws GeneralSecurityException;

    public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException
    {
        try {
            byte[] buf = encryptCipher.doFinal(data);
            if (base64Encoding) {
                return new BASE64Encoder().encode(buf).getBytes();
            } else {
                return buf;
            }
        } catch (Exception e) {
            throw new CryptoFailureException(this, e);
        }
    }

    public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException
    {
        try {
            byte[] dec = data;
            if (base64Encoding) {
                dec = new BASE64Decoder().decodeBuffer(new String(data));
            }
            return decryptCipher.doFinal(dec);
        } catch (Exception e) {
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