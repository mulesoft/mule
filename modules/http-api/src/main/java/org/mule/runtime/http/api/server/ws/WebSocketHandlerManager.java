/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.api.annotation.NoImplement;

/**
 * Object to manage a {@link WebSocketHandler} for a server.
 *
 * @since 4.2.0
 */
@NoImplement
public interface WebSocketHandlerManager {

  /**
   * Allows access to the handler.
   */

  void start();

  /**
   * Temporarily stops the handler from being accessed, resulting in a 503 status code return by the server.
   */
  void stop();

  /**
   * Removes the handler from the server.
   */
  void dispose();

}
