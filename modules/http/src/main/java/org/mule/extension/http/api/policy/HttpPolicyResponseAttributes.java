/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.policy;

import org.mule.runtime.api.message.Attributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of an HTTP response message attributes that can be created through
 * the expression language to modify the response parameters of the http:listener
 * using policies.
 *
 * @since 4.0
 */
public class HttpPolicyResponseAttributes implements Attributes {

  /**
   * HTTP status code of the response. Former 'http.status'.
   */
  private int statusCode;
  /**
   * HTTP reason phrase of the response. Former 'http.reason'.
   */
  private String reasonPhrase;

  /**
   * Map of HTTP headers in the message. Former properties.
   */
  private Map<String, String> headers = new HashMap<>();

  public int getStatusCode() {
    return statusCode;
  }

  public String getReasonPhrase() {
    return reasonPhrase;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public void setReasonPhrase(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

}
