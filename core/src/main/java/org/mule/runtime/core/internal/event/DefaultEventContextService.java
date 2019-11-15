/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.lang.System.lineSeparator;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.privileged.event.BaseEventContext;

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
    List<FlowStackEntry> flowStacks = new ArrayList<>();

    for (DefaultEventContext context : currentContexts) {
      flowStacks.add(new DefaultFlowStackEntry(context));
      context.forEachChild(childContext -> flowStacks.add(new DefaultFlowStackEntry(childContext)));
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
    private final String eventId;
    private final String originatingLocation;
    private final FlowCallStack flowCallStack;

    public DefaultFlowStackEntry(BaseEventContext context) {
      this.serverId = context.getServerId();
      this.eventId = context.getId();
      this.originatingLocation = context.getOriginatingLocation().getLocation();
      this.flowCallStack = context.getFlowCallStack().clone();
    }

    @Override
    public String getServerId() {
      return serverId;
    }

    @Override
    public String getEventId() {
      return eventId;
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
