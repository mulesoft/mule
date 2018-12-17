/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import java.util.HashMap;
import java.util.Map;

/**
 * The codes of the close frame that will be sent (or has been received) when closing a {@link WebSocket}
 *
 * @since 4.2.0
 */
public enum WebSocketCloseCode {

  /**
   * Indicates a normal closure, meaning whatever purpose the connection was established for has been fulfilled.
   */
  NORMAL_CLOSURE(1000),
  /**
   * Indicates that an endpoint is "going away", such as a server going down, or a browser having navigated away from
   * a page.
   */
  ENDPOINT_GOING_DOWN(1001),
  /**
   * Indicates that an endpoint is terminating the connection due to a protocol error.
   */
  PROTOCOL_ERROR(1002),
  /**
   * Indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept
   * (e.g. an endpoint that understands only text data may send this if it receives a binary message.)
   */
  INVALID_DATA(1003),
  /**
   * indicates that an endpoint is terminating the connection because it has received a message that is too large.
   */
  MESSAGE_TOO_LARGE(1004);

  private static final Map<Integer, WebSocketCloseCode> CODES = new HashMap<>(WebSocketCloseCode.values().length);

  static {
    for (WebSocketCloseCode code : WebSocketCloseCode.values()) {
      CODES.put(code.protocolCode, code);
    }
  }

  /**
   * Returns the {@link WebSocketCloseCode} that matches the given raw {@code protocolCode}
   *
   * @param protocolCode a protocol level code
   * @return a {@link WebSocketCloseCode}
   * @throws IllegalArgumentException if the {@code protocolCode} doesn't match any of the items in this enum
   */
  public static WebSocketCloseCode fromProtocolCode(int protocolCode) {
    WebSocketCloseCode code = CODES.get(protocolCode);
    if (code == null) {
      throw new IllegalArgumentException("Invalid protocol code " + protocolCode);
    }

    return code;
  }

  private final int protocolCode;

  WebSocketCloseCode(int protocolCode) {
    this.protocolCode = protocolCode;
  }

  public int getProtocolCode() {
    return protocolCode;
  }

}
