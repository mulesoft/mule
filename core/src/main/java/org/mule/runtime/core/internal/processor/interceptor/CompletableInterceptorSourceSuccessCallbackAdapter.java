/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.interception.SourceInterceptorFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;

/**
 * Hooks the {@link SourceInterceptor}s for a {@link MessageSource} successful callback into the {@code Reactor} response handling
 * pipeline.
 *
 * @since 4.3.0
 */
public class CompletableInterceptorSourceSuccessCallbackAdapter
    extends AbstractCompletableInterceptorSourceCallbackAdapter<SourcePolicySuccessResult> {


  public CompletableInterceptorSourceSuccessCallbackAdapter(SourceInterceptorFactory interceptorFactory) {
    super(interceptorFactory);
  }

  @Override
  protected SourcePolicySuccessResult applyBefore(InternalEvent event, SourcePolicySuccessResult result) {
    return new SourcePolicySuccessResult(event, result.getResponseParameters(),
                                         result.getMessageSourceResponseParametersProcessor());
  }

}
