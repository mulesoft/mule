/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.interception.SourceInterceptorFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;

/**
 * Hooks the {@link SourceInterceptor}s for a {@link MessageSource} failure callback into the {@code Reactor} response handling
 * pipeline.
 *
 * @since 4.3.0
 */
public class CompletableInterceptorSourceFailureCallbackAdapter
    extends AbstractCompletableInterceptorSourceCallbackAdapter<SourcePolicyFailureResult> {

  public CompletableInterceptorSourceFailureCallbackAdapter(SourceInterceptorFactory interceptorFactory) {
    super(interceptorFactory);
  }

  @Override
  protected SourcePolicyFailureResult applyBefore(InternalEvent event, SourcePolicyFailureResult result) {
    return new SourcePolicyFailureResult(new MessagingException(event, result.getMessagingException()),
                                         result.getErrorResponseParameters());
  }

}
