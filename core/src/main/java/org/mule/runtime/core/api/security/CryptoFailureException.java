/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.core.api.EncryptionStrategy;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.I18nMessage;

/**
 * <code>CryptoFailureException</code> is a generic exception thrown by an CryptoStrategy if encryption or decryption fails. The
 * constuctors of this exception accept a EncryptionStrategy that will be included in the exception message. Implementors of
 * EncryptionStrategy should provide a toString method that exposes *only* information that maybe useful for debugging not
 * passwords, secret keys, etc.
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
    super(CoreMessages.cryptoFailure(), cause);
    String s = (strategy == null ? "null" : strategy.toString());
    addInfo("Encryption", s);
    this.encryptionStrategy = strategy;

  }

  public EncryptionStrategy getEncryptionStrategy() {
    return encryptionStrategy;
  }
}
