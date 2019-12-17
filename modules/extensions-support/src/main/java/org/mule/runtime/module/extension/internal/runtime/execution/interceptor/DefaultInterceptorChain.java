/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.interceptor;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.List;

import org.slf4j.Logger;

class DefaultInterceptorChain implements InterceptorChain {

  private static final Logger LOGGER = getLogger(DefaultInterceptorChain.class);

  private static final String BEFORE = "before";
  private static final String ON_SUCCESS = "onSuccess";
  private static final String ON_ERROR = "onError";
  private static final String AFTER = "after";

  private final Interceptor interceptor;
  private DefaultInterceptorChain next;
  private DefaultInterceptorChain previous;


  static DefaultInterceptorChain of(List<Interceptor> interceptors) {
    final List<DefaultInterceptorChain> chains = interceptors.stream()
        .map(DefaultInterceptorChain::new)
        .collect(toList());

    DefaultInterceptorChain interceptor = chains.get(0);
    DefaultInterceptorChain previous = null;
    DefaultInterceptorChain next;

    final int len = chains.size();

    for (int i = 0; i < len; i++) {
      interceptor.previous = previous;
      previous = interceptor;
      int nextIndex = i + 1;
      next = nextIndex < len ? chains.get(nextIndex) : null;
      interceptor.next = next;
      interceptor = next;
    }

    return chains.get(0);
  }

  private DefaultInterceptorChain(Interceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public Throwable before(ExecutionContextAdapter executionContext, CompletableComponentExecutor.ExecutorCallback callback) {
    try {
      interceptor.before(executionContext);
      if (next != null) {
        return next.before(executionContext, callback);
      }

      return null;
    } catch (Throwable t) {
      logError(t, BEFORE, false);
      t = errorOnReverse(executionContext, t);
      callback.error(t);

      return t;
    }
  }

  @Override
  public void onSuccess(ExecutionContextAdapter executionContext, Object result) {
    try {
      interceptor.onSuccess(executionContext, result);
    } catch (Throwable t) {
      logError(t, ON_SUCCESS, true);
    }

    try {
      interceptor.after(executionContext, result);
    } catch (Throwable t) {
      logError(t, AFTER, true);
    }

    if (next != null) {
      next.onSuccess(executionContext, result);
    }
  }

  @Override
  public Throwable onError(ExecutionContextAdapter executionContext, Throwable t) {
    try {
      t = interceptor.onError(executionContext, t);
    } catch (Throwable t2) {
      logError(t2, ON_ERROR, true);
    }

    try {
      interceptor.after(executionContext, null);
    } catch (Throwable t2) {
      logError(t2, AFTER, true);
    }

    if (next != null) {
      t = next.onError(executionContext, t);
    }

    return t;
  }

  private Throwable errorOnReverse(ExecutionContextAdapter executionContext, Throwable t) {
    try {
      t = interceptor.onError(executionContext, t);
    } catch (Throwable t2) {
      logError(t2, ON_ERROR, true);
    }

    try {
      interceptor.after(executionContext, null);
    } catch (Throwable t2) {
      logError(t2, AFTER, true);
    }

    if (previous != null) {
      return previous.errorOnReverse(executionContext, t);
    } else {
      return t;
    }
  }

  private void logError(Throwable t, String phase, boolean executionContinues) {
    if (LOGGER.isDebugEnabled()) {
      StringBuilder builder = new StringBuilder();
      builder.append(format("Interceptor %s threw exception executing '%s' phase.", interceptor, phase));
      if (executionContinues) {
        builder.append(
                       " Exception will be ignored. Next interceptors (if any) will be executed and the operation's exception will be returned");
      }

      LOGGER.debug(builder.toString());
    }
  }
}
