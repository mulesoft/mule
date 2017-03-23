/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;


import org.mule.extension.ws.api.security.config.WssTrustStoreConfigurationAdapter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.api.security.VerifySignatureSecurityStrategy;
import org.mule.services.soap.api.security.config.WssTrustStoreConfiguration;


/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssVerifySignatureSecurityStrategy implements SecurityStrategyAdapter {

  private static final String WS_VERIFY_SIGNATURE_PROPERTIES_KEY = "verifySignatureProperties";

  /**
   * The truststore to use to verify the signature.
   */
  @Parameter
  @Optional
  private WssTrustStoreConfigurationAdapter trustStoreConfiguration;

  @Override
  public SecurityStrategy getSecurityStrategy() {
    WssTrustStoreConfiguration trustStoreConfig =
        new WssTrustStoreConfiguration(trustStoreConfiguration.getStorePath(), trustStoreConfiguration.getPassword(),
                                       trustStoreConfiguration.getType());
    return new VerifySignatureSecurityStrategy(trustStoreConfig);
  }
}
