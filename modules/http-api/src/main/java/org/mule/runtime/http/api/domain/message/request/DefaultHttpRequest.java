/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.request;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.BaseHttpMessage;

import java.net.URI;
import java.util.Collection;

/**
 * Basic implementation of {@link HttpRequest}. Instances can only be obtained through an {@link HttpRequestBuilder}.
 */
class DefaultHttpRequest extends BaseHttpMessage implements HttpRequest {

  private final URI uri;
  private final String path;
  private final String method;
  private HttpProtocol version;
  private MultiMap<String, String> headers;
  private MultiMap<String, String> queryParams;
  private HttpEntity entity;

  DefaultHttpRequest(URI uri, String path, String method, MultiMap<String, String> headers,
                     MultiMap<String, String> queryParams, HttpEntity entity) {
    this.uri = uri;
    this.path = path;
    this.method = method;
    this.headers = headers;
    this.queryParams = queryParams;
    this.entity = entity;
  }

  @Override
  public HttpProtocol getProtocol() {
    return this.version;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getMethod() {
    return method;
  }


  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @Override
  public String getHeaderValue(String headerName) {
    return headers.get(headerName);
  }

  @Override
  public Collection<String> getHeaderValues(String headerName) {
    return headers.getAll(headerName);
  }

  @Override
  public HttpEntity getEntity() {
    return entity;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public MultiMap<String, String> getQueryParams() {
    return queryParams;
  }

  @Override
  public String toString() {
    return "DefaultHttpRequest {" + lineSeparator()
        + "  uri: " + uri.toString() + "," + lineSeparator()
        + "  path: " + path + "," + lineSeparator()
        + "  method: " + method + "," + lineSeparator()
        + "  headers: " + headers.toString() + "," + lineSeparator()
        + "  queryParams: " + queryParams.toString() + lineSeparator()
        + "}";
  }

}
