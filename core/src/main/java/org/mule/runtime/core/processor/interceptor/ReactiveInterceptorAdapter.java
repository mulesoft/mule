/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.interceptor;

import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Hooks the {@link ProcessorInterceptor}s for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveInterceptorAdapter
    implements BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>, FlowConstructAware {

  private static final String AROUND_METHOD_NAME = "around";

  private ProcessorInterceptorFactory interceptorFactory;
  private FlowConstruct flowConstruct;

  public ReactiveInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public ReactiveProcessor apply(Processor component, ReactiveProcessor next) {
    if (!isInterceptable(component) || !interceptorFactory.intercept(((AnnotatedObject) component).getLocation())) {
      return next;
    }

    final ProcessorInterceptor interceptor = interceptorFactory.get();
    Map<String, String> dslParameters = (Map<String, String>) ((AnnotatedObject) component).getAnnotation(ANNOTATION_PARAMETERS);
    if (implementsAround(interceptor)) {
      return publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .flatMap(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next))
              .onErrorMap(CompletionException.class, completionException -> completionException.getCause()))
          .doOnError(MessagingException.class, error -> {
            interceptor.after(new DefaultInterceptionEvent(error.getEvent()), of(error.getCause()));
          })
          .map(doAfter(interceptor));
    } else {
      return publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .transform(next)
          .doOnError(MessagingException.class,
                     error -> interceptor.after(new DefaultInterceptionEvent(error.getEvent()), of(error.getCause())))
          .map(doAfter(interceptor));
    }
  }

  private boolean implementsAround(ProcessorInterceptor interceptor) {
    try {
      return !interceptor.getClass().getMethod(AROUND_METHOD_NAME, Map.class, InterceptionEvent.class, InterceptionAction.class)
          .isDefault();
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Function<Event, Event> doBefore(ProcessorInterceptor interceptor, Processor component,
                                          Map<String, String> dslParameters) {
    return event -> {
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
      interceptor.before(resolveParameters(event, component, dslParameters), interceptionEvent);
      return interceptionEvent.resolve();
    };
  }

  private CompletableFuture<Event> doAround(Event event, ProcessorInterceptor interceptor, Processor component,
                                            Map<String, String> dslParameters,
                                            ReactiveProcessor next) {
    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
    final ReactiveInterceptionAction reactiveInterceptionAction =
        new ReactiveInterceptionAction(interceptionEvent, next, component, flowConstruct.getMuleContext());
    return interceptor.around(resolveParameters(event, component, dslParameters), interceptionEvent,
                              reactiveInterceptionAction)
        .exceptionally(t -> {
          if (t instanceof MessagingException) {
            throw new CompletionException(t);
          } else {
            throw new CompletionException(new MessagingException(event, t.getCause(), component));
          }
        })
        .thenApply(interceptedEvent -> ((DefaultInterceptionEvent) interceptedEvent).resolve());
  }

  private Function<Event, Event> doAfter(ProcessorInterceptor interceptor) {
    return event -> {
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
      interceptor.after(interceptionEvent, empty());
      return interceptionEvent.resolve();
    };
  }

  private boolean isInterceptable(Processor component) {
    if (component instanceof AnnotatedObject) {
      ComponentLocation componentLocation =
          ((AnnotatedObject) component).getLocation();
      if (componentLocation != null) {
        return true;
      }
    }
    return false;

  }

  private Map<String, Object> resolveParameters(Event event, Processor processor, Map<String, String> parameters) {
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

    // TODO MULE-11527 avoid doing unnecesary evaluations
    if (processor instanceof ParametersResolverProcessor) {
      resolvedParameters.putAll(((ParametersResolverProcessor) processor).resolveParameters(event));
    }
    return resolvedParameters;
  }
}
