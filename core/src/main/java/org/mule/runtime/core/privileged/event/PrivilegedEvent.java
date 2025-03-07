/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import static java.util.Collections.emptyMap;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.internal.event.InternalEvent;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.MDC;

/**
 * Allows access to the privileged behavior of the {@link Event} implementation.
 *
 * @since 4.0
 */
@NoImplement
public interface PrivilegedEvent extends CoreEvent {

  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  class CurrentEventHolder {

    private static final ThreadLocal<PrivilegedEvent> currentEvent = new ThreadLocal<>();
  }

  /**
   * @return the context applicable to all events created from the same root {@link CoreEvent} from a {@link MessageSource}.
   */
  @Override
  BaseEventContext getContext();

  /**
   * @return the correlation ID to use for this event.
   * @deprecated TODO MULE-10706 Mule 4: remove this
   */
  @Deprecated
  String getLegacyCorrelationId();

  /**
   * Return the event associated with the currently executing thread.
   *
   * @return event for currently executing thread.
   */
  static PrivilegedEvent getCurrentEvent() {
    return CurrentEventHolder.currentEvent.get();
  }

  /**
   * Set the event to be associated with the currently executing thread.
   *
   * @param event event for currently executing thread.
   */
  static void setCurrentEvent(PrivilegedEvent event) {
    CurrentEventHolder.currentEvent.set(event);

    if (event == null) {
      MDC.remove(CORRELATION_ID_MDC_KEY);
    } else {
      event.getLoggingVariables().ifPresent(variables -> variables.forEach(MDC::put));
      MDC.put(CORRELATION_ID_MDC_KEY, event.getCorrelationId());
    }
  }

  /**
   * Gets the logging variables from the event. These variables are going to be added to the log4j MDC when
   * {@link PrivilegedEvent#setCurrentEvent} is called with this event.
   *
   * @return A dictionary with the logging variables.
   */
  Optional<Map<String, String>> getLoggingVariables();

  /**
   * Indicates if notifications should be fired when processing this message.
   *
   * @return true if notifications are enabled, false otherwise
   */
  boolean isNotificationsEnabled();

  /**
   * Create new {@link Builder}.
   *
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context) {
    return new DefaultEventBuilder((BaseEventContext) context);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance. The existing {@link EventContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(CoreEvent event) {
    return builder(event, false);
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance. The existing {@link EventContext} is conserved.
   *
   * @param event       existing event to use as a template to create builder instance
   * @param shallowCopy do not copy internals of the based component.
   *
   * @return new builder instance.
   */
  static Builder builder(CoreEvent event, boolean shallowCopy) {
    DefaultEventBuilder defaultEventBuilder = new DefaultEventBuilder((InternalEvent) event);

    if (shallowCopy) {
      defaultEventBuilder.internalParameters(emptyMap());
    }

    return defaultEventBuilder;
  }

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance and and {@link EventContext}. A new
   * {@link EventContext} is used instead of the existing instance referenced by the existing {@link CoreEvent}. This builder
   * should only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same {@link CoreEvent}
   * needs a new context.
   *
   * @param event   existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context, CoreEvent event) {
    return new DefaultEventBuilder((BaseEventContext) context, (InternalEvent) event);
  }

  @NoImplement
  public interface Builder extends CoreEvent.Builder {

    /**
     * Set correlationId overriding the correlationId from {@link EventContext#getCorrelationId()} that came from the source
     * system or that was configured in the connector source. This is only used to support transports and should not be used
     * otherwise.
     *
     * @param correlationId to override existing correlationId
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder correlationId(String correlationId);

    /**
     * /** Disables the firing of notifications when processing the produced event.
     *
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    Builder disableNotifications();

    /**
     * Build a new {@link PrivilegedEvent} based on the state configured in the {@link Builder}.
     *
     * @return new {@link PrivilegedEvent} instance.
     */
    @Override
    PrivilegedEvent build();

    @Override
    Builder message(Message message);

    @Override
    Builder message(Function<EventContext, Message> messageFactory);

    @Override
    Builder variables(Map<String, ?> variables);

    /**
     * Similar to {@link #variables(Map)}, but with optimizations when the variables map is obtained directly from another event
     * to overwrite the variables of the target event.
     *
     * @param variables variables to be set.
     * @return the builder instance
     */
    Builder variablesTyped(Map<String, TypedValue<?>> variables);

    @Override
    Builder addVariable(String key, Object value);

    @Override
    Builder addVariable(String key, Object value, DataType mediaType);

    @Override
    Builder removeVariable(String key);

    @Override
    Builder clearVariables();

    /**
     * Adds a logging variable to the event. See also {@link PrivilegedEvent#getLoggingVariables()}.
     *
     * @param key   The variable name.
     * @param value The variable value.
     * @return This builder.
     */
    Builder addLoggingVariable(String key, String value);

    /**
     * Removes a logging variable from the event. See also {@link PrivilegedEvent#getLoggingVariables()}.
     *
     * @param key The variable name.
     * @return This builder.
     */
    Builder removeLoggingVariable(String key);

    /**
     * Removes all logging variables from the event. See also {@link PrivilegedEvent#getLoggingVariables()}.
     *
     * @return This builder.
     */
    Builder clearLoggingVariables();

    @Override
    @Deprecated
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    @Override
    Builder error(org.mule.runtime.api.message.Error error);

    @Override
    Builder securityContext(SecurityContext securityContext);
  }
}
