/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.proxy;

/**
 * HTTP proxy configuration for making HTTP requests through an NTLM authenticated proxy.
 *
 * @since 4.0
 */
public interface NtlmProxyConfig extends ProxyConfig {

  /**
   * @return a {@link NtlmProxyConfigBuilder}.
   */
  static NtlmProxyConfigBuilder builder() {
    return new NtlmProxyConfigBuilder();
  }

  /**
   * @return the HTTP proxy user domain
   */
  String getNtlmDomain();

}
