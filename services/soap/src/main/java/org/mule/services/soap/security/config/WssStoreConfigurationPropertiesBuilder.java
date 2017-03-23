/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.security.config;


import org.mule.services.soap.security.SecurityStrategyCxfAdapter;

import java.util.Properties;

/**
 * Base contract for Security Stores that prepares additional properties for CXF in order to apply some
 * kind of Web Service Security.
 *
 * @since 4.0
 */
public interface WssStoreConfigurationPropertiesBuilder {

  /**
   * Name of the property where the crypto provider is defined.
   */
  String WS_CRYPTO_PROVIDER_KEY = "org.apache.ws.security.crypto.provider";

  /**
   * @return a set of {@link Properties} to configure a {@link SecurityStrategyCxfAdapter}.
   */
  Properties getConfigurationProperties();
}
