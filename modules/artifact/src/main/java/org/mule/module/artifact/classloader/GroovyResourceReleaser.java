/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.slf4j.Logger;

/**
 * A utility class to release all resources associated to Groovy Dependency on un-deployment to prevent classloader leaks.
 *
 * @since 4.4.0
 */
public class GroovyResourceReleaser implements ResourceReleaser {

  private final ClassLoader classLoader;
  private static final Logger LOGGER = getLogger(GroovyResourceReleaser.class);
  private static final String GROOVY_CLASS_INFO = "org.codehaus.groovy.reflection.ClassInfo";
  private static final String GROOVY_INVOKER_HELPER = "org.codehaus.groovy.runtime.InvokerHelper";

  /**
   * Creates a new Instance of GroovyResourceReleaser
   *
   * @param classLoader ClassLoader which loaded the Groovy Dependency.
   */
  public GroovyResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    unregisterAllClassesFromInvokerHelper();
  }

  private void unregisterAllClassesFromInvokerHelper() {
    try {
      Class<?> classInfoClass = this.classLoader.loadClass(GROOVY_CLASS_INFO);
      Method getAllClassInfoMethod = classInfoClass.getMethod("getAllClassInfo");
      Method getTheClassMethod = classInfoClass.getMethod("getTheClass");
      Class<?> invokerHelperClass = this.classLoader.loadClass(GROOVY_INVOKER_HELPER);
      Method removeClassMethod = invokerHelperClass.getMethod("removeClass", Class.class);
      Object classInfos = getAllClassInfoMethod.invoke(null);
      if (classInfos instanceof Collection) {
        unregisterClassesFromInvokerHelper(removeClassMethod, getTheClassMethod, (Collection) classInfos);
      }
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
      LOGGER.warn("Error trying to remove the Groovy's InvokerHelper classes", e);
    }
  }

  private void unregisterClassesFromInvokerHelper(Method removeClassMethod, Method getTheClassMethod,
                                                  Collection<?> classes) {
    for (Object classInfo : classes) {
      Object clazz = null;
      try {
        clazz = getTheClassMethod.invoke(classInfo);
        removeClassMethod.invoke(null, clazz);
      } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
        String className = clazz instanceof Class ? ((Class) clazz).getName() : "Unknown";
        LOGGER.warn("Could not remove the {} class from the Groovy's InvokerHelper", className, e);
      }
    }
  }
}
