/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security.config;

import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_TYPE;
import org.mule.services.soap.api.security.config.WssTrustStoreConfiguration;

import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;

/**
 * Default {@link WssStoreConfigurationPropertiesBuilder} implementation for Trust Stores, used for signature verification.
 *
 * @since 4.0
 */
public class WssTrustStoreConfigurationPropertiesBuilder implements WssStoreConfigurationPropertiesBuilder {

  private String trustStorePath;
  private String password;
  private String type;

  public WssTrustStoreConfigurationPropertiesBuilder(WssTrustStoreConfiguration trustStoreConfiguration) {
    this.password = trustStoreConfiguration.getPassword();
    this.trustStorePath = trustStoreConfiguration.getStorePath();
    this.type = trustStoreConfiguration.getType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties getConfigurationProperties() {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(TRUSTSTORE_FILE, trustStorePath);
    properties.setProperty(TRUSTSTORE_TYPE, type);
    properties.setProperty(TRUSTSTORE_PASSWORD, password);
    return properties;
  }
}
