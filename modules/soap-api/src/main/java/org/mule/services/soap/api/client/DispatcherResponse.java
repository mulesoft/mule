/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.client;


import java.io.InputStream;

/**
 * A simple object that carries the information retrieved after the message was dispatched with a {@link MessageDispatcher}.
 *
 * @since 4.0
 */
public class DispatcherResponse {

  private final String contentType;
  private final InputStream content;

  public DispatcherResponse(String contentType, InputStream content) {
    this.contentType = contentType;
    this.content = content;
  }

  /**
   * @return the raw Web Service response content.
   */
  public InputStream getContent() {
    return content;
  }

  /**
   * @return the content-type of the raw WS response content.
   */
  public String getContentType() {
    return contentType;
  }
}
