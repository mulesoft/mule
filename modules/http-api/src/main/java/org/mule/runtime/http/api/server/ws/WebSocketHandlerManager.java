/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;

@NoImplement
@Experimental
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
