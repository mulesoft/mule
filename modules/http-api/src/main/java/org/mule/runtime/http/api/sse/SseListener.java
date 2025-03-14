/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

/**
 * An observer of server-sent events.
 */
public interface SseListener {

  /**
   * Method to be invoked for each received {@link ServerSentEvent}.
   * 
   * @param event the received event.
   */
  void onEvent(ServerSentEvent event);

  /**
   * Method to be invoked when the full event stream was consumed.
   */
  default void onClose() {}
}
