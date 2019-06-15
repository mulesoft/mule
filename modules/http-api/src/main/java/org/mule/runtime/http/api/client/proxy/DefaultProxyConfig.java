/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

import static java.util.Objects.hash;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.Objects;

/**
 * Basic implementation of a {@link ProxyConfig}. Instances can only be obtained through a {@link ProxyConfigBuilder}.
 */
class DefaultProxyConfig implements ProxyConfig {

  private static final int MAXIMUM_PORT_NUMBER = 65535;

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String nonProxyHosts;

  DefaultProxyConfig(String host, int port, String username, String password, String nonProxyHosts) {
    checkArgument(host != null, "Host must be not null");
    checkArgument(port <= MAXIMUM_PORT_NUMBER,
                  "Port must be under 65535. Check that you set the port.");
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.nonProxyHosts = nonProxyHosts;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getNonProxyHosts() {
    return nonProxyHosts;
  }

  @Override
  public int hashCode() {
    return hash(host, port, password, username, nonProxyHosts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    DefaultProxyConfig that = (DefaultProxyConfig) obj;

    return Objects.equals(this.host, that.host)
        && Objects.equals(this.port, that.port)
        && Objects.equals(this.password, that.password)
        && Objects.equals(this.username, that.username)
        && Objects.equals(this.nonProxyHosts, that.nonProxyHosts);
  }

}
