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
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hooks the {@link ProcessorInterceptor}s for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveInterceptorAdapter
    implements BiFunction<Processor, Function<Publisher<Event>, Publisher<Event>>, Function<Publisher<Event>, Publisher<Event>>>,
    FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveInterceptorAdapter.class);
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
  public Function<Publisher<Event>, Publisher<Event>> apply(Processor component,
                                                            Function<Publisher<Event>, Publisher<Event>> next) {
    if (!isInterceptable(component)) {
      return next;
    }


    // LOGGER.debug("Applying interceptor: {} for componentLocation: {}", interceptor, componentLocation.getLocation());

    if (interceptorFactory.intercept(((AnnotatedObject) component).getLocation())) {
      final ProcessorInterceptor interceptor = interceptorFactory.get();
      Map<String, String> dslParameters =
          (Map<String, String>) ((AnnotatedObject) component).getAnnotation(ANNOTATION_PARAMETERS);
      if (implementsAround(interceptor)) {
        return publisher -> from(publisher)
            .map(doBefore(interceptor, component, dslParameters))
            .flatMap(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next))
                .mapError(CompletionException.class, completionException -> completionException.getCause()))
            .doOnError(MessagingException.class, error -> {
              interceptor.after(new DefaultInterceptionEvent(error.getEvent()), of(error.getCause()));
            })
            .map(doAfter(interceptor));
      } else {
        return publisher -> from(publisher)
            .map(doBefore(interceptor, component, dslParameters))
            .transform(next)
            .doOnError(MessagingException.class, error -> {
              interceptor.after(new DefaultInterceptionEvent(error.getEvent()), of(error.getCause()));
            })
            .map(doAfter(interceptor));
      }
    } else {
      return next;
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
                                            Function<Publisher<Event>, Publisher<Event>> next) {
    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(event);
    final ReactiveInterceptionAction reactiveInterceptionAction = new ReactiveInterceptionAction(interceptionEvent, next);
    return interceptor.around(resolveParameters(event, component, dslParameters), interceptionEvent,
                              reactiveInterceptionAction)
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
    // TODO MULE-11567 properly get SDK operation parameters
    // TODO MULE-11527 avoid doing unnecesary evaluations

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
