/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

import static java.lang.Integer.MAX_VALUE;

/**
 * Base implementation of a {@link ProxyConfig} builder. Implementations should extend it and indicate their type and the type of
 * the proxy they build.
 *
 * @param <P> the type of {@link ProxyConfig} this builder creates.
 * @param <B> the type of the builder itself.
 */
public abstract class BaseProxyConfigBuilder<P extends ProxyConfig, B extends BaseProxyConfigBuilder> {

  protected String host;
  protected int port = MAX_VALUE;
  protected String username = null;
  protected String password = null;
  protected String nonProxyHosts = null;

  public B host(String host) {
    this.host = host;
    return (B) this;
  }

  public B port(int port) {
    this.port = port;
    return (B) this;
  }

  public B username(String username) {
    this.username = username;
    return (B) this;
  }

  public B password(String password) {
    this.password = password;
    return (B) this;
  }

  public B nonProxyHosts(String nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
    return (B) this;
  }

  public abstract P build();
}
