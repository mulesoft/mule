/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.failedToCreate;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.util.Base64;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * A JCE based encryption strategy. It also provides base64 encoding of encrypted/decrypted data by setting the base64encoding
 * attribute.
 */
public abstract class AbstractJCEEncryptionStrategy extends AbstractNamedEncryptionStrategy {

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected KeySpec keySpec;
  protected SecretKey secretKey;
  protected Cipher encryptCipher;
  protected Cipher decryptCipher;

  protected String algorithm = null;

  protected boolean base64Encoding = true;

  @Override
  public void initialise() throws InitialisationException {
    if (algorithm == null) {
      throw new InitialisationException(objectIsNull("Algorithm"), this);
    } else {
      logger.debug("Using encryption algorithm: " + algorithm);
    }

    keySpec = createKeySpec();

    try {
      secretKey = getSecretKey();
      createAndInitCiphers();
    } catch (Exception e) {
      throw new InitialisationException(failedToCreate("encryption ciphers"), e, this);
    }
  }

  protected void createAndInitCiphers() throws GeneralSecurityException {
    encryptCipher = Cipher.getInstance(getAlgorithm());
    decryptCipher = Cipher.getInstance(getAlgorithm());

    AlgorithmParameterSpec paramSpec = createAlgorithmParameterSpec();
    if (paramSpec != null) {
      encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
      decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
    } else {
      encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
      decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
    }
  }

  protected abstract SecretKey getSecretKey() throws GeneralSecurityException;

  @Override
  public InputStream encrypt(InputStream data, Object info) throws CryptoFailureException {
    try {
      return new ByteArrayInputStream(this.encrypt(IOUtils.toByteArray(data), info));
    } catch (IOException e) {
      throw new CryptoFailureException(this, e);
    }
  }

  @Override
  public InputStream decrypt(InputStream data, Object info) throws CryptoFailureException {
    try {
      return new ByteArrayInputStream(this.decrypt(IOUtils.toByteArray(data), info));
    } catch (IOException e) {
      throw new CryptoFailureException(this, e);
    }
  }

  @Override
  public byte[] encrypt(byte[] data, Object info) throws CryptoFailureException {
    try {
      byte[] buf = encryptCipher.doFinal(data);
      if (base64Encoding) {
        return Base64.encodeBytes(buf).getBytes();
      } else {
        return buf;
      }
    } catch (Exception e) {
      throw new CryptoFailureException(this, e);
    }
  }

  @Override
  public byte[] decrypt(byte[] data, Object info) throws CryptoFailureException {
    try {
      byte[] dec = data;
      if (base64Encoding) {
        dec = Base64.decode(new String(data));
      }
      return decryptCipher.doFinal(dec);
    } catch (Exception e) {
      throw new CryptoFailureException(this, e);
    }
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("Algorithm=").append(algorithm);
    return buf.toString();
  }

  public boolean isBase64Encoding() {
    return base64Encoding;
  }

  public void setBase64Encoding(boolean base64Encoding) {
    this.base64Encoding = base64Encoding;
  }

  protected abstract KeySpec createKeySpec();

  protected abstract AlgorithmParameterSpec createAlgorithmParameterSpec();

}
