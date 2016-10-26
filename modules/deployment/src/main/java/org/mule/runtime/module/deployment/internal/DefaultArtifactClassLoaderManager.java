/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all the {@link ArtifactClassLoader} created on the container.
 */
public class DefaultArtifactClassLoaderManager implements ArtifactClassLoaderManager, ArtifactClassLoaderRepository {

  private final Map<String, ArtifactClassLoader> artifactClassLoaders = new ConcurrentHashMap<>();

  @Override
  public void register(ArtifactClassLoader artifactClassLoader) {

    checkArgument(artifactClassLoader != null, "artifactClassLoader cannot be null");

    artifactClassLoaders.put(artifactClassLoader.getArtifactId(), artifactClassLoader);
  }

  @Override
  public ArtifactClassLoader unregister(String classLoaderId) {
    checkClassLoaderId(classLoaderId);

    return artifactClassLoaders.remove(classLoaderId);
  }

  @Override
  public ArtifactClassLoader find(String classLoaderId) {
    checkClassLoaderId(classLoaderId);

    return artifactClassLoaders.get(classLoaderId);
  }

  private void checkClassLoaderId(String classLoaderId) {
    checkArgument(!StringUtils.isEmpty(classLoaderId), "artifactId cannot be empty");
  }
}
