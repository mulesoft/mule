/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.interception;

import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.message.ErrorBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This implementation is not thread-safe.
 *
 * @since 4.0
 */
public class DefaultInterceptionEvent implements InterceptionEvent {

  public static final String INTERCEPTION_RESOLVED_PARAMS = "core:interceptionResolvedParams";
  public static final String INTERCEPTION_RESOLVED_CONTEXT = "core:interceptionResolvedContext";

  private Event interceptedInput;
  private Event.Builder interceptedOutput;

  public DefaultInterceptionEvent(Event interceptedInput) {
    this.interceptedInput = interceptedInput;
    this.interceptedOutput = Event.builder(interceptedInput);
  }

  @Override
  public Message getMessage() {
    return interceptedInput.getMessage();
  }

  @Override
  public <T> TypedValue<T> getVariable(String key) {
    return interceptedInput.getVariable(key);
  }

  @Override
  public Set<String> getVariableNames() {
    return interceptedInput.getVariableNames();
  }

  @Override
  public Optional<Error> getError() {
    return interceptedInput.getError();
  }

  /**
   * @return the context applicable to all events created from the same root {@link Event} from a {@link MessageSource}.
   */
  public EventContext getContext() {
    return interceptedInput.getContext();
  }

  @Override
  public DefaultInterceptionEvent message(Message message) {
    interceptedOutput = interceptedOutput.message(message);
    return this;
  }

  @Override
  public DefaultInterceptionEvent variables(Map<String, Object> variables) {
    interceptedOutput = interceptedOutput.variables(variables);
    return this;
  }

  @Override
  public DefaultInterceptionEvent addVariable(String key, Object value) {
    interceptedOutput = interceptedOutput.addVariable(key, value);
    return this;
  }

  @Override
  public DefaultInterceptionEvent addVariable(String key, Object value, DataType mediaType) {
    interceptedOutput = interceptedOutput.addVariable(key, value, mediaType);
    return this;
  }

  @Override
  public DefaultInterceptionEvent removeVariable(String key) {
    interceptedOutput = interceptedOutput.removeVariable(key);
    return this;
  }

  public DefaultInterceptionEvent setError(ErrorType errorType, Throwable cause) {
    ErrorBuilder errorBuilder = ErrorBuilder.builder(cause);
    errorBuilder.errorType(errorType);

    interceptedOutput = interceptedOutput.error(errorBuilder.build());
    return this;
  }

  /**
   * Updates the state of this object if needed, overriding the {@code interceptedInput} with the result built from
   * {@code interceptedOutput} and resetting {@codeinterceptedOutput}.
   *
   * @return {@link Event} with the result.
   */
  public Event resolve() {
    final Event newEvent = interceptedOutput.build();
    if (interceptedInput != newEvent) {
      interceptedInput = newEvent;
      interceptedOutput = Event.builder(interceptedInput).removeParameter(INTERCEPTION_RESOLVED_PARAMS);
    }
    return interceptedInput;
  }

  /**
   * @return the output of {@link #interceptedOutput#build()}.
   */
  public Event getInterceptionResult() {
    return interceptedInput;
  }
}
