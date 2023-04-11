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

public class CustomLoggerFactory implements ILoggerFactory {

  private final ILoggerFactory delegate;
  private final ConcurrentMap<String, Logger> loggerMap;
  private final List<String> TESTS_WITH_CUSTOM_LOGGER =
      asList("org.mule.runtime.core.internal.connection.PoolingConnectionHandler",
             "org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource",
             "org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper");

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
    return TESTS_WITH_CUSTOM_LOGGER.stream().anyMatch(searchStr::equals);
  }
}
