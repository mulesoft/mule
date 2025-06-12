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
        // If the ClassInfo class was loaded by this classloader, then it means it is the owner of the Groovy dependency itself.
        // In that case we want to make sure there are no classes retained by the ClassInfos.
        boolean unregisterAllClasses = isOwnedClassLoader(classInfoClass.getClassLoader(), classLoader);
        unregisterClassesFromInvokerHelper(removeClassMethod, getTheClassMethod, (Collection<?>) classInfos,
                                           unregisterAllClasses);
      }
    } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
      LOGGER.warn("Error trying to remove the Groovy's InvokerHelper classes", e);
    }
  }

  private void unregisterClassesFromInvokerHelper(Method removeClassMethod,
                                                  Method getTheClassMethod,
                                                  Collection<?> classes,
                                                  boolean unregisterAllClasses) {
    for (Object classInfo : classes) {
      Class<?> clazz = null;
      try {
        clazz = (Class<?>) getTheClassMethod.invoke(classInfo);

        // Only unregister classes owned by this classloader, unless specified otherwise
        // This is to avoid interference between sibling artifacts
        if (unregisterAllClasses || isOwnedClassLoader(classLoader, clazz.getClassLoader())) {
          removeClassMethod.invoke(null, clazz);
        }
      } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
        String className = clazz != null ? clazz.getName() : "Unknown";
        LOGGER.atWarn()
            .setCause(e)
            .log("Could not remove the {} class from the Groovy's InvokerHelper", className);
      }
    }
  }

  private boolean isOwnedClassLoader(ClassLoader ownerClassLoader, ClassLoader classLoader) {
    // Traverse the hierarchy for this ClassLoader searching for the same instance of the ownerClassLoader.
    while (classLoader != null) {
      if (classLoader == ownerClassLoader) {
        return true;
      }
      classLoader = classLoader.getParent();
    }
    return false;
  }
}
