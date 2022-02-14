/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http2.api.message;

public enum Http2Method {

  GET("GET"),

  POST("POST"),

  PUT("PUT");

  // TODO: Add the others...

  private final String asString;

  Http2Method(String asString) {
    this.asString = asString;
  }

  @Override
  public String toString() {
    return asString;
  }

  public static Http2Method fromText(CharSequence asText) {
    for (Http2Method method : values()) {
      if (method.toString().equalsIgnoreCase(asText.toString())) {
        return method;
      }
    }
    return null;
  }
}
