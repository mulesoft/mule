/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.sse;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.OptionalLong.empty;
import static java.util.OptionalLong.of;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Server-sent event.
 */
public class ServerSentEventImpl implements Serializable, org.mule.sdk.api.http.sse.ServerSentEvent {

  @Serial
  private static final long serialVersionUID = -1211505868025654629L;

  private final String eventName;
  private final String eventData;
  private final String id;
  private final Long retryDelay;

  public ServerSentEventImpl(String eventName, String eventData, String id, Long retryDelay) {
    requireNonNull(eventName, "eventName cannot be null");
    requireNonNull(eventData, "eventData cannot be null");

    this.eventName = eventName;
    this.eventData = eventData;
    this.id = id;
    this.retryDelay = retryDelay;
  }

  @Override
  public String getEventName() {
    return eventName;
  }

  @Override
  public String getEventData() {
    return eventData;
  }

  @Override
  public Optional<String> getId() {
    return ofNullable(id);
  }

  @Override
  public OptionalLong getRetryDelay() {
    return null != retryDelay ? of(retryDelay) : empty();
  }

  @Override
  public String toString() {
    return "ServerSentEvent [name=" + eventName + ", data=" + eventData + ", id=" + id + ", retryDelay=" + retryDelay + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(eventName) + Objects.hashCode(eventData) + Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object o) {
    if (null == o || getClass() != o.getClass()) {
      return false;
    }
    ServerSentEventImpl that = (ServerSentEventImpl) o;
    return Objects.equals(eventName, that.eventName) && Objects.equals(eventData, that.eventData) && Objects.equals(id, that.id)
        && Objects.equals(retryDelay, that.retryDelay);
  }
}
