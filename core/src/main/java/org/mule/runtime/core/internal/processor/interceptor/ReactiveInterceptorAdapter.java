/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.util.ExceptionUtils.updateMessagingExceptionWithError;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_PARAMS;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor.ParametersResolverProcessorResult;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    implements BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>, MuleContextAware {

  private static final String INTERCEPTION_COMPONENT = "core:interceptionComponent";

  private static final String AROUND_METHOD_NAME = "around";

  private ProcessorInterceptorFactory interceptorFactory;
  private MuleContext muleContext;

  public ReactiveInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public ReactiveProcessor apply(Processor component, ReactiveProcessor next) {
    if (!isInterceptable(component)) {
      return next;
    }

    final ComponentLocation componentLocation = ((AnnotatedObject) component).getLocation();
    if (!interceptorFactory.intercept(componentLocation)) {
      return next;
    }

    final ProcessorInterceptor interceptor = interceptorFactory.get();
    Map<String, String> dslParameters = (Map<String, String>) ((AnnotatedObject) component).getAnnotation(ANNOTATION_PARAMETERS);
    if (implementsAround(interceptor)) {
      return publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .flatMapMany(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next))
              .onErrorMap(CompletionException.class, completionException -> completionException.getCause()))
          .doOnError(MessagingException.class, error -> {
            doAfter(componentLocation, interceptor, of(error.getCause())).apply(error.getEvent());
          })
          .map(doAfter(componentLocation, interceptor, empty()));
    } else {
      return publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .transform(next)
          .doOnError(MessagingException.class, error -> {
            doAfter(componentLocation, interceptor, of(error.getCause())).apply(error.getEvent());
          })
          .map(doAfter(componentLocation, interceptor, empty()));
    }
  }

  private boolean implementsAround(ProcessorInterceptor interceptor) {
    try {
      return !interceptor.getClass()
          .getMethod(AROUND_METHOD_NAME, ComponentLocation.class, Map.class, InterceptionEvent.class, InterceptionAction.class)
          .isDefault();
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Function<Event, Event> doBefore(ProcessorInterceptor interceptor, Processor component,
                                          Map<String, String> dslParameters) {
    return event -> {
      final Event eventWithResolvedParams =
          addResolvedParameters(event, component, dslParameters);

      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
      interceptor.before(((AnnotatedObject) component).getLocation(), getResolvedParams(eventWithResolvedParams),
                         interceptionEvent);
      return interceptionEvent.resolve();
    };
  }

  private CompletableFuture<Event> doAround(Event event, ProcessorInterceptor interceptor,
                                            Processor component, Map<String, String> dslParameters, ReactiveProcessor next) {
    final Event eventWithResolvedParams =
        addResolvedParameters(event, component, dslParameters);

    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
    final ReactiveInterceptionAction reactiveInterceptionAction =
        new ReactiveInterceptionAction(interceptionEvent, next, component, muleContext);
    return interceptor
        .around(((AnnotatedObject) component).getLocation(), getResolvedParams(eventWithResolvedParams), interceptionEvent,
                reactiveInterceptionAction)
        .exceptionally(t -> {
          if (t instanceof MessagingException) {
            throw new CompletionException(t);
          } else {
            throw new CompletionException(updateMessagingExceptionWithError(new MessagingException(eventWithResolvedParams,
                                                                                                   t.getCause(), component),
                                                                            component, muleContext));
          }
        })
        .thenApply(interceptedEvent -> ((DefaultInterceptionEvent) interceptedEvent).resolve());
  }

  private Map<String, Object> getResolvedParams(final Event eventWithResolvedParams) {
    return (Map<String, Object>) eventWithResolvedParams.getInternalParameters().get(INTERCEPTION_RESOLVED_PARAMS);
  }

  private Function<Event, Event> doAfter(ComponentLocation componentLocation, ProcessorInterceptor interceptor,
                                         Optional<Throwable> thrown) {
    return event -> {
      final Event eventWithResolvedParams = removeResolvedParameters(event);

      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
      interceptor.after(componentLocation, interceptionEvent, thrown);
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

  private Event addResolvedParameters(Event event, Processor component, Map<String, String> dslParameters) {
    boolean sameComponent = event.getInternalParameters().containsKey(INTERCEPTION_COMPONENT)
        ? component.equals(event.getInternalParameters().get(INTERCEPTION_COMPONENT)) : false;

    if (!sameComponent || !event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_PARAMS)) {
      return resolveParameters(removeResolvedParameters(event), component, dslParameters);
    } else {
      return event;
    }
  }

  private Event removeResolvedParameters(Event event) {
    if (event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
      Processor processor = (Processor) event.getInternalParameters().get(INTERCEPTION_COMPONENT);

      if (processor instanceof ParametersResolverProcessor) {
        ((ParametersResolverProcessor) processor)
            .disposeResolvedParameters((ExecutionContext<OperationModel>) event.getInternalParameters()
                .get(INTERCEPTION_RESOLVED_CONTEXT));
      }
    }

    return Event.builder(event)
        .removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS)
        .removeInternalParameter(INTERCEPTION_COMPONENT)
        .removeInternalParameter(INTERCEPTION_RESOLVED_CONTEXT)
        .build();
  }

  private Event resolveParameters(Event event, Processor processor, Map<String, String> parameters) {
    Map<String, Object> resolvedParameters = new HashMap<>();
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      Object value;
      String paramValue = entry.getValue();
      if (muleContext.getExpressionManager().isExpression(paramValue)) {
        value = muleContext.getExpressionManager().evaluate(paramValue, event, ((AnnotatedObject) processor).getLocation())
            .getValue();
      } else {
        value = valueOf(paramValue);
      }
      resolvedParameters.put(entry.getKey(), value);
    }

    Map<String, Object> interceptionEventParams = new HashMap<>();

    if (processor instanceof ParametersResolverProcessor) {
      try {
        ParametersResolverProcessorResult resolveParameters = ((ParametersResolverProcessor) processor).resolveParameters(event);
        resolvedParameters.putAll(resolveParameters.getParameters());
        interceptionEventParams.put(INTERCEPTION_RESOLVED_CONTEXT, resolveParameters.getContext());
      } catch (MuleException e) {
        throw new InterceptionException(e);
      }
    }

    interceptionEventParams.put(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters);
    interceptionEventParams.put(INTERCEPTION_COMPONENT, processor);

    return Event.builder(event).internalParameters(interceptionEventParams).build();
  }
}
