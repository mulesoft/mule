/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

/**
 * Finds native libraries in a particular class loading context
 */
public interface NativeLibraryFinder {

  /**
   * Finds a native library for the given name
   *
   * @param name native library to find
   * @param libraryPath library path for the given name in a parent class loading context. Can be null.
   * @return library path to use for the given name. Can be null is no library was found.
   */
  String findLibrary(String name, String libraryPath);
}
