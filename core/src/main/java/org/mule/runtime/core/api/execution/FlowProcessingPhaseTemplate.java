/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.exception.MessagingException;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface FlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link Event} created from the original message
   */
  Event getEvent() throws MuleException;

  /**
   * @return the original message
   */
  Object getOriginalMessage() throws MuleException;

  /**
   * Pre processing of the {@link Event} to route
   *
   * @param muleEvent
   */
  Event beforeRouteEvent(Event muleEvent) throws MuleException;

  /**
   * Routes the {@link Event} through the processors chain
   *
   * @param muleEvent {@link Event} created from the raw message of this context
   * @return the response {@link Event}
   * @throws MuleException
   */
  Event routeEvent(Event muleEvent) throws MuleException;

  /**
   * Post processing of the routed {@link Event}
   *
   * @param muleEvent
   */
  Event afterRouteEvent(Event muleEvent) throws MuleException;

  /**
   * Call after successfully processing the message through the flow This method will always be called when the flow execution was
   * successful.
   *
   * @param muleEvent
   */
  void afterSuccessfulProcessingFlow(Event muleEvent) throws MuleException;


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
