/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
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

    private Builder(){}

    public Builder<T> addInterceptor(Interceptor<T> interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    public Builder setExceptionMapper(Function<Throwable, Throwable> exceptionMapper) {
      this.exceptionMapper = exceptionMapper;
      return this;
    }

    public InterceptorChain<T> build() {
      final List<InterceptorChain<T>> chains = interceptors.stream()
          .map(i -> new InterceptorChain<>(i, exceptionMapper))
          .collect(toList());
      final int len = chains.size();

      if (len == 0) {
        //TODO: NullInterceptor
        return null;
      }

      InterceptorChain<T> interceptor = chains.get(0);
      InterceptorChain<T> previous = null;
      InterceptorChain<T> next;

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
  }

  public static Builder builder() {
    return new Builder();
  }

  private static final String BEFORE = "before";
  private static final String ON_SUCCESS = "onSuccess";
  private static final String ON_ERROR = "onError";
  private static final String AFTER = "after";

  private final Interceptor<T> interceptor;
  private final Function<Throwable, Throwable> exceptionMapper;
  private InterceptorChain<T> next;
  private InterceptorChain<T> previous;

  public InterceptorChain(Interceptor<T> interceptor,
                          Function<Throwable, Throwable> exceptionMapper) {
    this.interceptor = interceptor;
    this.exceptionMapper = exceptionMapper;
  }

  public Throwable before(ExecutionContextAdapter<T> executionContext, ExecutorCallback callback) throws Throwable {
    try {
      interceptor.before(executionContext);
      if (next != null) {
        return next.before(executionContext, callback);
      }

      return null;
    } catch (Throwable t) {
      t = exceptionMapper.apply(t);
      logError(t, BEFORE, false);
      t = errorOnReverse(executionContext, t);
      callback.error(t);

      return t;
    }
  }

  public void onSuccess(ExecutionContextAdapter<T> executionContext, Object result) {
    try {
      interceptor.onSuccess(executionContext, result);
    } catch (Throwable t) {
      t = exceptionMapper.apply(t);
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

  public Throwable onError(ExecutionContextAdapter<T> executionContext, Throwable t) {
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

  private Throwable errorOnReverse(ExecutionContextAdapter<T> executionContext, Throwable t) {
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
