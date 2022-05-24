/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.deployable;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;

/**
 * Generic factory to create a {@link DeployableProjectModel} representing the structure of a project.
 * <p>
 * Implementations may be coupled to a specific build tool, dependency management system or project structure.
 *
 * @since 4.5
 */
public interface DeployableProjectModelFactory {

  /**
   * Creates a {@link DeployableProjectModel} describing a domain.
   *
   * @return a {@link DeployableProjectModel} representing the structure of a domain.
   */
  DeployableProjectModel<MuleDomainModel> createDomainProjectModel();

  /**
   * Creates a {@link DeployableProjectModel} describing an application.
   *
   * @return a {@link DeployableProjectModel} representing the structure of an application.
   */
  DeployableProjectModel<MuleApplicationModel> createApplicationProjectModel();

}
