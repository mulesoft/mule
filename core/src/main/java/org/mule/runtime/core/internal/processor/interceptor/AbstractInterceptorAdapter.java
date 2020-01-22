/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.util.collection.SmallMap.forSize;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_COMPONENT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_PARAMS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Provides common interception functionality.
 */
class AbstractInterceptorAdapter {

  @Inject
  private MuleContext muleContext;

  @Inject
  protected ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  protected ExtendedExpressionManager expressionManager;

  protected Map<String, ProcessorParameterValue> getResolvedParams(final InternalEvent eventWithResolvedParams) {
    Map<String, ProcessorParameterValue> params = eventWithResolvedParams.getInternalParameter(INTERCEPTION_RESOLVED_PARAMS);
    if (params == null) {
      return emptyMap();
    }

    return params;
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
    Map<String, ProcessorParameterValue> resolvedParameters = forSize(parameters.size());
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

    return setInternalParamsForNotParamResolver(component, resolvedParameters, event, InternalEvent.builder(event));
  }

  protected InternalEvent setInternalParamsForNotParamResolver(Component component,
                                                               Map<String, ProcessorParameterValue> resolvedParameters,
                                                               InternalEvent event, InternalEvent.Builder builder) {
    return quickCopy(event, of(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters,
                               INTERCEPTION_COMPONENT, component));
  }

  protected MessagingException createMessagingException(CoreEvent event, Throwable cause, Component processor,
                                                        Optional<MessagingException> original) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    MessagingException me = new MessagingException(event, cause, processor);
    original.ifPresent(error -> error.getInfo().forEach(me::addInfo));

    return exceptionResolver.resolve(me, errorTypeLocator, exceptionContextProviders);
  }

  protected MuleContext getMuleContext() {
    return muleContext;
  }

}
