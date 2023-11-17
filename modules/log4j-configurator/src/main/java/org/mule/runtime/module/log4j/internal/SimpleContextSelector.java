/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * A simple {@link ContextSelector} which always returns the same {@link MuleLoggerContext} created through a
 * {@link MuleLog4jContextFactory}.
 * <p>
 * Log separation will always be disabled on the returned context.
 *
 * @since 4.5
 */
public class SimpleContextSelector implements ContextSelector {

  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();

  private LoggerContext context;

  public SimpleContextSelector() {
    this.context = loggerContextFactory.build(getClass().getClassLoader(), this, false, null);
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return context;
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    return context;
  }

  @Override
  public List<LoggerContext> getLoggerContexts() {
    return asList(context);
  }

  @Override
  public void removeContext(LoggerContext context) {

  }
}
