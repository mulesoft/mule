/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;

import java.io.File;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Describes an artifact that is deployable on the container
 *
 * @since 4.9
 */
@NoImplement
public interface DeployableArtifactDescriptor extends ArtifactDescriptor {

  boolean isRedeploymentEnabled();

  // void setRedeploymentEnabled(boolean redeploymentEnabled);

  // /**
  // * @param location the directory where the artifact content is stored.
  // */
  // void setArtifactLocation(File location);

  /**
   * @return the directory where the artifact content is stored.
   */
  File getArtifactLocation();

  /**
   * @return the config files within the artifact, as filenames relative to the root of the packaged artifact.
   */
  Set<String> getConfigResources();

  // void setConfigResources(Set<String> configResources);

  /**
   * @return the {@code ApplicationPluginDescriptor} that describe the plugins the application requires.
   */
  Set<? extends ArtifactPluginDescriptor> getPlugins();

  // /**
  // * @param plugins a set of {@code ApplicationPluginDescriptor} which are dependencies of the application.
  // */
  // void setPlugins(Set<ArtifactPluginDescriptor> plugins);

  /**
   * @return the artifact data storage folder name
   */
  String getDataFolderName();

  /**
   * Returns a {@link File} representing the descriptor file
   *
   * @return the descriptor file
   */
  File getDescriptorFile();

  // void setLogConfigFile(File logConfigFile);

  File getLogConfigFile();

  Optional<Properties> getDeploymentProperties();

  Set<String> getSupportedJavaVersions();

  // void setSupportedJavaVersions(Set<String> supportedJavaVersions);

  /**
   * Returns a {@link String} representing the Native Libraries Folder Name of the descriptor
   *
   * @return the descriptor's Native Libraries Folder Name
   */
  String getLoadedNativeLibrariesFolderName();
}
