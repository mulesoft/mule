/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;

import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Provides password-based encryption using JCE. Users must specify a password and optionally a salt and iteration count as well.
 * The default algorithm is PBEWithMD5AndDES, but users can specify any valid algorithm supported by JCE.
 */
public class PasswordBasedEncryptionStrategy extends AbstractJCEEncryptionStrategy {

  public static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";
  public static final int DEFAULT_ITERATION_COUNT = 20;

  private byte[] salt = null;

  private int iterationCount = DEFAULT_ITERATION_COUNT;

  private char[] password;

  public PasswordBasedEncryptionStrategy() {
    algorithm = DEFAULT_ALGORITHM;
  }

  public void initialise() throws InitialisationException {
    if (salt == null) {
      salt = new byte[] {(byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99};
      logger.debug("Salt is not set. Using default salt");
    }

    if (password == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("Password"), this);
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

  public void setPassword(String password) {
    this.password = password.toCharArray();
  }

  protected SecretKey getSecretKey() throws GeneralSecurityException {
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(getAlgorithm());
    return keyFactory.generateSecret(keySpec);
  }
}
