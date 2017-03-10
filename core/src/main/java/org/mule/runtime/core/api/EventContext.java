/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.time.OffsetTime;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * Context representing a message that is received by a Mule Runtime via a connector source. This context is immutable and
 * maintained during all execution originating from a given source message and all instances of {@link Event} created as part of
 * the processing of the source message will maintain a reference to this instance. Wherever a Flow references another Flow this
 * {@link EventContext} will be maintained, while whenever there is a connector boundary a new instance will be created by the
 * receiving source.
 *
 * @see Event
 * @since 4.0
 */
public interface EventContext {

  /**
   * Unique time-based id (UUID) for this {@link EventContext}.
   *
   * @return the UUID for this {@link EventContext}
   */
  String getId();

  /**
   * The correlation ID is used to correlate messages between different flows and systems.
   * <p>
   * If the connector that receives the source message supports the concept of a correlation ID then the connector should create
   * an instance of {@link EventContext} using this value. If on the other hand, no correlation ID is received by the source
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
   * TODO MULE-10517 Review this
   * 
   * @return the name of the flow that processes events of this context.
   */
  String getOriginatingFlowName();

  /**
   * TODO MULE-10517 Review this
   * 
   * @return the name of the connector that generated the message for the first event of this context.
   */
  String getOriginatingConnectorName();

  /**
   * Complete this {@link EventContext} successfully with no result {@link Event}.
   */
  void success();

  /**
   * Complete this {@link EventContext} successfully with a result {@link Event}.
   *
   * @param event the result event.
   */
  void success(Event event);

  /**
   * Complete this {@link EventContext} unsuccessfully with an error
   *
   * @param throwable the throwable.
   */
  void error(Throwable throwable);

  /**
   * @returns information about the times spent processing the events for this context (so far).
   */
  ProcessingTime getProcessingTime();

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
   * @return An immutable list with all the child context which were produced from {@code this} instance
   */
  List<EventContext> getChildContexts();

  /**
   * Returns {@code this} context's parent if it has one
   * @return {@code this} context's parent or {@link Optional#empty()} if it doesn't have one
   */
  Optional<EventContext> getParentContext();

  /**
   * Indicates that the owning {@link Event} is involved in at least one streaming operation
   */
  void streaming();

  /**
   * @return Whether {@code this} context or any of its childs is taking part in a streaming operation
   */
  boolean isStreaming();

  Publisher<Event> getResponsePublisher();

  Publisher<Void> getCompletionPublisher();
}
