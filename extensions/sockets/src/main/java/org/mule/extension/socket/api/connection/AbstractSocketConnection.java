/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection;

import org.mule.extension.socket.api.ConnectionSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains common configuration parameters to all the kinds of {@link SocketConnection}
 *
 * @since 4.0
 */
public abstract class AbstractSocketConnection implements SocketConnection {

  protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
  protected final ConnectionSettings connectionSettings;
  protected boolean wasDisconnected = false;

  protected AbstractSocketConnection(ConnectionSettings connectionSettings) {
    this.connectionSettings = connectionSettings;
  }

  @Override
  public final void disconnect() {
    LOGGER.debug("Closing socket");
    doDisconnect();
    wasDisconnected = true;
    LOGGER.debug("Socket was closed");
  }

  /**
   * Template method that does the concrete disconnection for the socket
   */
  protected abstract void doDisconnect();
}
