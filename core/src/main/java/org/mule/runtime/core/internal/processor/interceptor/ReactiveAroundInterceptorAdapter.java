/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.fromFuture;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.FlowInterceptorFactory;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;

import reactor.util.context.Context;

/**
 * Hooks the {@link ProcessorInterceptor}s
 * {@link ProcessorInterceptor#around(ComponentLocation, Map, InterceptionEvent, InterceptionAction)
 * around} method for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveAroundInterceptorAdapter extends ReactiveInterceptorAdapter {

  private static final Logger LOGGER = getLogger(ReactiveAroundInterceptorAdapter.class);

  public ReactiveAroundInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    super(interceptorFactory);
  }

  public ReactiveAroundInterceptorAdapter(FlowInterceptorFactory interceptorFactory) {
    super(interceptorFactory);
  }

  public ReactiveAroundInterceptorAdapter(ComponentInterceptorFactoryAdapter interceptorFactoryAdapter) {
    super(interceptorFactoryAdapter);
  }

  @Override
  protected ReactiveProcessor doApply(ReactiveProcessor component, ReactiveProcessor next, ComponentLocation componentLocation,
                                      ComponentInterceptorAdapter interceptor, Map<String, String> dslParameters) {
    if (interceptor.implementsAround()) {
      LOGGER.debug("Configuring interceptor '{}' around processor '{}'...", interceptor, componentLocation.getLocation());

      return publisher -> subscriberContext()
          .flatMapMany(ctx -> from(publisher)
              .cast(InternalEvent.class)
              .flatMap(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next, ctx))
                  .onErrorMap(CompletionException.class, CompletionException::getCause)));
    } else {
      return next;
    }
  }

  private CompletableFuture<InternalEvent> doAround(InternalEvent event, ComponentInterceptorAdapter interceptor,
                                                    ReactiveProcessor component, Map<String, String> dslParameters,
                                                    ReactiveProcessor next, Context ctx) {
    final InternalEvent eventWithResolvedParams = addResolvedParameters(event, (Component) component, dslParameters);

    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
    final ReactiveInterceptionAction reactiveInterceptionAction =
        new ReactiveInterceptionAction(interceptionEvent, next, ctx, component, errorTypeLocator);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Calling around() for '{}' in processor '{}'...", interceptor,
                   ((Component) component).getLocation().getLocation());
    }

    try {
      Thread currentThread = currentThread();
      ClassLoader currentClassLoader = currentThread.getContextClassLoader();
      ClassLoader contextClassLoader = interceptor.getClass().getClassLoader();
      setContextClassLoader(currentThread, currentClassLoader, contextClassLoader);
      CompletableFuture<InterceptionEvent> interception;
      try {
        interception = interceptor.around(((Component) component).getLocation(),
                                          getResolvedParams(eventWithResolvedParams), interceptionEvent,
                                          reactiveInterceptionAction);
      } finally {
        setContextClassLoader(currentThread, contextClassLoader, currentClassLoader);
      }

      return interception.exceptionally(t -> {
        if (t instanceof MessagingException) {
          throw new CompletionException(t);
        } else {
          throw new CompletionException(createMessagingException(eventWithResolvedParams,
                                                                 t instanceof CompletionException ? t.getCause()
                                                                     : t,
                                                                 (Component) component, empty()));
        }
      }).thenApply(interceptedEvent -> interceptedEvent != null
          ? ((DefaultInterceptionEvent) interceptedEvent).resolve()
          : null);
    } catch (Exception e) {
      throw propagate(createMessagingException(interceptionEvent.resolve(), e, (Component) component, empty()));
    }
  }
}
