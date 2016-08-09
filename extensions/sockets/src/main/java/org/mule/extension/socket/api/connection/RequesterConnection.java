/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection;

import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.client.SocketClient;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 * Provides the capability of obtaining a {@link SocketClient} which is aware of the underlying connection and can
 * {@link SocketClient#write(Object)} and {@link SocketClient#read()} to and from the host to which it is connected.
 *
 * The {@link SocketClient} enables the {@link SocketOperations#send(RequesterConnection, Object, String, String, MuleMessage)}
 * operation.
 *
 * @since 4.0
 */
@Alias("request-connection")
public interface RequesterConnection extends SocketConnection {

  /**
   * Returns a new instance of {@link SocketClient} particular to this connection.
   * 
   * @return a {@link SocketClient} that can {@link SocketClient#read()} and {@link SocketClient#write(Object)} through the
   *         socket.
   */
  SocketClient getClient();
}
