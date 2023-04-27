/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.slf4j.impl;

import org.mule.runtime.core.internal.logger.CustomLoggerFactory;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;

/**
 * This is a test class and will be used only by the tests in core-tests and extension-support
 */
public class StaticLoggerBinder implements org.slf4j.spi.SLF4JServiceProvider {

  /**
   * The unique instance of this class.
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

  /**
   * The ILoggerFactory instance returned by the {@link #getLoggerFactory} method should always be the same object.
   */
  private final ILoggerFactory loggerFactory;

  public StaticLoggerBinder() {
    loggerFactory = new CustomLoggerFactory(LoggerFactory.getILoggerFactory());
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return new BasicMarkerFactory();
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return new NOPMDCAdapter();
  }

  @Override
  public String getRequestedApiVersion() {
    return "2.0.7";
  }

  @Override
  public void initialize() {
    // nothing to do
  }
}

