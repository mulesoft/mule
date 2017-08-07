/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.interception;

import static org.mule.runtime.internal.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.internal.el.BindingContextUtils.addEventBindings;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.message.ErrorBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * This implementation is not thread-safe.
 *
 * @since 4.0
 */
public class DefaultInterceptionEvent implements InternalInterceptionEvent {

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
  public Map<String, TypedValue<?>> getVariables() {
    return interceptedInput.getVariables();
  }

  @Override
  public Map<String, TypedValue<?>> getProperties() {
    return interceptedInput.getProperties();
  }

  @Override
  public Map<String, TypedValue<?>> getParameters() {
    return interceptedInput.getParameters();
  }

  @Override
  public Optional<Error> getError() {
    return interceptedInput.getError();
  }

  @Override
  public org.mule.runtime.api.event.EventContext getContext() {
    return interceptedInput.getContext();
  }

  @Override
  public SecurityContext getSecurityContext() {
    return interceptedInput.getSecurityContext();
  }

  @Override
  public String getCorrelationId() {
    return interceptedInput.getCorrelationId();
  }

  @Override
  public MuleSession getSession() {
    return interceptedInput.getSession();
  }

  @Override
  public DefaultInterceptionEvent message(Message message) {
    interceptedOutput = interceptedOutput.message(message).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public DefaultInterceptionEvent variables(Map<String, Object> variables) {
    interceptedOutput = interceptedOutput.variables(variables).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public DefaultInterceptionEvent addVariable(String key, Object value) {
    interceptedOutput = interceptedOutput.addVariable(key, value).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public DefaultInterceptionEvent addVariable(String key, Object value, DataType mediaType) {
    interceptedOutput =
        interceptedOutput.addVariable(key, value, mediaType).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public DefaultInterceptionEvent removeVariable(String key) {
    interceptedOutput = interceptedOutput.removeVariable(key).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public DefaultInterceptionEvent session(MuleSession session) {
    interceptedOutput = interceptedOutput.session(session).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  public DefaultInterceptionEvent setError(ErrorType errorType, Throwable cause) {
    ErrorBuilder errorBuilder = ErrorBuilder.builder(cause);
    errorBuilder.errorType(errorType);

    interceptedOutput = interceptedOutput.error(errorBuilder.build()).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    return this;
  }

  @Override
  public BindingContext asBindingContext() {
    return addEventBindings(resolve(), NULL_BINDING_CONTEXT);
  }

  /**
   * Updates the state of this object if needed, overriding the {@code interceptedInput} with the result built from
   * {@code interceptedOutput} and resetting {@code interceptedOutput}.
   *
   * @return {@link Event} with the result.
   */
  public Event resolve() {
    final Event newEvent = interceptedOutput.build();
    if (interceptedInput != newEvent) {
      interceptedInput = newEvent;
      interceptedOutput = Event.builder(interceptedInput).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
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
