/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.message.muletosdk;

import org.mule.sdk.api.http.sse.ServerSentEvent;

import java.util.Optional;

public class ServerSentEventWrapper implements ServerSentEvent {

  private final org.mule.runtime.http.api.sse.ServerSentEvent event;

  public ServerSentEventWrapper(org.mule.runtime.http.api.sse.ServerSentEvent event) {
    this.event = event;
  }

  @Override
  public String getName() {
    return event.getName();
  }

  @Override
  public String getData() {
    return event.getData();
  }

  @Override
  public Optional<String> getId() {
    return event.getId();
  }

  @Override
  public Optional<Long> getRetryDelay() {
    return event.getRetryDelay();
  }
}
