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

public class ServerSentEvent implements Serializable {

  @Serial
  private static final long serialVersionUID = -1211505868025654629L;

  private final String eventName;
  private final String eventData;

  public ServerSentEvent(String eventName, String eventData) {
    this.eventName = eventName;
    this.eventData = eventData;
  }

  public String getEventName() {
    return eventName;
  }

  public String getEventData() {
    return eventData;
  }

  @Override
  public String toString() {
    return "ServerSentEvent [eventName=" + eventName + ", eventData=" + eventData + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(eventName) + Objects.hashCode(eventData);
  }

  @Override
  public boolean equals(Object o) {
    if (null == o || getClass() != o.getClass()) {
      return false;
    }
    ServerSentEvent that = (ServerSentEvent) o;
    return Objects.equals(eventName, that.eventName) && Objects.equals(eventData, that.eventData);
  }
}
