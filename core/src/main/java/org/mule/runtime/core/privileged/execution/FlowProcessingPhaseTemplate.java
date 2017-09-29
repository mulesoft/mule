/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface FlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link CoreEvent} created from the original message
   */
  CoreEvent getEvent() throws MuleException;

  /**
   * @return the original message
   */
  Object getOriginalMessage() throws MuleException;

  /**
   * Pre processing of the {@link CoreEvent} to route
   *
   * @param muleEvent
   */
  CoreEvent beforeRouteEvent(CoreEvent muleEvent) throws MuleException;

  /**
   * Routes the {@link CoreEvent} through the processors chain
   *
   * @param muleEvent {@link CoreEvent} created from the raw message of this context
   * @return the response {@link CoreEvent}
   * @throws MuleException
   */
  CoreEvent routeEvent(CoreEvent muleEvent) throws MuleException;

  /**
   * Post processing of the routed {@link CoreEvent}
   *
   * @param muleEvent
   */
  CoreEvent afterRouteEvent(CoreEvent muleEvent) throws MuleException;

  /**
   * Call after successfully processing the message through the flow This method will always be called when the flow execution was
   * successful.
   *
   * @param muleEvent
   */
  void afterSuccessfulProcessingFlow(CoreEvent muleEvent) throws MuleException;

  /**
   * Call when the processing of the message through the flow fails in an exception strategy
   *
   * @param exception
   */
  void afterFailureProcessingFlow(MuleException exception) throws MuleException;

}
