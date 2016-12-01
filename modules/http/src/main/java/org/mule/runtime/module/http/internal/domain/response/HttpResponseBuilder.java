/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.domain.response;

import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.response.HttpResponse;

import java.util.Collection;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;

public class HttpResponseBuilder {

  private MultiMap headers = new MultiValueMap();
  private ResponseStatus responseStatus = new ResponseStatus();
  private HttpEntity body;

  public HttpResponseBuilder addHeader(String name, Object value) {
    headers.put(name, value);
    return this;
  }

  public HttpResponseBuilder setStatusCode(Integer statusCode) {
    this.responseStatus.setStatusCode(statusCode);
    return this;
  }

  public HttpResponseBuilder setReasonPhrase(String reasonPhrase) {
    this.responseStatus.setReasonPhrase(reasonPhrase);
    return this;
  }

  public String getFirstHeader(String headerName) {
    final Object value = headers.get(headerName);
    return (String) (value == null ? value : ((Collection) value).iterator().next());
  }

  public Collection<String> getHeader(String headerName) {
    return (Collection<String>) headers.get(headerName);
  }

  public HttpResponseBuilder setEntity(HttpEntity body) {
    this.body = body;
    return this;
  }

  public HttpResponse build() {
    return new DefaultHttpResponse(responseStatus, headers, body);
  }

}
