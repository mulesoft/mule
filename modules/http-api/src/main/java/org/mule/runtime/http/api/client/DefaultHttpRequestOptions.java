/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import static java.util.Optional.ofNullable;
import org.mule.api.annotation.Experimental;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;

import java.util.Optional;

/**
 * Basic implementation of {@link HttpRequestOptions}. Instances can only be obtained through {@link HttpRequestOptionsBuilder}.
 *
 * @since 4.1.5 as experimental. GA in 4.2
 */
@Experimental
class DefaultHttpRequestOptions implements HttpRequestOptions {

  private int responseTimeout;
  private boolean followsRedirect;
  private HttpAuthentication authentication;
  private Boolean streamResponse;
  private ProxyConfig proxyConfig;
  private Integer responseBufferSize;

  DefaultHttpRequestOptions(int responseTimeout, boolean followsRedirect, HttpAuthentication authentication,
                            Boolean streamResponse, Integer responseBufferSize, ProxyConfig proxyConfig) {
    this.responseTimeout = responseTimeout;
    this.followsRedirect = followsRedirect;
    this.authentication = authentication;
    this.streamResponse = streamResponse;
    this.responseBufferSize = responseBufferSize;
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
  public Optional<Boolean> isStreamResponse() {
    return ofNullable(streamResponse);
  }

  @Override
  public Optional<Integer> getResponseBufferSize() {
    return ofNullable(responseBufferSize);
  }

  @Override
  public Optional<ProxyConfig> getProxyConfig() {
    return ofNullable(proxyConfig);
  }
}
