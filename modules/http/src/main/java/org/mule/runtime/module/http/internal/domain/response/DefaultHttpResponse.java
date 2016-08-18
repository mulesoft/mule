/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.domain.response;

import org.mule.runtime.module.http.internal.domain.BaseHttpMessage;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.response.HttpResponse;

import java.util.Collection;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

public class DefaultHttpResponse extends BaseHttpMessage implements HttpResponse {

  private final HttpEntity body;
  private ResponseStatus responseStatus = new ResponseStatus();
  private MultiMap headers = new MultiValueMap();

  public DefaultHttpResponse(ResponseStatus responseStatus, MultiMap headers, HttpEntity body) {
    this.responseStatus = responseStatus;
    this.headers = headers;
    this.body = body;
  }

  @Override
  public HttpEntity getEntity() {
    return body;
  }

  @Override
  public Collection<String> getHeaderNames() {
    return headers.keySet();
  }

  @Override
  public String getHeaderValue(String headerName) {
    final Object value = headers.get(headerName);
    if (value == null) {
      return null;
    }
    return (String) ((Collection) value).iterator().next();
  }

  @Override
  public Collection<String> getHeaderValues(String headerName) {
    return (Collection<String>) headers.get(headerName);
  }

  @Override
  public int getStatusCode() {
    return this.responseStatus.getStatusCode();
  }

  @Override
  public void setStatusCode(int statusCode) {
    this.responseStatus.setStatusCode(statusCode);
  }

  @Override
  public String getReasonPhrase() {
    return this.responseStatus.getReasonPhrase();
  }

  @Override
  public void setReasonPhrase(String reasonPhrase) {
    this.responseStatus.setReasonPhrase(reasonPhrase);
  }

}
