/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;

public class InterceptorChain<T extends ComponentModel> {

  private static final Logger LOGGER = getLogger(InterceptorChain.class);

  public static class Builder<T extends ComponentModel> {

    private final List<Interceptor<T>> interceptors = new ArrayList<>(2);
    private Function<Throwable, Throwable> exceptionMapper = identity();

    public Builder<T> addInterceptor(Interceptor<T> interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    public InterceptorChain<T> build() {
      InterceptorChain<T> root = null;
      InterceptorChain<T> previous = null;
      final int len = interceptors.size();

      for (int i = len-1; i >= 0; i--) {
        int prevIndex = i - 1;
        Interceptor<T> prev = prevIndex >= 0 ? interceptors.get(prevIndex) : null;

        //InterceptorChain chain = new InterceptorChain(interceptors.get(i), next, previous, exceptionMapper);
        //previous = chain;
        if (root == null) {
          //root = chain;
        }
      }

      return root;
    }
  }

  private static final String BEFORE = "before";
  private static final String ON_SUCCESS = "onSuccess";
  private static final String ON_ERROR = "onError";
  private static final String AFTER = "after";

  private final Interceptor<T> interceptor;
  private final InterceptorChain<T> nextInterceptor;
  private final InterceptorChain<T> previousInterceptor;
  private final Function<Throwable, Throwable> exceptionMaper;

  private InterceptorChain(Interceptor<T> interceptor,
                          InterceptorChain<T> nextInterceptor,
                          InterceptorChain<T> previousInterceptor,
                          Function<Throwable, Throwable> exceptionMaper) {
    this.interceptor = interceptor;
    this.nextInterceptor = nextInterceptor;
    this.previousInterceptor = previousInterceptor;
    this.exceptionMaper = exceptionMaper;
  }

  public Throwable before(ExecutionContextAdapter<T> executionContext, ExecutorCallback callback) throws Throwable {
    try {
      interceptor.before(executionContext);
      if (nextInterceptor != null) {
        return nextInterceptor.before(executionContext, callback);
      }

      return null;
    } catch (Throwable t) {
      t = exceptionMaper.apply(t);
      logError(t, BEFORE, false);
      t = reserveOnError(executionContext, BEFORE, t);
      callback.error(t);

      return t;
    }
  }

  public void onSuccess(ExecutionContextAdapter<T> executionContext, Object result) {
    try {
      interceptor.onSuccess(executionContext, result);
    } catch (Throwable t) {
      t = exceptionMaper.apply(t);
      logError(t, ON_SUCCESS, true);
    }

    if (nextInterceptor != null) {
      nextInterceptor.onSuccess(executionContext, result);
    }
  }

  public Throwable onError(ExecutionContextAdapter<T> executionContext, Throwable t) {
    try {
      t = interceptor.onError(executionContext, t);
    } catch (Throwable t2) {
      logError(t2, ON_ERROR, true);
    }

    if (nextInterceptor != null) {
      t = nextInterceptor.onError(executionContext, t);
    }

    return t;
  }

  public void after(ExecutionContextAdapter<T> executionContext, Object result) {
    try {
      interceptor.after(executionContext, result);
    } catch (Throwable t) {
      logError(t, AFTER, true);
    }

    if (nextInterceptor != null) {
      nextInterceptor.after(executionContext, result);
    }
  }

  private Throwable reserveOnError(ExecutionContextAdapter<T> executionContext, String phase, Throwable t) {
    try {
      t = interceptor.onError(executionContext, t);
    } catch (Throwable t2) {
      logError(t2, ON_ERROR, true);
    }

    if (previousInterceptor != null) {
      return previousInterceptor.reserveOnError(executionContext, phase, t);
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
