/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.core.api.event.EventContextService.EventContextState.COMPLETE;
import static org.mule.runtime.core.api.event.EventContextService.EventContextState.EXECUTING;
import static org.mule.runtime.core.api.event.EventContextService.EventContextState.RESPONSE_PROCESSED;
import static org.mule.runtime.core.api.event.EventContextService.EventContextState.TERMINATED;

import static java.lang.System.lineSeparator;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link EventContextService} that keeps a reference to all active {@link DefaultEventContext}s in the Mule
 * Runtime.
 *
 * @since 4.1
 */
public class DefaultEventContextService implements EventContextService {

  private final Set<DefaultEventContext> currentContexts = newKeySet(512);

  @Override
  public List<FlowStackEntry> getCurrentlyActiveFlowStacks() {
    var now = now();
    List<FlowStackEntry> flowStacks = new ArrayList<>();

    for (DefaultEventContext context : currentContexts) {
      flowStacks.add(new DefaultFlowStackEntry(context, now));
      context.forEachChild(childContext -> flowStacks.add(new DefaultFlowStackEntry(childContext, now)));
    }

    return flowStacks;
  }

  public void addContext(DefaultEventContext context) {
    currentContexts.add(context);
  }

  public void removeContext(DefaultEventContext context) {
    currentContexts.remove(context);
  }

  private static final class DefaultFlowStackEntry implements FlowStackEntry {

    private final String serverId;
    private final String parentEventId;
    private final String eventId;
    private final EventContextState state;
    private final Duration executingTime;
    private final String originatingLocation;
    private final FlowCallStack flowCallStack;

    public DefaultFlowStackEntry(BaseEventContext context, Instant now) {
      this.serverId = context.getServerId();
      this.parentEventId = context.getParentContext().map(BaseEventContext::getId).orElse(null);
      this.eventId = context.getId();

      if (context.isTerminated()) {
        state = TERMINATED;
      } else if (context.isComplete()) {
        state = COMPLETE;
      } else if (context.isResponseDone()) {
        state = RESPONSE_PROCESSED;
      } else {
        state = EXECUTING;
      }

      this.executingTime = between(context.getStartTime(), now);
      this.originatingLocation = context.getOriginatingLocation().getLocation();
      this.flowCallStack = context.getFlowCallStack().clone();
    }

    @Override
    public String getServerId() {
      return serverId;
    }

    @Override
    public String getParentEventId() {
      return parentEventId;
    }

    @Override
    public String getEventId() {
      return eventId;
    }

    @Override
    public EventContextState getState() {
      return state;
    }

    @Override
    public Duration getExecutingTime() {
      return executingTime;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return flowCallStack;
    }

    @Override
    public String toString() {
      return "eventId: " + eventId + " @ " + originatingLocation + ";" + lineSeparator() + getFlowCallStack().toString();
    }
  }
}
