/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.util.ServiceLoader.load;

import org.mule.runtime.module.artifact.internal.classloader.NoOpBlockingLoggerResolutionClassRegistry;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Marks a class that owns a {@link org.slf4j.Logger} so that the logger's implementation obtention is performed in a synchronized
 * way by our logging separation logic.
 *
 * @since 4.10, 4.9.7, 4.6.18
 */
public interface BlockingLoggerResolutionClassRegistry {

  /**
   * Discovers an implementation of {@link BlockingLoggerResolutionClassRegistry} through {@code SPI}.
   */
  static BlockingLoggerResolutionClassRegistry getBlockingLoggerResolutionClassRegistry() {
    ServiceLoader<BlockingLoggerResolutionClassRegistry> factories =
        load(BlockingLoggerResolutionClassRegistry.class, BlockingLoggerResolutionClassRegistry.class.getClassLoader());
    Iterator<BlockingLoggerResolutionClassRegistry> iterator = factories.iterator();
    if (!iterator.hasNext()) {
      return new NoOpBlockingLoggerResolutionClassRegistry();
    }

    return iterator.next();
  }

  /**
   * Registers the class to mark the logger's implementation obtention has to be performed in a synchronized way.
   *
   * @param loggerClass the class owning a {@link org.slf4j.Logger}.
   */
  void registerClassNeedingBlockingLoggerResolution(Class<?> loggerClass);

}
