/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.proxy;

import static java.util.Objects.hash;

import java.util.Objects;

/**
 * Basic implementation of a {@link NtlmProxyConfig}. Instances can only be obtained through a {@link NtlmProxyConfigBuilder}.
 */
class DefaultNtlmProxyConfig extends DefaultProxyConfig implements ProxyConfig.NtlmProxyConfig {

  private final String ntlmDomain;

  DefaultNtlmProxyConfig(String host, int port, String username, String password, String ntlmDomain, String nonProxyHosts) {
    super(host, port, username, password, nonProxyHosts);
    this.ntlmDomain = ntlmDomain;
  }

  @Override
  public String getNtlmDomain() {
    return ntlmDomain;
  }

  @Override
  public int hashCode() {
    return hash(super.hashCode(), ntlmDomain);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    DefaultNtlmProxyConfig that = (DefaultNtlmProxyConfig) obj;

    return Objects.equals(this.getHost(), that.getHost())
        && Objects.equals(this.getPort(), that.getPort())
        && Objects.equals(this.getPassword(), that.getPassword())
        && Objects.equals(this.getUsername(), that.getUsername())
        && Objects.equals(this.getNonProxyHosts(), that.getNonProxyHosts())
        && Objects.equals(this.ntlmDomain, that.ntlmDomain);
  }


}
