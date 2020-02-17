/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.log4j2;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

public class SimpleContextSelector implements ContextSelector {

  private MuleLoggerContext context;

  public SimpleContextSelector() {
    this.context = new MuleLoggerContext("SingleLoggerContext", this, )
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return null;
  }

  @Override
  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    return null;
  }

  @Override
  public List<LoggerContext> getLoggerContexts() {
    return null;
  }

  @Override
  public void removeContext(LoggerContext context) {

  }
}
