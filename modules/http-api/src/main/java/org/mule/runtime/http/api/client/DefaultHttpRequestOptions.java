/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

  DefaultHttpRequestOptions(int responseTimeout, boolean followsRedirect, HttpAuthentication authentication,
                            ProxyConfig proxyConfig) {
    this.responseTimeout = responseTimeout;
    this.followsRedirect = followsRedirect;
    this.authentication = authentication;
    this.proxyConfig = proxyConfig;
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
}
