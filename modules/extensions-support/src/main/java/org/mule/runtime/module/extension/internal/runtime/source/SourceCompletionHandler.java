/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.Map;

/**
 * Handles the result of flow processing.
 *
 * @since 4.0
 */
public interface SourceCompletionHandler {

  /**
   * Invoked on successful completion of flow processing.
   * <p>
   * Exceptions found while processing are to be notified through
   * the {@code exceptionCallback}, which might (depending on {@code HandledCompletionExceptionResult})
   * produce a new value as the result of handling such error
   *
   * @param event    the result of the flow execution
   * @param callback the callback to be completed as a result
   */
  void onCompletion(CoreEvent event, Map<String, Object> parameters, CompletableCallback<Void> callback);

  /**
   * Invoked when a failure occurs during the flow processing
   *
   * @param exception the exception thrown during processing
   * @param callback  the callback to be completed as a result
   */
  void onFailure(MessagingException exception, Map<String, Object> parameters, CompletableCallback<Void> callback);

  void onTerminate(Either<MessagingException, CoreEvent> eventOrException) throws Exception;

  /**
   * Resolves the set of parameters of the response function of the source against
   * the supplied {@code Event}.
   *
   * @param event the {@code Event} with the result of the successful flow processing.
   * @return the response function parameters with it's values.
   */
  Map<String, Object> createResponseParameters(CoreEvent event) throws MessagingException;

  /**
   * Resolves the set of parameters of the failure response function of the source against
   * the supplied {@code Event}.
   *
   * @param event the {@code Event} with the result of the failed flow processing.
   * @return the failed response function parameters with it's values.
   */
  Map<String, Object> createFailureResponseParameters(CoreEvent event) throws MessagingException;

}
