/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security;


import static org.apache.ws.security.components.crypto.Merlin.LOAD_CA_CERTS;
import static org.apache.ws.security.handler.WSHandlerConstants.SIGNATURE;
import static org.apache.ws.security.handler.WSHandlerConstants.SIG_PROP_REF_ID;
import org.mule.services.soap.api.security.config.WssTrustStoreConfiguration;
import org.mule.services.soap.security.callback.WSPasswordCallbackHandler;
import org.mule.services.soap.security.config.WssStoreConfigurationPropertiesBuilder;
import org.mule.services.soap.security.config.WssTrustStoreConfigurationPropertiesBuilder;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;


/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssVerifySignatureSecurityStrategyCxfAdapter implements SecurityStrategyCxfAdapter {

  private static final String WS_VERIFY_SIGNATURE_PROPERTIES_KEY = "verifySignatureProperties";

  /**
   * The truststore to use to verify the signature.
   */
  private final WssTrustStoreConfigurationPropertiesBuilder trustStoreConfiguration;

  public WssVerifySignatureSecurityStrategyCxfAdapter(WssTrustStoreConfiguration trustStoreConfiguration) {
    this.trustStoreConfiguration = new WssTrustStoreConfigurationPropertiesBuilder(trustStoreConfiguration);
  }

  public WssVerifySignatureSecurityStrategyCxfAdapter() {
    this.trustStoreConfiguration = null;
  }

  @Override
  public SecurityStrategyType securityType() {
    return SecurityStrategyType.INCOMING;
  }

  @Override
  public java.util.Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return java.util.Optional.empty();
  }

  @Override
  public String securityAction() {
    return SIGNATURE;
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    Properties signatureProps = trustStoreConfiguration != null ? trustStoreConfiguration.getConfigurationProperties()
        : getDefaultTrustStoreConfigurationProperties();

    return ImmutableMap.<String, Object>builder()
        .put(SIG_PROP_REF_ID, WS_VERIFY_SIGNATURE_PROPERTIES_KEY)
        .put(WS_VERIFY_SIGNATURE_PROPERTIES_KEY, signatureProps)
        .build();
  }

  private Properties getDefaultTrustStoreConfigurationProperties() {
    Properties properties = new Properties();
    properties.setProperty(WssStoreConfigurationPropertiesBuilder.WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(LOAD_CA_CERTS, String.valueOf(true));
    return properties;
  }
}
