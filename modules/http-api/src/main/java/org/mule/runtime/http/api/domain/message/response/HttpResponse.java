/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
