/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.Serializable;
import java.util.stream.Stream;

public class MemoryAppenderResource {

  private static final String APPENDER_NAME = "memory";

  private final ClassLoader loggerContextClassLoader;
  private final Layout<? extends Serializable> layout;
  private final Filter filter;
  private MemoryAppender appender;

  public MemoryAppenderResource(ClassLoader loggerContextClassLoader) {
    this(loggerContextClassLoader, null, null);
  }

  public MemoryAppenderResource(ClassLoader loggerContextClassLoader, Layout<? extends Serializable> layout, Filter filter) {
    this.loggerContextClassLoader = loggerContextClassLoader;
    this.layout = layout;
    this.filter = filter;
    initializeAppender();
  }

  private void initializeAppender() {
    this.appender = MemoryAppender.createAppender(layout, filter, APPENDER_NAME, "false");
    Configuration configuration = getLoggerContext().getConfiguration();
    appender.start();
    configuration.addAppender(appender);

    updateLoggers(appender, configuration, null, null);
  }

  private LoggerContext getLoggerContext() {
    return (LoggerContext) LogManager.getContext(loggerContextClassLoader, false);
  }

  private void updateLoggers(Appender appender, Configuration config, Level level, Filter filter) {
    for (LoggerConfig loggerConfig : config.getLoggers().values()) {
      loggerConfig.addAppender(appender, level, filter);
    }
    config.getRootLogger().addAppender(appender, level, filter);
  }

  private void removeAppender(Appender appender, Configuration config) {
    for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
      loggerConfig.removeAppender(appender.getName());
    }
    config.getRootLogger().removeAppender(appender.getName());
  }

  public Stream<String> getLogLines() {
    return this.appender.getLogLines();
  }

  public void tearDown() {
    removeAppender(appender, getLoggerContext().getConfiguration());
  }
}
