/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import org.mule.sdk.api.http.client.auth.HttpAuthenticationConfig;
import org.mule.sdk.api.http.client.proxy.ProxyConfig;

import java.util.function.Consumer;

public class ProxyConfigImpl implements ProxyConfig, HttpAuthenticationConfig {

  private String host;
  private int port;
  private String username;
  private String password;
  private String nonProxyHosts;

  private String ntlmDomain;
  private boolean isNtlm = false;

  @Override
  public ProxyConfig host(String host) {
    this.host = host;
    return this;
  }

  @Override
  public ProxyConfig port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public ProxyConfig nonProxyHosts(String nonProxyHosts) {
    this.nonProxyHosts = nonProxyHosts;
    return this;
  }

  @Override
  public ProxyConfig auth(Consumer<HttpAuthenticationConfig> authConfigurer) {
    authConfigurer.accept(this);
    return this;
  }

  @Override
  public void basic(String username, String password, boolean preemptive) {
    if (preemptive) {
      throw new IllegalArgumentException("Preemptive basic authentication is not supported for proxy");
    }
    this.username = username;
    this.password = password;
  }

  @Override
  public void digest(String username, String password, boolean preemptive) {
    throw new IllegalArgumentException("Digest authentication is not supported for proxy");
  }

  @Override
  public void ntlm(String username, String password, boolean preemptive, String domain, String workstation) {
    if (preemptive) {
      throw new IllegalArgumentException("Preemptive NTLM authentication is not supported for proxy");
    }
    if (workstation != null) {
      throw new IllegalArgumentException("NTLM workstation can't be configured for proxy");
    }
    this.username = username;
    this.password = password;
    this.ntlmDomain = domain;
    this.isNtlm = true;
  }

  public org.mule.runtime.http.api.client.proxy.ProxyConfig build() {
    if (isNtlm) {
      return org.mule.runtime.http.api.client.proxy.ProxyConfig.NtlmProxyConfig.builder()
          .host(host).port(port)
          .username(username).password(password)
          .nonProxyHosts(nonProxyHosts)
          .ntlmDomain(ntlmDomain)
          .build();
    } else {
      return org.mule.runtime.http.api.client.proxy.ProxyConfig.builder()
          .host(host).port(port)
          .username(username).password(password)
          .nonProxyHosts(nonProxyHosts)
          .build();
    }
  }
}
