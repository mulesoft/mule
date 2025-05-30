/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.client;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

/**
 * Error context. A successful SSE response is a 200 OK response that contains the header {@code Content-Type: text/event-stream}.
 * Then, an SSE connection can fail because of an exception, or because the response doesn't satisfy the mentioned condition. This
 * context provides the corresponding object (the error or the response). It also has a method to stop the retry mechanism in case
 * it's necessary for the consumer.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @since 4.9.3, 4.10.0
 */
@Experimental
@NoImplement
public interface SseFailureContext {

  /**
   * @return the exception.
   */
  Throwable error();

  /**
   * @return the SSE connection failure response.
   */
  HttpResponse response();

  /**
   * When a connection failure handler calls it, the source should abort the retrying mechanism.
   */
  void stopRetrying();
}
