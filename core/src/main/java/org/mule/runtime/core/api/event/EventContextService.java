/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;

import java.util.List;

/**
 * Provides methods to query the internal state of event processing in the Mule Runtime.
 *
 * @since 4.1
 */
public interface EventContextService {

  public static final String REGISTRY_KEY = "_muleEventContextService";

  /**
   * The returned list will contain an element for each currently active {@link EventContext}.
   * <p>
   * An {@link EventContext} is considered active after it has been constructed and before its termination.
   *
   * @return the {@link FlowStackEntry}s for all the {@link Event}s that are currently being in process.
   */
  List<FlowStackEntry> getCurrentlyActiveFlowStacks();

  /**
   * Contains a {@link FlowCallStack} and context information about its owner.
   */
  public interface FlowStackEntry {

    /**
     * @return the serverId of the artifact (containing the name) that created the event for the {@link FlowCallStack}.
     */
    String getServerId();

    /**
     * @return the id of the event the {@link FlowCallStack} belongs to.
     */
    String getEventId();

    /**
     * @return the {@link FlowCallStack} of a single event.
     */
    FlowCallStack getFlowCallStack();
  }
}
