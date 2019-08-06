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

import java.util.Optional;

public abstract class AbstractWebSocketRuntimeException extends MuleRuntimeException {

  protected final String webSocketId;
  protected transient WebSocket webSocket;

  public AbstractWebSocketRuntimeException(String message, WebSocket webSocket) {
    super(createStaticMessage(message));
    this.webSocketId = webSocket.getId();
    this.webSocket = webSocket;
  }

  public final Optional<WebSocket> getWebSocket() {
    return ofNullable(webSocket);
  }

  public final String getWebSocketId() {
    return webSocketId;
  }

}
