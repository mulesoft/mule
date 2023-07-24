/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.log4j.internal;

import org.mule.runtime.api.lifecycle.Disposable;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;

public class ContextSelectorWrapper implements ContextSelector, Disposable {

  private ContextSelector delegate;

  public ContextSelectorWrapper() {
    setDelegate(new BasicContextSelector());
  }

  public ContextSelectorWrapper(ContextSelector delegate) {
    setDelegate(delegate);
  }

  public void setDelegate(ContextSelector delegate) {
    this.delegate = delegate;
  }

  @Override
  public void shutdown(String fqcn, ClassLoader loader, boolean currentContext, boolean allContexts) {
    delegate.shutdown(fqcn, loader, currentContext, allContexts);
  }

  @Override
  public boolean hasContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return delegate.hasContext(fqcn, loader, currentContext);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return delegate.getContext(fqcn, loader, currentContext);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, Map.Entry<String, Object> entry, boolean currentContext) {
    return delegate.getContext(fqcn, loader, entry, currentContext);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    return delegate.getContext(fqcn, loader, currentContext, configLocation);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, Map.Entry<String, Object> entry, boolean currentContext,
                                  URI configLocation) {
    return delegate.getContext(fqcn, loader, entry, currentContext, configLocation);
  }

  @Override
  public List<LoggerContext> getLoggerContexts() {
    return delegate.getLoggerContexts();
  }

  @Override
  public void removeContext(LoggerContext context) {
    delegate.removeContext(context);
  }

  @Override
  public boolean isClassLoaderDependent() {
    return delegate.isClassLoaderDependent();
  }

  @Override
  public void dispose() {
    if (delegate instanceof Disposable) {
      ((Disposable) delegate).dispose();
    }
  }
}
