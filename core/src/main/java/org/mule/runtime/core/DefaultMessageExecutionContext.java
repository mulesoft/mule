/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.time.OffsetTime.now;

import org.mule.runtime.core.api.CoreMessageExecutionContext;
import org.mule.runtime.core.api.MessageExecutionContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.io.Serializable;
import java.time.OffsetTime;

/**
 * Default immutable implementation of {@link MessageExecutionContext}.
 *
 * @since 4.0
 */
public final class DefaultMessageExecutionContext implements CoreMessageExecutionContext, Serializable {

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
    return new DefaultMessageExecutionContext(flow, correlationId);
  }

  private final String id;
  private final String correlationId;
  private final OffsetTime receivedDate = now();

  private final String serverId;
  private final String flowName;

  private final ProcessingTime processingTime;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
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
  public ProcessingTime getProcessingTime() {
    return processingTime;
  }

  @Override
  public String getServerId() {
    return serverId;
  }

  /**
   * Builds a new execution context with the given parameters.
   * 
   * @param flow the flow that processes events of this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link MuleEvent} of this
   *        context, if available.
   */
  private DefaultMessageExecutionContext(FlowConstruct flow, String correlationId) {
    this.id = flow.getMuleContext().getUniqueIdString();
    this.serverId = flow.getMuleContext().getId();
    this.flowName = flow.getName();
    this.processingTime = ProcessingTime.newInstance(flow);
    this.correlationId = correlationId;
  }

  @Override
  public String toString() {
    return "DefaultMessageExecutionContext { id: " + id + "; correlationId: " + correlationId + "; flowName: " + flowName
        + "; serverId: " + serverId + " }";
  }
}
