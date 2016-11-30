/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.exception.MessagingException;

import java.time.OffsetTime;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
public interface EventContext extends Publisher<Event> {


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

  void complete();

  void complete(Event event);

  void error(MessagingException messagingException);

}
