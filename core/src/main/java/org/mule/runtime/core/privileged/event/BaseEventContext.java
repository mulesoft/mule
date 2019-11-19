/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.ProcessingTime;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.reactivestreams.Publisher;

/**
 * Context representing a message that is received by a Mule Runtime via a connector source. This context is immutable and
 * maintained during all execution originating from a given source message and all instances of {@link CoreEvent} created as part
 * of the processing of the source message will maintain a reference to this instance. Wherever a Flow references another Flow
 * this {@link BaseEventContext} will be maintained, while whenever there is a connector boundary a new instance will be created
 * by the receiving source.
 *
 * @see CoreEvent
 * @since 4.0
 */
@NoImplement
public interface BaseEventContext extends EventContext {

  /**
   * Complete this {@link BaseEventContext} successfully with no result {@link CoreEvent}.
   *
   * @return {@link Publisher<Void>} that completes when response processing is complete.
   */
  void success();

  /**
   * Complete this {@link BaseEventContext} successfully with a result {@link CoreEvent}.
   *
   * @param event the result event.
   * @return {@link Publisher<Void>} that completes when response processing is complete.
   */
  void success(CoreEvent event);

  /**
   * Complete this {@link BaseEventContext} unsuccessfully with an error.
   *
   * @param throwable the throwable.
   * @return {@link Publisher<Void>} that completes when error processing is complete.
   */
  Publisher<Void> error(Throwable throwable);

  /**
   * @returns information about the times spent processing the events for this context (so far).
   */
  Optional<ProcessingTime> getProcessingTime();

  /**
   * Events have a stack of executed flows (same as a call stack), so that at any given instant an application developer can
   * determine where this event came from.
   *
   * @return the flow stack associated to this event.
   *
   * @since 3.8.0
   */
  default FlowCallStack getFlowCallStack() {
    return null;
  }

  /**
   * The list will always be empty.
   *
   * @since 3.8.0
   *
   * @deprecated Use the message history functionality form the agent instead.
   */
  @Deprecated
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
   * @return {@code this} context's root context or the same instance if the root is itself.
   */
  BaseEventContext getRootContext();

  /**
   * Use to determine if the event context is complete. A {@link EventContext} is considered terminated once:
   * <ul>
   * <li>The event context is complete</li>
   * <li>Response consumer callbacks have been executed and response publisher subscribers signalled.</li>
   * <li>The external completion publisher, if provided, is completed.</li>
   * </ul>
   *
   * Completion callback consumers are executed after event context completeness status is updated.
   *
   * @return {@code true} if {@code this} context is complete.
   */
  boolean isTerminated();

  /**
   * Use to determine if the event context is complete. A {@link EventContext} is considered complete once:
   * <ul>
   * <li>A response event/error is available.</li>
   * <li>All child event contexts have completed.</li>
   * </ul>
   *
   * @return {@code true} if {@code this} context is complete.
   */
  boolean isComplete();

  /**
   * Register a {@link BiConsumer} callback that will be executed when this {@link EventContext} terminates. There are currently
   * no guarantees given regarding the order of callback execution.
   * <p/>
   * Consumers should not plan on throwing exceptions. Any exceptions thrown will be caught and logged.
   *
   * @param consumer callback to execute on event context completion.
   * @throws NullPointerException if consumer is {@code null}
   */
  void onTerminated(BiConsumer<CoreEvent, Throwable> consumer);

  /**
   * Register a {@link BiConsumer} callback that will be executed when this {@link EventContext} completes. There are currently no
   * guarantees given regarding the order of callback execution.
   * <p/>
   * Consumers should not plan on throwing exceptions. Any exceptions thrown will be caught and logged.
   *
   * @param consumer callback to execute on event context completion.
   * @throws NullPointerException if consumer is {@code null}
   */
  void onComplete(BiConsumer<CoreEvent, Throwable> consumer);

  /**
   * Register a {@link BiConsumer} callback that will be executed when a response event or error is available for this
   * {@link EventContext}. There are currently no guarantees given regarding the order of callback execution.
   * <p/>
   * Consumers should not plan on throwing exceptions. Any exceptions thrown will be caught and logged.
   *
   * @param consumer callback to execute on event context response.
   * @throws NullPointerException if consumer is {@code null}
   */
  void onResponse(BiConsumer<CoreEvent, Throwable> consumer);

  /**
   * A {@link Publisher} that completes when a response is ready or an error was produced for this {@link BaseEventContext}.
   * <p/>
   * Any asynchronous processing initiated as part of processing the request {@link CoreEvent} maybe still be in process when this
   * {@link Publisher} completes. In order to be notified when all processing is complete use a {@link #onTerminated(BiConsumer)}
   * callback.
   * <p/>
   * Response publisher subscribers are notified after execution of response callbacks registered via
   * {@link #onResponse(BiConsumer)}.
   *
   * @return publisher that completes when this {@link BaseEventContext} instance has a response of error.
   */
  Publisher<CoreEvent> getResponsePublisher();

  /**
   * Provides an identifier string containing the name of the artifact that generated this {@link EventContext}.
   *
   * @return a unique identifier of the artifact that created this {@link EventContext}.
   *
   * @since 4.1
   */
  default String getServerId() {
    return null;
  }

  /**
   * @return the distance of this eventContext to its root ,counting one for every ancestor.
   *
   * @since 4.1.3
   */
  int getDepthLevel();

}
