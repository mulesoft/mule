/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.lang.String.valueOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mule.runtime.dsl.api.component.config.ComponentIdentifier.ANNOTATION_PARAMETERS;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.InterceptionHandler;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.MessageProcessorPathResolver;
import org.mule.runtime.core.api.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.api.interception.ProcessorParameterResolver;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 */
public class ReactiveInterceptorAdapter implements BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>,
    FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveInterceptorAdapter.class);
  public static final String AROUND_METHOD_NAME = "around";

  private InterceptionHandler interceptionHandler;
  private FlowConstruct flowConstruct;

  public ReactiveInterceptorAdapter(InterceptionHandler interceptionHandler) {
    this.interceptionHandler = interceptionHandler;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public ReactiveProcessor apply(Processor component, ReactiveProcessor next) {
    //TODO move this logic to validate before building the interception chain for processing
    if (!isInterceptable(component)) {
      return next;
    }

    ComponentIdentifier componentIdentifier = ((AnnotatedObject) component).getIdentifier();
    ComponentLocation componentLocation =
        ((AnnotatedObject) component).getLocation(((MessageProcessorPathResolver) flowConstruct).getProcessorPath(component));
    Map<String, String> dslParameters = (Map<String, String>) ((AnnotatedObject) component).getAnnotation(ANNOTATION_PARAMETERS);
    LOGGER.warn("Applying interceptor: {} for componentLocation: {}", interceptionHandler, componentLocation.getPath());

    if (interceptionHandler.intercept(componentIdentifier, componentLocation)) {
      try {
        //if (interceptionHandler.getClass().getMethod(AROUND_METHOD_NAME, Map.class, InterceptionEvent.class, InterceptionAction.class).isDefault()) {
        if (!interceptionHandler.getClass()
            .getMethod(AROUND_METHOD_NAME, Map.class, InterceptionEvent.class, InterceptionAction.class).getDeclaringClass()
            .equals(interceptionHandler.getClass())) {
          return publisher -> from(publisher)
              .map(doBefore(component, dslParameters))
              .transform(next)
              .map(doAfter());
        } else {
          return publisher -> from(publisher)
              .map(doBefore(component, dslParameters))
              .flatMap(event -> fromFuture(doAround(event, component, dslParameters, next)))
              .map(doAfter());
        }
        //TODO change this!
      } catch (NoSuchMethodException e) {
        throw new MuleRuntimeException(e);
      }
    }

    return next;
  }

  private CompletableFuture<Event> doAround(Event event, Processor component, Map<String, String> dslParameters,
                                            ReactiveProcessor next) {
    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
    final ReactiveInterceptionAction reactiveInterceptionAction = new ReactiveInterceptionAction(event, next);
    interceptionHandler.around(resolveParameters(event, component, dslParameters), interceptionEvent,
                               reactiveInterceptionAction);
    //TODO Optional
    final CompletableFuture<Event> future = reactiveInterceptionAction.getFuture();
    if (future != null) {
      return future;
    }
    return completedFuture(interceptionEvent.resolve());
  }

  private Function<Event, Event> doAfter() {
    return event -> {
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
      interceptionHandler.after(interceptionEvent);
      return interceptionEvent.resolve();
    };
  }

  private Function<Event, Event> doBefore(Processor component, Map<String, String> dslParameters) {
    return event -> {
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
      interceptionHandler.before(resolveParameters(event, component, dslParameters), interceptionEvent);
      return interceptionEvent.resolve();
    };
  }

  private boolean isInterceptable(Processor component) {
    if (component instanceof AnnotatedObject) {
      ComponentLocation componentLocation =
          ((AnnotatedObject) component).getLocation(((MessageProcessorPathResolver) flowConstruct).getProcessorPath(component));
      if (componentLocation != null) {
        return true;
      }
    }
    return false;

  }

  private Map<String, Object> resolveParameters(Event event, Processor processor, Map<String, String> parameters) {
    if (processor instanceof ProcessorParameterResolver) {
      try {
        return ((ProcessorParameterResolver) processor).resolve(event);
      } catch (MuleException e) {
        //TODO Improve error message!
        throw new MuleRuntimeException(e);
      }
    }

    Map<String, Object> resolvedParameters = new HashMap<>();
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      Object value;
      String paramValue = entry.getValue();
      final MuleContext muleContext = flowConstruct.getMuleContext();
      if (muleContext.getExpressionManager().isExpression(paramValue)) {
        value = muleContext.getExpressionManager().evaluate(paramValue, event, flowConstruct).getValue();
      } else {
        value = valueOf(paramValue);
      }
      resolvedParameters.put(entry.getKey(), value);
    }
    return resolvedParameters;
  }
}
