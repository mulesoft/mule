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
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.MessageSourceResponseParametersProcessor;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface ModuleFlowProcessingPhaseTemplate extends MessageProcessTemplate, MessageSourceResponseParametersProcessor {

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
   */
  Event routeEvent(Event event) throws MuleException;

  /**
   * Routes the {@link Event} through the processors chain using async API.
   *
   * @param event {@link Event} created from the raw message of this context
   * @return the {@link Publisher} that will ne siganlled on processing completion
   */
  Publisher<Event> routeEventAsync(Event event);

  /**
   * Template method to send a response after processing the message.
   * <p>
   * This method is executed within the flow so if it fails it will trigger the exception strategy.
   *
   * @param flowExecutionResponse the result of the flow execution
   * @param parameters the resolved set of parameters required to send the response.
   * @param errorResponseParametersFunction function that generates the error parameters to be sent in the case of a failure.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation
   */
  Publisher<Void> sendResponseToClient(Event flowExecutionResponse, Map<String, Object> parameters,
                                       Function<Event, Map<String, Object>> errorResponseParametersFunction,
                                       ResponseCompletionCallback responseCompletionCallback);


  /**
   * @param exception exception thrown during the flow execution.
   * @param parameters the resolved set of parameters required to send the failure response.
   * @param responseCompletionCallback callback to be used for notifying the result of the operation
   */
  Publisher<Void> sendFailureResponseToClient(MessagingException exception, Map<String, Object> parameters,
                                              ResponseCompletionCallback responseCompletionCallback);

  /**
   * @param either that communicates the result of the flow execution.
   *               <ul>
   *               <li>{@link Event} if the execution finished correctly</li>
   *               <li>{@link MessagingException} if an error occurred during the execution</li>
   *               </ul>
   */
  void sendAfterTerminateResponseToClient(Either<MessagingException, Event> either);
}
