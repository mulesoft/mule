/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.security.config;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.soap.security.config.WssStoreConfiguration;

/**
 * Default {@link WssStoreConfiguration} implementation for Trust Stores, used for signature verification.
 *
 * @since 4.0
 */
@Alias("wss-trust-store-configuration")
public class WssTrustStoreConfigurationAdapter implements WssStoreConfiguration {

  @Parameter
  @Summary("The location of the TrustStore file")
  private String trustStorePath;

  @Parameter
  @Summary("The password to access the store.")
  @Password
  private String password;

  @Parameter
  @Optional(defaultValue = "jks")
  @Summary("The type of store (jks, pkcs12, jceks, or any other)")
  private String type;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getStorePath() {
    return trustStorePath;
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
