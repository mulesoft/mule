/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class CustomLoggerFactoryBinder implements LoggerFactoryBinder {

  private static final String LOGGER_FACTORY_CLASS_NAME = CustomLoggerFactoryBinder.class.getName();
  private final ILoggerFactory loggerFactory;

  public CustomLoggerFactoryBinder() {
    this.loggerFactory = new CustomLoggerFactory(LoggerFactory.getILoggerFactory());
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public String getLoggerFactoryClassStr() {
    return LOGGER_FACTORY_CLASS_NAME;
  }
}

