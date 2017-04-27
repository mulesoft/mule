/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.lang.System.identityHashCode;
import static java.time.OffsetTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.context.notification.DefaultProcessorsTrace;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.io.Serializable;
import java.time.OffsetTime;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Default immutable implementation of {@link EventContext}.
 *
 * @since 4.0
 */
public final class DefaultEventContext extends AbstractEventContext implements Serializable {

  private static final long serialVersionUID = -3664490832964509653L;

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   */
  public static EventContext create(FlowConstruct flow, ComponentLocation location) {
    return create(flow, location, null);
  }

  /**
   * Builds a new execution context with the given parameters and an empty publisher.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   */
  public static EventContext create(FlowConstruct flow, ComponentLocation location, String correlationId) {
    return create(flow, location, correlationId, Mono.empty());
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   * @param externalCompletionPublisher void publisher that completes when source completes enabling completion of
   *        {@link EventContext} to depend on completion of source.
   */
  public static EventContext create(FlowConstruct flow, ComponentLocation location, String correlationId,
                                    Publisher<Void> externalCompletionPublisher) {
    return new DefaultEventContext(flow, location, correlationId, externalCompletionPublisher);
  }

  /**
   * Builds a new child execution context from a parent context. A child context delegates all getters to the parent context but
   * has it's own completion lifecycle. Completion of the child context will not cause the parent context to complete. This is
   * typically used in {@code flow-ref} type scenarios where a the referenced Flow should complete the child context, but should
   * not complete the parent context
   * 
   * @param parent the parent context
   * @return a new child context
   */
  public static EventContext child(EventContext parent) {
    EventContext child = new ChildEventContext(parent);
    if (parent instanceof AbstractEventContext) {
      ((AbstractEventContext) parent).addChildContext(child);
    }

    return child;
  }

  private final String id;
  private final String correlationId;
  private final OffsetTime receivedDate = now();

  private final String serverId;
  private final ComponentLocation location;
  private final String flowName;
  private final String connectorName;
  private final String sourceName;

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
  public ComponentLocation getOriginatingLocation() {
    return location;
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
  public String getOriginatingSourceName() {
    return sourceName;
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

  @Override
  public Optional<EventContext> getParentContext() {
    return empty();
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link Event} of this
   *        context, if available.
   * @param externalCompletionPublisher void publisher that completes when source completes enabling completion of
   *        {@link EventContext} to depend on completion of source.
   */
  private DefaultEventContext(FlowConstruct flow, ComponentLocation location, String correlationId,
                              Publisher<Void> externalCompletionPublisher) {
    super(externalCompletionPublisher);
    this.id = flow.getUniqueIdString();
    this.serverId = flow.getServerId();
    this.flowName = flow.getName();
    this.location = location;
    this.connectorName = location.getComponentIdentifier().getIdentifier().getNamespace();
    this.sourceName = location.getComponentIdentifier().getIdentifier().getName();
    this.processingTime = ProcessingTime.newInstance(flow);
    this.correlationId = correlationId;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " { id: " + id + "; correlationId: " + correlationId + "; flowName: " + flowName
        + "; serverId: " + serverId + " }";
  }

  private static class ChildEventContext extends AbstractEventContext implements Serializable {

    private static final long serialVersionUID = 1054412872901205234L;

    private final EventContext parent;

    private ChildEventContext(EventContext parent) {
      super(Mono.empty());
      this.parent = parent;
    }

    @Override
    public String getId() {
      return parent.getId() + identityHashCode(this);
    }

    @Override
    public String getCorrelationId() {
      return parent.getCorrelationId();
    }

    @Override
    public OffsetTime getReceivedTime() {
      return parent.getReceivedTime();
    }

    @Override
    public ComponentLocation getOriginatingLocation() {
      return parent.getOriginatingLocation();
    }

    @Override
    public String getOriginatingFlowName() {
      return parent.getOriginatingFlowName();
    }

    @Override
    public String getOriginatingConnectorName() {
      return parent.getOriginatingConnectorName();
    }

    @Override
    public String getOriginatingSourceName() {
      return parent.getOriginatingSourceName();
    }

    @Override
    public ProcessingTime getProcessingTime() {
      return parent.getProcessingTime();
    }

    @Override
    public ProcessorsTrace getProcessorsTrace() {
      return parent.getProcessorsTrace();
    }

    @Override
    public boolean isCorrelationIdFromSource() {
      return parent.isCorrelationIdFromSource();
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + " { id: " + parent.getId() + "; correlationId: " + parent.getCorrelationId()
          + "; flowName: " + parent.getOriginatingFlowName() + " }";
    }

    @Override
    public Optional<EventContext> getParentContext() {
      return of(parent);
    }

  }

}
