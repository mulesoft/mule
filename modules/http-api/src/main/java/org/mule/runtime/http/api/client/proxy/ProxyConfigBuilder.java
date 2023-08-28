/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.proxy;

/**
 * Builder of {@link ProxyConfig}s. Instances can only be obtained using {@link ProxyConfig#builder()}. At the very least, a host
 * and port must be provided. A username and password should be provided if Basic authentication is required.
 * 
 * @since 4.0
 */
public final class ProxyConfigBuilder extends BaseProxyConfigBuilder<ProxyConfig, ProxyConfigBuilder> {

  ProxyConfigBuilder() {}

  @Override
  public ProxyConfig build() {
    return new DefaultProxyConfig(host, port, username, password, nonProxyHosts);
  }

}
