/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.api.annotation.NoImplement;

/**
 * Manages which {@link DeploymentListener} are listening for deployment notifications.
 */
@NoImplement
public interface DeploymentListenerManager {

  void addDeploymentListener(DeploymentListener listener);

  void removeDeploymentListener(DeploymentListener listener);
}
