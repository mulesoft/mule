/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server.async;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Handler for sending an HTTP response asynchronously
 *
 * @since 4.0
 */
@NoImplement
public interface HttpResponseReadyCallback {

  /**
   * Method to send response to the client.
   *
   * @param response               HTTP response content.
   * @param responseStatusCallback callback to be called if there's a failure while sending the response or when it is
   *                               successfully sent.
   */
  void responseReady(HttpResponse response, ResponseStatusCallback responseStatusCallback);

  /**
   * Method to send a delayed response to the client. The provided {@link HttpResponse} will be used to send all HTTP metadata
   * (status code, headers and so on) but its {@link org.mule.runtime.http.api.domain.entity.HttpEntity} will be ignored and the
   * HTTP body will be written through the returned {@link Writer}. The connection will remain open until the {@link Writer} is
   * closed.
   *
   * @param response               HTTP response content
   * @param responseStatusCallback callback to be called if there's a failure while sending the response or when it is
   *                               successfully sent.
   * @param encoding               the encoding the {@link Writer} will use
   * @return an HTTP body {@link Writer}
   *
   * @since 4.2
   */
  Writer startResponse(HttpResponse response, ResponseStatusCallback responseStatusCallback, Charset encoding);

}
