/*
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.descriptor;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.launcher.domain.Domain.DEFAULT_DOMAIN_NAME;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ApplicationDescriptor extends DeployableArtifactDescriptor {

  public static final String DEFAULT_CONFIGURATION_RESOURCE = "mule-config.xml";
  public static final String DEFAULT_APP_PROPERTIES_RESOURCE = "mule-app.properties";

  private String encoding;
  private String domain = DEFAULT_DOMAIN_NAME;
  private String[] configResources = new String[] {DEFAULT_CONFIGURATION_RESOURCE};
  private String[] absoluteResourcePaths;
  private File[] configResourcesFile;
  private Map<String, String> appProperties = new HashMap<String, String>();
  private File logConfigFile;
  private Set<ArtifactPluginDescriptor> plugins = new HashSet<>(0);

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
    checkArgument(!StringUtils.isEmpty(domain), "Domain name cannot be empty");
    this.domain = domain;
  }

  public String[] getConfigResources() {
    return configResources;
  }

  public void setConfigResources(String[] configResources) {
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
}
