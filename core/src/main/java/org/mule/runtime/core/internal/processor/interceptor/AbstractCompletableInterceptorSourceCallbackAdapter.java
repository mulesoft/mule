/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.interception.SourceInterceptorFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicyResult;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * Hooks the {@link SourceInterceptor}s for a {@link MessageSource} callback into the {@code Reactor} response handling pipeline.
 *
 * @since 4.3.0
 */
public abstract class AbstractCompletableInterceptorSourceCallbackAdapter<T extends SourcePolicyResult>
    extends AbstractInterceptorAdapter
    implements BiFunction<MessageSource, BiConsumer<T, CompletableCallback<Void>>, BiConsumer<T, CompletableCallback<Void>>> {

  private static final Logger LOGGER = getLogger(AbstractCompletableInterceptorSourceCallbackAdapter.class);

  private SourceInterceptorFactory interceptorFactory;

  public AbstractCompletableInterceptorSourceCallbackAdapter(SourceInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public BiConsumer<T, CompletableCallback<Void>> apply(MessageSource source, BiConsumer<T, CompletableCallback<Void>> next) {

    if (!isInterceptable(source)) {
      return next;
    }

    final ComponentLocation componentLocation = source.getLocation();
    if (!interceptorFactory.intercept(componentLocation)) {
      return next;
    }

    final SourceInterceptor interceptor = interceptorFactory.get();
    Map<String, String> dslParameters = (Map<String, String>) (source).getAnnotation(ANNOTATION_PARAMETERS);

    return (result, callback) -> {
      T interceptedBeforeResult =
          applyBefore(doBefore(interceptor, source, dslParameters).apply((InternalEvent) result.getResult()), result);

      try {
        next.accept(interceptedBeforeResult, callback);
        doAfter(interceptor, source, empty()).apply((InternalEvent) interceptedBeforeResult.getResult());
      } catch (Throwable t) {
        doAfter(interceptor, source, of(t)).apply((InternalEvent) interceptedBeforeResult.getResult());
        callback.error(t);
      }
    };
  }

  /**
   * Applies the result of the before interception to the given result
   *
   * @param event  Resulting event of the before interception
   * @param result Input result of the source
   * @return Result with the before event applied
   */
  protected abstract T applyBefore(InternalEvent event, T result);

  protected Function<InternalEvent, InternalEvent> doBefore(SourceInterceptor interceptor, Component component,
                                                            Map<String, String> dslParameters) {
    return event -> {
      final InternalEvent eventWithResolvedParams = addResolvedParameters(event, component, dslParameters);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      LOGGER.debug("Calling before() for '{}' in processor '{}'...", interceptor, component.getLocation().getLocation());

      try {
        Thread currentThread = currentThread();
        ClassLoader originalTCCL = currentThread.getContextClassLoader();
        ClassLoader ctxClassLoader = interceptor.getClass().getClassLoader();
        setContextClassLoader(currentThread, originalTCCL, ctxClassLoader);
        try {
          interceptor.beforeCallback(component.getLocation(),
                                     getResolvedParams(eventWithResolvedParams),
                                     interceptionEvent);
        } finally {
          setContextClassLoader(currentThread, ctxClassLoader, originalTCCL);
        }
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(new MessagingException(interceptionEvent.resolve(), e.getCause(), component));
      }
    };
  }

  protected Function<InternalEvent, InternalEvent> doAfter(SourceInterceptor interceptor, Component component,
                                                           Optional<Throwable> thrown) {
    return event -> {
      final InternalEvent eventWithResolvedParams = removeResolvedParameters(event);
      DefaultInterceptionEvent interceptionEvent = new DefaultInterceptionEvent(eventWithResolvedParams);

      LOGGER.debug("Calling after() for '{}' in processor '{}'...", interceptor, component.getLocation().getLocation());

      try {
        Thread currentThread = currentThread();
        ClassLoader originalTCCL = currentThread.getContextClassLoader();
        ClassLoader ctxClassLoader = interceptor.getClass().getClassLoader();
        setContextClassLoader(currentThread, originalTCCL, ctxClassLoader);
        try {
          interceptor.afterCallback(component.getLocation(), interceptionEvent, thrown);
        } finally {
          setContextClassLoader(currentThread, ctxClassLoader, originalTCCL);
        }
        return interceptionEvent.resolve();
      } catch (Exception e) {
        throw propagate(createMessagingException(interceptionEvent.resolve(), e.getCause(), component, empty()));
      }
    };
  }

  private boolean isInterceptable(Component component) {
    return component.getLocation() != null;
  }
}
