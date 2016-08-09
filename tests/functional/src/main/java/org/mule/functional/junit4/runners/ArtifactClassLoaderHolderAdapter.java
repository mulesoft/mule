/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.apache.commons.beanutils.MethodUtils.invokeMethod;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.functional.api.classloading.isolation.ArtifactClassLoaderHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Just an utility class to have in one place the calls using reflection due to {@link org.mule.functional.api.classloading.isolation.ArtifactClassLoaderHolder}
 * is loaded with the launcher class loader and used from test classes (loaded with application class loader).
 *
 * @since 4.0
 */
public class ArtifactClassLoaderHolderAdapter {

  private Object artifactClassLoaderHolder;

  /**
   * Creates an instance of the adapter
   *
   * @param artifactClassLoaderHolder the {@link ArtifactClassLoaderHolder} to be adapted
   */
  public ArtifactClassLoaderHolderAdapter(Object artifactClassLoaderHolder) {
    checkArgument(artifactClassLoaderHolder != null, "artifactClassLoaderHolder cannot be null");
    checkArgument(artifactClassLoaderHolder.getClass().getName().equals(ArtifactClassLoaderHolder.class.getName()),
                  "artifactClassLoaderHolder is an incorrect type");

    this.artifactClassLoaderHolder = artifactClassLoaderHolder;
  }

  public List<ArtifactClassLoaderAdapter> getServicesArtifactClassLoaders() {
    List<Object> artifactClassLoaders = (List<Object>) doInvokeMethod("getServicesArtifactClassLoaders");
    return adaptArtifactClassLoaders(artifactClassLoaders);
  }

  public List<ArtifactClassLoaderAdapter> getPluginsArtifactClassLoaders() {
    List<Object> artifactClassLoaders = (List<Object>) doInvokeMethod("getPluginsArtifactClassLoaders");
    return adaptArtifactClassLoaders(artifactClassLoaders);
  }

  private List<ArtifactClassLoaderAdapter> adaptArtifactClassLoaders(List<Object> artifactClassLoaders) {
    return artifactClassLoaders.stream().map(artifactClassLoader -> new ArtifactClassLoaderAdapter(artifactClassLoader)).collect(
                                                                                                                                 Collectors
                                                                                                                                     .toList());
  }

  private Object doInvokeMethod(String methodName) {
    try {
      return invokeMethod(artifactClassLoaderHolder, methodName, null);
    } catch (Exception e) {
      throw new RuntimeException("Error while getting services artifact class loaders", e);
    }
  }
}
