/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

/**
 * Generic builder to create a {@link DeployableProjectModel} representing the structure of a project.
 * <p>
 * Implementations may be coupled to a specific build tool, dependency management system or project structure.
 *
 * @since 4.5
 */
public interface DeployableProjectModelBuilder {

  /**
   * Creates a {@link DeployableProjectModel}.
   *
   * @return a {@link DeployableProjectModel} representing the structure of a project.
   */
  DeployableProjectModel build();

}
