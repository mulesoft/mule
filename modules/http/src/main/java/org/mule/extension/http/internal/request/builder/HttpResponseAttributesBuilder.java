/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.builder;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.model.ParameterMap;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;

/**
 * Creates {@link HttpResponseAttributes} based on an {@HttpResponse} and it's parts.
 */
public class HttpResponseAttributesBuilder {

  HttpResponse response;

  public HttpResponseAttributesBuilder setResponse(HttpResponse response) {
    this.response = response;
    return this;
  }

  public HttpResponseAttributes build() {
    ParameterMap headers = new ParameterMap();
    for (String headerName : response.getHeaderNames()) {
      headers.put(headerName, response.getHeaderValues(headerName));
    }

    int statusCode = response.getStatusCode();
    String reasonPhrase = response.getReasonPhrase();

    return new HttpResponseAttributes(statusCode, reasonPhrase, headers);
  }
}
