/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.server;

import org.mule.runtime.http2.api.message.Http2Request;

import java.io.IOException;

/**
 * Handles HTTP/2 requests. Implementations must be able to handle many requests concurrently.
 */
public interface Http2RequestHandler {

  /**
   * Method to be invoked on each request.
   *
   * @param request        is the request to be handled.
   * @param responseSender is a callback object to send the response.
   */
  void handle(Http2Request request, Http2ResponseSender responseSender) throws IOException;
}
