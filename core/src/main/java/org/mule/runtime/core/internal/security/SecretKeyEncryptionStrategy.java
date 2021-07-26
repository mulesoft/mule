/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.api.security.SecretKeyFactory;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecretKey based encryption using JCE. Users must specify a key as an array of bytes. This can be set directly on the strategy
 * or a keyFactory can be specified. A keyFactory is an implementation of {@link SecretKeyFactory} and must return a byte array.
 * The default algorthm used by this strategy is Blowfish, but users can specify any valid algorithm supported by JCE.
 * 
 * @see SecretKeyFactory
 * @deprecated This class is deprecated and will be removed in Mule 4.0. Use {@link PasswordBasedEncryptionStrategy} instead,
 *             which follows the correct way of transforming a string password into a cryptographic key
 */
@Deprecated
public class SecretKeyEncryptionStrategy extends AbstractJCEEncryptionStrategy {

  public static final String DEFAULT_ALGORITHM = "Blowfish";

  private byte[] key;
  private SecretKeyFactory keyFactory;

  public SecretKeyEncryptionStrategy() {
    algorithm = DEFAULT_ALGORITHM;
  }

  public void initialise() throws InitialisationException {
    if (key == null) {
      if (keyFactory == null) {
        throw new InitialisationException(CoreMessages.objectIsNull("Key / KeyFactory"), this);
      } else {
        try {
          key = keyFactory.getKey();
        } catch (Exception e) {
          throw new InitialisationException(e, this);
        }
      }
    }
    super.initialise();
  }

  @Override
  protected void createAndInitCiphers() throws GeneralSecurityException {
    encryptCipher = Cipher.getInstance(getAlgorithm());
    decryptCipher = Cipher.getInstance(getAlgorithm());

    AlgorithmParameterSpec paramSpec = createAlgorithmParameterSpec();
    if (paramSpec != null) {
      encryptCipher.init(Cipher.ENCRYPT_MODE, (SecretKeySpec) keySpec, paramSpec);
      decryptCipher.init(Cipher.DECRYPT_MODE, (SecretKeySpec) keySpec, paramSpec);
    } else {
      encryptCipher.init(Cipher.ENCRYPT_MODE, (SecretKeySpec) keySpec);
      decryptCipher.init(Cipher.DECRYPT_MODE, (SecretKeySpec) keySpec);
    }
  }

  protected KeySpec createKeySpec() {
    return new SecretKeySpec(key, algorithm);
  }

  protected AlgorithmParameterSpec createAlgorithmParameterSpec() {
    return null;
  }

  public void setKey(byte[] rawKey) {
    this.key = rawKey;
  }

  public void setKey(String rawKey) {
    this.key = StringMessageUtils.getBytes(rawKey);
  }

  public SecretKeyFactory getKeyFactory() {
    return keyFactory;
  }

  public void setKeyFactory(SecretKeyFactory keyFactory) {
    this.keyFactory = keyFactory;
  }

  protected SecretKey getSecretKey() throws GeneralSecurityException {
    return KeyGenerator.getInstance(algorithm).generateKey();
  }

}
