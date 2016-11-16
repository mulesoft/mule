/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security;

import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_ALIAS;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_PRIVATE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_TYPE;
import static org.apache.ws.security.components.crypto.Merlin.LOAD_CA_CERTS;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_TYPE;
import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;

import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;

/**
 * Helper class to create objects with the required properties to configure key stores and trust stores for WS-Security.
 *
 * @since 4.0
 */
public class EncryptionHelper {

  /**
   * Name of the property where the crypto provider is defined.
   */
  private static final String WS_CRYPTO_PROVIDER_KEY = "org.apache.ws.security.crypto.provider";

  /**
   * Creates a {@link Properties} object with the attributes of a key store.
   */
  public Properties createKeyStoreProperties(TlsContextKeyStoreConfiguration keyStoreConfiguration) {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(KEYSTORE_TYPE, keyStoreConfiguration.getType());
    properties.setProperty(KEYSTORE_PASSWORD, keyStoreConfiguration.getPassword());
    properties.setProperty(KEYSTORE_PRIVATE_PASSWORD, keyStoreConfiguration.getKeyPassword());
    properties.setProperty(KEYSTORE_ALIAS, keyStoreConfiguration.getAlias());
    properties.setProperty(KEYSTORE_FILE, keyStoreConfiguration.getPath());
    return properties;
  }

  /**
   * Creates a {@link Properties} object with the attributes of a trust store.
   */
  public Properties createTrustStoreProperties(TlsContextTrustStoreConfiguration trustStoreConfiguration) {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(TRUSTSTORE_TYPE, trustStoreConfiguration.getType());
    properties.setProperty(TRUSTSTORE_PASSWORD, trustStoreConfiguration.getPassword());
    properties.setProperty(TRUSTSTORE_FILE, trustStoreConfiguration.getPath());
    return properties;
  }

  /**
   * Creates a {@link Properties} object configured to use the default trust store.
   */
  public Properties createDefaultTrustStoreProperties() {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(LOAD_CA_CERTS, String.valueOf(true));
    return properties;
  }
}
