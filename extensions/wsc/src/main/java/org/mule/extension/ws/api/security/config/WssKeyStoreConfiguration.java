/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security.config;

import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_ALIAS;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_PRIVATE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.KEYSTORE_TYPE;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;

/**
 * Default {@link WssStoreConfiguration} implementation for Key Stores, used for encryption, decryption and signing.
 *
 * @since 4.0
 */
public class WssKeyStoreConfiguration implements WssStoreConfiguration {

  @Parameter
  @Summary("The alias of the private key to use")
  private String alias;

  @Parameter
  @Summary("The password used to access the private key.")
  @Password
  private String keyPassword;

  @Parameter
  @Summary("The location of the KeyStore file")
  private String keyStorePath;

  @Parameter
  @Summary("The password to access the store.")
  @Password
  private String password;

  @Parameter
  @Optional(defaultValue = "jks")
  @Summary("The type of store (jks, pkcs12, jceks, or any other)")
  private String type;

  /**
   * @return The password used to access the private key.
   */
  public String getKeyPassword() {
    return keyPassword;
  }

  /**
   * @return The alias of the private key to use.
   */
  public String getAlias() {
    return alias;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStorePath() {
    return keyStorePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties getConfigurationProperties() {
    Properties properties = new Properties();
    properties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());
    properties.setProperty(KEYSTORE_TYPE, type);
    properties.setProperty(KEYSTORE_PASSWORD, password);
    properties.setProperty(KEYSTORE_PRIVATE_PASSWORD, keyPassword);
    properties.setProperty(KEYSTORE_ALIAS, alias);
    properties.setProperty(KEYSTORE_FILE, keyStorePath);
    return properties;
  }
}
