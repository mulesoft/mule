/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.boot.internal;

import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * A wrapper for {@link ContextSelector} that only forwards the methods to a delegate. If a delegate is not set, the logger
 * context getters return a default {@link LoggerContext} with name {@code "boot"}.
 *
 * @since 4.5.0
 */
public class ContextSelectorWrapper implements ContextSelector {

  private static final LazyValue<LoggerContext> DEFAULT_LOGGER_CONTEXT = new LazyValue<>(() -> new LoggerContext("boot"));

  private ContextSelector delegate;
  private Consumer<ContextSelector> delegateDisposer;

  public ContextSelectorWrapper() {}

  public ContextSelectorWrapper(ContextSelector delegate, Consumer<ContextSelector> delegateDisposer) {
    setDelegate(delegate, delegateDisposer);
  }

  /**
   * Changes the delegate.
   *
   * @param delegate         the new delegate {@link ContextSelector}.
   * @param delegateDisposer a callback used to dispose the delegate.
   */
  public void setDelegate(ContextSelector delegate, Consumer<ContextSelector> delegateDisposer) {
    this.delegate = delegate;
    this.delegateDisposer = delegateDisposer;
  }

  /**
   * Disposes the delegate by using the callback provided in {@link #setDelegate(ContextSelector, Consumer)}.
   */
  public void disposeDelegate() {
    if (delegate != null && delegateDisposer != null) {
      delegateDisposer.accept(delegate);
    }
  }

  @Override
  public void shutdown(String fqcn, ClassLoader loader, boolean currentContext, boolean allContexts) {
    if (delegate != null) {
      delegate.shutdown(fqcn, loader, currentContext, allContexts);
    }
  }

  @Override
  public boolean hasContext(String fqcn, ClassLoader loader, boolean currentContext) {
    if (delegate == null) {
      return false;
    } else {
      return delegate.hasContext(fqcn, loader, currentContext);
    }
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    if (delegate == null) {
      return DEFAULT_LOGGER_CONTEXT.get();
    } else {
      return delegate.getContext(fqcn, loader, currentContext);
    }
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, Map.Entry<String, Object> entry, boolean currentContext) {
    if (delegate == null) {
      return DEFAULT_LOGGER_CONTEXT.get();
    } else {
      return delegate.getContext(fqcn, loader, entry, currentContext);
    }
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    if (delegate == null) {
      return DEFAULT_LOGGER_CONTEXT.get();
    } else {
      return delegate.getContext(fqcn, loader, currentContext, configLocation);
    }
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, Map.Entry<String, Object> entry, boolean currentContext,
                                  URI configLocation) {
    if (delegate == null) {
      return DEFAULT_LOGGER_CONTEXT.get();
    } else {
      return delegate.getContext(fqcn, loader, entry, currentContext, configLocation);
    }
  }

  @Override
  public List<LoggerContext> getLoggerContexts() {
    if (delegate == null) {
      return singletonList(DEFAULT_LOGGER_CONTEXT.get());
    } else {
      return delegate.getLoggerContexts();
    }
  }

  @Override
  public void removeContext(LoggerContext context) {
    if (delegate != null) {
      delegate.removeContext(context);
    }
  }

  @Override
  public boolean isClassLoaderDependent() {
    if (delegate == null) {
      return false;
    } else {
      return delegate.isClassLoaderDependent();
    }
  }

  private static class LazyValue<T> {

    private final Supplier<T> supplier;
    private T value;

    public LazyValue(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    public T get() {
      if (value == null) {
        synchronized (this) {
          if (value == null) {
            value = supplier.get();
          }
        }
      }
      return value;
    }
  }
}
