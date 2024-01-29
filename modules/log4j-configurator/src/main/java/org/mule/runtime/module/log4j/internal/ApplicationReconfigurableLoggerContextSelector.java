/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;

import java.net.URI;
import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

/**
 * A {@link ContextSelector} that will use the context for the application once it is deployed. It may be used in environments
 * that deploys only one app (W-12356154).
 *
 * @since 4.7.0
 */
class ApplicationReconfigurableLoggerContextSelector implements ContextSelector {

  private final MuleLoggerContextFactory loggerContextFactory = new MuleLoggerContextFactory();
  private static final ClassLoader SYSTEM_CLASSLOADER = getSystemClassLoader();
  private LoggerContext loggerContext;

  public ApplicationReconfigurableLoggerContextSelector() {
    // The logger context is created with no logging separation.
    // This will guarantee that by default this will work as no separation in logs.
    this.loggerContext =
        this.loggerContextFactory.build(SYSTEM_CLASSLOADER, this, false);
  }

  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
    return loggerContext;
  }

  public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
    return getContext(fqcn, loader, currentContext);
  }

  public List<LoggerContext> getLoggerContexts() {
    return asList(loggerContext);
  }

  public void removeContext(LoggerContext context) {
    throw new UnsupportedOperationException("It is not allowed to remove contexts in a selector that depends on a single app.");
  }

  public void reconfigureAccordingToAppClassloader(ClassLoader classloader) {
    LoggerContext applicationClassLoaderLoggerContext = this.loggerContextFactory
        .build(classloader, this, true);

    applicationClassLoaderLoggerContext.reconfigure();

    // We reconfigure the loggers that were already provided to get the app log4j configuration.
    loggerContext.updateLoggers(applicationClassLoaderLoggerContext.getConfiguration());

    // We change the configuration for the logger context. This only sets the configuration.
    loggerContext.reconfigure(applicationClassLoaderLoggerContext.getConfiguration());

    // This is needed so that the configuration set is reconfigured.
    loggerContext.reconfigure();

    loggerContext = applicationClassLoaderLoggerContext;
  }
}
