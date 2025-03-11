/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Server-sent event.
 */
public interface ServerSentEvent {

  /**
   * @return the event name, the topic of the event.
   */
  String getEventName();

  /**
   * @return the full data as string.
   */
  String getEventData();

  /**
   * @return event id.
   */
  Optional<String> getId();

  /**
   * The server may set the reconnection delay.
   * 
   * @return the new retry delay, if the server configured it.
   */
  OptionalLong getRetryDelay();
}
