/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.response;

import static java.lang.System.lineSeparator;
import org.mule.runtime.http.api.domain.CaseInsensitiveMultiMap;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.BaseHttpMessage;

/**
 * Basic implementation of {@link HttpResponse}. Instances can only be obtained through an {@link HttpResponseBuilder}.
 */
class DefaultHttpResponse extends BaseHttpMessage implements HttpResponse {

  private final HttpEntity body;
  private final ResponseStatus responseStatus;


  DefaultHttpResponse(ResponseStatus responseStatus, CaseInsensitiveMultiMap headers, HttpEntity body) {
    super(headers);
    this.responseStatus = responseStatus;
    this.body = body;
  }

  @Override
  public HttpEntity getEntity() {
    return body;
  }

  @Override
  public int getStatusCode() {
    return this.responseStatus.getStatusCode();
  }

  @Override
  public String getReasonPhrase() {
    return this.responseStatus.getReasonPhrase();
  }

  @Override
  public String toString() {
    return "DefaultHttpResponse {" + lineSeparator()
        + "  responseStatus: " + responseStatus.getStatusCode() + " (" + responseStatus.getReasonPhrase() + ")," + lineSeparator()
        + "  headers: " + headers.toString() + lineSeparator()
        + "}";
  }

}
