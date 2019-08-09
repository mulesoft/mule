/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws.exception;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.http.api.ws.WebSocket;

import java.io.Serializable;
import java.util.Optional;

/**
 * Base class for {@link RuntimeException} implementations that refer to a specific {@link WebSocket}.
 * <p>
 * Because {@link WebSocket} is not {@link Serializable}, it is not guaranteed that the referred socket will
 * always be available, reason why the {@link #getWebSocket()} method returns an {@link Optional}. It will always be
 * possible however to recover the socket's ID.
 *
 * @since 4.2.2
 */
public abstract class WebSocketRuntimeException extends MuleRuntimeException {

  protected final String webSocketId;
  protected transient WebSocket webSocket;

  /**
   * Creates a new instance
   *
   * @param message   the exception message
   * @param webSocket the referred {@link WebSocket}
   */
  public WebSocketRuntimeException(String message, WebSocket webSocket) {
    this(message, webSocket, null);
  }

  /**
   * @param message   the exception message
   * @param webSocket the referred {@link WebSocket}
   * @param cause     this exception's cause
   */
  public WebSocketRuntimeException(String message, WebSocket webSocket, Throwable cause) {
    super(createStaticMessage(message), cause);
    this.webSocketId = webSocket.getId();
    this.webSocket = webSocket;
  }

  /**
   * Optionally returns the referred {@link WebSocket}. The return value will be empty if {@code this} exception has been
   * deserialized.
   *
   * @return an {@link Optional} {@link WebSocket}
   */
  public final Optional<WebSocket> getWebSocket() {
    return ofNullable(webSocket);
  }

  /**
   * @return The referred socket ID.
   */
  public final String getWebSocketId() {
    return webSocketId;
  }

}
