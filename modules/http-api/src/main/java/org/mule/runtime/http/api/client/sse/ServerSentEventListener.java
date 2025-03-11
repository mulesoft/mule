/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.sse;

import org.mule.runtime.http.api.domain.sse.ServerSentEvent;

/**
 * An observer of server-sent events.
 */
public interface ServerSentEventListener {

  // TODO: Move to another class, this one should only handle events.
  default void onOpen() {}

  // TODO: Move to another class, this one should only handle events.
  default void onClose() {}

  // TODO: Move to another class, this one should only handle events.
  default void onError(Throwable error) {}

  /**
   * Method to be invoked for each received {@link ServerSentEvent}.
   * 
   * @param event the received event.
   */
  default void onEvent(ServerSentEvent event) {}
}
