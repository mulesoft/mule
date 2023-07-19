/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

import java.util.List;

/**
 * Finds native libraries in a particular class loading context
 */
public interface NativeLibraryFinder {

  /**
   * Finds a native library for the given name
   *
   * @param name        native library to find
   * @param libraryPath library path for the given name in a parent class loading context. Can be null.
   * @return library path to use for the given name. Can be null is no library was found.
   */
  String findLibrary(String name, String libraryPath);

  /**
   * Provides the native library names
   *
   * @return the native library names. Can be an empty list if no native library name is found.
   */
  List<String> findLibraryNames();
  // TODO: Delete after completing W-12786373.
}
