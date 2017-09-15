/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface FlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link BaseEvent} created from the original message
   */
  BaseEvent getEvent() throws MuleException;

  /**
   * @return the original message
   */
  Object getOriginalMessage() throws MuleException;

  /**
   * Pre processing of the {@link BaseEvent} to route
   *
   * @param muleEvent
   */
  BaseEvent beforeRouteEvent(BaseEvent muleEvent) throws MuleException;

  /**
   * Routes the {@link BaseEvent} through the processors chain
   *
   * @param muleEvent {@link BaseEvent} created from the raw message of this context
   * @return the response {@link BaseEvent}
   * @throws MuleException
   */
  BaseEvent routeEvent(BaseEvent muleEvent) throws MuleException;

  /**
   * Post processing of the routed {@link BaseEvent}
   *
   * @param muleEvent
   */
  BaseEvent afterRouteEvent(BaseEvent muleEvent) throws MuleException;

  /**
   * Call after successfully processing the message through the flow This method will always be called when the flow execution was
   * successful.
   *
   * @param muleEvent
   */
  void afterSuccessfulProcessingFlow(BaseEvent muleEvent) throws MuleException;


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
