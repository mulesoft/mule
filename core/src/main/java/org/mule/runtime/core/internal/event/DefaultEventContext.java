/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import static org.mule.runtime.core.privileged.event.DefaultFlowCallStack.newDefaultFlowCallStack;

import static java.lang.System.lineSeparator;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.management.stats.ProcessingTime;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.streaming.EventStreamingState;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

/**
 * Default immutable implementation of {@link BaseEventContext}.
 *
 * @since 4.0
 */
public final class DefaultEventContext extends AbstractEventContext implements Serializable {

  private static final Logger LOGGER = getLogger(DefaultEventContext.class);

  private static final long serialVersionUID = -3664490832964509653L;
  private transient SpanContext distributedTraceContext;

  /**
   * Builds a new child execution context from a parent context. A child context delegates all getters to the parent context but
   * has it's own completion lifecycle. Completion of the child context will not cause the parent context to complete. This is
   * typically used in {@code flow-ref} type scenarios where a the referenced Flow should complete the child context, but should
   * not complete the parent context
   *
   * @param parent            the parent context
   * @param componentLocation he location of the component that creates the child context and operates on result if available.
   * @return a new child context
   */
  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation) {
    return child(parent, componentLocation, NullExceptionHandler.getInstance());
  }

  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation,
                                       final String correlationId) {
    return child(parent, componentLocation, NullExceptionHandler.getInstance(), correlationId);
  }

  /**
   * Builds a new child execution context from a parent context. A child context delegates all getters to the parent context but
   * has it's own completion lifecycle. Completion of the child context will not cause the parent context to complete. This is
   * typically used in {@code flow-ref} type scenarios where a the referenced Flow should complete the child context, but should
   * not complete the parent context
   *
   * @param parent            the parent context
   * @param componentLocation the location of the component that creates the child context and operates on result if available.
   * @param exceptionHandler  used to handle {@link MessagingException}'s.
   * @return a new child context
   */
  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation,
                                       FlowExceptionHandler exceptionHandler) {
    return child(parent, componentLocation, exceptionHandler, null);
  }

  public static BaseEventContext child(BaseEventContext parent, Optional<ComponentLocation> componentLocation,
                                       FlowExceptionHandler exceptionHandler, final String correlationId) {
    BaseEventContext child = new ChildEventContext(parent, componentLocation.orElse(null), exceptionHandler,
                                                   parent.getDepthLevel() + 1, correlationId);
    if (parent instanceof AbstractEventContext aec) {
      aec.addChildContext(child);
    }

    return child;
  }

  private final String id;
  private final String correlationId;
  private final Instant receivedDate = now();

  private final String serverId;
  private final ComponentLocation location;

  private final ProcessingTime processingTime;

  private transient EventStreamingState streamingState;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getRootId() {
    return getRootContext().getCorrelationId();
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
  public Instant getStartTime() {
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
    return Collections::emptyList;
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
   * @param flow                the flow that processes events of this context.
   * @param eventContextService the keeper of all currently active {@link EventContext}s to generate a dump on demand.
   * @param location            the location of the component that received the first message for this context.
   * @param correlationId       the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of
   *                            this context, if available.
   * @param externalCompletion  future that completes when source completes enabling termination of {@link BaseEventContext} to
   *                            depend on completion of source.
   */
  public DefaultEventContext(FlowConstruct flow,
                             EventContextService eventContextService,
                             ComponentLocation location,
                             String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion) {
    this(flow, NullExceptionHandler.getInstance(), eventContextService, location, correlationId, externalCompletion);
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow                the flow that processes events of this context.
   * @param exceptionHandler    the exception handler that will deal with an error context. This will be used instead of the one
   *                            from the given {@code flow}
   * @param eventContextService the keeper of all currently active {@link EventContext}s to generate a dump on demand.
   * @param location            the location of the component that received the first message for this context.
   * @param correlationId       the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of
   *                            this context, if available.
   * @param externalCompletion  future that completes when source completes enabling termination of {@link BaseEventContext} to
   *                            depend on completion of source.
   */
  public DefaultEventContext(FlowConstruct flow,
                             FlowExceptionHandler exceptionHandler,
                             EventContextService eventContextService,
                             ComponentLocation location,
                             String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion) {
    super(exceptionHandler, 0, externalCompletion);
    this.id = requireNonNull(flow, "'flow' can't be null").getUniqueIdString();
    this.serverId = flow.getServerId();
    this.location = location;
    this.processingTime = ProcessingTime.newInstance(flow);
    this.correlationId = correlationId;

    eventContextMaintain(eventContextService);
    this.flowCallStack = newDefaultFlowCallStack();
    createStreamingState();
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param id                 the unique id for this event context.
   * @param serverId           the id of the running mule server
   * @param location           the location of the component that received the first message for this context.
   * @param correlationId      the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of
   *                           this context, if available.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *                           depend on completion of source.
   */
  public DefaultEventContext(String id, String serverId, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion) {
    this(id, serverId, location, correlationId, externalCompletion, NullExceptionHandler.getInstance());
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param id                 the unique id for this event context.
   * @param serverId           the id of the running mule server
   * @param location           the location of the component that received the first message for this context.
   * @param correlationId      the correlation id that was set by the {@link MessageSource} for the first {@link CoreEvent} of
   *                           this context, if available.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *                           depend on completion of source.
   * @param exceptionHandler   the exception handler that will deal with an error context
   *
   * @deprecated since 4.3.0, use {@link #DefaultEventContext(String, String, ComponentLocation, String, Optional)} instead and
   *             rely on the provided {@code processor} to do the error handling.
   */
  @Deprecated
  public DefaultEventContext(String id, String serverId, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion, FlowExceptionHandler exceptionHandler) {
    super(exceptionHandler, 0, externalCompletion);
    this.id = id;
    this.serverId = serverId;
    this.location = location;
    this.processingTime = null;
    this.correlationId = correlationId;
    this.flowCallStack = newDefaultFlowCallStack();
    createStreamingState();
  }

  void createStreamingState() {
    if (streamingState == null) {
      initCompletionLists();
      streamingState = new EventStreamingState();
      onTerminated((event, e) -> streamingState.dispose());
    }
  }

  /**
   * Tracks the given {@code provider} as one owned by this event. Upon completion of this context, the {@code provider} will be
   * automatically closed and its resources freed.
   * <p>
   * Consumers of this method <b>MUST</b> discard the passed {@code provider} and continue using the returned one instead.
   *
   * @param provider    a {@link CursorStreamProvider}
   * @param ghostBuster the {@link StreamingGhostBuster}
   * @return a tracked {@link CursorProvider}.
   * @since 4.3.0
   */
  public <T extends Cursor> CursorProvider<T> track(ManagedCursorProvider<T> provider, StreamingGhostBuster ghostBuster) {
    return streamingState.addProvider(provider, ghostBuster);
  }

  private void eventContextMaintain(EventContextService eventContextService) {
    if (eventContextService instanceof DefaultEventContextService decs) {
      decs.addContext(this);
      this.onTerminated((e, t) -> decs.removeContext(DefaultEventContext.this));
    }
  }

  @Override
  public String toString() {
    if (LOGGER.isTraceEnabled()) {
      return lineSeparator() + detailedToString(0, this) + lineSeparator();
    } else {
      return basicToString();
    }
  }

  @Override
  protected String basicToString() {
    return getClass().getSimpleName() + " { state: " + getState() + "; id: " + id + "; flowName: "
        + getOriginatingLocation().getRootContainerName() + " }";
  }

  @Override
  public SpanContext getSpanContext() {
    return distributedTraceContext;
  }

  @Override
  public void setSpanContext(SpanContext distributedTraceContext) {
    this.distributedTraceContext = distributedTraceContext;
  }

  private static class ChildEventContext extends AbstractEventContext implements Serializable {

    private static final long serialVersionUID = 1054412872901205234L;
    private transient BaseEventContext root;
    private final BaseEventContext parent;
    private final ComponentLocation componentLocation;
    private final String id;
    private final String correlationId;
    private final Instant startTime = now();
    private transient SpanContext spanContext;
    private final String rootId;

    private ChildEventContext(BaseEventContext parent, ComponentLocation componentLocation,
                              FlowExceptionHandler messagingExceptionHandler, int depthLevel, final String correlationId) {
      super(messagingExceptionHandler, depthLevel, empty());
      this.flowCallStack = parent.getFlowCallStack().clone();
      this.root = parent.getRootContext();
      this.parent = parent;
      this.componentLocation = componentLocation;
      this.id = parent.nextChildId();
      this.correlationId = correlationId != null ? correlationId : parent.getCorrelationId();
      this.rootId = root.getRootId();
      if (parent instanceof SpanContextAware sca) {
        SpanContext parentSpanContext = getParentDistributedTraceContext(sca);
        if (parentSpanContext != null) {
          spanContext = parentSpanContext.copy();
        }
      }
    }

    private SpanContext getParentDistributedTraceContext(SpanContextAware parent) {
      return parent.getSpanContext();
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getRootId() {
      return this.rootId;
    }

    @Override
    public String getCorrelationId() {
      return correlationId;
    }

    @Override
    public SpanContext getSpanContext() {
      return spanContext;
    }

    @Override
    public void setSpanContext(SpanContext spanContext) {
      this.spanContext = spanContext;
    }

    @Override
    public Instant getReceivedTime() {
      return parent.getReceivedTime();
    }

    @Override
    public Instant getStartTime() {
      return startTime;
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
      if (LOGGER.isTraceEnabled()) {
        return lineSeparator() + ((AbstractEventContext) root).detailedToString(0, this) + lineSeparator();
      } else {
        return basicToString();
      }
    }

    @Override
    public String basicToString() {
      return getClass().getSimpleName() + " { state: " + getState() + "; id: " + getId() + "; componentLocation: "
          + (componentLocation != null ? componentLocation.getLocation() : EMPTY) + " }";
    }

    @Override
    public Optional<BaseEventContext> getParentContext() {
      return of(parent);
    }

  }
}
