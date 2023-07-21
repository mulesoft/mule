/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.plugin;

import java.util.Properties;

/**
 * Allows to associate context through deployment properties without changing the API for
 * {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader}.
 *
 * @since 4.2.0
 */
public abstract class ExtendedDeploymentProperties extends Properties {

  /**
   * Creates an instance of the deployment properties that can be extended.
   *
   * @param deploymentProperties the original properties passed from deployment service. Can be null.
   */
  public ExtendedDeploymentProperties(Properties deploymentProperties) {
    super(deploymentProperties);
  }

}
