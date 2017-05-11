/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.api;

import org.mule.runtime.module.embedded.internal.DefaultDeploymentConfigurationBuilder;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * Specific deployment configuration for the artifact. This deployment configuration overrides default deployment configuration
 * for the artifact.
 * 
 * @since 1.0
 */
public interface DeploymentConfiguration extends Serializable {

  /**
   * @return true if the scheduler message sources must be stop during artifact startup
   */
  boolean disableSchedulerMessageSources();

  /**
   * @return artifact properties to use to configure the artifact on deployment.
   */
  Map<String, String> getArtifactProperties();

  /**
   * @return if true, it adds the test dependencies of the artifact into the artifact classpath. This is useful when using
   *         configuration files for testing that may make use of testing libraries.
   */
  boolean enableTestDependencies();

  /**
   * @return a new builder for the {@link DeploymentConfiguration}
   */
  static DeploymentConfigurationBuilder builder() {
    return new DefaultDeploymentConfigurationBuilder();
  }

  /**
   * Builder interface for {@link DeploymentConfiguration}. Instances must be created using
   * {@link DeploymentConfiguration#builder()} method.
   */
  interface DeploymentConfigurationBuilder {

    /**
     * Disables the scheduler message sources from triggering messages on startup.
     * 
     * @param disabled if true, then scheduler message sources won't be started by default. Default value is false.
     * @return same builder
     */
    DeploymentConfigurationBuilder withSchedulerMessageSourcesDisabled(boolean disabled);

    /**
     * This method allows to configure properties that may override the default configuration properties bundled with the
     * artifact. For instance, it may be used to point to a different set of credentials for accesing a database that may change
     * between environments.
     * 
     * @param deploymentProperties the properties to use during deployment. Default value is an empty map.
     * @return same builder
     */
    DeploymentConfigurationBuilder withArtifactProperties(Map<String, String> deploymentProperties);

    /**
     * Makes available in the classpath those dependencies of the artifact configured for test. Useful when running the embedded
     * container for testing purposes.
     * 
     * @param enabled true if test dependencies must be part of the application classpath, false otherwise. Default value is
     *        false.
     * @return same builder
     */
    DeploymentConfigurationBuilder withTestDependenciesEnabled(boolean enabled);

    /**
     * @return builds a {@link DeploymentConfiguration} deployment configuration instance with the provided configuration.
     */
    DeploymentConfiguration build();

  }

}
