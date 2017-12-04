/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.lang.System.lineSeparator;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.slf4j.Logger;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link EventContextService} that keeps a reference to all active {@link DefaultEventContext}s in the Mule
 * Runtime.
 *
 * @since 4.1
 */
public class DefaultEventContextService implements EventContextService {

  private static Logger LOGGER = getLogger(DefaultEventContextService.class);

  private final ReferenceQueue<DefaultEventContext> queue = new ReferenceQueue<>();
  private Set<WeakReference<DefaultEventContext>> currentContexts = newKeySet(512);

  @Override
  public List<FlowStackEntry> getCurrentlyActiveFlowStacks() {
    List<FlowStackEntry> flowStacks = new ArrayList<>();

    Set<WeakReference<DefaultEventContext>> gcdContexts = new HashSet<>();

    for (WeakReference<DefaultEventContext> contextRef : currentContexts) {
      DefaultEventContext context = contextRef.get();

      if (context == null) {
        gcdContexts.add(contextRef);
      } else {
        flowStacks.add(new DefaultFlowStackEntry(context));
        context.forEachChild(childContext -> flowStacks.add(new DefaultFlowStackEntry(childContext)));
      }
    }

    currentContexts.removeAll(gcdContexts);

    return flowStacks;
  }

  public void addContext(DefaultEventContext context) {
    currentContexts.add(new WeakReference<>(context, queue));
  }

  public void removeContext(DefaultEventContext context) {
    // MULE-14151 This is a temporary workaround until all possible causes of the logged warning are fixed.
    Reference<? extends DefaultEventContext> polled = queue.poll();
    while (polled != null) {
      LOGGER.warn("EventContext with id {} was not terminated.", polled.get().getId());
      currentContexts.remove(polled);

      polled = queue.poll();
    }
  }

  private static final class DefaultFlowStackEntry implements FlowStackEntry {

    private final String serverId;
    private final String eventId;
    private final FlowCallStack flowCallStack;

    public DefaultFlowStackEntry(BaseEventContext context) {
      this.serverId = context.getServerId();
      this.eventId = context.getId();
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
      return "eventId: " + eventId + ";" + lineSeparator() + getFlowCallStack().toString();
    }
  }
}
