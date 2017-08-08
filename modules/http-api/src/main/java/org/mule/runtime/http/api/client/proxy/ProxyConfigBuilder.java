/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

/**
 * Builder of {@link ProxyConfig}s. Instances can only be obtained using {@link ProxyConfig#builder()}.
 * At the very least, a host and port must be provided. A username and password should be provided if Basic authentication is
 * required.
 * @since 4.0
 */
public final class ProxyConfigBuilder extends BaseProxyConfigBuilder<ProxyConfig, ProxyConfigBuilder> {

  ProxyConfigBuilder() {}

  @Override
  public ProxyConfig build() {
    return new DefaultProxyConfig(host, port, username, password, nonProxyHosts);
  }

}
