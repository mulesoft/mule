/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.message.muletosdk;

import org.mule.runtime.api.util.MultiMap;
import org.mule.sdk.api.http.domain.entity.HttpEntity;
import org.mule.sdk.api.http.domain.message.response.HttpResponse;

import java.util.Collection;

public class HttpResponseWrapper implements HttpResponse {

  private final org.mule.runtime.http.api.domain.message.response.HttpResponse delegate;

  public HttpResponseWrapper(org.mule.runtime.http.api.domain.message.response.HttpResponse delegate) {
    this.delegate = delegate;
  }

  @Override
  public int getStatusCode() {
    return delegate.getStatusCode();
  }

  @Override
  public String getReasonPhrase() {
    return delegate.getReasonPhrase();
  }

  @Override
  public HttpEntity getEntity() {
    return new HttpEntityWrapper(delegate.getEntity());
  }

  @Override
  public Collection<String> getHeaderNames() {
    return delegate.getHeaderNames();
  }

  @Override
  public boolean containsHeader(String headerName) {
    return delegate.containsHeader(headerName);
  }

  @Override
  public String getHeaderValue(String headerName) {
    return delegate.getHeaderValue(headerName);
  }

  @Override
  public Collection<String> getHeaderValues(String headerName) {
    return delegate.getHeaderValues(headerName);
  }

  @Override
  public MultiMap<String, String> getHeaders() {
    return delegate.getHeaders();
  }
}
