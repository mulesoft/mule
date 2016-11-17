/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface ExtensionFlowProcessingPhaseTemplate extends MessageProcessTemplate {

  /**
   * @return a {@link Message} created from the original message
   */
  Message getMessage() throws MuleException;

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
   *
   * @param flowExecutionResponse the result of the flow execution
   * @param parameters the resolved set of parameters required to send the response.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation
   * @throws MuleException exception thrown when processing the message to send the response. If there's a failure when writing
   *         the response using the underlying transport or connector then the exception to throw must be a
   *         {@link ResponseDispatchException}.
   */
  void sendResponseToClient(Event flowExecutionResponse, Map<String, Object> parameters,
                            ResponseCompletionCallback responseCompletionCallback)
      throws MuleException;


  /**
   * @param exception exception thrown during the flow execution.
   * @param parameters the resolved set of parameters required to send the failure response.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation @throws MuleException
   *        exception thrown when processing the message to send the response.
   */
  void sendFailureResponseToClient(MessagingException exception, Map<String, Object> parameters,
                                   ResponseCompletionCallback responseCompletionCallback)
      throws MuleException;

  /**
   * Generates the response function parameters. This function is later used to generate the response parameters
   * and use them create a {@code Message} that will be routed through the source policy pipeline.
   *
   * @return a function to resolve the response function parameters.
   */
  Function<Event, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction();

  /**
   * Generates the failure response function parameters. This function is later used to generate the response parameters
   * and use them create a {@code Message} that will be routed through the source policy pipeline.
   *
   * @return a function to resolve the failure response function parameters.
   */
  Function<Event, Map<String, Object>> getFailedExecutionResponseParametersFunction();

}
