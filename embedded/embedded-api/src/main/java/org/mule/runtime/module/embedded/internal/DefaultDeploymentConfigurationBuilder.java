/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal;

import org.mule.runtime.module.embedded.api.DeploymentConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link DeploymentConfiguration.DeploymentConfigurationBuilder}.
 * 
 * @since 1.0
 */
public class DefaultDeploymentConfigurationBuilder implements DeploymentConfiguration.DeploymentConfigurationBuilder {

  private boolean schedulerMessageSourceDisabled;
  private Map<String, String> artifactProperties = new HashMap<>();
  private boolean enabledTestDependencies;

  @Override
  public DeploymentConfiguration.DeploymentConfigurationBuilder withSchedulerMessageSourcesDisabled(boolean disabled) {
    this.schedulerMessageSourceDisabled = disabled;
    return this;
  }

  @Override
  public DeploymentConfiguration.DeploymentConfigurationBuilder withArtifactProperties(Map<String, String> artifactProperties) {
    this.artifactProperties = artifactProperties;
    return this;
  }

  @Override
  public DeploymentConfiguration.DeploymentConfigurationBuilder withTestDependenciesEnabled(boolean enabled) {
    enabledTestDependencies = enabled;
    return this;
  }

  @Override
  public DeploymentConfiguration build() {
    return new DeploymentConfigurationImpl(schedulerMessageSourceDisabled, artifactProperties, enabledTestDependencies);
  }

  static class DeploymentConfigurationImpl implements DeploymentConfiguration {

    private final Map<String, String> artifactProperties;
    private final boolean schedulerMessageSourceDisabled;
    private final boolean enabledTestDependencies;

    public DeploymentConfigurationImpl(boolean schedulerMessageSourceDisabled, Map<String, String> artifactProperties,
                                       boolean enableTestDependencies) {
      this.schedulerMessageSourceDisabled = schedulerMessageSourceDisabled;
      this.artifactProperties = artifactProperties;
      this.enabledTestDependencies = enableTestDependencies;
    }

    @Override
    public boolean disableSchedulerMessageSources() {
      return schedulerMessageSourceDisabled;
    }

    public Map<String, String> getArtifactProperties() {
      return artifactProperties;
    }

    @Override
    public boolean enableTestDependencies() {
      return enabledTestDependencies;
    }

  }
}
