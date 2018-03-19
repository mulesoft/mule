/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.runtime.http.api.client.auth.HttpAuthentication;

/**
 * Builder of {@link HttpRequestOptions}. Instances can only be obtained through {@link HttpRequestOptions#builder()}.
 * By default, a 30 seconds timeout is set with follows redirect and no authentication nor streaming.
 *
 * @since 4.2
 */
public final class HttpRequestOptionsBuilder {

  private int responseTimeout = 30000;
  private boolean followsRedirect = true;
  private HttpAuthentication authentication;

  HttpRequestOptionsBuilder() {}

  HttpRequestOptionsBuilder(HttpRequestOptions options) {
    this.responseTimeout = options.getResponseTimeout();
    this.followsRedirect = options.isFollowsRedirect();
    this.authentication = options.getAuthentication();
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

  public HttpRequestOptions build() {
    return new DefaultHttpRequestOptions(responseTimeout, followsRedirect, authentication);
  }

}
