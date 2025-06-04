/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.runtime.http.api.sse.server.SseResponseCustomizer;

public class SseResponseCustomizerWrapper implements org.mule.sdk.api.http.sse.server.SseResponseCustomizer {

  private final SseResponseCustomizer customizer;

  public SseResponseCustomizerWrapper(SseResponseCustomizer customizer) {
    this.customizer = customizer;
  }

  @Override
  public org.mule.sdk.api.http.sse.server.SseResponseCustomizer addResponseHeader(String name, String value) {
    customizer.addResponseHeader(name, value);
    return this;
  }
}
