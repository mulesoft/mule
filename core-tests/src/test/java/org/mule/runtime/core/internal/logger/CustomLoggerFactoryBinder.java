/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.logger;

import io.qameta.allure.Issue;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * In some tests we were replacing the original logger in a class with a mocked logger via reflection and then setting it back to
 * the original. This reflective access will not work in Java 17, hence writing a custom logger binder that will be used with
 * specific tests. The tests that need Custom logger are defined in {@link CustomLoggerFactory}
 */
@Issue("W-12625671")
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

