/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;

/**
 * Helper class that creates an {@link CoreEvent} maintaining variables from different scopes.
 *
 * @since 4.0
 */
public class PolicyEventConverter {

  /**
   * Creates a new {@link CoreEvent} based on a message and another event which is used to get the variables and
   * {@link BaseEventContext}
   *
   * @param event provider of the message and session parts of the event
   * @param originalEvent provider of the variables part of the event
   * @return the created event
   */
  public PrivilegedEvent createEvent(PrivilegedEvent event, PrivilegedEvent originalEvent) {
    return createEvent(event, originalEvent, builder -> {
    }, true);
  }

  /**
   * Creates a new {@link CoreEvent} based on a message and another event which is used to get the variables,
   * {@link BaseEventContext} and, if specified, the {@link Message}
   *
   * @param event provider of the message and session parts of the event
   * @param originalEvent provider of the variables and message of the event
   * @param keepMessage if true, the current event message is used as opposed to the original event message
   * @return the created event
   */
  public PrivilegedEvent createEvent(PrivilegedEvent event, PrivilegedEvent originalEvent, boolean keepMessage) {
    return createEvent(event, originalEvent, builder -> {
    }, keepMessage);
  }

  /**
   * Creates a new {@link CoreEvent} based on a message and another event which is used to get the variables and
   * {@link BaseEventContext}
   *
   * @param event provider of the message and session parts of the event
   * @param originalEvent provider of the variables part of the event
   * @param eventConfigurer additional work to be done on the eventBuilder before the new event is built
   * @return the created event
   */
  public PrivilegedEvent createEvent(PrivilegedEvent event, PrivilegedEvent originalEvent,
                                     Consumer<PrivilegedEvent.Builder> eventConfigurer) {
    return createEvent(event, originalEvent, eventConfigurer, true);
  }

  /**
   * Creates a new {@link CoreEvent} based on a message and another event which is used to get the variables,
   * {@link BaseEventContext} and, if specified, the {@link Message}
   *
   * @param event provider of the message and session parts of the event
   * @param originalEvent provider of the variables and message of the event
   * @param eventConfigurer additional work to be done on the eventBuilder before the new event is built
   * @param keepMessage if true, the current event message is used as opposed to the original event message
   * @return the created event
   */
  public PrivilegedEvent createEvent(PrivilegedEvent event, PrivilegedEvent originalEvent,
                                     Consumer<PrivilegedEvent.Builder> eventConfigurer, boolean keepMessage) {
    PrivilegedEvent.Builder eventBuilder = PrivilegedEvent
        .builder(event)
        .variablesTyped(originalEvent.getVariables());

    if (!keepMessage) {
      eventBuilder.message(originalEvent.getMessage());
    }

    eventConfigurer.accept(eventBuilder);

    return eventBuilder.build();
  }

  public PrivilegedEvent restoreVariables(PrivilegedEvent event, Map<String, TypedValue<?>> variables) {
    PrivilegedEvent.Builder eventBuilder = PrivilegedEvent
        .builder(event)
        .variablesTyped(variables != null ? variables : emptyMap());

    return eventBuilder.build();
  }

  public PrivilegedEvent restoreVariables(PrivilegedEvent event, Map<String, TypedValue<?>> variables,
                                          Consumer<PrivilegedEvent.Builder> eventConfigurer) {
    PrivilegedEvent.Builder eventBuilder = PrivilegedEvent
        .builder(event)
        .variablesTyped(variables != null ? variables : emptyMap());

    eventConfigurer.accept(eventBuilder);

    return eventBuilder.build();
  }
}
