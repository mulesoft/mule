/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.http.api.client.proxy.NtlmProxyConfigBuilder;
import org.mule.runtime.http.api.client.proxy.ProxyConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfigurer;

import java.util.function.Consumer;

public class ProxyConfigurerImpl implements ProxyConfigurer {

  private String host;
  private int port;
  private String username;
  private String password;
  private String nonProxyHosts;
  private Consumer<NtlmProxyConfigurer> ntlmConfigurer;

  @Override
  public ProxyConfigurer host(String host) {
    this.host = host;
    return this;
  }

  @Override
  public ProxyConfigurer port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public ProxyConfigurer username(String username) {
    this.username = username;
    return this;
  }

  @Override
  public ProxyConfigurer password(String password) {
    this.password = password;
    return this;
  }

  @Override
  public ProxyConfigurer nonProxyHosts(String nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
    return this;
  }

  @Override
  public ProxyConfigurer ntlm(Consumer<NtlmProxyConfigurer> ntlmConfigurer) {
    this.ntlmConfigurer = ntlmConfigurer;
    return this;
  }

  public ProxyConfig build() {
    if (ntlmConfigurer == null) {
      return ProxyConfig.builder()
          .host(host).port(port)
          .username(username).password(password)
          .nonProxyHosts(nonProxyHosts)
          .build();
    } else {
      NtlmProxyConfigBuilder builder = ProxyConfig.NtlmProxyConfig.builder()
          .host(host).port(port)
          .username(username).password(password)
          .nonProxyHosts(nonProxyHosts);
      ntlmConfigurer.accept(new NtlmProxyConfigurerImpl(builder));
      return builder.build();
    }
  }

  private static class NtlmProxyConfigurerImpl implements NtlmProxyConfigurer {

    private final NtlmProxyConfigBuilder builder;

    public NtlmProxyConfigurerImpl(NtlmProxyConfigBuilder builder) {
      this.builder = builder;
    }

    @Override
    public NtlmProxyConfigurer domain(String domain) {
      builder.ntlmDomain(domain);
      return this;
    }
  }
}
