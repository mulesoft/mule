/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.api;

import org.mule.runtime.module.embedded.internal.DefaultArtifactConfigurationBuilder;

import java.io.Serializable;

/**
 * Configuration of an artifact to be run by the embedded container.
 * 
 * @since 1.0
 */
public interface ApplicationConfiguration extends Serializable {

  /**
   * @return the {@link DeploymentConfiguration} of the artifact
   */
  DeploymentConfiguration getDeploymentConfiguration();

  /**
   * @return the configuration of the application to run.
   */
  Application getApplication();

  /**
   * @return a new builder to create an {@link Application} instance.
   */
  static ApplicationConfigurationBuilder builder() {
    return new DefaultArtifactConfigurationBuilder();
  }

  /**
   * Builder interface to create instances of {@link Application}.
   */
  interface ApplicationConfigurationBuilder {

    /**
     * @param application the application to run.
     * @return same builder
     */
    ApplicationConfigurationBuilder withApplication(Application application);

    /**
     * @param deploymentConfiguration the specific configuration for running the application.
     * @return same builder
     */
    ApplicationConfigurationBuilder withDeploymentConfiguration(DeploymentConfiguration deploymentConfiguration);

    /**
     * @return a new instance of {@link ApplicationConfiguration} with the provided configuration.
     */
    ApplicationConfiguration build();

  }

}
