/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.ws;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.http.api.ws.WebSocket;
import org.mule.runtime.http.api.ws.WebSocketCloseCode;

import java.io.InputStream;

/**
 * A Callback to receive outbound {@link WebSocket} events.
 * <p>
 * Implementations <b>MUST</b> be thread safe
 *
 * @since 4.2.0
 */
public interface WebSocketCallback {

  /**
   * Invoked when the {@code webSocket} has been established
   *
   * @param webSocket the created {@link WebSocket}
   */
  void onConnect(WebSocket webSocket);

  /**
   * Invoked then the given {@code webSocket} is closed. This method will be invoked whether the socket was
   * closed locally or remotely.
   *
   * @param webSocket the closed {@link WebSocket}
   * @param code      the close code used
   * @param reason    the reason provided
   */
  void onClose(WebSocket webSocket, WebSocketCloseCode code, String reason);

  /**
   * Invoked when the given {@code webSocket} receives data.
   *
   * @param webSocket the {@link WebSocket} that received the data
   * @param content   the received content
   */
  void onMessage(WebSocket webSocket, TypedValue<InputStream> content);

}
