/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Hooks the {@link ProcessorInterceptor}s for a {@link MessageSource} callback into the {@code Reactor} response handling
 * pipeline.
 *
 * @since 4.0
 */
public class ReactiveInterceptorSourceCallbackAdapter extends AbstractInterceptorAdapter implements
    BiFunction<MessageSource, Function<SourcePolicySuccessResult, Publisher<Void>>, Function<SourcePolicySuccessResult, Publisher<Void>>> {

  private ProcessorInterceptorFactory interceptorFactory;

  public ReactiveInterceptorSourceCallbackAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public Function<SourcePolicySuccessResult, Publisher<Void>> apply(MessageSource source,
                                                                    Function<SourcePolicySuccessResult, Publisher<Void>> next) {
    if (!isInterceptable(source)) {
      return next;
    }

    final ComponentLocation componentLocation = ((Component) source).getLocation();
    if (!interceptorFactory.intercept(componentLocation)) {
      return next;
    }

    return result -> {
      final ProcessorInterceptor interceptor = interceptorFactory.get();
      Map<String, String> dslParameters = (Map<String, String>) (source).getAnnotation(ANNOTATION_PARAMETERS);

      SourcePolicySuccessResult interceptedBeforeResult =
          new SourcePolicySuccessResult(doBefore(interceptor, source, dslParameters).apply((InternalEvent) result.getResult()),
                                        result.getResponseParameters(), result.getMessageSourceResponseParametersProcessor());

      try {
        Publisher<Void> publisher = next.apply(interceptedBeforeResult);
        doAfter(interceptor, source, empty()).apply((InternalEvent) interceptedBeforeResult.getResult());
        return publisher;
      } catch (Throwable t) {
        doAfter(interceptor, source, of(t)).apply((InternalEvent) interceptedBeforeResult.getResult());
        return error(t);
      }
    };
  }

  private boolean isInterceptable(Component component) {
    return component.getLocation() != null;
  }

}
