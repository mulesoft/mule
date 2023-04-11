/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.slf4j.impl;

import org.mule.runtime.core.internal.logger.CustomLoggerFactory;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;


public class StaticLoggerBinder implements LoggerFactoryBinder {

  /**
   * xx` The unique instance of this class.
   */
  private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  /**
   * Return the singleton of this class.
   *
   * @return the StaticLoggerBinder singleton
   */
  public static final StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }


  private static final String loggerFactoryClassStr = CustomLoggerFactory.class.getName();

  /**
   * The ILoggerFactory instance returned by the {@link #getLoggerFactory} method should always be the same object.
   */
  private final ILoggerFactory loggerFactory;

  private StaticLoggerBinder() {
    loggerFactory = new CustomLoggerFactory(LoggerFactory.getILoggerFactory());
  }

  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  public String getLoggerFactoryClassStr() {
    return loggerFactoryClassStr;
  }
}

