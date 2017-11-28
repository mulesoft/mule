/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.EventContextDumpService;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultEventContextDumpService implements EventContextDumpService {

  static Set<DefaultEventContext> currentContexts = newKeySet(512);

  @Override
  public List<FlowStackDumpEntry> getCurrentlyActiveFlowStacks() {
    List<FlowStackDumpEntry> flowStacks = new ArrayList<>();

    for (DefaultEventContext context : currentContexts) {
      flowStacks.add(new DefaultFlowStackDumpEntry(context));
      context.forEachChild(childContext -> flowStacks.add(new DefaultFlowStackDumpEntry(childContext)));
    }

    return flowStacks;
  }

  private static final class DefaultFlowStackDumpEntry implements FlowStackDumpEntry {

    private final String serverId;
    private final String eventId;
    private final FlowCallStack flowCallStack;

    public DefaultFlowStackDumpEntry(BaseEventContext context) {
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
  }
}
