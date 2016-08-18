/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.client.proxy;

/**
 * HTTP proxy configuration for making http requests.
 *
 * @since 4.0
 */
public interface ProxyConfig {

  /**
   * @return the http proxy host
   */
  String getHost();

  /**
   * @return the http proxy port
   */
  int getPort();

  /**
   * @return the http proxy authentication username
   */
  String getUsername();

  /**
   * @return the http proxy authentication password
   */
  String getPassword();

}
