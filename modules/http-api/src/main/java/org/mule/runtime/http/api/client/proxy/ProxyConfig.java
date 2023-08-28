/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.client.proxy;

/**
 * HTTP proxy configuration for making HTTP requests.
 *
 * @since 4.0
 */
public interface ProxyConfig {

  /**
   * @return a {@link ProxyConfigBuilder}.
   */
  static ProxyConfigBuilder builder() {
    return new ProxyConfigBuilder();
  }

  /**
   * @return the HTTP proxy host
   */
  String getHost();

  /**
   * @return the HTTP proxy port
   */
  int getPort();

  /**
   * @return the HTTP proxy authentication username
   */
  String getUsername();

  /**
   * @return the HTTP proxy authentication password
   */
  String getPassword();

  /**
   * @return A list of comma separated hosts against which the proxy should not be used
   */
  String getNonProxyHosts();

  /**
   * HTTP proxy configuration for making HTTP requests through an NTLM authenticated proxy.
   *
   * @since 4.0
   */
  interface NtlmProxyConfig extends ProxyConfig {

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
}
