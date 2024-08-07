/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static java.lang.Class.forName;

/**
 * A class that implements the {@link NativeLibraryUnLoader} interface to provide the functionality for unloading native libraries
 * in environments where this is not possible.
 */
public class NooOpNativeLibraryUnLoader implements NativeLibraryUnLoader {


  /**
   * Unloads the native libraries. This method should contain the logic to properly release any native resources that were loaded
   * in environments where this is not possible.
   */
  @Override
  public void unloadNativeLibraries(ClassLoader classLoader) {
    // Nothing to do for the moment.
  }
}
