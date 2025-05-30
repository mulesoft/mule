/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.server;

import org.mule.api.annotation.Experimental;

/**
 * Interface intended to customize the SSE initiator response header, allowing to configure only certain parameters.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @since 4.10.0, 4.9.6
 */
@Experimental
public interface SseResponseCustomizer {

  /**
   * Adds a header to the SSE response. Having two headers with the same name is allowed.
   *
   * @param name  header name.
   * @param value header value.
   */
  SseResponseCustomizer addResponseHeader(String name, String value);
}
