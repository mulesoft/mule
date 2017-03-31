/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;

/**
 * Helper class that creates an {@link Event} maintaining variables from different scopes.
 * 
 * @since 4.0
 */
public class PolicyEventConverter {

  /**
   * Creates a new {@link Event} based on a message and another event which is used to get the variables
   * and {@link org.mule.runtime.core.api.EventContext}
   *
   * @param message message part of the event
   * @param variablesProviderEvent provider of the variables part of the event
   * @return
   */
  public Event createEvent(Message message, Event variablesProviderEvent) {
    Event.Builder eventBuilder = Event.builder(variablesProviderEvent.getContext()).message(message);
    for (String variableName : variablesProviderEvent.getVariableNames()) {
      eventBuilder.addVariable(variableName, variablesProviderEvent.getVariable(variableName));
    }
    return eventBuilder.build();
  }

}
