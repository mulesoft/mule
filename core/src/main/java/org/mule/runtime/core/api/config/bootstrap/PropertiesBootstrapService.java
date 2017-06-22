/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config.bootstrap;

import org.mule.runtime.core.api.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Provides a bootstrapping service from a given set of properties
 */
public class PropertiesBootstrapService implements BootstrapService {

  private final ClassLoader classLoader;
  private final Properties properties;

  /**
   * Creates a new instance
   *
   * @param classLoader classLoader used to create bootstrapping objects
   * @param properties properties defining the bootstrapping objects
   */
  public PropertiesBootstrapService(ClassLoader classLoader, Properties properties) {
    this.classLoader = classLoader;
    this.properties = properties;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public Object instantiateClass(String name, Object... constructorArgs) throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, InstantiationException, IllegalAccessException {
    return ClassUtils.instantiateClass(name, constructorArgs, classLoader);
  }

  @Override
  public Class forName(String name) throws ClassNotFoundException {
    return Class.forName(name, true, classLoader);
  }
}
