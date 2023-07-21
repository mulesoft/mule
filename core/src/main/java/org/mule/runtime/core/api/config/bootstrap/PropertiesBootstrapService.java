/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config.bootstrap;

import org.mule.runtime.core.api.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Provides a bootstrapping service from a given set of properties
 */
public final class PropertiesBootstrapService implements BootstrapService {

  private final ClassLoader classLoader;
  private final Properties properties;

  /**
   * Creates a new instance
   *
   * @param classLoader classLoader used to create bootstrapping objects
   * @param properties  properties defining the bootstrapping objects
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
