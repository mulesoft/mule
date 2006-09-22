/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.security;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ObjectFactory;
import org.mule.util.StringMessageUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

/**
 * SecretKey based encryption using JCE. Users must specify a key as an array of bytes.
 * This can be set directly on the strategy or a keyFactory can be specified.  A keyFactory
 * is an implementation of org.mule.util.ObjectFactory and must return a byte array.
 * The default algorthm used by this strategy is Blowfish, but users can specify any valid
 * algorithm supported by JCE.
 *
 * @see ObjectFactory
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SecretKeyEncryptionStrategy extends AbstractJCEEncryptionStrategy {

    public static final String DEFAULT_ALGORITHM = "Blowfish";

    private byte[] key;
    private ObjectFactory keyFactory;

    public SecretKeyEncryptionStrategy() {
        algorithm = DEFAULT_ALGORITHM;
    }

    public void initialise() throws InitialisationException {
        if(key==null) {
            if(keyFactory==null) {
                throw new InitialisationException(new Message(Messages.X_IS_NULL, "Key / KeyFactory"), this);
            } else {
                try {
                    key = (byte[])keyFactory.create();
                } catch (Exception e) {
                    throw new InitialisationException(e, this);
                }
            }
        }
        super.initialise();
    }

    protected KeySpec createKeySpec() {
        return new SecretKeySpec(key, algorithm);
    }

    protected AlgorithmParameterSpec createAlgorithmParameterSpec() {
        return null;
    }

    public byte[] getkey() {
        return key;
    }

    public void setkey(byte[] rawKey) {
        this.key = rawKey;
    }

    public void setkey(String rawKey) {
        this.key = StringMessageUtils.getBytes(rawKey);
    }

    public ObjectFactory getKeyFactory() {
        return keyFactory;
    }

    public void setKeyFactory(ObjectFactory keyFactory) {
        this.keyFactory = keyFactory;
    }

    protected SecretKey getSecretKey() throws GeneralSecurityException {
        KeyGenerator kgen = KeyGenerator.getInstance(algorithm);
        return kgen.generateKey();
    }
}
