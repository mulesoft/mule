/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.proxy;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

/**
 * Builder of {@link ProxyConfig.NtlmProxyConfig}s. Instances can only be obtained using
 * {@link ProxyConfig.NtlmProxyConfig#builder()}. At the very least, a host, port, username, password and NTLM domain must be
 * provided.
 *
 * @since 4.0
 */
public final class NtlmProxyConfigBuilder extends BaseProxyConfigBuilder<ProxyConfig.NtlmProxyConfig, NtlmProxyConfigBuilder> {

  private String ntlmDomain;

  protected NtlmProxyConfigBuilder() {}

  public NtlmProxyConfigBuilder ntlmDomain(String ntlmDomain) {
    this.ntlmDomain = ntlmDomain;
    return this;
  }

  @Override
  public ProxyConfig.NtlmProxyConfig build() {
    checkArgument(username != null, "A username must be provided for an NTLM proxy.");
    checkArgument(password != null, "A password must be provided for an NTLM proxy.");
    return new DefaultNtlmProxyConfig(host, port, username, password, ntlmDomain, nonProxyHosts);
  }
}
