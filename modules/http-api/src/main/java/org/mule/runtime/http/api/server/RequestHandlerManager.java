/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import org.mule.api.annotation.NoImplement;

/**
 * Object to manage a {@link RequestHandler} for a server.
 *
 * @since 4.0
 */
@NoImplement
public interface RequestHandlerManager {

  /**
   * Temporarily stops the handler from being accessed, resulting in a 503 status code return by the server.
   */
  void stop();

  /**
   * Allows access to the handler.
   */
  void start();

  /**
   * Removes the handler from the server.
   */
  void dispose();

}
