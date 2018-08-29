/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;

/**
 * Builder of {@link HttpRequestOptions}. Instances can only be obtained through {@link HttpRequestOptions#builder()}.
 * By default, a 30 seconds timeout is set with follows redirect and no authentication, streaming or proxy settings. The last
 * two can be configured at the {@link HttpClient} level as well, so if not set, the client's configuration will be used.
 *
 * @since 4.1.5 as experimental. GA in 4.2
 */
@Experimental
public final class HttpRequestOptionsBuilder {

  private int responseTimeout = 30000;
  private boolean followsRedirect = true;
  private HttpAuthentication authentication;
  private Boolean streamResponse;
  private ProxyConfig proxyConfig;
  private Integer responseBufferSize;

  HttpRequestOptionsBuilder() {}

  HttpRequestOptionsBuilder(HttpRequestOptions options) {
    this.responseTimeout = options.getResponseTimeout();
    this.followsRedirect = options.isFollowsRedirect();
    this.authentication = options.getAuthentication().orElse(null);
    this.streamResponse = options.isStreamResponse().orElse(null);
    this.responseBufferSize = options.getResponseBufferSize().orElse(null);
    this.proxyConfig = options.getProxyConfig().orElse(null);
  }

  public HttpRequestOptionsBuilder responseTimeout(int responseTimeout) {
    this.responseTimeout = responseTimeout;
    return this;
  }

  public HttpRequestOptionsBuilder followsRedirect(boolean followsRedirect) {
    this.followsRedirect = followsRedirect;
    return this;
  }

  public HttpRequestOptionsBuilder authentication(HttpAuthentication authentication) {
    this.authentication = authentication;
    return this;
  }

  public HttpRequestOptionsBuilder streamResponse(boolean streamResponse) {
    this.streamResponse = streamResponse;
    return this;
  }

  public HttpRequestOptionsBuilder responseBufferSize(int responseBufferSize) {
    this.responseBufferSize = responseBufferSize;
    return this;
  }

  public HttpRequestOptionsBuilder proxyConfig(ProxyConfig proxyConfig) {
    this.proxyConfig = proxyConfig;
    return this;
  }

  public HttpRequestOptions build() {
    return new DefaultHttpRequestOptions(responseTimeout, followsRedirect, authentication, streamResponse, responseBufferSize,
                                         proxyConfig);
  }

}
