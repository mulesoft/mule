/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.junit4.runners;

import static org.apache.commons.beanutils.MethodUtils.invokeMethod;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.functional.api.classloading.isolation.ArtifactsClassLoaderHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides access to an {@link ArtifactsClassLoaderHolder} using reflection.
 * <p/>
 * It has to be accessed using reflection due to {@link ArtifactsClassLoaderHolder} is loaded with the launcher class loader during
 * creation of class loaders as part of the classification process. If reflection is not used the class would be a different one
 * due to when this class is loaded it is going to be loaded with the application class loader  and the injected
 * {@link ArtifactsClassLoaderHolder} was loaded with the launcher class loader. So it won't be able to access its methods.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderHolderReflector {

  private Object artifactClassLoaderHolder;

  /**
   * Creates an instance of the reflector
   *
   * @param artifactClassLoaderHolder the {@link ArtifactsClassLoaderHolder} to be called using reflection.
   */
  public ArtifactClassLoaderHolderReflector(Object artifactClassLoaderHolder) {
    checkArgument(artifactClassLoaderHolder != null, "artifactClassLoaderHolder cannot be null");
    checkArgument(artifactClassLoaderHolder.getClass().getName().equals(ArtifactsClassLoaderHolder.class.getName()),
                  "artifactClassLoaderHolder is an incorrect type");

    this.artifactClassLoaderHolder = artifactClassLoaderHolder;
  }

  public List<ArtifactClassLoaderReflector> getServicesArtifactClassLoaders() {
    List<Object> artifactClassLoaders = (List<Object>) doInvokeMethod("getServicesArtifactClassLoaders");
    return adaptArtifactClassLoaders(artifactClassLoaders);
  }

  public List<ArtifactClassLoaderReflector> getPluginsArtifactClassLoaders() {
    List<Object> artifactClassLoaders = (List<Object>) doInvokeMethod("getPluginsArtifactClassLoaders");
    return adaptArtifactClassLoaders(artifactClassLoaders);
  }

  private List<ArtifactClassLoaderReflector> adaptArtifactClassLoaders(List<Object> artifactClassLoaders) {
    return artifactClassLoaders.stream().map(artifactClassLoader -> new ArtifactClassLoaderReflector(artifactClassLoader))
        .collect(
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
