/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.interception.SourceInterceptorFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;

/**
 * Hooks the {@link SourceInterceptor}s for a {@link MessageSource} successful callback into the {@code Reactor} response handling
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
