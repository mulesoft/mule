/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.runtime.http.api.server.HttpServer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Context to be passed to the {@link HttpServer#sse(String, Consumer, Consumer)}'s second parameter, when an SSE endpoint is hit
 * by a request. This context can be used to reject an SSE connection before starting to send the event stream.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 */
@Experimental
@NoImplement
public interface SseRequestContext {

  /**
   * @return the received request that matched an SSE endpoint.
   */
  HttpRequest getRequest();

  /**
   * By default, a {@link SseClient} will be created with a UUID as identifier. This method allows overriding that client id.
   * Avoid collisions is a responsibility of the method caller.
   * 
   * @param overrideId the new id to be used instead of the UUID.
   */
  void setClientId(String overrideId);

  /**
   * This method can be used to reject an SSE connection when a certain condition is not satisfied by the request. Once you called
   * this method, the event stream will not be sent.
   * 
   * @param response the alternative response to be sent instead of the normal SSE response.
   * @return a future that will be completed when the response was successfully sent. It can also be completed exceptionally if
   *         the response failed to be sent.
   */
  CompletableFuture<Void> reject(HttpResponse response);
}
