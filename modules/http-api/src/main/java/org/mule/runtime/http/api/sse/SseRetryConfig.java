/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

/**
 * The {@link ServerSentEventSource} implements the corresponding
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">server-sent-events spec</a>. This interface allows
 * configuring the retry mechanism described there.
 */
public interface SseRetryConfig {

  SseRetryConfig DEFAULT = new SseRetryConfig() {};

  /**
   * The server may use the retry key in the event to override the retry delay. With this configuration, the user can set if the
   * retry mechanism has to accept that parameter.
   * 
   * @return {@code true} if the "retry" parameter is allowed, or {@code false} otherwise.
   */
  default boolean allowRetryDelayOverride() {
    return true;
  }

  /**
   * The spec states: "A reconnection time, in milliseconds. This must initially be an implementation-defined value, probably in
   * the region of a few seconds."
   * 
   * @return the initial retry delay. The default is 2 seconds
   */
  default long getInitialRetryDelayMillis() {
    return 2000L;
  }

  /**
   * By default, an event source will reconnect when the response stream ended, but this method allows configuring whether
   * reconnecting or not.
   * 
   * @return whether the source should reconnect on end of response. {@code true} by default.
   */
  default boolean shouldRetryOnStreamEnd() {
    return true;
  }

  /**
   * By default, an event source will reconnect when an error happens before receiving the response (for example, when the server
   * is not available). This method allows configuring whether reconnecting or not.
   * 
   * @param error the exception seen instead of a response. Notice that it is not a successful error response (like a 404 or a
   *              503), but an exception.
   * @return whether the source should reconnect on error. {@code true} by default.
   */
  default boolean shouldRetryOnError(Throwable error) {
    return true;
  }

  /**
   * Determines if a reconnection is needed after a response with the passed statusCode.
   * 
   * @param statusCode the response status code.
   * @return whether a reconnection is needed or not.
   */
  default boolean shouldRetryOnStatus(int statusCode) {
    return true;
  }
}
