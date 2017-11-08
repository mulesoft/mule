/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import static java.util.Optional.empty;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.internal.event.DefaultEventContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Factory interface for creating a new {@link EventContext}
 *
 * @since 4.0
 */
public interface EventContextFactory {

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   */
  static EventContext create(FlowConstruct flow, ComponentLocation location) {
    return create(flow, location, null);
  }

  /**
   * Builds a new execution context with the given parameters and an empty publisher.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   */
  static EventContext create(FlowConstruct flow, ComponentLocation location, String correlationId) {
    return create(flow, location, correlationId, empty());
  }

  /**
   * Builds a new execution context with the given parameters.
   * 
   * @param id the unique id for this event context.
   * @param serverId the id of the running mule server
   * @param location the location of the component that received the first message for this context.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static EventContext create(String id, String serverId, ComponentLocation location,
                             FlowExceptionHandler exceptionHandler) {
    return create(id, serverId, location, null, exceptionHandler);
  }

  /**
   * Builds a new execution context with the given parameters and an empty publisher.
   * 
   * @param id the unique id for this event context.
   * @param serverId the id of the running mule server
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static EventContext create(String id, String serverId, ComponentLocation location, String correlationId,
                             FlowExceptionHandler exceptionHandler) {
    return create(id, serverId, location, correlationId, empty(), exceptionHandler);
  }

  /**
   * Builds a new execution context with the given parameters.
   *
   * @param flow the flow that processes events of this context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *        depend on completion of source.
   */
  static EventContext create(FlowConstruct flow, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion) {
    return new DefaultEventContext(flow, location, correlationId, externalCompletion);
  }

  /**
   * Builds a new execution context with the given parameters.
   * 
   * @param id the unique id for this event context.
   * @param location the location of the component that received the first message for this context.
   * @param correlationId See {@link EventContext#getCorrelationId()}.
   * @param externalCompletion future that completes when source completes enabling termination of {@link BaseEventContext} to
   *        depend on completion of source.
   * @param exceptionHandler the exception handler that will deal with an error context
   */
  static EventContext create(String id, String serverId, ComponentLocation location, String correlationId,
                             Optional<CompletableFuture<Void>> externalCompletion,
                             FlowExceptionHandler exceptionHandler) {
    return new DefaultEventContext(id, serverId, location, correlationId, externalCompletion, exceptionHandler);
  }
}
