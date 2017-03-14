/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.security.config;

/**
 * {@link WssStoreConfiguration} implementation for Trust Stores, used for signature verification.
 *
 * @since 4.0
 */
public final class WssTrustStoreConfiguration implements WssStoreConfiguration {

  private String trustStorePath;
  private String password;
  private String type;

  public WssTrustStoreConfiguration(String trustStorePath, String password, String type) {
    this.trustStorePath = trustStorePath;
    this.password = password;
    this.type = type;
  }

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
