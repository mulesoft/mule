/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.api;

import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.module.embedded.internal.DefaultEmbeddedContainerBuilder;

import java.net.URL;

/**
 * Interface that represents an embedded container
 *
 * @since 4.0
 */
public interface EmbeddedContainer {

  /**
   * Starts the container.
   */
  void start();

  /**
   * Stops the container.
   */
  void stop();

  /**
   * @return a new builder to create an {@link EmbeddedContainer}.
   */
  static EmbeddedContainerBuilder builder() {
    return new DefaultEmbeddedContainerBuilder();
  }


  /**
   * Builder for {@link EmbeddedContainer} instances. To create an instance of this builder use
   * {@link EmbeddedContainer#builder()} method
   */
  interface EmbeddedContainerBuilder {

    /**
     * @param muleVersion mule version to use for running the artifact.
     * @return same builder
     */
    EmbeddedContainerBuilder withMuleVersion(String muleVersion);

    /**
     * @param containerBaseFolder folder to use as the mule base folder.
     * @return same builder
     */
    EmbeddedContainerBuilder withContainerBaseFolder(URL containerBaseFolder);

    /**
     * @param applicationConfiguration the configuration of the application to run.
     * @return same builder
     */
    EmbeddedContainerBuilder withApplicationConfiguration(ApplicationConfiguration applicationConfiguration);

    /**
     * Customizes the log4j configuration file for the artifact.
     *
     * @param log4JConfigurationFile absolute path to the log4j configuration file.
     * @return same builder
     */
    EmbeddedContainerBuilder withLog4jConfigurationFile(String log4JConfigurationFile);

    /**
     * Customizes the maven configuration for the execution.
     *
     * @param mavenConfiguration maven configuration.
     * @return same buildeer
     */
    EmbeddedContainerBuilder withMavenConfiguration(MavenConfiguration mavenConfiguration);

    /**
     * @return creates a new {@link EmbeddedContainer} with the provided configuration.
     */
    EmbeddedContainer build();

  }

}
