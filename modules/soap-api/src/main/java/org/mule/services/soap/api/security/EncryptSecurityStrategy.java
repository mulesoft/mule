/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.security;

import org.mule.services.soap.api.security.config.WssKeyStoreConfiguration;

/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public final class EncryptSecurityStrategy implements SecurityStrategy {

  /**
   * The keystore to use when encrypting the message.
   */
  private final WssKeyStoreConfiguration keyStoreConfiguration;

  public EncryptSecurityStrategy(WssKeyStoreConfiguration keyStoreConfiguration) {
    this.keyStoreConfiguration = keyStoreConfiguration;
  }

  public WssKeyStoreConfiguration getKeyStoreConfiguration() {
    return keyStoreConfiguration;
  }

  @Override
  public void accept(SecurityStrategyVisitor visitor) {
    visitor.visitEncrypt(this);
  }
}
