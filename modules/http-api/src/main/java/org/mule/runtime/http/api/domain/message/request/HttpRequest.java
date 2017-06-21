/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.request;

import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.message.HttpMessage;

/**
 * Representation of an HTTP request message.
 *
 * @since 4.0
 */
public interface HttpRequest extends HttpMessage {

  /**
   * @return an {@link HttpRequestBuilder}
   */
  static HttpRequestBuilder builder() {
    return new HttpRequestBuilder();
  }

  /**
   * @return the protocol version
   */
  HttpProtocol getProtocol();

  /**
   * @return the request path extracted from the URI
   */
  String getPath();

  /**
   * @return the request HTTP method
   */
  String getMethod();

  /**
   * @return the request URI
   */
  String getUri();

  /**
   * @return the query parameters
   */
  MultiMap<String, String> getQueryParams();

}
