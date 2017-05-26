/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Collections.emptyMap;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.ExceptionCallback;

import java.util.Map;

/**
 * {@code SourceCompletionHandler} that does nothing.
 *
 * @since 4.0
 */
public class NullSourceCompletionHandler implements SourceCompletionHandler {

  @Override
  public void onCompletion(Event event, Map<String, Object> parameters, ExceptionCallback<Throwable> exceptionCallback) {
    // Nothing to do.
  }

  @Override
  public void onFailure(MessagingException exception, Map<String, Object> parameters) {
    // Nothing to do.
  }

  @Override
  public void onTerminate(Either<Event, MessagingException> eventOrException) {

  }

  @Override
  public Map<String, Object> createResponseParameters(Event event) {
    return emptyMap();
  }

  @Override
  public Map<String, Object> createFailureResponseParameters(Event event) {
    return emptyMap();
  }

  @Override
  public Map<String, Object> createTerminateResponseParameters(Event event) {
    return emptyMap();
  }
}
