/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws;

import org.mule.api.annotation.Experimental;

@Experimental
public enum WebSocketProtocol {

  WS("ws"), WSS("wss");

  private final String scheme;

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
