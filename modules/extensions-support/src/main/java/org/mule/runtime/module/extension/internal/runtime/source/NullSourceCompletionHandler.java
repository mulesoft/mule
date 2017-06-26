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
import org.mule.runtime.core.api.exception.MessagingException;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * {@code SourceCompletionHandler} that does nothing.
 *
 * @since 4.0
 */
public class NullSourceCompletionHandler implements SourceCompletionHandler {

  @Override
  public Publisher<Void> onCompletion(Event event, Map<String, Object> parameters) {
    return Mono.empty();
  }

  @Override
  public Publisher<Void> onFailure(MessagingException exception, Map<String, Object> parameters) {
    return Mono.empty();
  }

  @Override
  public void onTerminate(Either<MessagingException, Event> eventOrException) {
    // Nothing to do.
  }

  @Override
  public Map<String, Object> createResponseParameters(Event event) {
    return emptyMap();
  }

  @Override
  public Map<String, Object> createFailureResponseParameters(Event event) {
    return emptyMap();
  }
}
