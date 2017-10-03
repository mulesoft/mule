/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.MessageSourceResponseParametersProcessor;

import org.reactivestreams.Publisher;

import java.util.Map;

/**
 * Template methods for {@link MessageSource} specific behavior during flow execution.
 */
public interface ModuleFlowProcessingPhaseTemplate extends MessageProcessTemplate, MessageSourceResponseParametersProcessor {

  /**
   * @return a {@link Message} created from the original message
   */
  Message getMessage();

  /**
   * Routes the {@link CoreEvent} through the processors chain
   *
   * @param event {@link CoreEvent} created from the raw message of this context
   * @return the response {@link CoreEvent}
   * @throws MuleException
   */
  CoreEvent routeEvent(CoreEvent event) throws MuleException;

  /**
   * Routes the {@link CoreEvent} through the processors chain using async API.
   *
   * @param event {@link CoreEvent} created from the raw message of this context
   * @return the {@link Publisher} that will ne siganlled on processing completion
   */
  Publisher<CoreEvent> routeEventAsync(CoreEvent event);

  /**
   * Template method to send a response after processing the message.
   * <p>
   * This method is executed within the flow so if it fails it will trigger the exception strategy.
   * 
   * @param response the result of the flow execution
   * @param parameters the resolved set of parameters required to send the response.
   * @return void publisher that will signal the success or failure of sending response to client.
   */
  Publisher<Void> sendResponseToClient(CoreEvent response, Map<String, Object> parameters);


  /**
   * Template method to send a failure response after processing the message.
   *
   * @param exception exception thrown during the flow execution.
   * @param parameters the resolved set of parameters required to send the failure response.
   * @return void publisher that will signal the success or failure of sending failure response to client.
   */
  Publisher<Void> sendFailureResponseToClient(MessagingException exception, Map<String, Object> parameters);

  /**
   * Template method to be executed after the flow completes it's execution including any policy that may be applied.
   * <p/>
   * This method will always be executed and the {@code either} parameter will indicate the result of the execution.
   * 
   * @param either that communicates the result of the flow execution.
   *        <ul>
   *        <li>{@link CoreEvent} if the execution finished correctly</li>
   *        <li>{@link MessagingException} if an error occurred during the execution</li>
   *        </ul>
   */
  void afterPhaseExecution(Either<MessagingException, CoreEvent> either);
}
