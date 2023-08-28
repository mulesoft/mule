/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
