/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.security.config;

import org.mule.extension.ws.api.security.SecurityStrategy;

import java.util.Properties;

/**
 * Provides methods to access the configuration of a store used to add WS security to the SOAP requests made using the
 * Web Service Consumer.
 *
 * @since 4.0
 */
public interface WssStoreConfiguration {

  /**
   * Name of the property where the crypto provider is defined.
   */
  static final String WS_CRYPTO_PROVIDER_KEY = "org.apache.ws.security.crypto.provider";

  /**
   * @return The location of the store.
   */
  String getStorePath();

  /**
   * @return The password to access the store.
   */
  String getPassword();

  /**
   * @return The type of store ("jks", "pkcs12", "jceks", or any other).
   */
  String getType();

  /**
   * @return a set of {@link Properties} to configure a {@link SecurityStrategy}.
   */
  Properties getConfigurationProperties();
}
