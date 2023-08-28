/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.message.request;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.http.api.domain.message.HttpMessage;

import java.net.URI;

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
    return new HttpRequestBuilder(false);
  }

  static HttpRequestBuilder builder(boolean preserveHeadersCase) {
    return new HttpRequestBuilder(preserveHeadersCase);
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
  URI getUri();

  /**
   * @return the query parameters
   */
  MultiMap<String, String> getQueryParams();

}
