/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.interception;

import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This implementation is not thread-safe.
 *
 * @since 4.0
 */
public class DefaultInterceptionEvent implements InterceptionEvent {

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

  @Override
  public DefaultInterceptionEvent error(Error error) {
    interceptedOutput = interceptedOutput.error(error);
    return this;
  }

  /**
   * Updates the state of this object, overriding the {@link #interceptedInput} with the result of
   * {@link #interceptedOutput#build()} and resetting {@link #interceptedOutput}.
   *
   * @return {@link Event} with the result.
   */
  public Event resolve() {
    interceptedInput = interceptedOutput.build();
    interceptedOutput = Event.builder(interceptedInput);
    return interceptedInput;
  }

  /**
   * @return the output of {@link #interceptedOutput#build()}.
   */
  public Event getInterceptionResult() {
    return interceptedInput;
  }
}
