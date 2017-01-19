/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.time.OffsetTime.now;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.context.notification.DefaultProcessorsTrace;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Subscriber;

import reactor.core.publisher.MonoProcessor;

/**
 * Default immutable implementation of {@link EventContext}.
 *
 * @since 4.0
 */
public final class DefaultEventContext implements EventContext, Serializable {

  private static final long serialVersionUID = -3664490832964509653L;

  private transient MonoProcessor<Event> monoProcessor = MonoProcessor.create();

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param connectorName the name of the connector that received the first message for this context.
   */
  public static EventContext create(FlowConstruct flow, String connectorName) {
    return create(flow, connectorName, null);
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param connectorName the name of the connector that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   */
  public static EventContext create(FlowConstruct flow, String connectorName, String correlationId) {
    return new DefaultEventContext(flow, connectorName, correlationId);
  }

  private final String id;
  private final String correlationId;
  private final OffsetTime receivedDate = now();

  private final String serverId;
  private final String flowName;
  private final String connectorName;

  private final ProcessingTime processingTime;

  private final ProcessorsTrace processorsTrace = new DefaultProcessorsTrace();

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
  public String getOriginatingFlowName() {
    return flowName;
  }

  @Override
  public String getOriginatingConnectorName() {
    return connectorName;
  }

  @Override
  public ProcessingTime getProcessingTime() {
    return processingTime;
  }

  @Override
  public boolean isCorrelationIdFromSource() {
    return correlationId != null;
  }

  @Override
  public ProcessorsTrace getProcessorsTrace() {
    return processorsTrace;
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param connectorName the name of the connector that received the first message for this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link Event} of this
   *        context, if available.
   */
  private DefaultEventContext(FlowConstruct flow, String connectorName, String correlationId) {
    this.id = flow.getUniqueIdString();
    this.serverId = flow.getServerId();
    this.flowName = flow.getName();
    this.connectorName = connectorName;
    this.processingTime = ProcessingTime.newInstance(flow);
    this.correlationId = correlationId;
  }

  @Override
  public void success() {
    monoProcessor.onComplete();
  }

  @Override
  public void success(Event event) {
    monoProcessor.onNext(event);
  }

  @Override
  public void error(MessagingException messagingException) {
    monoProcessor.onError(messagingException);
  }

  @Override
  public String toString() {
    return "DefaultMessageExecutionContext { id: " + id + "; correlationId: " + correlationId + "; flowName: " + flowName
        + "; serverId: " + serverId + " }";
  }

  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    monoProcessor = MonoProcessor.create();
  }

  @Override
  public void subscribe(Subscriber<? super Event> s) {
    monoProcessor.subscribe(s);
  }
}
