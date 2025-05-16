/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.internal;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.module.artifact.api.classloader.LoggerClassRegistry;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link LoggerClassRegistry} for classes owning a Log4j logger.
 */
public class Log4jLoggerClassRegistry implements LoggerClassRegistry {

  private static final Collection<Class<?>> loggerClasses = new ArrayList<>();

  @Override
  public void register(Class<?> loggerClass) {
    loggerClasses.add(loggerClass);
  }

  public static Collection<String> getLoggerClassesNames() {
    return loggerClasses.stream().map(Class::getName).toList();
  }

}
