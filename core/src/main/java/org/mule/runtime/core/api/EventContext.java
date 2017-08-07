/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.management.stats.ProcessingTime;

import java.time.OffsetTime;
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
public interface EventContext extends org.mule.runtime.api.event.EventContext {

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
   *
   * @return the location where this context's events come from
   */
  ComponentLocation getOriginatingLocation();

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
   * Complete this {@link EventContext} unsuccessfully with an error.
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
  Optional<org.mule.runtime.api.event.EventContext> getParentContext();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link EventContext} but
   * importantly before the Response {@link Publisher} obtained via {@link #getResponsePublisher()} completes. This allows for
   * response subscribers that are executed before the source, client or parent flow receives to be registered. In order to
   * subscribe after response processing you can use the response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link Event} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link EventContext} instance has a response of error.
   * @see #getResponsePublisher()
   * @see #getCompletionPublisher()
   */
  Publisher<Event> getBeforeResponsePublisher();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link EventContext}. Any
   * subscribers registered before the response completes will be executed after the response has been processed by the source,
   * client or parent flow. In order to subscribe before response processing you can use the before response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link Event} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link EventContext} instance has a response of error.
   * @see #getBeforeResponsePublisher() ()
   * @see #getCompletionPublisher()
   */
  Publisher<Event> getResponsePublisher();

  /**
   * A {@link Publisher} that completes when a this {@link EventContext} and all child {@link EventContext}'s have completed. In
   * practice this means that this {@link Publisher} completes once all branches of execution have completed regardless of is they
   * are synchronous or asynchronous. This {@link Publisher} will never complete with an error.
   *
   * @return publisher that completes when this {@link EventContext} and all child context have completed.
   */
  Publisher<Void> getCompletionPublisher();

  Optional<EventContext> getInternalParentContext();
}
