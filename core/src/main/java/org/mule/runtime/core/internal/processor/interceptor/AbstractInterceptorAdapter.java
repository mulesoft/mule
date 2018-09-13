/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.String.valueOf;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_PARAMS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Provides common interception functionality.
 */
class AbstractInterceptorAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractInterceptorAdapter.class);

  protected static final String INTERCEPTION_COMPONENT = "core:interceptionComponent";

  @Inject
  private MuleContext muleContext;

  @Inject
  protected ExtendedExpressionManager expressionManager;

  protected Map<String, ProcessorParameterValue> getResolvedParams(final InternalEvent eventWithResolvedParams) {
    return (Map<String, ProcessorParameterValue>) eventWithResolvedParams.getInternalParameters()
        .get(INTERCEPTION_RESOLVED_PARAMS);
  }

  protected InternalEvent addResolvedParameters(InternalEvent event, Component component, Map<String, String> dslParameters) {
    boolean sameComponent = internalParametersFrom(event).containsKey(INTERCEPTION_COMPONENT)
        ? component.equals(internalParametersFrom(event).get(INTERCEPTION_COMPONENT))
        : false;

    if (!sameComponent || !internalParametersFrom(event).containsKey(INTERCEPTION_RESOLVED_PARAMS)) {
      return resolveParameters(removeResolvedParameters(event), component, dslParameters);
    } else {
      return event;
    }
  }

  protected InternalEvent removeResolvedParameters(InternalEvent event) {
    return InternalEvent.builder(event)
        .removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS)
        .removeInternalParameter(INTERCEPTION_COMPONENT)
        .removeInternalParameter(INTERCEPTION_RESOLVED_CONTEXT)
        .build();
  }

  protected Map<String, ?> internalParametersFrom(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameters();
  }

  protected InternalEvent resolveParameters(InternalEvent event, Component component, Map<String, String> parameters) {
    Map<String, ProcessorParameterValue> resolvedParameters = new HashMap<>();
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      String providedValue = entry.getValue();
      resolvedParameters.put(entry.getKey(), new DefaultProcessorParameterValue(entry.getKey(), providedValue, () -> {
        // By using a lambda here the evaluation is deferred until it is actually needed.
        // This not only avoids evaluating expressions which result may not be used, but also avoids
        // handling exceptions here in the interceptor adapter code. Any exception is to be handling by the interceptor
        // implementation
        if (expressionManager.isExpression(providedValue)) {
          return expressionManager.evaluate(providedValue, event, component.getLocation()).getValue();
        } else {
          return valueOf(providedValue);
        }
      }));
    }

    InternalEvent.Builder builder = InternalEvent.builder(event);

    setInternalParamsForNotParamResolver(component, resolvedParameters, builder);

    return builder.build();
  }

  protected void setInternalParamsForNotParamResolver(Component component,
                                                      Map<String, ProcessorParameterValue> resolvedParameters,
                                                      InternalEvent.Builder builder) {
    Map<String, Object> interceptionEventParams = new HashMap<>();
    interceptionEventParams.put(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters);
    interceptionEventParams.put(INTERCEPTION_COMPONENT, component);
    builder.internalParameters(interceptionEventParams);
  }

  protected MessagingException createMessagingException(CoreEvent event, Throwable cause, Component processor,
                                                        Optional<MessagingException> original) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    MessagingException me = new MessagingException(event, cause, processor);
    original.ifPresent(error -> error.getInfo().forEach((name, info) -> me.addInfo(name, info)));

    return exceptionResolver.resolve(me, ((PrivilegedMuleContext) muleContext).getErrorTypeLocator(),
                                     muleContext.getExceptionContextProviders());
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

}
