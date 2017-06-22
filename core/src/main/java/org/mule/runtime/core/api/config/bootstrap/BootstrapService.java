/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.config.bootstrap;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * Creates objects from bootstrap.properties files
 */
public interface BootstrapService {

  /**
   * @return Properties defined in the service. Non null
   */
  Properties getProperties();

  /**
   * Instantiates a class using the service's class loader
   *
   * @param name fully qualified class name to instantiate
   * @param constructorArgs constructor arguments
   * @return a non null instance of the given class
   * @throws ClassNotFoundException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  Object instantiateClass(String name, Object... constructorArgs) throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, InstantiationException, IllegalAccessException;

  /**
   * Searches for a class in the service's class loader
   *
   * @param name fully qualified name of the class to search for
   * @return a class matching the fully qualified name
   * @throws ClassNotFoundException if the class was not found in the service's class loader
   */
  Class forName(String name) throws ClassNotFoundException;
}
