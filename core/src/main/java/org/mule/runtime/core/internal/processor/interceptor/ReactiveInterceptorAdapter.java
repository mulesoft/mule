/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.util.collection.SmallMap.forSize;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_COMPONENT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_CONTEXT;
import static org.mule.runtime.core.internal.interception.DefaultInterceptionEvent.INTERCEPTION_RESOLVED_PARAMS;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.core.internal.processor.ParametersResolverProcessor;
import org.mule.runtime.core.internal.processor.simple.ParseTemplateProcessor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hooks the {@link ProcessorInterceptor}s for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveInterceptorAdapter extends AbstractInterceptorAdapter
    implements BiFunction<Processor, ReactiveProcessor, ReactiveProcessor> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveInterceptorAdapter.class);

  private static final String BEFORE_METHOD_NAME = "before";
  private static final String AFTER_METHOD_NAME = "after";

  private final ProcessorInterceptorFactory interceptorFactory;

  public ReactiveInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  // TODO MULE-13449 Loggers in this method must be INFO
  @Override
  public ReactiveProcessor apply(Processor component, ReactiveProcessor next) {
    if (!isInterceptable(component)) {
      return next;
    }

    final ComponentLocation componentLocation = ((Component) component).getLocation();
    if (!interceptorFactory.intercept(componentLocation)) {
      return next;
    }

    final ProcessorInterceptor interceptor = interceptorFactory.get();
    Map<String, String> dslParameters = (Map<String, String>) ((Component) component).getAnnotation(ANNOTATION_PARAMETERS);

    ReactiveProcessor interceptedProcessor = doApply(component, next, componentLocation, interceptor, dslParameters);

    LOGGER.debug("Interceptor '{}' for processor '{}' configured.", interceptor, componentLocation.getLocation());
    return interceptedProcessor;
  }

  protected ReactiveProcessor doApply(Processor component, ReactiveProcessor next, final ComponentLocation componentLocation,
                                      final ProcessorInterceptor interceptor, Map<String, String> dslParameters) {
    if (implementsBeforeOrAfter(interceptor)) {
      LOGGER.debug("Configuring interceptor '{}' before and after processor '{}'...", interceptor,
                   componentLocation.getLocation());

      return publisher -> subscriberContext()
          .flatMapMany(ctx -> from(publisher)
              .flatMap(event -> just(event)
                  .cast(InternalEvent.class)
                  .map(doBefore(interceptor, (Component) component, dslParameters))
                  .cast(CoreEvent.class)
                  .transform(next)
                  .onErrorMap(MessagingException.class,
                              error -> createMessagingException(doAfter(interceptor, (Component) component, of(error.getCause()))
                                  .apply((InternalEvent) error.getEvent()),
                                                                error.getCause(), (Component) component, of(error)))
                  .cast(InternalEvent.class)
                  .map(doAfter(interceptor, (Component) component, empty()))
                  .subscriberContext(innerCtx -> innerCtx.put(WITHIN_PROCESS_TO_APPLY, true))
                  .onErrorStop()));
    } else {
      return next;
    }
  }

  protected Function<InternalEvent, InternalEvent> doBefore(ProcessorInterceptor interceptor, Component component,
                                                            Map<String, String> dslParameters) {
    return event -> {
      final InternalEvent eventWithResolvedParams = addResolvedParameters(event, component, dslParameters);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Calling before() for '{}' in processor '{}'...", interceptor,
                     component.getLocation().getLocation());
      }

      try {
        Thread currentThread = currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        ClassLoader interceptorClassLoader = interceptor.getClass().getClassLoader();
        setContextClassLoader(currentThread, originalClassLoader, interceptorClassLoader);
        try {
          interceptor.before(component.getLocation(),
                             getResolvedParams(eventWithResolvedParams),
                             interceptionEvent);
        } finally {
          setContextClassLoader(currentThread, interceptorClassLoader, originalClassLoader);
        }
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(new MessagingException(interceptionEvent.resolve(), e.getCause(), component));
      }
    };
  }

  protected Function<InternalEvent, InternalEvent> doAfter(ProcessorInterceptor interceptor, Component component,
                                                           Optional<Throwable> thrown) {
    return event -> {
      final InternalEvent eventWithResolvedParams = removeResolvedParameters(event);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Calling after() for '{}' in processor '{}'...", interceptor,
                     component.getLocation().getLocation());
      }

      try {
        Thread currentThread = currentThread();
        ClassLoader originalTCCL = currentThread.getContextClassLoader();
        ClassLoader contextClassLoader = interceptor.getClass().getClassLoader();
        setContextClassLoader(currentThread, originalTCCL, contextClassLoader);
        try {
          interceptor.after(component.getLocation(), interceptionEvent, thrown);
        } finally {
          setContextClassLoader(currentThread, contextClassLoader, originalTCCL);
        }
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(createMessagingException(interceptionEvent.resolve(), e.getCause(), component, empty()));
      }
    };
  }

  private boolean implementsBeforeOrAfter(ProcessorInterceptor interceptor) {
    try {
      return !(interceptor.getClass().getMethod(BEFORE_METHOD_NAME, ComponentLocation.class, Map.class, InterceptionEvent.class)
          .isDefault()
          && interceptor.getClass()
              .getMethod(AFTER_METHOD_NAME, ComponentLocation.class, InterceptionEvent.class, Optional.class).isDefault());
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private boolean isInterceptable(Processor component) {
    return ((Component) component).getLocation() != null;
  }

  @Override
  protected InternalEvent removeResolvedParameters(InternalEvent event) {
    if (internalParametersFrom(event).containsKey(INTERCEPTION_RESOLVED_CONTEXT)) {
      Processor processor = (Processor) internalParametersFrom(event).get(INTERCEPTION_COMPONENT);

      if (processor instanceof ParametersResolverProcessor) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Disposing resolved parameters for processor {}...",
                       ((Component) processor).getLocation().getLocation());
        }

        ((ParametersResolverProcessor) processor)
            .disposeResolvedParameters((ExecutionContext) internalParametersFrom(event).get(INTERCEPTION_RESOLVED_CONTEXT));
      }
    }

    return super.removeResolvedParameters(event);
  }

  @Override
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
          if (component instanceof LoggerMessageProcessor || component instanceof ParseTemplateProcessor) {
            return expressionManager.parseLogTemplate(providedValue, event, component.getLocation(),
                                                      NULL_BINDING_CONTEXT);
          } else {
            return expressionManager.evaluate(providedValue, event, component.getLocation()).getValue();
          }
        } else {
          return valueOf(providedValue);
        }
      }));
    }

    return setInternalParamsForNotParamResolver(component, resolvedParameters, event, InternalEvent.builder(event));
  }

  @Override
  protected InternalEvent setInternalParamsForNotParamResolver(Component component,
                                                               Map<String, ProcessorParameterValue> resolvedParameters,
                                                               InternalEvent event, InternalEvent.Builder builder) {
    if (component instanceof ParametersResolverProcessor) {
      try {
        ((ParametersResolverProcessor<?>) component).resolveParameters(builder, (params, context) -> {
          resolvedParameters.putAll(params.entrySet().stream()
              .collect(toMap(e -> e.getKey(),
                             e -> new DefaultProcessorParameterValue(e.getKey(), null, () -> e.getValue().get()))));

          builder.addInternalParameter(INTERCEPTION_RESOLVED_CONTEXT, context);
          builder.addInternalParameter(INTERCEPTION_RESOLVED_PARAMS, resolvedParameters);
          builder.addInternalParameter(INTERCEPTION_COMPONENT, component);
        });
        return builder.build();
      } catch (ExpressionRuntimeException | IllegalArgumentException e) {
        // Some operation parameter threw an expression exception.
        // Continue with the interception as it it were not an operation so that the call to `before` is guaranteed.
        return super.setInternalParamsForNotParamResolver(component, resolvedParameters, event, builder);
      } catch (MuleException e) {
        throw new InterceptionException(e);
      }
    } else {
      return super.setInternalParamsForNotParamResolver(component, resolvedParameters, event, builder);
    }
  }
}
