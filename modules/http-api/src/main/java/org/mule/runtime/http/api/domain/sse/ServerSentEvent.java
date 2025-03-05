/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.sse;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Server-sent event.
 */
public class ServerSentEvent implements Serializable {

  @Serial
  private static final long serialVersionUID = -1211505868025654629L;

  private final String eventName;
  private final String eventData;
  private final String id;

  public ServerSentEvent(String eventName, String eventData, String id) {
    this.eventName = eventName;
    this.eventData = eventData;
    this.id = id;
  }

  /**
   * @return the event name, the topic of the event.
   */
  public String getEventName() {
    return eventName;
  }

  /**
   * @return the full data as string. // TODO: Add a method to iterate line-by-line.
   */
  public String getEventData() {
    return eventData;
  }

  /**
   * @return event id.
   */
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return "ServerSentEvent [name=" + eventName + ", data=" + eventData + ", id=" + id + "]";
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
    ServerSentEvent that = (ServerSentEvent) o;
    return Objects.equals(eventName, that.eventName) && Objects.equals(eventData, that.eventData) && Objects.equals(id, that.id);
  }
}
