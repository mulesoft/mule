/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.processor;

import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.dsl.api.xml.parser.ConfigFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents the application configuration files that describe the integrations. It does not include resource files or
 * application descriptors.
 *
 * An application configuration is defined by an application name and a set of configuration files containing the integration
 * required components.
 *
 * @since 4.0
 * @deprecated since 4.4 use {@link AstXmlParser} instead.
 */
@Deprecated
public final class ArtifactConfig {

  private String artifactName;
  private final List<ConfigFile> configFiles = new ArrayList<>();

  private ArtifactConfig() {}

  public String getArtifactName() {
    return artifactName;
  }

  public List<ConfigFile> getConfigFiles() {
    return Collections.unmodifiableList(configFiles);
  }

  /**
   * Builder for {@link ArtifactConfig} instances.
   */
  public static class Builder {

    private final ArtifactConfig applicationConfig = new ArtifactConfig();

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
     * @param configFiles a collection of {@code ConfigFile} to be added to the application.
     * @return the builder
     */
    public Builder addConfigFiles(Collection<ConfigFile> configFiles) {
      this.applicationConfig.configFiles.addAll(configFiles);
      return this;
    }

    public ArtifactConfig build() {
      return this.applicationConfig;
    }
  }
}
