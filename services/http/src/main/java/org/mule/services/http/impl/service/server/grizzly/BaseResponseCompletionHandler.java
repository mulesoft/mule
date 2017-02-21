/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.mule.service.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.service.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.service.http.api.HttpHeaders.Values.CLOSE;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.service.http.api.domain.message.response.HttpResponse;

import java.util.Collection;

import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.slf4j.Logger;

public abstract class BaseResponseCompletionHandler extends EmptyCompletionHandler<WriteResult> {

  private static final Logger LOGGER = getLogger(BaseResponseCompletionHandler.class);

  protected HttpResponsePacket buildHttpResponsePacket(HttpRequestPacket sourceRequest, HttpResponse httpResponse) {
    final HttpResponsePacket.Builder responsePacketBuilder = HttpResponsePacket.builder(sourceRequest)
        .status(httpResponse.getStatusCode()).reasonPhrase(httpResponse.getReasonPhrase());

    final Collection<String> allHeaders = httpResponse.getHeaderNames();
    for (String headerName : allHeaders) {
      final Collection<String> values = httpResponse.getHeaderValues(headerName);
      for (String value : values) {
        responsePacketBuilder.header(headerName, value);
      }
    }
    HttpResponsePacket httpResponsePacket = responsePacketBuilder.build();
    httpResponsePacket.setProtocol(sourceRequest.getProtocol());
    if (httpResponse.getHeaderValueIgnoreCase(TRANSFER_ENCODING) != null) {
      httpResponsePacket.setChunked(true);
    }

    if (CLOSE.equalsIgnoreCase(httpResponsePacket.getHeader(CONNECTION))) {
      httpResponsePacket.getProcessingState().setKeepAlive(false);
    }
    return httpResponsePacket;
  }

  @Override
  public void cancelled() {
    LOGGER.warn("HTTP response sending task was cancelled");
  }

  @Override
  public void failed(Throwable throwable) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(String.format("HTTP response sending task failed with error: %s", throwable.getMessage()));
    }
  }

}
