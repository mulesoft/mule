/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

/**
 * Enables {@link DeploymentService} injection.
 *
 * @deprecated on 4.1, use @Inject on a field or setter method of type {@link DeploymentService}
 */
@Deprecated
public interface DeploymentServiceAware {

  void setDeploymentService(DeploymentService deploymentService);
}
