/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.request;

import static java.lang.System.lineSeparator;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;
import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.BaseHttpMessage;

import java.net.URI;

/**
 * Basic implementation of {@link HttpRequest}. Instances can only be obtained through an {@link HttpRequestBuilder}.
 */
class DefaultHttpRequest extends BaseHttpMessage implements HttpRequest {

  private final URI uri;
  private final String path;
  private final HttpProtocol protocol;
  private final String method;
  private final MultiMap<String, String> queryParams;
  private final HttpEntity entity;

  DefaultHttpRequest(URI uri, String path, String method, HttpProtocol protocol, CaseInsensitiveMultiMap headers,
                     MultiMap<String, String> queryParams, HttpEntity entity) {
    super(headers);
    this.uri = uri;
    this.path = path;
    this.protocol = protocol;
    this.method = method;
    this.queryParams = queryParams;
    this.entity = entity;
  }

  @Override
  public HttpProtocol getProtocol() {
    return protocol;
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
  public HttpEntity getEntity() {
    return entity;
  }

  @Override
  public URI getUri() {
    return uri;
  }

  @Override
  public MultiMap<String, String> getQueryParams() {
    return queryParams.toImmutableMultiMap();
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
