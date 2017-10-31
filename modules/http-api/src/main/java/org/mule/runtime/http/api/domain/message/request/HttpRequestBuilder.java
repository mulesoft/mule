/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.request;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.http.api.HttpConstants.Method.GET;
import static org.mule.runtime.http.api.utils.UriCache.getUriFromString;

import org.mule.runtime.http.api.HttpConstants.Method;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.message.HttpMessageBuilder;
import org.mule.runtime.http.api.utils.UriCache;

import java.net.URI;

/**
 * Builder of {@link HttpRequest}s. Instances can only be obtained using {@link HttpRequest#builder()}. At the very least, the
 * request URI needs to be provided via {@link #uri(String)}. By default, GET is used as method with empty headers, query
 * params and entity.
 *
 * @since 4.0
 */
public final class HttpRequestBuilder extends HttpMessageBuilder<HttpRequestBuilder, HttpRequest> {

  private String path;
  private URI uri;
  private String method = GET.name();
  private MultiMap<String, String> queryParams = new MultiMap<>();

  HttpRequestBuilder() {}

  /**
   * Declares the URI where this {@link HttpRequest} will be sent. Minimum required configuration.
   *
   * @param uri the URI (as a String) of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder uri(String uri) {
    int queryPos = uri.indexOf("?");
    this.path = queryPos > -1 ? uri.substring(0, queryPos) : uri;
    this.uri = getUriFromString(uri);
    return this;
  }

  /**
   * Declares the URI where this {@link HttpRequest} will be sent. Minimum required configuration.
   *
   * @param uri the URI of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder uri(URI uri) {
    this.uri = uri;
    return this;
  }

  /**
   * Allows for using extension methods, as defined in the rfc. In general, {@link #method(Method)} should be used.
   * 
   * @param method the HTTP method of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder method(String method) {
    this.method = method;
    return this;
  }

  /**
   * @param method the HTTP method of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder method(Method method) {
    this.method = method.name();
    return this;
  }

  /**
   * @param queryParams a {@link MultiMap} representing the HTTP query params of the {@link HttpRequest} desired. Non null.
   * @return this builder
   */
  public HttpRequestBuilder queryParams(MultiMap<String, String> queryParams) {
    this.queryParams = queryParams;
    return this;
  }

  /**
   * @return the current URI configured in the builder.
   */
  public URI getUri() {
    return uri;
  }

  /**
   * @return the current HTTP method configured in the builder.
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return an immutable version of the current query parameters in the builder.
   */
  public MultiMap<String, String> getQueryParams() {
    return queryParams.toImmutableMultiMap();
  }

  /**
   * @return an {@link HttpRequest} as described
   */
  @Override
  public HttpRequest build() {
    checkNotNull(uri, "URI must be specified to create an HTTP request");
    return new DefaultHttpRequest(uri, path, method, headers.toImmutableMultiMap(), queryParams.toImmutableMultiMap(),
                                  entity);

  }

}
