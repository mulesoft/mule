/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.domain;

/**
 * Represents the http message protocol.
 */
public enum HttpProtocol {
  HTTP_0_9("HTTP/0.9"), HTTP_1_0("HTTP/1.0"), HTTP_1_1("HTTP/1.1");

  private final String protocolName;

  HttpProtocol(String protocolName) {
    this.protocolName = protocolName;
  }

  public String asString() {
    return protocolName;
  }
}
