/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import static java.util.Optional.ofNullable;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;

import java.util.Optional;

/**
 * Basic implementation of {@link HttpRequestOptions}. Instances can only be obtained through {@link HttpRequestOptionsBuilder}.
 *
 * @since 4.2
 */
class DefaultHttpRequestOptions implements HttpRequestOptions {

  private int responseTimeout;
  private boolean followsRedirect;
  private HttpAuthentication authentication;
  private ProxyConfig proxyConfig;
  private boolean sendBodyAlways;

  DefaultHttpRequestOptions(int responseTimeout, boolean followsRedirect, HttpAuthentication authentication,
                            ProxyConfig proxyConfig, boolean sendBodyAlways) {
    this.responseTimeout = responseTimeout;
    this.followsRedirect = followsRedirect;
    this.authentication = authentication;
    this.proxyConfig = proxyConfig;
    this.sendBodyAlways = sendBodyAlways;
  }

  @Override
  public int getResponseTimeout() {
    return responseTimeout;
  }

  @Override
  public boolean isFollowsRedirect() {
    return followsRedirect;
  }

  @Override
  public Optional<HttpAuthentication> getAuthentication() {
    return ofNullable(authentication);
  }

  @Override
  public Optional<ProxyConfig> getProxyConfig() {
    return ofNullable(proxyConfig);
  }

  @Override
  public boolean shouldSendBodyAlways() {
    return sendBodyAlways;
  }
}
