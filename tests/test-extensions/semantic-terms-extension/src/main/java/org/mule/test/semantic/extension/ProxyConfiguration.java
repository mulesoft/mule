/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresProxy;
import org.mule.sdk.api.annotation.semantics.connectivity.Host;
import org.mule.sdk.api.annotation.semantics.connectivity.Port;
import org.mule.sdk.api.annotation.semantics.connectivity.Url;
import org.mule.sdk.api.annotation.semantics.security.Password;
import org.mule.sdk.api.annotation.semantics.security.Username;

import java.util.Objects;

@ConfiguresProxy
public class ProxyConfiguration {

  @Parameter
  @Host
  private String host;

  @Parameter
  @Port
  private int port;

  @Parameter
  @Url
  private String url;

  @Parameter
  @Username
  private String username;

  @Parameter
  @Password
  private String password;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProxyConfiguration that = (ProxyConfiguration) o;
    return port == that.port && Objects.equals(host, that.host) && Objects.equals(url, that.url)
        && Objects.equals(username, that.username) && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port, url, username, password);
  }
}
