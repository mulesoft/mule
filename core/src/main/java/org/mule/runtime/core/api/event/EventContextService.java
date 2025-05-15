/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;

import java.time.Duration;
import java.util.List;

/**
 * Provides methods to query the internal state of event processing in the Mule Runtime.
 *
 * @since 4.1
 */
@NoImplement
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
   * The state of the event context.
   *
   * @since 4.10
   */
  public enum EventContextState {
    /**
     * Event is being executed by the flow or executable component, or finished but the response is still being processed.
     */
    EXECUTING,
    /**
     * Event execution complete and the response was already handled.
     */
    RESPONSE_PROCESSED,
    /**
     * Same as {@link #RESPONSE_PROCESSED}, but all of the child events are {@link #RESPONSE_PROCESSED} as well.
     */
    COMPLETE,
    /**
     * After {@link #COMPLETE}, and all completion callbacks of the context were executed.
     */
    TERMINATED
  }

  /**
   * Contains a {@link FlowCallStack} and context information about its owner.
   */
  @NoImplement
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
     * @return the state of this entry.
     */
    EventContextState getState();

    /**
     * @return the time that this execution has been executing.
     */
    Duration getExecutingTime();

    /**
     * @return the {@link FlowCallStack} of a single event.
     */
    FlowCallStack getFlowCallStack();
  }
}
