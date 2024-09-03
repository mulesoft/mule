/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.getMavenConfig;
import static org.mule.runtime.globalconfig.api.maven.MavenClientFactory.createMavenClient;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider.serviceClassLoaderConfigurationLoader;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.application.DeployableMavenClassLoaderConfigurationLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.PluginMavenClassLoaderConfigurationLoader;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;

/**
 * This class is responsible of returning the {@link BundleDescriptor} of a given plugin's location and also creating a
 * {@link ClassLoaderConfiguration}
 *
 * @since 4.0
 */
// TODO MULE-11878 - consolidate with other aether usages in mule.
public class MavenClassLoaderConfigurationLoader implements ClassLoaderConfigurationLoader, Disposable {

  protected static final Logger LOGGER = getLogger(MavenClassLoaderConfigurationLoader.class);

  private DeployableMavenClassLoaderConfigurationLoader deployableMavenClassLoaderConfigurationLoader;
  private PluginMavenClassLoaderConfigurationLoader pluginMavenClassLoaderConfigurationLoader;
  private ClassLoaderConfigurationLoader libFolderClassLoaderConfigurationLoader;
  private volatile MavenConfiguration mavenRuntimeConfig;
  private volatile MavenClient mavenClient;

  private final StampedLock lock = new StampedLock();

  private void refresh() {
    long stamp = lock.readLock();
    try {
      MavenConfiguration updatedMavenConfiguration = getMavenConfig();
      if (!updatedMavenConfiguration.equals(mavenRuntimeConfig)) {
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        if (writeStamp == 0L) {
          lock.unlockRead(stamp);
          stamp = lock.writeLock();
        } else {
          stamp = writeStamp;
        }
        if (!updatedMavenConfiguration.equals(mavenRuntimeConfig)) {
          mavenRuntimeConfig = updatedMavenConfiguration;
          createClassLoaderConfigurationLoaders();
        }
      }
    } finally {
      lock.unlock(stamp);
    }
  }

  private void createClassLoaderConfigurationLoaders() {
    // This method might be invoked more than once if the Maven configuration changes, so the previous mavenClient instance (if
    // any) must be disposed
    closeMavenClient();
    mavenClient = createMavenClient(mavenRuntimeConfig);

    deployableMavenClassLoaderConfigurationLoader = new DeployableMavenClassLoaderConfigurationLoader(ofNullable(mavenClient));
    pluginMavenClassLoaderConfigurationLoader = new PluginMavenClassLoaderConfigurationLoader(ofNullable(mavenClient));

    libFolderClassLoaderConfigurationLoader = serviceClassLoaderConfigurationLoader();
  }

  @Override
  public String getId() {
    return MULE_LOADER_ID;
  }

  @Override
  public ClassLoaderConfiguration load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    refresh();

    if (deployableMavenClassLoaderConfigurationLoader.supportsArtifactType(artifactType)) {
      return deployableMavenClassLoaderConfigurationLoader.load(artifactFile, attributes, artifactType);
    } else if (pluginMavenClassLoaderConfigurationLoader.supportsArtifactType(artifactType)) {
      return pluginMavenClassLoaderConfigurationLoader.load(artifactFile, attributes, artifactType);
    } else if (libFolderClassLoaderConfigurationLoader.supportsArtifactType(artifactType)) {
      return libFolderClassLoaderConfigurationLoader.load(artifactFile, attributes, artifactType);
    } else {
      throw new IllegalStateException(format("Artifact type %s not supported", artifactType));
    }
  }

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    refresh();

    return deployableMavenClassLoaderConfigurationLoader.supportsArtifactType(artifactType)
        || pluginMavenClassLoaderConfigurationLoader.supportsArtifactType(artifactType)
        || libFolderClassLoaderConfigurationLoader.supportsArtifactType(artifactType);
  }

  @Override
  public void dispose() {
    closeMavenClient();
  }

  private void closeMavenClient() {
    if (mavenClient != null) {
      try {
        mavenClient.close();
      } catch (Exception e) {
        LOGGER.error("Error while disposing 'mavenClient'", e);
      }
    }
  }
}
