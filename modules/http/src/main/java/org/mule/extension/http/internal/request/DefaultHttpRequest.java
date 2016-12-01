/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import org.mule.runtime.module.http.internal.domain.BaseHttpMessage;
import org.mule.service.http.api.domain.ParameterMap;
import org.mule.service.http.api.domain.HttpProtocol;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.request.HttpRequest;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Default implementation of an {@link HttpRequest}.
 *
 * @since 4.0
 */
public class DefaultHttpRequest extends BaseHttpMessage implements HttpRequest {

  private final String uri;
  private final String path;
  private final String method;
  private HttpProtocol version;
  private ParameterMap headers;
  private ParameterMap queryParams;
  private HttpEntity entity;

  DefaultHttpRequest(String uri, String path, String method, ParameterMap headers, ParameterMap queryParams, HttpEntity entity) {
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
    if (headers == null) {
      return new ArrayList<>();
    }
    return headers.keySet();
  }

  @Override
  public String getHeaderValue(String headerName) {
    if (headers == null) {
      return null;
    }
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
  public String getUri() {
    return uri;
  }

  @Override
  public ParameterMap getQueryParams() {
    return queryParams;
  }

  @Override
  public InputStreamHttpEntity getInputStreamEntity() {
    return null;
  }
}
