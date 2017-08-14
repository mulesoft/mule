/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api;

import static java.util.Collections.emptyList;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes an artifact that is deployable on the container
 */
public class DeployableArtifactDescriptor extends ArtifactDescriptor {

  public static final String DEFAULT_DEPLOY_PROPERTIES_RESOURCE = "mule-deploy.properties";
  public static final String PROPERTY_REDEPLOYMENT_ENABLED = "redeployment.enabled";
  public static final String PROPERTY_CONFIG_RESOURCES = "config.resources";

  private boolean redeploymentEnabled = true;
  private File location;
  private List<String> configResources;
  private File[] configResourcesFile;
  private String[] absoluteResourcePaths;
  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);

  /**
   * Creates a new deployable artifact descriptor
   *
   * @param name artifact name. Non empty.
   */
  public DeployableArtifactDescriptor(String name) {
    super(name);
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

  public List<String> getConfigResources() {
    return configResources;
  }

  public void setConfigResources(List<String> configResources) {
    this.configResources = configResources;
  }

  public void setConfigResourcesFile(File[] configResourcesFile) {
    this.configResourcesFile = configResourcesFile;
  }

  public File[] getConfigResourcesFile() {
    return configResourcesFile;
  }

  public String[] getAbsoluteResourcePaths() {
    return absoluteResourcePaths;
  }

  public void setAbsoluteResourcePaths(String[] absoluteResourcePaths) {
    this.absoluteResourcePaths = absoluteResourcePaths;
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

  protected List<String> getDefaultConfigResources() {
    return emptyList();
  }
}
