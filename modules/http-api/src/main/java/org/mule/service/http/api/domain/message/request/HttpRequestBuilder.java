/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.message.request;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.message.HttpMessageBuilder;

/**
 * Builder of {@link HttpRequest}s. Instances can only be obtained using {@link HttpRequest#builder()}.
 * At the very least, the request URI needs to be provided via {@link #setUri(String)}. By default, GET is used as method with
 * empty headers, query params and entity.
 *
 * @since 4.0
 */
public final class HttpRequestBuilder extends HttpMessageBuilder<HttpRequestBuilder, HttpRequest> {

  private String path;
  private String uri;
  private String method = "GET";
  private ParameterMap queryParams = new ParameterMap();

  HttpRequestBuilder() {}

  /**
   * Declares the URI where this {@link HttpRequest} will be sent. Minimum required configuration.
   *
   * @param uri the URI of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder setUri(String uri) {
    int queryPos = uri.indexOf("?");
    this.path = queryPos > -1 ? uri.substring(0, queryPos) : uri;
    this.uri = uri;
    return this;
  }

  /**
   * @param method the HTTP method of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder setMethod(String method) {
    this.method = method;
    return this;
  }

  /**
   * @param headersMap a {@link ParameterMap} representing the HTTP headers of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder setHeaders(ParameterMap headersMap) {
    headersMap.keySet().forEach(
                                key -> headersMap.getAll(key).forEach(
                                                                      value -> this.headers.put(key, value)));
    return this;
  }

  /**
   * @param queryParams a {@link ParameterMap} representing the HTTP query params of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder setQueryParams(ParameterMap queryParams) {
    this.queryParams = queryParams;
    return this;
  }

  /**
   * @return an {@link HttpRequest} as described
   */
  @Override
  public HttpRequest build() {
    checkNotNull(uri, "URI must be specified to create an HTTP request");
    return new DefaultHttpRequest(uri, path, method, headers.toImmutableParameterMap(), queryParams.toImmutableParameterMap(),
                                  entity);

  }

}
