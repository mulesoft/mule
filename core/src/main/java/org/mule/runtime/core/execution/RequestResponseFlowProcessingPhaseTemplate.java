/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;

/**
 * Extension of {@link FlowProcessingPhaseTemplate} for those {@link org.mule.runtime.core.api.source.MessageSource} that requires
 * sending a response of the message processed.
 */
public interface RequestResponseFlowProcessingPhaseTemplate extends FlowProcessingPhaseTemplate {

  /**
   * Template method to send a response after processing the message.
   * <p/>
   * This method is executed within the flow so if it fails it will trigger the exception strategy.
   *
   * @param muleEvent the event with the content of the response to be sent.
   * @throws MuleException exception thrown when processing the message to send the response. If there's a failure when writing
   *         the response using the underlying transport or connector then the exception to throw must be a
   *         {@link ResponseDispatchException}.
   */
  void sendResponseToClient(Event muleEvent) throws MuleException;

  /**
   * @param messagingException exception thrown during the flow execution.
   * @throws MuleException exception thrown when processing the message to send the response.
   */
  void sendFailureResponseToClient(MessagingException messagingException) throws MuleException;
}
