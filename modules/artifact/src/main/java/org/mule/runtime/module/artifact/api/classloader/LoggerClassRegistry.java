/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Marks a class that owns a {@link org.slf4j.Logger} so that the logger's implementation obtention is performed in a synchronized
 * way by our logging separation logic.
 *
 * @since 4.10, 4.9.7, 4.6.18
 */
public interface LoggerClassRegistry {

  /**
   * Discovers an implementation of {@link LoggerClassRegistry} through {@code SPI}.
   */
  static LoggerClassRegistry getLoggerClassRegistry() {
    ServiceLoader<LoggerClassRegistry> factories = load(LoggerClassRegistry.class, LoggerClassRegistry.class.getClassLoader());
    Iterator<LoggerClassRegistry> iterator = factories.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format("Could not find %s service implementation through SPI",
                                                    LoggerClassRegistry.class.getName()));
    }
    return iterator.next();
  }

  /**
   * Registers the class to mark the logger's implementation obtention has to be performed in a synchronized way.
   *
   * @param loggerClass the class owning a {@link org.slf4j.Logger}.
   */
  void register(Class<?> loggerClass);

}
