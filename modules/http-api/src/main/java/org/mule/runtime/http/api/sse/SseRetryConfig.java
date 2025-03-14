/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

import org.mule.api.annotation.Experimental;

/**
 * The {@link SseSource} implements the corresponding
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">server-sent-events spec</a>. This interface allows
 * configuring the retry mechanism described there.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @param allowRetryDelayOverride The server may use the retry key in the event to override the retry delay. With this
 *                                configuration, the user can set if the retry mechanism has to accept that parameter or not.
 *                                Default value is {@code true}.
 * @param initialRetryDelayMillis The spec states: "A reconnection time, in milliseconds. This must initially be an
 *                                implementation-defined value, probably in the region of a few seconds." This parameter allows
 *                                configuring that timeout. Default value is {@code 2000L}.
 * @param shouldRetryOnStreamEnd  By default, an event source will reconnect when the response stream ended, but this method
 *                                allows configuring whether reconnecting or not. Default value is {@code true}.
 * @since 4.9.3, 4.10.0
 */
@Experimental
public record SseRetryConfig(boolean allowRetryDelayOverride, long initialRetryDelayMillis, boolean shouldRetryOnStreamEnd) {

  public static final Long DEFAULT_RETRY_DELAY_MILLIS = 2000L;

  public static SseRetryConfig defaultConfig() {
    return new SseRetryConfig(true, DEFAULT_RETRY_DELAY_MILLIS, true);
  }
}
