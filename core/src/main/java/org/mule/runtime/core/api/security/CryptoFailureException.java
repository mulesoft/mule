/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

/**
 * {@code CryptoFailureException} is a generic exception thrown by an CryptoStrategy if encryption or decryption fails.
 * The constructors of this exception accept a {@link EncryptionStrategy} that will be included in the exception message.
 * Implementors of {@link EncryptionStrategy} should provide a toString method that exposes *only* information
 * that maybe useful for debugging <b>not</b> passwords, secret keys, etc.
 *
 * @since 4.0
 */
public class CryptoFailureException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1336343718508294379L;

  private transient EncryptionStrategy encryptionStrategy;

  public CryptoFailureException(I18nMessage message, EncryptionStrategy strategy) {
    super(message);
    String s = (strategy == null ? "null" : strategy.toString());
    addInfo("Encryption", s);
    this.encryptionStrategy = strategy;
  }

  public CryptoFailureException(I18nMessage message, EncryptionStrategy strategy, Throwable cause) {
    super(message, cause);
    String s = (strategy == null ? "null" : strategy.toString());
    addInfo("Encryption", s);
    this.encryptionStrategy = strategy;
  }

  public CryptoFailureException(EncryptionStrategy strategy, Throwable cause) {
    super(I18nMessageFactory.createStaticMessage("Crypto Failure"), cause);
    String s = (strategy == null ? "null" : strategy.toString());
    addInfo("Encryption", s);
    this.encryptionStrategy = strategy;

  }

  public EncryptionStrategy getEncryptionStrategy() {
    return encryptionStrategy;
  }
}
