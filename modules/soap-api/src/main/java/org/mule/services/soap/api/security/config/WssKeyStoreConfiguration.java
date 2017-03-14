/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.security.config;

/**
 * {@link WssStoreConfiguration} implementation for Key Stores, used for encryption, decryption and signing.
 *
 * @since 4.0
 */
public final class WssKeyStoreConfiguration implements WssStoreConfiguration {

  private final String alias;
  private final String keyPassword;
  private final String password;
  private final String keyStorePath;
  private final String type;

  public WssKeyStoreConfiguration(String alias, String keyPassword, String password, String keyStorePath, String type) {
    this.alias = alias;
    this.keyPassword = keyPassword;
    this.password = password;
    this.keyStorePath = keyStorePath;
    this.type = type;
  }

  public WssKeyStoreConfiguration(String alias, String password, String keyStorePath) {
    this(alias, null, password, keyStorePath, "jks");
  }

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
}
