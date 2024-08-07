/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

/**
 * Utility class for obtaining an appropriate {@link NativeLibraryUnLoader} implementation based on the current Java version.
 */
public class NativeLibraryUnLoaderUtils {

  private static final NativeLibraryUnLoader JAVA8_NATIVE_LIBRARIES_UNLOADER = new Java8NativeLibraryUnLoader();

  private static final NativeLibraryUnLoader JAVA11_NATIVE_LIBRARIES_UNLOADER = new Java11NativeLibraryUnLoader();

  private static final NativeLibraryUnLoader NOOP_NATIVE_LIBRARIES_UNLOADER = new NooOpNativeLibraryUnLoader();

  /**
   * Returns an instance of {@link NativeLibraryUnLoader} appropriate for the current Java version.
   * <p>
   * This method checks the Java version and returns an implementation specific to that version. Currently, it supports Java 8 and
   * Java 11.
   * </p>
   *
   * @return an instance of {@link NativeLibraryUnLoader}
   */
  public static NativeLibraryUnLoader getNativeLibraryUnLoader(String version) {
    if (version.startsWith("1.8")) {
      return JAVA8_NATIVE_LIBRARIES_UNLOADER;
    } else if (version.startsWith("11")) {
      return JAVA11_NATIVE_LIBRARIES_UNLOADER;
    }

    return NOOP_NATIVE_LIBRARIES_UNLOADER;
  }
}
