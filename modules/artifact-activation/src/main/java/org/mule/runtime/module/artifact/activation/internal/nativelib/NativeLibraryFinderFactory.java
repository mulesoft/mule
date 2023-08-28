/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

import java.net.URL;

/**
 * Creates {@link NativeLibraryFinder} instances
 */
public interface NativeLibraryFinderFactory {

  /**
   * Creates a native library finder for a given Mule artifact
   *
   * @param name name of the artifact owning the finder
   * @param urls all the URLs that are contained on the artifact's class loader
   * @return a non null instance
   */
  NativeLibraryFinder create(String name, URL[] urls);
}
