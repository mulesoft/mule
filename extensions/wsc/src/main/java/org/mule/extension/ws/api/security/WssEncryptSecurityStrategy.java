/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;

import static org.apache.ws.security.handler.WSHandlerConstants.ENCRYPT;
import static org.apache.ws.security.handler.WSHandlerConstants.ENCRYPTION_USER;
import static org.apache.ws.security.handler.WSHandlerConstants.ENC_PROP_REF_ID;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.api.security.config.WssKeyStoreConfiguration;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssEncryptSecurityStrategy implements SecurityStrategy {

  private static final String WS_ENCRYPT_PROPERTIES_KEY = "encryptProperties";

  /**
   * The keystore to use when encrypting the message.
   */
  @Parameter
  private WssKeyStoreConfiguration keyStoreConfiguration;

  @Override
  public SecurityStrategyType securityType() {
    return OUTGOING;
  }

  @Override
  public String securityAction() {
    return ENCRYPT;
  }

  @Override
  public java.util.Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return java.util.Optional.empty();
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    return ImmutableMap.<String, Object>builder().put(ENC_PROP_REF_ID, WS_ENCRYPT_PROPERTIES_KEY)
        .put(WS_ENCRYPT_PROPERTIES_KEY, keyStoreConfiguration.getConfigurationProperties())
        .put(ENCRYPTION_USER, keyStoreConfiguration.getAlias())
        .build();
  }
}
