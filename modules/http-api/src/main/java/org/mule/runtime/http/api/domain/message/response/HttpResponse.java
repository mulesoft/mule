/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.message.response;

import org.mule.runtime.http.api.domain.message.HttpMessage;

/**
 * Representation of an HTTP response message.
 *
 * @since 4.0
 */
public interface HttpResponse extends HttpMessage {

  /**
   * @return an {@link HttpResponseBuilder}
   */
  static HttpResponseBuilder builder() {
    return new HttpResponseBuilder();
  }

  /**
   * @return the HTTP status line code
   */
  int getStatusCode();

  /**
   * @return the HTTP status line reason phrase
   */
  String getReasonPhrase();

}
