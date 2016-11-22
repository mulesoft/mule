/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;


import static java.util.Optional.*;
import static org.apache.ws.security.handler.WSHandlerConstants.SIGNATURE;
import static org.apache.ws.security.handler.WSHandlerConstants.SIG_PROP_REF_ID;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.INCOMING;
import org.mule.extension.ws.internal.security.EncryptionHelper;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;


/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssVerifySignatureSecurityStrategy implements SecurityStrategy {

  private static final String WS_VERIFY_SIGNATURE_PROPERTIES_KEY = "verifySignatureProperties";
  private static final EncryptionHelper encryptionHelper = new EncryptionHelper();

  private TlsContextFactory tlsContextFactory;

  @Override
  public void initializeTlsContextFactory(TlsContextFactory tlsContextFactory) throws ConnectionException {
    if (tlsContextFactory == null) {
      throw new ConnectionException("Verify Signature security strategy required a TLS context and no one was provided");
    }
    this.tlsContextFactory = tlsContextFactory;
  }

  @Override
  public SecurityStrategyType securityType() {
    return INCOMING;
  }

  @Override
  public Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    return empty();
  }

  @Override
  public String securityAction() {
    return SIGNATURE;
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    Properties signature = tlsContextFactory == null ? encryptionHelper.createDefaultTrustStoreProperties()
        : encryptionHelper.createTrustStoreProperties(tlsContextFactory.getTrustStoreConfiguration());

    return ImmutableMap.<String, Object>builder()
        .put(SIG_PROP_REF_ID, WS_VERIFY_SIGNATURE_PROPERTIES_KEY)
        .put(WS_VERIFY_SIGNATURE_PROPERTIES_KEY, signature)
        .build();
  }
}
