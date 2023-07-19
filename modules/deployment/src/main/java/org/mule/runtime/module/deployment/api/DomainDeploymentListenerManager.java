/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.api.annotation.NoImplement;

/**
 * Manages which {@link DeploymentListener} are listening for domain deployment notifications.
 */
@NoImplement
public interface DomainDeploymentListenerManager {

  void addDomainDeploymentListener(DeploymentListener listener);

  void removeDomainDeploymentListener(DeploymentListener listener);
}
