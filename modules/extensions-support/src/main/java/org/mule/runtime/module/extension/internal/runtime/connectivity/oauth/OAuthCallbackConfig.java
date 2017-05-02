/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * Groups the sum of all the parameters that a user configured in order to provision
 * an OAuth access token callback
 *
 * @since 4.0
 */
public final class OAuthCallbackConfig {

  private final String listenerConfig;
  private final String host;
  private final int port;
  private final String callbackPath;
  private final String localAuthorizePath;

  public OAuthCallbackConfig(String listenerConfig, String host, int port, String callbackPath, String localAuthorizePath) {
    this.listenerConfig = listenerConfig;
    this.host = host;
    this.port = port;
    this.callbackPath = callbackPath;
    this.localAuthorizePath = localAuthorizePath;
  }

  public Optional<String> getListenerConfig() {
    return ofNullable(listenerConfig);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getCallbackPath() {
    return callbackPath;
  }

  public String getLocalAuthorizePath() {
    return localAuthorizePath;
  }
}
