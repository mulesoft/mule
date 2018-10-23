/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

/**
 * The supported protocols
 *
 * @since 4.2.0
 */
public enum WebSocketProtocol {

  /**
   * {@code ws} protocol. Used when the Upgrade request was sent over standard HTTP
   */
  WS("ws"),

  /**
   * {@code wss} protocol. Used when the Upgrade request was sent over standard HTTPS
   */
  WSS("wss");

  private final String scheme;

  /**
   * Returns the {@link WebSocketProtocol} that corresponds to the given {@code scheme}
   *
   * @param scheme a scheme
   * @return a {@link WebSocketProtocol}
   * @throws IllegalArgumentException if the scheme doesn't match any of the items in this enum
   */
  public static WebSocketProtocol forScheme(String scheme) {
    if ("ws".equals(scheme)) {
      return WS;
    } else if ("wss".equals(scheme)) {
      return WSS;
    } else {
      throw new IllegalArgumentException("Invalid scheme: " + scheme);
    }
  }

  WebSocketProtocol(String scheme) {
    this.scheme = scheme;
  }

  public String getScheme() {
    return scheme;
  }
}
