/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

}
