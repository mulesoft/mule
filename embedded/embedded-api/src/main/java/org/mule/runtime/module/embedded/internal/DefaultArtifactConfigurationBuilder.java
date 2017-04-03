/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal;

import static com.google.common.base.Preconditions.checkArgument;
import org.mule.runtime.module.embedded.api.Application;
import org.mule.runtime.module.embedded.api.ApplicationConfiguration;
import org.mule.runtime.module.embedded.api.DeploymentConfiguration;

/**
 * Default implementation of {@link ApplicationConfiguration.ApplicationConfigurationBuilder}.
 * 
 * @since 1.0
 */
public class DefaultArtifactConfigurationBuilder implements ApplicationConfiguration.ApplicationConfigurationBuilder {

  private Application application;
  private DeploymentConfiguration deploymentConfiguration = DeploymentConfiguration.builder().build();

  @Override
  public ApplicationConfiguration.ApplicationConfigurationBuilder withApplication(Application application) {
    this.application = application;
    return this;
  }

  @Override
  public ApplicationConfiguration.ApplicationConfigurationBuilder withDeploymentConfiguration(DeploymentConfiguration deploymentConfiguration) {
    this.deploymentConfiguration = deploymentConfiguration;
    return this;
  }

  @Override
  public ApplicationConfiguration build() {
    return new ApplicationConfigurationImpl(deploymentConfiguration, application);
  }

  /**
   * Default implementation for {@link ApplicationConfiguration}
   */
  static class ApplicationConfigurationImpl implements ApplicationConfiguration {

    private DeploymentConfiguration deploymentConfiguration;
    private Application application;

    public ApplicationConfigurationImpl(DeploymentConfiguration deploymentConfiguration, Application application) {
      checkArgument(deploymentConfiguration != null, "deploymentConfiguration cannot be null");
      checkArgument(application != null, "application cannot be null");
      this.deploymentConfiguration = deploymentConfiguration;
      this.application = application;
    }

    @Override
    public DeploymentConfiguration getDeploymentConfiguration() {
      return deploymentConfiguration;
    }

    @Override
    public Application getApplication() {
      return application;
    }

  }
}
