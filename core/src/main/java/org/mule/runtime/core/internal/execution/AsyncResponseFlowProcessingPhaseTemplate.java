/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.execution.MessageProcessTemplate;
import org.mule.runtime.core.api.execution.ResponseDispatchException;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface AsyncResponseFlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link Event} created from the original message
   */
  Event getEvent() throws MuleException;

  /**
   * Routes the {@link Event} through the processors chain
   *
   * @param event {@link Event} created from the raw message of this context
   * @return the response {@link Event}
   * @throws MuleException
   * @param event {@link Event} created from the raw message of this context
   * @return the response {@link Event}
   * @throws MuleException
   */
  Event routeEvent(Event event) throws MuleException;

  /**
   * Template method to send a response after processing the message.
   * <p/>
   * This method is executed within the flow so if it fails it will trigger the exception strategy.
   *
   * @param event the event with the content of the response to be sent.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation
   * @throws MuleException exception thrown when processing the message to send the response. If there's a failure when writing
   *         the response using the underlying transport or connector then the exception to throw must be a
   *         {@link ResponseDispatchException}.
   */
  void sendResponseToClient(Event event, ResponseCompletionCallback responseCompletionCallback) throws MuleException;


  /**
   *
   * @param exception exception thrown during the flow execution.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation
   * @throws MuleException exception thrown when processing the message to send the response.
   */
  void sendFailureResponseToClient(MessagingException exception, ResponseCompletionCallback responseCompletionCallback)
      throws MuleException;

}
