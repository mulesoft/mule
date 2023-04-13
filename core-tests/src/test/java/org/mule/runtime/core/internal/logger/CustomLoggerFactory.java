/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import static java.util.Arrays.asList;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A factory for creating a CustomLogger, that will be used by some tests to avoid use of relection in those tests that replaced
 * original logger with a mocked logger.
 */
public class CustomLoggerFactory implements ILoggerFactory {

  private final ILoggerFactory delegate;
  private final ConcurrentMap<String, Logger> loggerMap;
  private final List<String> CLASSES_WITH_CUSTOM_LOGGER_IN_TEST =
      asList("org.mule.runtime.core.internal.connection.PoolingConnectionHandler",
             "org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource",
             "org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper",
             "org.mule.runtime.core.internal.connection.PoolingConnectionManagementStrategy");

  public CustomLoggerFactory(ILoggerFactory delegate) {
    this.delegate = delegate;
    this.loggerMap = new ConcurrentHashMap<>();
  }

  @Override
  public Logger getLogger(String name) {
    return loggerMap.computeIfAbsent(name, this::createLogger);
  }

  private Logger createLogger(String name) {
    if (contains(name)) {
      return new CustomLogger(delegate.getLogger(name), name);
    }
    return delegate.getLogger(name);
  }

  private boolean contains(String searchStr) {
    return CLASSES_WITH_CUSTOM_LOGGER_IN_TEST.stream().anyMatch(searchStr::equals);
  }
}
