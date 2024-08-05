/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that implements the {@link NativeLibraryUnLoader} interface to provide the functionality for unloading native libraries
 * in a Java 8 environment.
 */
public class Java8NativeLibraryUnLoader implements NativeLibraryUnLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(Java8NativeLibraryUnLoader.class);
  public static final String NATIVE_LIBRARIES_FIELD = "nativeLibraries";
  public static final String FINALIZE_METHOD = "finalize";
  public static final String ERROR_ON_UNLOADING_NATIVE_LIBRARY_MESSAGE = "Error on unloading native library: ";

  /**
   * Unloads the native libraries. This method should contain the logic to properly release any native resources that were loaded
   * in a Java 8 environment.
   */
  @Override
  public void unloadNativeLibraries(ClassLoader classLoader) {
    try {
      Field field = ClassLoader.class.getDeclaredField(NATIVE_LIBRARIES_FIELD);
      field.setAccessible(true);
      Vector nativeLibraries = (Vector) field.get(classLoader);

      if (nativeLibraries != null) {
        nativeLibraries.forEach(nativeLibrary -> {
          try {
            Method finalizeMethod = nativeLibrary.getClass().getDeclaredMethod(FINALIZE_METHOD);
            finalizeMethod.setAccessible(true);
            finalizeMethod.invoke(nativeLibrary);
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        });
      }

    } catch (Throwable e) {
      LOGGER.warn(ERROR_ON_UNLOADING_NATIVE_LIBRARY_MESSAGE, e);
    }
  }
}
