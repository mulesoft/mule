/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.interception;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.interception.InternalInterceptionEvent;

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

  private InternalEvent interceptedInput;
  private InternalEvent.Builder interceptedOutput;

  public DefaultInterceptionEvent(InternalEvent interceptedInput) {
    reset(interceptedInput);
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
  public Optional<Error> getError() {
    return interceptedInput.getError();
  }

  @Override
  public String getCorrelationId() {
    return interceptedInput.getCorrelationId();
  }

  @Override
  public EventContext getContext() {
    return interceptedInput.getContext();
  }

  @Override
  public Optional<Authentication> getAuthentication() {
    return interceptedInput.getAuthentication();
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
   * @return {@link PrivilegedEvent} with the result.
   */
  @Override
  public InternalEvent resolve() {
    final InternalEvent newEvent = interceptedOutput.build();
    if (interceptedInput != newEvent) {
      interceptedInput = newEvent;
      interceptedOutput = InternalEvent.builder(interceptedInput).removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    }
    return interceptedInput;
  }

  /**
   * @return the output of {@link #interceptedOutput#build()}.
   */
  public InternalEvent getInterceptionResult() {
    return interceptedInput;
  }

  public void reset(InternalEvent newEvent) {
    this.interceptedInput = newEvent;
    this.interceptedOutput = InternalEvent.builder(newEvent);
  }
}
