/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

/**
 * Basic implementation of a {@link NtlmProxyConfig}. Instances can only be obtained through a {@link NtlmProxyConfigBuilder}.
 */
class DefaultNtlmProxyConfig extends DefaultProxyConfig implements ProxyConfig.NtlmProxyConfig {

  private String ntlmDomain;

  DefaultNtlmProxyConfig(String host, int port, String username, String password, String ntlmDomain, String nonProxyHosts) {
    super(host, port, username, password, nonProxyHosts);
    this.ntlmDomain = ntlmDomain;
  }

  @Override
  public String getNtlmDomain() {
    return ntlmDomain;
  }
}
