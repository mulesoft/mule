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
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_PARAMS;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.ParametersResolverProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.inject.Inject;

/**
 * Hooks the {@link ProcessorInterceptor}s for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveInterceptorAdapter implements BiFunction<Processor, ReactiveProcessor, ReactiveProcessor> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveInterceptorAdapter.class);

  private static final String INTERCEPTION_COMPONENT = "core:interceptionComponent";

  private static final String AROUND_METHOD_NAME = "around";

  @Inject
  private MuleContext muleContext;

  @Inject
  private ExtendedExpressionManager expressionManager;

  private ProcessorInterceptorFactory interceptorFactory;

  public ReactiveInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
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

    ReactiveProcessor interceptedProcessor;
    if (implementsAround(interceptor)) {
      LOGGER.info("Configuring interceptor '{}' around processor '{}'...", interceptor, componentLocation.getLocation());
      interceptedProcessor = publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .flatMapMany(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next))
              .onErrorMap(CompletionException.class, completionException -> completionException.getCause()))
          .onErrorMap(MessagingException.class, error -> {
            return createMessagingException(doAfter(interceptor, component, of(error.getCause())).apply(error.getEvent()),
                                            error.getCause(), (AnnotatedObject) component);
          })
          .map(doAfter(interceptor, component, empty()));
    } else {
      LOGGER.info("Configuring interceptor '{}' before and after processor '{}'...", interceptor,
                  componentLocation.getLocation());
      interceptedProcessor = publisher -> from(publisher)
          .map(doBefore(interceptor, component, dslParameters))
          .transform(next)
          .onErrorMap(MessagingException.class, error -> {
            return createMessagingException(doAfter(interceptor, component, of(error.getCause())).apply(error.getEvent()),
                                            error.getCause(), (AnnotatedObject) component);
          })
          .map(doAfter(interceptor, component, empty()));
    }

    LOGGER.info("Interceptor '{}' for processor '{}' configured.", interceptor, componentLocation.getLocation());
    return interceptedProcessor;
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

  private Function<InternalEvent, InternalEvent> doBefore(ProcessorInterceptor interceptor, Processor component,
                                                          Map<String, String> dslParameters) {
    return event -> {
      final InternalEvent eventWithResolvedParams = addResolvedParameters(event, component, dslParameters);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Calling before() for '{}' in processor '{}'...", interceptor,
                     ((AnnotatedObject) component).getLocation().getLocation());
      }

      try {
        interceptor.before(((AnnotatedObject) component).getLocation(), getResolvedParams(eventWithResolvedParams),
                           interceptionEvent);
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(new MessagingException(interceptionEvent.resolve(), e, (AnnotatedObject) component));
      }
    };
  }

  private CompletableFuture<InternalEvent> doAround(InternalEvent event, ProcessorInterceptor interceptor,
                                                    Processor component, Map<String, String> dslParameters,
                                                    ReactiveProcessor next) {
    final InternalEvent eventWithResolvedParams = addResolvedParameters(event, component, dslParameters);

    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
    final ReactiveInterceptionAction reactiveInterceptionAction =
        new ReactiveInterceptionAction(interceptionEvent, next, component, muleContext.getErrorTypeLocator());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Calling around() for '{}' in processor '{}'...", interceptor,
                   ((AnnotatedObject) component).getLocation().getLocation());
    }

    try {
      return interceptor
          .around(((AnnotatedObject) component).getLocation(), getResolvedParams(eventWithResolvedParams), interceptionEvent,
                  reactiveInterceptionAction)
          .exceptionally(t -> {
            if (t instanceof MessagingException) {
              throw new CompletionException(t);
            } else {
              throw new CompletionException(createMessagingException(eventWithResolvedParams,
                                                                     t instanceof CompletionException ? t.getCause() : t,
                                                                     ((AnnotatedObject) component)));
            }
          })
          .thenApply(interceptedEvent -> ((DefaultInterceptionEvent) interceptedEvent).resolve());
    } catch (Exception e) {
      throw propagate(createMessagingException(interceptionEvent.resolve(), e, (AnnotatedObject) component));
    }
  }

  private Map<String, ProcessorParameterValue> getResolvedParams(final InternalEvent eventWithResolvedParams) {
    return (Map<String, ProcessorParameterValue>) eventWithResolvedParams.getInternalParameters()
        .get(INTERCEPTION_RESOLVED_PARAMS);
  }

  private Function<InternalEvent, InternalEvent> doAfter(ProcessorInterceptor interceptor, Processor component,
                                                         Optional<Throwable> thrown) {
    return event -> {
      final InternalEvent eventWithResolvedParams = removeResolvedParameters(event);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Calling after() for '{}' in processor '{}'...", interceptor,
                     ((AnnotatedObject) component).getLocation().getLocation());
      }

      try {
        interceptor.after(((AnnotatedObject) component).getLocation(), interceptionEvent, thrown);
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(createMessagingException(interceptionEvent.resolve(), e, (AnnotatedObject) component));
      }
    };
  }

  private boolean isInterceptable(Processor component) {
    return ((AnnotatedObject) component).getLocation() != null;
  }

  private InternalEvent addResolvedParameters(InternalEvent event, Processor component, Map<String, String> dslParameters) {
    boolean sameComponent = event.getInternalParameters().containsKey(INTERCEPTION_COMPONENT)
        ? component.equals(event.getInternalParameters().get(INTERCEPTION_COMPONENT))
        : false;

    if (!sameComponent || !event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_PARAMS)) {
      return resolveParameters(removeResolvedParameters(event), component, dslParameters);
    } else {
      return event;
    }
  }

  private InternalEvent removeResolvedParameters(InternalEvent event) {
    if (event.getInternalParameters().containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
      Processor processor = (Processor) event.getInternalParameters().get(INTERCEPTION_COMPONENT);

      if (processor instanceof ParametersResolverProcessor) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Disposing resolved parameters for processor {}...",
                       ((AnnotatedObject) processor).getLocation().getLocation());
        }

        ((ParametersResolverProcessor) processor)
            .disposeResolvedParameters((ExecutionContext<OperationModel>) event.getInternalParameters()
                .get(INTERCEPTION_RESOLVED_CONTEXT));
      }
    }

    return InternalEvent.builder(event)
        .removeInternalParameter(INTERCEPTION_RESOLVED_PARAMS)
        .removeInternalParameter(INTERCEPTION_COMPONENT)
        .removeInternalParameter(INTERCEPTION_RESOLVED_CONTEXT)
        .build();
  }

  private InternalEvent resolveParameters(InternalEvent event, Processor processor, Map<String, String> parameters) {
    Map<String, ProcessorParameterValue> resolvedParameters = new HashMap<>();
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      String providedValue = entry.getValue();
      resolvedParameters.put(entry.getKey(), new DefaultProcessorParameterValue(entry.getKey(), providedValue, () -> {
        // By using a lambda here the evaluation is deferred until it is actually needed.
        // This not only avoids evaluating expressions which result may not be used, but also avoids
        // handling exceptions here in the interceptor adapter code. Any exception is to be handling by the interceptor
        // implementation
        if (expressionManager.isExpression(providedValue)) {
          return expressionManager.evaluate(providedValue, event, ((AnnotatedObject) processor).getLocation()).getValue();
        } else {
          return valueOf(providedValue);
        }
      }));
    }

    InternalEvent.Builder builder = InternalEvent.builder(event);

    if (processor instanceof ParametersResolverProcessor) {
      try {
        ((ParametersResolverProcessor) processor).resolveParameters(builder, (params, context) -> {
          resolvedParameters.putAll(params.entrySet().stream()
              .collect(toMap(e -> e.getKey(), e -> new DefaultProcessorParameterValue(e.getKey(), null, () -> e.getValue()))));
          Map<String, Object> interceptionEventParams = new HashMap<>();
          interceptionEventParams.put(INTERCEPTION_RESOLVED_CONTEXT, context);
          interceptionEventParams.put(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters);
          interceptionEventParams.put(INTERCEPTION_COMPONENT, processor);

          builder.internalParameters(interceptionEventParams);
        });
      } catch (MuleException e) {
        throw new InterceptionException(e);
      }
    } else {
      Map<String, Object> interceptionEventParams = new HashMap<>();
      interceptionEventParams.put(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters);
      interceptionEventParams.put(INTERCEPTION_COMPONENT, processor);
      builder.internalParameters(interceptionEventParams);
    }

    return builder.build();
  }

  private MessagingException createMessagingException(InternalEvent event, Throwable cause, AnnotatedObject processor) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    MessagingException me = new MessagingException(event, cause, processor);

    return exceptionResolver.resolve(me, muleContext);
  }

}
