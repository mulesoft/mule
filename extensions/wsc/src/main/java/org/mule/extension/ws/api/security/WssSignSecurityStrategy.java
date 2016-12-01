/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;

import static java.util.Optional.of;
import static org.apache.ws.security.WSPasswordCallback.SIGNATURE;
import static org.apache.ws.security.handler.WSHandlerConstants.SIGNATURE_USER;
import static org.apache.ws.security.handler.WSHandlerConstants.SIG_PROP_REF_ID;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.api.security.config.WssKeyStoreConfiguration;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;

import org.apache.ws.security.handler.WSHandlerConstants;

/**
 * Signs the SOAP request that is being sent, using the private key of the key-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssSignSecurityStrategy implements SecurityStrategy {

  private static final String WS_SIGN_PROPERTIES_KEY = "signProperties";

  /**
   * The keystore to use when signing the message.
   */
  @Parameter
  private WssKeyStoreConfiguration keyStoreConfiguration;

  @Override
  public SecurityStrategyType securityType() {
    return OUTGOING;
  }

  @Override
  public Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return of(new WSPasswordCallbackHandler(SIGNATURE, cb -> cb.setPassword(keyStoreConfiguration.getPassword())));
  }

  @Override
  public String securityAction() {
    return WSHandlerConstants.SIGNATURE;
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    return ImmutableMap.<String, Object>builder()
        .put(SIG_PROP_REF_ID, WS_SIGN_PROPERTIES_KEY)
        .put(WS_SIGN_PROPERTIES_KEY, keyStoreConfiguration.getConfigurationProperties())
        .put(SIGNATURE_USER, keyStoreConfiguration.getAlias())
        .build();
  }
}
