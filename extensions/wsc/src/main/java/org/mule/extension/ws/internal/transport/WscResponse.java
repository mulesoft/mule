/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;

import java.io.InputStream;

/**
 * An generic web service response object that can be constructed with any protocol specific response content such as JMS or HTTP.
 * <p>
 * An instance of this class aims to be returned in each {@link WscDispatcher} implementation.
 *
 * @since 1.0
 */
public class WscResponse {

  private final InputStream body;
  private final String contentType;

  public WscResponse(InputStream body, String contentType) {
    this.body = body;
    this.contentType = contentType;
  }

  public InputStream getBody() {
    return body;
  }

  public String getContentType() {
    return contentType;
  }
}
