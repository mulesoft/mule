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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

/**
 * PRovides password-based encryption using JCE.  Users must specify a password
 * and optionally a salt and iteration count as well.  The default algorithm is
 * PBEWithMD5AndDES, but users can specify any valid
 * algorithm supported by JCE.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PasswordBasedEncryptionStrategy extends AbstractJCEEncryptionStrategy {

    public static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";
    private byte[] salt = null;

    private int iterationCount = 20;
    private char[] password;

    public PasswordBasedEncryptionStrategy() {
        algorithm = DEFAULT_ALGORITHM;
    }

    public void initialise() throws InitialisationException {
         if (salt == null) {
            salt = new byte[] { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8,
                    (byte) 0xee, (byte) 0x99 };
            logger.debug("Salt is not set. Using default salt");
        }

        if (password == null) {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "Password"), this);
        }
        super.initialise();
    }


    protected KeySpec createKeySpec() {
        return new PBEKeySpec(password);
    }

    protected AlgorithmParameterSpec createAlgorithmParameterSpec() {
        return new PBEParameterSpec(salt, iterationCount);
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public void setIterationCount(int iterationCount) {
        this.iterationCount = iterationCount;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    protected SecretKey getSecretKey() throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(getAlgorithm());
        return keyFactory.generateSecret(keySpec);
    }
}
