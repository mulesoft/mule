/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.security;


import static java.util.Optional.ofNullable;
import org.mule.services.soap.api.security.config.WssTrustStoreConfiguration;

import java.util.Optional;


/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public final class VerifySignatureSecurityStrategy implements SecurityStrategy {

  /**
   * The truststore to use to verify the signature.
   */
  private final WssTrustStoreConfiguration trustStoreConfiguration;

  public VerifySignatureSecurityStrategy(WssTrustStoreConfiguration trustStoreConfiguration) {
    this.trustStoreConfiguration = trustStoreConfiguration;
  }

  public VerifySignatureSecurityStrategy() {
    this.trustStoreConfiguration = null;
  }

  public Optional<WssTrustStoreConfiguration> getTrustStoreConfiguration() {
    return ofNullable(trustStoreConfiguration);
  }

  @Override
  public void accept(SecurityStrategyVisitor visitor) {
    visitor.visitVerify(this);
  }
}
