/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.exception.MessagingException;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface FlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link InternalEvent} created from the original message
   */
  InternalEvent getEvent() throws MuleException;

  /**
   * @return the original message
   */
  Object getOriginalMessage() throws MuleException;

  /**
   * Pre processing of the {@link InternalEvent} to route
   *
   * @param muleEvent
   */
  InternalEvent beforeRouteEvent(InternalEvent muleEvent) throws MuleException;

  /**
   * Routes the {@link InternalEvent} through the processors chain
   *
   * @param muleEvent {@link InternalEvent} created from the raw message of this context
   * @return the response {@link InternalEvent}
   * @throws MuleException
   */
  InternalEvent routeEvent(InternalEvent muleEvent) throws MuleException;

  /**
   * Post processing of the routed {@link InternalEvent}
   *
   * @param muleEvent
   */
  InternalEvent afterRouteEvent(InternalEvent muleEvent) throws MuleException;

  /**
   * Call after successfully processing the message through the flow This method will always be called when the flow execution was
   * successful.
   *
   * @param muleEvent
   */
  void afterSuccessfulProcessingFlow(InternalEvent muleEvent) throws MuleException;


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
