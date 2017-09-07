/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.management.stats.ProcessingTime;

import java.time.OffsetTime;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Context representing a message that is received by a Mule Runtime via a connector source. This context is immutable and
 * maintained during all execution originating from a given source message and all instances of {@link InternalEvent} created as part of
 * the processing of the source message will maintain a reference to this instance. Wherever a Flow references another Flow this
 * {@link InternalEventContext} will be maintained, while whenever there is a connector boundary a new instance will be created by the
 * receiving source.
 *
 * @see InternalEvent
 * @since 4.0
 */
public interface InternalEventContext extends EventContext {

  /**
   * Unique time-based id (UUID) for this {@link InternalEventContext}.
   *
   * @return the UUID for this {@link InternalEventContext}
   */
  String getId();

  /**
   * The correlation ID is used to correlate messages between different flows and systems.
   * <p>
   * If the connector that receives the source message supports the concept of a correlation ID then the connector should create
   * an instance of {@link InternalEventContext} using this value. If on the other hand, no correlation ID is received by the source
   * connector then a time-based UUID, also available via {@link #getId()} is used.
   *
   * @return the correlation id.
   */
  String getCorrelationId();

  /**
   * @return a timestamp indicating when the message was received by the connector source
   */
  OffsetTime getReceivedTime();

  /**
   *
   * @return the location where this context's events come from
   */
  ComponentLocation getOriginatingLocation();

  /**
   * Complete this {@link InternalEventContext} successfully with no result {@link InternalEvent}.
   */
  void success();

  /**
   * Complete this {@link InternalEventContext} successfully with a result {@link InternalEvent}.
   *
   * @param event the result event.
   */
  void success(InternalEvent event);

  /**
   * Complete this {@link InternalEventContext} unsuccessfully with an error.
   *
   * @param throwable the throwable.
   */
  Publisher<Void> error(Throwable throwable);

  /**
   * @returns information about the times spent processing the events for this context (so far).
   */
  Optional<ProcessingTime> getProcessingTime();

  /**
   * Events have a list of message processor paths it went trough so that the execution path of an event can be reconstructed
   * after it has executed.
   * <p/>
   * This will only be enabled if {@link DefaultMuleConfiguration#isFlowTrace()} is {@code true}. If {@code false}, the list will
   * always be empty.
   * 
   * @return the message processors trace associated to this event.
   * 
   * @since 3.8.0
   */
  ProcessorsTrace getProcessorsTrace();

  /**
   * Used to determine if the correlation was set by the source connector or was generated.
   *
   * @return {@code true} if the source system provided a correlation id, {@code false otherwise}.
   */
  boolean isCorrelationIdFromSource();

  /**
   * Returns {@code this} context's parent if it has one
   * 
   * @return {@code this} context's parent or {@link Optional#empty()} if it doesn't have one
   */
  Optional<InternalEventContext> getParentContext();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link InternalEventContext} but
   * importantly before the Response {@link Publisher} obtained via {@link #getResponsePublisher()} completes. This allows for
   * response subscribers that are executed before the source, client or parent flow receives to be registered. In order to
   * subscribe after response processing you can use the response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link InternalEvent} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link InternalEventContext} instance has a response of error.
   * @see #getResponsePublisher()
   * @see #getCompletionPublisher()
   */
  Publisher<InternalEvent> getBeforeResponsePublisher();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link InternalEventContext}. Any
   * subscribers registered before the response completes will be executed after the response has been processed by the source,
   * client or parent flow. In order to subscribe before response processing you can use the before response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link InternalEvent} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link InternalEventContext} instance has a response of error.
   * @see #getBeforeResponsePublisher() ()
   * @see #getCompletionPublisher()
   */
  Publisher<InternalEvent> getResponsePublisher();

  /**
   * A {@link Publisher} that completes when a this {@link InternalEventContext} and all child {@link InternalEventContext}'s have completed. In
   * practice this means that this {@link Publisher} completes once all branches of execution have completed regardless of is they
   * are synchronous or asynchronous. This {@link Publisher} will never complete with an error.
   *
   * @return publisher that completes when this {@link InternalEventContext} and all child context have completed.
   */
  Publisher<Void> getCompletionPublisher();

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   */
  static InternalEventContext create(FlowConstruct flow, ComponentLocation location) {
    return create(flow, location, null);
  }

  /**
   * Builds a new execution context with the given parameters and an empty publisher.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link InternalEventContext#getCorrelationId()}.
   */
  static InternalEventContext create(FlowConstruct flow, ComponentLocation location, String correlationId) {
    return create(flow, location, correlationId, Mono.empty());
  }

  /**
   * Builds a new execution context with the given parameters.
   *  @param id the unique id for this event context.
   * @param serverId the id of the running mule server
   * @param location the location of the component that received the first message for this context.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static InternalEventContext create(String id, String serverId, ComponentLocation location,
                                     MessagingExceptionHandler exceptionHandler) {
    return create(id, serverId, location, null, exceptionHandler);
  }

  /**
   * Builds a new execution context with the given parameters and an empty publisher.
   *  @param id the unique id for this event context.
   * @param serverId the id of the running mule server
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link InternalEventContext#getCorrelationId()}.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static InternalEventContext create(String id, String serverId, ComponentLocation location, String correlationId,
                                     MessagingExceptionHandler exceptionHandler) {
    return create(id, serverId, location, correlationId, Mono.empty(), exceptionHandler);
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link InternalEventContext#getCorrelationId()}.
   * @param externalCompletionPublisher void publisher that completes when source completes enabling completion of
   *        {@link InternalEventContext} to depend on completion of source.
   */
  static InternalEventContext create(FlowConstruct flow, ComponentLocation location, String correlationId,
                                     Publisher<Void> externalCompletionPublisher) {
    return new DefaultEventContext(flow, location, correlationId, externalCompletionPublisher);
  }

  /**
   * Builds a new execution context with the given parameters.
   *  @param id the unique id for this event context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link InternalEventContext#getCorrelationId()}.
   * @param externalCompletionPublisher void publisher that completes when source completes enabling completion of
   *        {@link InternalEventContext} to depend on completion of source.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static InternalEventContext create(String id, String serverId, ComponentLocation location, String correlationId,
                                     Publisher<Void> externalCompletionPublisher,
                                     MessagingExceptionHandler exceptionHandler) {
    return new DefaultEventContext(id, serverId, location, correlationId, externalCompletionPublisher, exceptionHandler);
  }

}
