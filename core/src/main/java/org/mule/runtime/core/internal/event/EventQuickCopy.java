/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.util.collection.SmallMap.copy;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Utilities for creating new events without copying all of its the internal state.
 *
 * @since 4.1.4
 */
public final class EventQuickCopy {

  private EventQuickCopy() {
    // Nothing to do
  }

  /**
   * Creates a new {@link CoreEvent} based on an existing {@link CoreEvent} instance and a {@link EventContext}.
   * <p>
   * A new {@link EventContext} is used instead of the existing instance referenced by the existing {@link CoreEvent}. This method
   * should only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same {@link CoreEvent}
   * needs a new context.
   *
   * @param event   existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new {@link CoreEvent} instance.
   */
  public static CoreEvent quickCopy(EventContext context, CoreEvent event) {
    if (event instanceof EventQuickCopyContextDecorator
        && ((EventQuickCopyContextDecorator) event).getEvent().getContext() == context) {
      return ((EventQuickCopyContextDecorator) event).getEvent();
    } else {
      return (event instanceof InternalEvent && context instanceof BaseEventContext)
          ? new EventQuickCopyContextDecorator((BaseEventContext) context, (InternalEvent) event)
          : CoreEvent.builder(context, event).build();
    }
  }

  /**
   * Creates a new {@link PrivilegedEvent} based on an existing {@link PrivilegedEvent} instance and a {@link EventContext}.
   * <p>
   * A new {@link EventContext} is used instead of the existing instance referenced by the existing {@link PrivilegedEvent}. This
   * method should only be used in some specific scenarios like {@code flow-ref} where a new Flow executing the same
   * {@link PrivilegedEvent} needs a new context.
   *
   * @param event   existing event to use as a template to create builder instance
   * @param context the context to create event instance with.
   * @return new {@link PrivilegedEvent} instance.
   */
  public static PrivilegedEvent quickCopy(EventContext context, PrivilegedEvent event) {
    if (event instanceof EventQuickCopyContextDecorator
        && ((EventQuickCopyContextDecorator) event).getEvent().getContext() == context) {
      return ((EventQuickCopyContextDecorator) event).getEvent();
    } else {
      return (event instanceof InternalEvent && context instanceof BaseEventContext)
          ? new EventQuickCopyContextDecorator((BaseEventContext) context, (InternalEvent) event)
          : PrivilegedEvent.builder(context, event).build();
    }
  }

  public static CoreEvent quickCopy(Error error, CoreEvent event) {
    if (event instanceof InternalEvent) {
      return new EventQuickCopyErrorDecorator(error, (InternalEvent) event);
    } else {
      return CoreEvent.builder(event).error(error).build();
    }
  }

  /**
   * Creates a new {@link CoreEvent} based on an existing {@link CoreEvent} instance and a {@link Map} of
   * {@link InternalEvent#getInternalParameters()}.
   * <p>
   * This is functionally the same as building a new {@link CoreEvent} setting its {@link InternalEvent#getInternalParameters()},
   * but avoids copying the whole event.
   *
   * @return new {@link CoreEvent} instance.
   */
  public static InternalEvent quickCopy(CoreEvent event, Map<String, Object> internalParameters) {
    if (event instanceof EventQuickCopyInternalParametersDecorator) {
      final EventQuickCopyInternalParametersDecorator quickCopy = (EventQuickCopyInternalParametersDecorator) event;

      final Map<String, Object> resolvedParams = (Map<String, Object>) copy(quickCopy.internalParameters);
      resolvedParams.putAll(internalParameters);

      return quickCopy(quickCopy.getEvent(), resolvedParams);
    } else {
      return (event instanceof InternalEvent)
          ? new EventQuickCopyInternalParametersDecorator((InternalEvent) event, internalParameters)
          : InternalEvent.builder(event).internalParameters(internalParameters).build();
    }
  }

  private static class EventQuickCopyContextDecorator extends BaseEventDecorator {

    private static final long serialVersionUID = -2674520914985642327L;

    private final BaseEventContext context;

    public EventQuickCopyContextDecorator(BaseEventContext context, InternalEvent event) {
      super(event);
      this.context = context;
    }

    @Override
    public BaseEventContext getContext() {
      return context;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return context.getFlowCallStack();
    }

    @Override
    public String getCorrelationId() {
      return getLegacyCorrelationId() != null ? getLegacyCorrelationId() : getContext().getCorrelationId();
    }
  }

  private static class EventQuickCopyErrorDecorator extends BaseEventDecorator {

    private static final long serialVersionUID = 7605973213141261979L;

    private final Optional<Error> error;

    public EventQuickCopyErrorDecorator(Error error, InternalEvent event) {
      super(event);
      this.error = Optional.of(error);
    }

    @Override
    public Optional<Error> getError() {
      return error;
    }
  }

  private static class EventQuickCopyInternalParametersDecorator extends BaseEventDecorator {

    private static final long serialVersionUID = -8748877786435182694L;

    private final Map<String, ?> internalParameters;

    public EventQuickCopyInternalParametersDecorator(InternalEvent event, Map<String, Object> internalParameters) {
      super(event);
      this.internalParameters = internalParameters;
    }

    @Override
    public Map<String, ?> getInternalParameters() {
      final Map<String, Object> eventInternalParameters = (Map<String, Object>) getEvent().getInternalParameters();
      if (eventInternalParameters.isEmpty()) {
        return internalParameters;
      }

      final Map<String, Object> resolvedParams = copy(eventInternalParameters);
      resolvedParams.putAll(internalParameters);
      return resolvedParams;
    }

    @Override
    public <T> T getInternalParameter(String key) {
      final Object outerValue = internalParameters.get(key);

      return outerValue != null
          ? (T) outerValue
          : getEvent().getInternalParameter(key);
    }

  }
}
