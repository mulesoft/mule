/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import java.util.Properties;

/**
 * Allows to associate context through deployment properties without changing the API for
 * {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader}.
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
