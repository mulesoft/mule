/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Class loading utility methods.
 * 
 * @since 4.2.0
 */
public class ClassLoaderUtil {

  private static Optional<ClassLoader> platformClassLoader;

  /**
   * Method that lookups the platform classloader used to load JDK classes since java 9.
   * <p/>
   * It does the lookup using reflection so the code works also with JDK 1.8.
   * 
   * @return the platform classloader or empty.
   */
  public static Optional<ClassLoader> getPlatformClassLoader() {
    return empty();
    //if (platformClassLoader == null) {
    //  try {
    //    Method getPlatformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader", new Class[0]);
    //    platformClassLoader = of((ClassLoader) getPlatformClassLoader.invoke(null, new Object[0]));
    //
    //  } catch (NoSuchMethodException e) {
    //    platformClassLoader = empty();
    //  } catch (IllegalAccessException | InvocationTargetException e) {
    //    throw new MuleRuntimeException(e);
    //  }
    //}
    //return platformClassLoader;
  }

}
