/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.core.api.source.MessageSource;

import java.util.Date;
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
   * @return the correlation id that was set by the {@link MessageSource} for the first {@link MuleEvent} of this context, if
   *         available.
   */
  Optional<String> getSourceCorrelationId();

  /**
   * @return a timestamp indicating when the message was received by the {@link MessageSource}.
   */
  Date getReceivedDate();
}
