/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static java.lang.Class.forName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that implements the {@link NativeLibraryUnLoader} interface to provide the functionality for unloading native libraries
 * in a Java 11 environment.
 */
public class Java11NativeLibraryUnLoader implements NativeLibraryUnLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(Java11NativeLibraryUnLoader.class);
  public static final String NATIVE_LIBRARIES_FIELD = "nativeLibraries";
  public static final String HANDLE_FIELD = "handle";
  public static final String UNLOADER_CLASS = "java.lang.ClassLoader$NativeLibrary$Unloader";

  public static final String ERROR_ON_UNLOADING_NATIVE_LIBRARY_MESSAGE = "Error on unloading native library: ";

  /**
   * Unloads the native libraries. This method should contain the logic to properly release any native resources that were loaded
   * in a Java 11 environment.
   */
  @Override
  public void unloadNativeLibraries(ClassLoader classLoader) {
    try {
      Field field = ClassLoader.class.getDeclaredField(NATIVE_LIBRARIES_FIELD);
      field.setAccessible(true);
      Map nativeLibraries = (Map) field.get(classLoader);

      if (nativeLibraries != null) {
        nativeLibraries.forEach((nativeLibraryName, nativeLibrary) -> {
          try {
            Field handleField = nativeLibrary.getClass().getDeclaredField(HANDLE_FIELD);
            handleField.setAccessible(true);
            long handle = handleField.getLong(nativeLibrary);
            Class<?> unloaderClass = forName(UNLOADER_CLASS);
            Constructor<?> constructor = unloaderClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Runnable unloader = (Runnable) constructor.newInstance(nativeLibraryName, handle, false);
            unloader.run();
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
