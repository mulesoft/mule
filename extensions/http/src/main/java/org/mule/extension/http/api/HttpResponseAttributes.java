/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import org.mule.service.http.api.domain.ParameterMap;

/**
 * Representation of an HTTP response message attributes.
 *
 * @since 4.0
 */
public class HttpResponseAttributes extends HttpAttributes {

  /**
   * HTTP status code of the response. Former 'http.status'.
   */
  private final int statusCode;
  /**
   * HTTP reason phrase of the response. Former 'http.reason'.
   */
  private final String reasonPhrase;

  public HttpResponseAttributes(int statusCode, String reasonPhrase, ParameterMap headers) {
    super(headers);
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

}
