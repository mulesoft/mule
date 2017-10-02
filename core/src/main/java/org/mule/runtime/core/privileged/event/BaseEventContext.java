/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.ProcessingTime;

import org.reactivestreams.Publisher;

import java.util.Optional;

/**
 * Context representing a message that is received by a Mule Runtime via a connector source. This context is immutable and
 * maintained during all execution originating from a given source message and all instances of {@link CoreEvent} created as part of
 * the processing of the source message will maintain a reference to this instance. Wherever a Flow references another Flow this
 * {@link BaseEventContext} will be maintained, while whenever there is a connector boundary a new instance will be created by the
 * receiving source.
 *
 * @see CoreEvent
 * @since 4.0
 */
public interface BaseEventContext extends EventContext {

  /**
   * Complete this {@link BaseEventContext} successfully with no result {@link CoreEvent}.
   */
  void success();

  /**
   * Complete this {@link BaseEventContext} successfully with a result {@link CoreEvent}.
   *
   * @param event the result event.
   */
  void success(CoreEvent event);

  /**
   * Complete this {@link BaseEventContext} unsuccessfully with an error.
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
   * <p>
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
  Optional<BaseEventContext> getParentContext();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link BaseEventContext} but
   * importantly before the Response {@link Publisher} obtained via {@link #getResponsePublisher()} completes. This allows for
   * response subscribers that are executed before the source, client or parent flow receives to be registered. In order to
   * subscribe after response processing you can use the response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link CoreEvent} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link BaseEventContext} instance has a response of error.
   * @see #getResponsePublisher()
   * @see #getCompletionPublisher()
   */
  Publisher<CoreEvent> getBeforeResponsePublisher();

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link BaseEventContext}. Any
   * subscribers registered before the response completes will be executed after the response has been processed by the source,
   * client or parent flow. In order to subscribe before response processing you can use the before response {@link Publisher}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link CoreEvent} maybe still be in process when this
   * {@link Publisher} completes. The completion {@link Publisher} can be used to perform an action after all processing is
   * complete.
   *
   * @return publisher that completes when this {@link BaseEventContext} instance has a response of error.
   * @see #getBeforeResponsePublisher() ()
   * @see #getCompletionPublisher()
   */
  Publisher<CoreEvent> getResponsePublisher();

  /**
   * A {@link Publisher} that completes when a this {@link BaseEventContext} and all child {@link BaseEventContext}'s have completed. In
   * practice this means that this {@link Publisher} completes once all branches of execution have completed regardless of is they
   * are synchronous or asynchronous. This {@link Publisher} will never complete with an error.
   *
   * @return publisher that completes when this {@link BaseEventContext} and all child context have completed.
   */
  Publisher<Void> getCompletionPublisher();

}
