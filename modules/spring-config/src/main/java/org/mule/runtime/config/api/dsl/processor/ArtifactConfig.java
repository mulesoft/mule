/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the application configuration files that describe the integrations. It does not include resource files or
 * application descriptors.
 *
 * An application configuration is defined by an application name and a set of configuration files containing the integration
 * required components.
 *
 * @since 4.0
 */
public class ArtifactConfig {

  private String artifactName;
  private List<ConfigFile> configFiles = new ArrayList<>();
  private Map<String, String> artifactProperties;

  private ArtifactConfig() {}

  public String getArtifactName() {
    return artifactName;
  }

  public List<ConfigFile> getConfigFiles() {
    return Collections.unmodifiableList(configFiles);
  }

  /**
   * @return properties associated to the configuration.
   */
  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  /**
   * Builder for {@link ArtifactConfig} instances.
   */
  public static class Builder {

    private ArtifactConfig applicationConfig = new ArtifactConfig();

    /**
     * @param applicationName the artifact name
     * @return the builder
     */
    public Builder setApplicationName(String applicationName) {
      this.applicationConfig.artifactName = applicationName;
      return this;
    }

    /**
     * @param configFile a {@code ConfigFile} to be added to the application.
     * @return the builder
     */
    public Builder addConfigFile(ConfigFile configFile) {
      this.applicationConfig.configFiles.add(configFile);
      return this;
    }

    /**
     * @param artifactProperties properties associated to the configuration.
     * @return the builder
     */
    public Builder setArtifactProperties(Map<String, String> artifactProperties) {
      this.applicationConfig.artifactProperties = artifactProperties;
      return this;
    }

    public ArtifactConfig build() {
      return this.applicationConfig;
    }
  }
}
