/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

public class SimpleContextSelector implements ContextSelector {

  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();

  private LoggerContext context;

  public SimpleContextSelector() {
    this.context = loggerContextFactory.build(getClass().getClassLoader(), this, false);
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
