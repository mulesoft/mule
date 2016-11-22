/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security;

import static java.util.Optional.of;
import static org.apache.ws.security.WSPasswordCallback.DECRYPT;
import static org.apache.ws.security.handler.WSHandlerConstants.DEC_PROP_REF_ID;
import static org.apache.ws.security.handler.WSHandlerConstants.ENCRYPT;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.INCOMING;
import org.mule.extension.ws.internal.security.EncryptionHelper;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.WSPasswordCallbackHandler;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Decrypts an encrypted SOAP response, using the private key of the key-store in the provided TLS context.
 *
 * @since 4.0
 */
public class WssDecryptSecurityStrategy implements SecurityStrategy {

  private static final String WS_DECRYPT_PROPERTIES_KEY = "decryptProperties";
  private static final EncryptionHelper encryptionHelper = new EncryptionHelper();

  private TlsContextFactory tlsContextFactory;

  @Override
  public void initializeTlsContextFactory(TlsContextFactory tlsContextFactory) throws ConnectionException {
    if (tlsContextFactory == null) {
      throw new ConnectionException("Decrypt security strategy required a TLS context and no one was provided");
    }
    this.tlsContextFactory = tlsContextFactory;
  }

  @Override
  public SecurityStrategyType securityType() {
    return INCOMING;
  }

  @Override
  public String securityAction() {
    return ENCRYPT;
  }

  @Override
  public Optional<WSPasswordCallbackHandler> buildPasswordCallbackHandler() {
    validateTls();
    TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();
    return of(new WSPasswordCallbackHandler(DECRYPT, cb -> cb.setPassword(keyStoreConfig.getKeyPassword())));
  }

  @Override
  public Map<String, Object> buildSecurityProperties() {
    validateTls();
    TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();
    Properties decryptionProperties = encryptionHelper.createKeyStoreProperties(keyStoreConfig);

    return ImmutableMap.<String, Object>builder()
        .put(DEC_PROP_REF_ID, WS_DECRYPT_PROPERTIES_KEY)
        .put(WS_DECRYPT_PROPERTIES_KEY, decryptionProperties)
        .build();
  }

  private void validateTls() {
    if (tlsContextFactory == null) {
      throw new IllegalStateException("Tls Context Factory was not initialized, cannot apply Decrypt security");
    }
  }
}
