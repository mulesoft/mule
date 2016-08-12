/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.api.source.MessageSource;

import java.time.OffsetTime;
import java.util.Optional;

/**
 * Provides context about the execution of a set of related events/messages, applicable to the end-to-end processing.
 * 
 * @since 4.0
 */
public interface MessageExecutionContext {

  /**
   * @return the unique id that identifies all {@link MuleEvent}s of the same context.
   */
  String getId();

  /**
   * The correlation Id can be used by components in the system to manage message relations.
   * <p>
   * The id is associated with the message using the underlying transport protocol. As such not all messages will support the
   * notion of a id i.e. tcp or file. In this situation the correlation Id is set as a property of the message where it's up to
   * developer to keep the association with the message. For example if the message is serialised to xml the id will be available
   * in the message.
   * 
   * @return the correlation id that was set by the {@link MessageSource} for the first {@link MuleEvent} of this context, if
   *         available.
   */
  Optional<String> getCorrelationId();

  /**
   * @return a timestamp indicating when the message was received by the {@link MessageSource}.
   */
  OffsetTime getReceivedTime();

  /**
   * @return the name of the flow that processes events of this context.
   */
  String getFlowName();

  /**
   * @return a unique identifier of the server where events of this context are being processed.
   */
  String getServerId();
}
