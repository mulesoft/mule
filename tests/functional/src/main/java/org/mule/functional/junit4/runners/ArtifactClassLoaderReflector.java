/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.apache.commons.beanutils.MethodUtils.invokeMethod;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

/**
 * Provides access to an {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} using reflection.
 * <p/>
 * It has to be accessed using reflection due to {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} is
 * loaded with the launcher class loader during creation of class loaders as part of the classification process. If reflection is
 * not used the class would be a different one due to when this class is loaded it is going to be loaded with the application
 * class loader and the injected {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} was loaded with the
 * launcher class loader. So it won't be able to access its methods.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderReflector {

  private Object artifactClassLoader;

  /**
   * Creates an instance of the reflector
   *
   * @param artifactClassLoader the {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} to be called using
   *        reflection
   */
  public ArtifactClassLoaderReflector(Object artifactClassLoader) {
    checkArgument(artifactClassLoader != null, "artifactClassLoader cannot be null");

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
