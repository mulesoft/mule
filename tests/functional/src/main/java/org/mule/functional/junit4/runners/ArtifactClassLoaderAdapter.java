/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.apache.commons.beanutils.MethodUtils.invokeMethod;

/**
 * Just an utility class to have in one place the calls using reflection due to {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}
 * is loaded with the launcher class loader and used from test classes (loaded with application class loader).
 */
public class ArtifactClassLoaderAdapter {

  private Object artifactClassLoader;

  /**
   * Creates an instance of the adapter.
   *
   * @param artifactClassLoader the {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} to be adapted
   */
  public ArtifactClassLoaderAdapter(Object artifactClassLoader) {
    this.artifactClassLoader = artifactClassLoader;
  }

  public ClassLoader getClassLoader() {
    return (ClassLoader) doInvokeMethod("getClassLoader");
  }

  public String getArtifactName() {
    return (String) doInvokeMethod("getArtifactName");
  }

  private Object doInvokeMethod(String methodName) {
    try {
      return invokeMethod(artifactClassLoader, methodName, null);
    } catch (Exception e) {
      throw new RuntimeException("Error while getting services artifact class loaders", e);
    }
  }
}
