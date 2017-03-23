/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security;

import static java.util.Optional.of;
import static org.apache.ws.security.WSPasswordCallback.DECRYPT;
import static org.apache.ws.security.handler.WSHandlerConstants.DEC_PROP_REF_ID;
import static org.apache.ws.security.handler.WSHandlerConstants.ENCRYPT;
import org.mule.services.soap.api.security.config.WssKeyStoreConfiguration;
import org.mule.services.soap.security.callback.WSPasswordCallbackHandler;
import org.mule.services.soap.security.config.WssKeyStoreConfigurationPropertiesBuilder;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

/**
 * Decrypts an encrypted SOAP response, using the private key of the key-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssDecryptSecurityStrategyCxfAdapter implements SecurityStrategyCxfAdapter {

  private static final String WS_DECRYPT_PROPERTIES_KEY = "decryptProperties";

  /**
   * The keystore to use when decrypting the message.
   */
  private WssKeyStoreConfigurationPropertiesBuilder keyStoreConfiguration;

  public WssDecryptSecurityStrategyCxfAdapter(WssKeyStoreConfiguration keyStoreConfiguration) {
    this.keyStoreConfiguration = new WssKeyStoreConfigurationPropertiesBuilder(keyStoreConfiguration);
  }

  @Override
  public SecurityStrategyType securityType() {
    return SecurityStrategyType.INCOMING;
  }

  @Override
  public String securityAction() {
    return ENCRYPT;
  }

  @Override
  public Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return of(new WSPasswordCallbackHandler(DECRYPT, cb -> cb.setPassword(keyStoreConfiguration.getKeyPassword())));
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    return ImmutableMap.<String, Object>builder()
        .put(DEC_PROP_REF_ID, WS_DECRYPT_PROPERTIES_KEY)
        .put(WS_DECRYPT_PROPERTIES_KEY, keyStoreConfiguration.getConfigurationProperties())
        .build();
  }
}
