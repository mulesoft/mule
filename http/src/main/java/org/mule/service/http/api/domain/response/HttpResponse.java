/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain.response;

import org.mule.service.http.api.domain.HttpMessage;

/**
 * Representation of an HTTP response message.
 *
 * @since 4.0
 */
public interface HttpResponse extends HttpMessage {

  /**
   * @return the HTTP status line code
   */
  int getStatusCode();

  //TODO - MULE-10760: Remove setters once builders are available
  void setStatusCode(int statusCode);

  /**
   * @return the HTTP status line reason phrase
   */
  String getReasonPhrase();

  void setReasonPhrase(String reasonPhrase);

}
