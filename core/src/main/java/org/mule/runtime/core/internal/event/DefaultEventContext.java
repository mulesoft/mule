/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.lang.System.identityHashCode;
import static java.time.Instant.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.StringUtils.EMPTY;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.management.stats.ProcessingTime;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.context.notification.DefaultProcessorsTrace;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Default immutable implementation of {@link BaseEventContext}.
 *
 * @since 4.0
 */
public final class DefaultEventContext extends AbstractEventContext implements Serializable {

  private static final long serialVersionUID = -3664490832964509653L;

  /**
   * Builds a new child execution context from a parent context. A child context delegates all getters to the parent context but
   * has it's own completion lifecycle. Completion of the child context will not cause the parent context to complete. This is
   * typically used in {@code flow-ref} type scenarios where a the referenced Flow should complete the child context, but should
   * not complete the parent context
   *
   * @param parent the parent context
   * @param componentLocation he location of the component that creates the child context and operates on result if available.
   * @return a new child context
   */
  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation) {
    return child(parent, componentLocation, NullExceptionHandler.getInstance());
  }

  /**
   * Builds a new child execution context from a parent context. A child context delegates all getters to the parent context but
   * has it's own completion lifecycle. Completion of the child context will not cause the parent context to complete. This is
   * typically used in {@code flow-ref} type scenarios where a the referenced Flow should complete the child context, but should
   * not complete the parent context
   *
   * @param parent the parent context
   * @param componentLocation the location of the component that creates the child context and operates on result if available.
   * @param exceptionHandler used to handle {@link MessagingException}'s.
   * @return a new child context
   */
  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation,
                                       FlowExceptionHandler exceptionHandler) {
    BaseEventContext child =
        new ChildEventContext(parent, componentLocation.orElse(null), exceptionHandler, parent.getDepthLevel() + 1);
    if (parent instanceof AbstractEventContext) {
      ((AbstractEventContext) parent).addChildContext(child);
    }

    return child;
  }

  private final String id;
  private final String correlationId;
  private final Instant receivedDate = now();

  private final String serverId;
  private final ComponentLocation location;

  private final ProcessingTime processingTime;
  private final ProcessorsTrace processorsTrace = new DefaultProcessorsTrace();

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getServerId() {
    return serverId;
  }

  @Override
  public String getCorrelationId() {
    return correlationId != null ? correlationId : id;
  }

  @Override
  public Instant getReceivedTime() {
    return receivedDate;
  }

  @Override
  public ComponentLocation getOriginatingLocation() {
    return location;
  }

  @Override
  public Optional<ProcessingTime> getProcessingTime() {
    return ofNullable(processingTime);
  }

  @Override
  public boolean isCorrelationIdFromSource() {
    return correlationId != null;
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    return flowCallStack;
  }

  @Override
  public ProcessorsTrace getProcessorsTrace() {
    return processorsTrace;
  }

  @Override
  public Optional<BaseEventContext> getParentContext() {
    return empty();
  }

  @Override
  public BaseEventContext getRootContext() {
    return this;
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of this
   *        context, if available.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *        depend on completion of source.
   */
  public DefaultEventContext(FlowConstruct flow, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion) {
    this(flow, flow.getExceptionListener(), location, correlationId, externalCompletion);
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param exceptionHandler the exception handler that will deal with an error context. This will be used instead of the one from
   *        the given {@code flow}
   * @param location the location of the component that received the first message for this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of this
   *        context, if available.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *        depend on completion of source.
   */
  public DefaultEventContext(FlowConstruct flow, FlowExceptionHandler exceptionHandler, ComponentLocation location,
                             String correlationId, Optional<CompletableFuture<Void>> externalCompletion) {
    super(exceptionHandler, 0, externalCompletion);
    this.id = flow.getUniqueIdString();
    this.serverId = flow.getServerId();
    this.location = location;
    this.processingTime = ProcessingTime.newInstance(flow);
    this.correlationId = correlationId;

    // Only generate flowStack dump information for when the eventContext is created for a flow.
    if (DefaultMuleConfiguration.isFlowTrace() && flow != null && flow.getMuleContext() != null) {
      eventContextMaintain(flow.getMuleContext().getEventContextService());
    }
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param id the unique id for this event context.
   * @param serverId the id of the running mule server
   * @param location the location of the component that received the first message for this context.
   * @param correlationId the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of this
   *        context, if available.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *        depend on completion of source.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  public DefaultEventContext(String id, String serverId, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion, FlowExceptionHandler exceptionHandler) {
    super(exceptionHandler, 0, externalCompletion);
    this.id = id;
    this.serverId = serverId;
    this.location = location;
    this.processingTime = null;
    this.correlationId = correlationId;
  }

  private void eventContextMaintain(EventContextService eventContextService) {
    if (eventContextService != null && eventContextService instanceof DefaultEventContextService) {
      ((DefaultEventContextService) eventContextService).addContext(this);
      this.onTerminated((e, t) -> {
        ((DefaultEventContextService) eventContextService).removeContext(DefaultEventContext.this);
      });
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " { id: " + id + "; correlationId: " + correlationId + "; flowName: "
        + getOriginatingLocation().getRootContainerName() + "; serverId: " + serverId + " }";
  }

  private static class ChildEventContext extends AbstractEventContext implements Serializable {

    private static final long serialVersionUID = 1054412872901205234L;

    private transient BaseEventContext root;
    private final BaseEventContext parent;
    private final ComponentLocation componentLocation;
    private final String id;

    private ChildEventContext(BaseEventContext parent, ComponentLocation componentLocation,
                              FlowExceptionHandler messagingExceptionHandler, int depthLevel) {
      super(messagingExceptionHandler, depthLevel, empty());
      this.flowCallStack = parent.getFlowCallStack().clone();
      this.root = parent.getRootContext();
      this.parent = parent;
      this.componentLocation = componentLocation;
      this.id = parent.getId() != null
          ? parent.getId().concat("_").concat(Integer.toString(identityHashCode(this)))
          : Integer.toString(identityHashCode(this));
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getCorrelationId() {
      return parent.getCorrelationId();
    }

    @Override
    public Instant getReceivedTime() {
      return parent.getReceivedTime();
    }

    @Override
    public ComponentLocation getOriginatingLocation() {
      return parent.getOriginatingLocation();
    }

    @Override
    public Optional<ProcessingTime> getProcessingTime() {
      return parent.getProcessingTime();
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return flowCallStack;
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
    public BaseEventContext getRootContext() {
      if (root == null) {
        root = this.parent.getRootContext();
      }
      return root;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + " { id: " + getId() + "; correlationId: " + parent.getCorrelationId()
          + "; flowName: " + parent.getOriginatingLocation().getRootContainerName() + "; componentLocation: "
          + (componentLocation != null ? componentLocation.getLocation() : EMPTY) + ";";
    }

    @Override
    public Optional<BaseEventContext> getParentContext() {
      return of(parent);
    }

  }
}
