/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.time.OffsetTime.now;

import org.mule.runtime.core.api.MessageExecutionContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;

import java.io.Serializable;
import java.time.OffsetTime;

/**
 * Default immutable implementation of {@link MessageExecutionContext}.
 *
 * @since 4.0
 */
public final class DefaultMessageExecutionContext implements MessageExecutionContext, Serializable {

  private static final long serialVersionUID = -3664490832964509653L;

  /**
   * Builds a new execution context with the given parameters.
   * @param flow the flow that processes events of this context. 
   */
  public static MessageExecutionContext create(FlowConstruct flow) {
    return create(flow, flow.getMuleContext().getUniqueIdString());
  }

  /**
   * Builds a new execution context with the given parameters.
   * @param flow the flow that processes events of this context.
   * @param correlationId See {@link MessageExecutionContext#getCorrelationId()}.
   */
  public static MessageExecutionContext create(FlowConstruct flow, String correlationId) {
    DefaultMessageExecutionContext executionContext =
        new DefaultMessageExecutionContext(flow.getMuleContext().getUniqueIdString(), correlationId);
    executionContext.serverId = flow.getMuleContext().getId();
    executionContext.flowName = flow.getName();
    return executionContext;
  }

  private final String id;
  private final String correlationId;
  private final OffsetTime receivedDate = now();

  private String serverId;
  private String flowName;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getCorrelationId() {
    return correlationId != null ? correlationId : id;
  }

  @Override
  public OffsetTime getReceivedTime() {
    return receivedDate;
  }

  @Override
  public String getFlowName() {
    return flowName;
  }

  @Override
  public String getServerId() {
    return serverId;
  }

  /**
   * Builds a new execution context with the given parameters.
   * 
   * @param id the unique id that identifies all {@link MuleEvent}s of the same context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link MuleEvent} of
   *        this context, if available.
   */
  private DefaultMessageExecutionContext(String id, String correlationId) {
    this.id = id;
    this.correlationId = correlationId;
  }

  @Override
  public String toString() {
    return "DefaultMessageExecutionContext { id: " + id + "; correlationId: " + correlationId + "; flowName: " + flowName
        + "; serverId: " + serverId + " }";
  }
}
