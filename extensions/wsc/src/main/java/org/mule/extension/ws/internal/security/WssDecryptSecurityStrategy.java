/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security;

import org.mule.extension.ws.internal.security.config.WssKeyStoreConfigurationAdapter;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.security.DecryptSecurityStrategy;
import org.mule.runtime.extension.api.soap.security.SecurityStrategy;
import org.mule.runtime.extension.api.soap.security.config.WssKeyStoreConfiguration;

/**
 * Decrypts an encrypted SOAP response, using the private key of the key-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssDecryptSecurityStrategy implements SecurityStrategyAdapter {

  /**
   * The keystore to use when decrypting the message.
   */
  @Parameter
  private WssKeyStoreConfigurationAdapter keyStoreConfiguration;

  @Override
  public SecurityStrategy getSecurityStrategy() {
    WssKeyStoreConfiguration keyStore =
        new WssKeyStoreConfiguration(keyStoreConfiguration.getAlias(), keyStoreConfiguration.getKeyPassword(),
                                     keyStoreConfiguration.getPassword(), keyStoreConfiguration.getStorePath(),
                                     keyStoreConfiguration.getType());
    return new DecryptSecurityStrategy(keyStore);
  }
}
