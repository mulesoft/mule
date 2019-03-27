/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.util.Optional;

/**
 * Options for setting up an {@link HttpRequest}. Instances can only be obtained through a {@link HttpRequestOptionsBuilder}.
 *
 * @since 4.2
 */
@NoImplement
public interface HttpRequestOptions {

  /**
   * @return a fresh {@link HttpRequestOptionsBuilder} to create instances.
   */
  static HttpRequestOptionsBuilder builder() {
    return new HttpRequestOptionsBuilder();
  }

  /**
   * @param options {@link HttpRequestOptions} to set up builder with.
   * @return a fresh {@link HttpRequestOptionsBuilder} to create instances.
   */
  static HttpRequestOptionsBuilder builder(HttpRequestOptions options) {
    return new HttpRequestOptionsBuilder(options);
  }

  /**
   * @return the time (in milliseconds) to wait for a response
   */
  int getResponseTimeout();

  /**
   * @return whether or not to follow redirect responses
   */
  boolean isFollowsRedirect();

  /**
   * @return the {@link HttpAuthentication} to use, if any.
   */
  Optional<HttpAuthentication> getAuthentication();

  /**
   * @return the {@link ProxyConfig} to use, if any.
   */
  Optional<ProxyConfig> getProxyConfig();

}
