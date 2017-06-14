/*
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.application;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppConfigFolderPath;
import static org.mule.runtime.deployment.model.api.domain.Domain.DEFAULT_DOMAIN_NAME;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ApplicationDescriptor extends DeployableArtifactDescriptor {

  public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-config.xml";
  public static final String DEFAULT_CONFIGURATION_RESOURCE_LOCATION = Paths.get("mule", "mule-config.xml").toString();
  public static final String REPOSITORY_FOLDER = "repository";
  public static final String MULE_APPLICATION_JSON = "mule-application.json";
  public static final String MULE_APPLICATION_JSON_LOCATION =
      Paths.get("META-INF", "mule-artifact", "mule-application.json").toString();

  private String encoding;
  private String domain = DEFAULT_DOMAIN_NAME;
  private List<String> configResources =
      ImmutableList.<String>builder().add(getAppConfigFolderPath() + DEFAULT_CONFIGURATION_RESOURCE).build();
  private String[] absoluteResourcePaths;
  private File[] configResourcesFile;
  private Map<String, String> appProperties = new HashMap<String, String>();
  private File logConfigFile;
  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);
  private ArtifactDeclaration artifactDeclaration;

  /**
   * Creates a new application descriptor
   *
   * @param name application name. Non empty.
   */
  public ApplicationDescriptor(String name) {
    super(name);
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public Map<String, String> getAppProperties() {
    return appProperties;
  }

  public void setAppProperties(Map<String, String> appProperties) {
    this.appProperties = appProperties;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    checkArgument(!isEmpty(domain), "Domain name cannot be empty");
    this.domain = domain;
  }

  public List<String> getConfigResources() {
    return configResources;
  }

  public void setConfigResources(List<String> configResources) {
    this.configResources = configResources;
  }

  public String[] getAbsoluteResourcePaths() {
    return absoluteResourcePaths;
  }

  public void setAbsoluteResourcePaths(String[] absoluteResourcePaths) {
    this.absoluteResourcePaths = absoluteResourcePaths;
  }

  public void setConfigResourcesFile(File[] configResourcesFile) {
    this.configResourcesFile = configResourcesFile;
  }

  public File[] getConfigResourcesFile() {
    return configResourcesFile;
  }

  public void setLogConfigFile(File logConfigFile) {
    this.logConfigFile = logConfigFile;
  }

  public File getLogConfigFile() {
    return logConfigFile;
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
   * @return programmatic definition of the application configuration.
   */
  public ArtifactDeclaration getArtifactDeclaration() {
    return artifactDeclaration;
  }

  /**
   * @param artifactDeclaration programmatic definition of the application configuration.
   */
  public void setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
    this.artifactDeclaration = artifactDeclaration;
  }
}
