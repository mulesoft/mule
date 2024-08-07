/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

/**
 * This interface defines a contract for unloading native libraries. Implementations of this interface should provide the logic to
 * unload native libraries that were previously loaded.
 */
public interface NativeLibraryUnLoader {

  /**
   * Unloads the native libraries. Implementations of this method should ensure that any native resources that were loaded are
   * properly released.
   *
   * @param classLoader the classloader to unload the native libraries from.
   */
  void unloadNativeLibraries(ClassLoader classLoader);
}
