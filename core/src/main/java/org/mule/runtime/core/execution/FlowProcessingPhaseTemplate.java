/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface FlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link org.mule.runtime.core.api.MuleEvent} created from the original message
   */
  MuleEvent getMuleEvent() throws MuleException;

  /**
   * @return the original message
   */
  Object getOriginalMessage() throws MuleException;

  /**
   * Pre processing of the {@link MuleEvent} to route
   *
   * @param muleEvent
   */
  MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException;

  /**
   * Routes the {@link MuleEvent} through the processors chain
   *
   * @param muleEvent {@link MuleEvent} created from the raw message of this context
   * @return the response {@link MuleEvent}
   * @throws MuleException
   */
  MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException;

  /**
   * Post processing of the routed {@link MuleEvent}
   *
   * @param muleEvent
   */
  MuleEvent afterRouteEvent(MuleEvent muleEvent) throws MuleException;

  /**
   * Call after successfully processing the message through the flow This method will always be called when the flow execution was
   * successful.
   *
   * @param muleEvent
   */
  void afterSuccessfulProcessingFlow(MuleEvent muleEvent) throws MuleException;


  /**
   * Call when the processing of the message through the flow fails. This method will always be called when the flow execution
   * failed.
   *
   * @param messagingException
   */
  void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException;

  /**
   * Call when the processing of the message through the flow fails in an exception strategy
   *
   * @param exception
   */
  void afterFailureProcessingFlow(MuleException exception) throws MuleException;

}
