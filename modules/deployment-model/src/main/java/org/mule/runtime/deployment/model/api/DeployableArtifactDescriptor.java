/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Describes an artifact that is deployable on the container
 */
public class DeployableArtifactDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_DEPLOY_PROPERTIES_RESOURCE = "mule-deploy.properties";
  public static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
  public static final String PROPERTY_CONFIG_RESOURCES = "config.resources";

  public static final String MULE_POM = "pom.xml";
  public static final String MULE_POM_PROPERTIES = "pom.properties";

  private boolean redeploymentEnabled = true;
  private File location;
  private Set<String> configResources;
  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);
  private File logConfigFile;

  /**
   * Creates a new deployable artifact descriptor
   *
   * @param name artifact name. Non empty.
   */
  public DeployableArtifactDescriptor(String name) {
    super(name);
    configResources = getDefaultConfigResources();
  }

  public DeployableArtifactDescriptor(String name, Optional<Properties> properties) {
    super(name, properties);
    configResources = getDefaultConfigResources();
  }

  public boolean isRedeploymentEnabled() {
    return redeploymentEnabled;
  }

  public void setRedeploymentEnabled(boolean redeploymentEnabled) {
    this.redeploymentEnabled = redeploymentEnabled;
  }

  /**
   * @param location the directory where the artifact content is stored.
   */
  public void setArtifactLocation(File location) {
    this.location = location;
  }

  /**
   * @return the directory where the artifact content is stored.
   */
  public File getArtifactLocation() {
    return this.location;
  }

  public Set<String> getConfigResources() {
    return configResources;
  }

  public void setConfigResources(Set<String> configResources) {
    this.configResources = sanitizePaths(configResources);
  }

  private Set<String> sanitizePaths(Set<String> configResources) {
    if (configResources == null || configResources.isEmpty()) {
      return configResources;
    }

    return configResources.stream().map(s -> separatorsToUnix(s)).collect(toSet());
  }

  /**
   * @return the {@code ApplicationPluginDescriptor} that describe the plugins the application requires.
   */
  public Set<ArtifactPluginDescriptor> getPlugins() {
    return plugins;
  }

  /**
   * @param plugins a set of {@code ApplicationPluginDescriptor} which are dependencies of the application.
   */
  public void setPlugins(Set<ArtifactPluginDescriptor> plugins) {
    this.plugins = plugins;
  }

  /**
   * @return the artifact data storage folder name
   */
  public String getDataFolderName() {
    return getArtifactLocation().getName();
  }

  /**
   * Returns a {@link File} representing the descriptor file
   *
   * @return the descriptor file
   */
  public File getDescriptorFile() {
    return new File(getRootFolder(), MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION);
  }

  protected Set<String> getDefaultConfigResources() {
    return emptySet();
  }

  public void setLogConfigFile(File logConfigFile) {
    this.logConfigFile = logConfigFile;
  }

  public File getLogConfigFile() {
    return logConfigFile;
  }

}
