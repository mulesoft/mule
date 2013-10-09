/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.slf4j.impl;

import org.mule.module.logging.MuleLoggerFactory;

import org.apache.log4j.Level;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 * Bound to Mule logger instead.
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

  /**
   * The unique instance of this class.
   * 
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
   * Declare the version of the SLF4J API this implementation is compiled
   * against. The value of this field is usually modified with each release.
   */
  // to avoid constant folding by the compiler, this field must *not* be final
  public static String REQUESTED_API_VERSION = "1.6"; // !final

  private static final String loggerFactoryClassStr = MuleLoggerFactory.class
      .getName();

  /**
   * The ILoggerFactory instance returned by the {@link #getLoggerFactory}
   * method should always be the same object
   */
  private final ILoggerFactory loggerFactory;

  private StaticLoggerBinder() {
    loggerFactory = new MuleLoggerFactory();
    try {
      Level level = Level.TRACE;
    } catch (NoSuchFieldError nsfe) {
      Util
          .report("This version of SLF4J requires log4j version 1.2.12 or later. See also http://www.slf4j.org/codes.html#log4j_version");
    }
  }

  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  public String getLoggerFactoryClassStr() {
    return loggerFactoryClassStr;
  }
}
