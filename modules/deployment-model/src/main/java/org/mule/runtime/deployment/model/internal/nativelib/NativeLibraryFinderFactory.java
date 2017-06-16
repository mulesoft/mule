/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

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
