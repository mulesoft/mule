/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromFuture;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Hooks the {@link ProcessorInterceptor}s
 * {@link ProcessorInterceptor#around(org.mule.runtime.api.component.location.ComponentLocation, java.util.Map, org.mule.runtime.api.interception.InterceptionEvent, org.mule.runtime.api.interception.InterceptionAction)
 * around} method for a {@link Processor} into the {@code Reactor} pipeline.
 *
 * @since 4.0
 */
public class ReactiveAroundInterceptorAdapter extends ReactiveInterceptorAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveAroundInterceptorAdapter.class);

  private static final String AROUND_METHOD_NAME = "around";

  public ReactiveAroundInterceptorAdapter(ProcessorInterceptorFactory interceptorFactory) {
    super(interceptorFactory);
  }

  @Override
  protected ReactiveProcessor doApply(Processor component, ReactiveProcessor next, ComponentLocation componentLocation,
                                      ProcessorInterceptor interceptor, Map<String, String> dslParameters) {
    if (implementsAround(interceptor)) {
      LOGGER.debug("Configuring interceptor '{}' around processor '{}'...", interceptor, componentLocation.getLocation());
      return publisher -> from(publisher)
          .cast(InternalEvent.class)
          .flatMapMany(event -> fromFuture(doAround(event, interceptor, component, dslParameters, next))
              .onErrorMap(CompletionException.class, completionException -> completionException.getCause()));
    } else {
      return next;
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

  private CompletableFuture<InternalEvent> doAround(InternalEvent event, ProcessorInterceptor interceptor,
                                                    Processor component, Map<String, String> dslParameters,
                                                    ReactiveProcessor next) {
    final InternalEvent eventWithResolvedParams = addResolvedParameters(event, component, dslParameters);

    DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);
    final ReactiveInterceptionAction reactiveInterceptionAction =
        new ReactiveInterceptionAction(interceptionEvent, next, component,
                                       ((PrivilegedMuleContext) getMuleContext()).getErrorTypeLocator());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Calling around() for '{}' in processor '{}'...", interceptor,
                   ((Component) component).getLocation().getLocation());
    }

    try {
      return withContextClassLoader(interceptor.getClass().getClassLoader(), () -> interceptor
          .around(((Component) component).getLocation(),
                  getResolvedParams(eventWithResolvedParams), interceptionEvent,
                  reactiveInterceptionAction))
                      .exceptionally(t -> {
                        if (t instanceof MessagingException) {
                          throw new CompletionException(t);
                        } else {
                          throw new CompletionException(createMessagingException(eventWithResolvedParams,
                                                                                 t instanceof CompletionException ? t.getCause()
                                                                                     : t,
                                                                                 ((Component) component)));
                        }
                      })
                      .thenApply(interceptedEvent -> ((DefaultInterceptionEvent) interceptedEvent).resolve());
    } catch (Exception e) {
      throw propagate(createMessagingException(interceptionEvent.resolve(), e, (Component) component));
    }
  }

}
