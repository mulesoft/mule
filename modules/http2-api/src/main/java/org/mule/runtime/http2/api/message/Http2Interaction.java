/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message;

import org.mule.runtime.http2.api.server.Http2ResponseSender;

import java.io.IOException;

public class Http2Interaction {

  private Http2ResponseSender responseSender;
  private Http2Response response;

  public void setResponseSender(Http2ResponseSender responseSender) {
    this.responseSender = responseSender;
  }

  public void setResponse(Http2Response response) {
    this.response = response;
  }

  public void sendResponse() throws IOException {
    responseSender.sendResponse(response);
  }
}
