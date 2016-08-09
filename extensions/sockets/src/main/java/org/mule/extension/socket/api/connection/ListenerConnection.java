/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection;

import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.source.SocketListener;
import org.mule.extension.socket.api.worker.SocketWorker;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.extension.api.runtime.MessageHandler;

import java.io.IOException;
import java.io.InputStream;

import javax.resource.spi.work.Work;

/**
 * This kind of {@link SocketConnection} enables the {@link SocketListener} to await for new incoming connections and wraps each
 * one of those into a {@link SocketWorker} so they can run independently in their own thread.
 *
 * @since 4.0
 */
public interface ListenerConnection extends SocketConnection {

  /**
   * This method blocks until a new connection is received or timeout exception is thrown.
   *
   * @param messageHandler used in the {@link SocketWorker} to deliver the new received messages
   * @return a {@link Work} that will represent a new received connection. The {@link Work} should be scheduled with a
   *         {@link WorkManager} in it's own thread.
   * @throws IOException if the connection was suddenly closed
   * @throws ConnectionException if the connection was closed and the cause was the invocation of
   *         {@link SocketConnection#disconnect()}
   */
  SocketWorker listen(MessageHandler<InputStream, SocketAttributes> messageHandler) throws IOException, ConnectionException;
}
