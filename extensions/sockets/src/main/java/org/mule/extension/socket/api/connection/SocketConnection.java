/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

/**
 * Represents a connection established with a socket
 *
 * @since 4.0
 */
public interface SocketConnection {

  /**
   * Disconnects the connection. Does nothing if the connection was not previously connected. If an error occur while
   * disconnecting, it will be logged with an ERROR level and the disconnection will continue.
   */
  void disconnect();

  /**
   * Establish a connection ready to be used.
   *
   * @throws ConnectionException if the connection couldn't be established
   */
  void connect() throws ConnectionException;

  /**
   * Validates socket's connection
   */
  ConnectionValidationResult validate();
}
