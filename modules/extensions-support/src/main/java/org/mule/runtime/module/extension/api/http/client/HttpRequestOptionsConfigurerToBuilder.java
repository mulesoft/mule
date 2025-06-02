/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.http.api.client.HttpRequestOptionsBuilder;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfigurer;
import org.mule.sdk.api.http.client.auth.HttpAuthentication;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;

import java.util.Objects;

public class HttpRequestOptionsConfigurerToBuilder implements HttpRequestOptionsConfigurer {

  private final HttpRequestOptionsBuilder builder;

  HttpRequestOptionsConfigurerToBuilder(HttpRequestOptionsBuilder builder) {
    this.builder = builder;
  }

  @Override
  public HttpRequestOptionsConfigurer setResponseTimeout(int responseTimeout) {
    builder.responseTimeout(responseTimeout);
    return this;
  }

  @Override
  public HttpRequestOptionsConfigurer setFollowsRedirect(boolean followsRedirect) {
    builder.followsRedirect(followsRedirect);
    return this;
  }

  @Override
  public HttpRequestOptionsConfigurer setAuthentication(HttpAuthentication authentication) {
    // builder.authentication(authentication);
    return this;
  }

  @Override
  public HttpRequestOptionsConfigurer setProxyConfig(ProxyConfig proxyConfig) {
    // builder.proxyConfig(proxyConfig);
    return this;
  }

  @Override
  public HttpRequestOptionsConfigurer setSendBodyAlways(boolean sendBodyAlways) {
    builder.sendBodyAlways(sendBodyAlways);
    return this;
  }

  public HttpRequestOptionsBuilder builder() {
    return builder;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    var that = (HttpRequestOptionsConfigurerToBuilder) obj;
    return Objects.equals(this.builder, that.builder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(builder);
  }

  @Override
  public String toString() {
    return "HttpRequestOptionsConfigurerToBuilder[" +
        "builder=" + builder + ']';
  }

}
