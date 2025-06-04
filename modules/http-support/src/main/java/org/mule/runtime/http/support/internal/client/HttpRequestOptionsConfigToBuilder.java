/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.runtime.http.api.client.HttpRequestOptionsBuilder;
import org.mule.sdk.api.http.client.HttpRequestOptionsConfig;
import org.mule.sdk.api.http.client.auth.HttpAuthenticationConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;

import java.util.Objects;
import java.util.function.Consumer;

public class HttpRequestOptionsConfigToBuilder implements HttpRequestOptionsConfig {

  private final HttpRequestOptionsBuilder builder;

  HttpRequestOptionsConfigToBuilder(HttpRequestOptionsBuilder builder) {
    this.builder = builder;
  }

  @Override
  public HttpRequestOptionsConfig setResponseTimeout(int responseTimeout) {
    builder.responseTimeout(responseTimeout);
    return this;
  }

  @Override
  public HttpRequestOptionsConfig setFollowsRedirect(boolean followsRedirect) {
    builder.followsRedirect(followsRedirect);
    return this;
  }

  @Override
  public HttpRequestOptionsConfig setAuthentication(Consumer<HttpAuthenticationConfig> authConfigCallback) {
    HttpAuthenticationConfigurerImpl configurer = new HttpAuthenticationConfigurerImpl();
    authConfigCallback.accept(configurer);
    builder.authentication(configurer.build());
    return this;
  }

  @Override
  public HttpRequestOptionsConfig setProxyConfig(Consumer<ProxyConfig> proxyConfigCallback) {
    ProxyConfigImpl configurer = new ProxyConfigImpl();
    proxyConfigCallback.accept(configurer);
    builder.proxyConfig(configurer.build());
    return this;
  }

  @Override
  public HttpRequestOptionsConfig setSendBodyAlways(boolean sendBodyAlways) {
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
    var that = (HttpRequestOptionsConfigToBuilder) obj;
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
