/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.message.sdktomule;

import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.http.api.domain.HttpProtocol;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;

import java.net.URI;
import java.util.Collection;

public class HttpRequestWrapper implements HttpRequest {

  private final org.mule.sdk.api.http.domain.message.request.HttpRequest request;

  public HttpRequestWrapper(org.mule.sdk.api.http.domain.message.request.HttpRequest request) {
    this.request = request;
  }

  @Override
  public HttpProtocol getProtocol() {
    return HttpProtocol.valueOf(request.getProtocol().name());
  }

  @Override
  public String getPath() {
    return request.getPath();
  }

  @Override
  public String getMethod() {
    return request.getMethod();
  }

  @Override
  public URI getUri() {
    return request.getUri();
  }

  @Override
  public MultiMap<String, String> getQueryParams() {
    return request.getQueryParams();
  }

  @Override
  public HttpEntity getEntity() {
    return new HttpEntityWrapper(request.getEntity());
  }

  @Override
  public Collection<String> getHeaderNames() {
    return request.getHeaderNames();
  }

  @Override
  public boolean containsHeader(String headerName) {
    return request.containsHeader(headerName);
  }

  @Override
  public String getHeaderValue(String headerName) {
    return request.getHeaderValue(headerName);
  }

  @Override
  public String getHeaderValueIgnoreCase(String headerName) {
    return request.getHeaderValueIgnoreCase(headerName);
  }

  @Override
  public Collection<String> getHeaderValues(String headerName) {
    return request.getHeaderValues(headerName);
  }

  @Override
  public Collection<String> getHeaderValuesIgnoreCase(String headerName) {
    return request.getHeaderValuesIgnoreCase(headerName);
  }

  @Override
  public MultiMap<String, String> getHeaders() {
    return request.getHeaders();
  }
}
