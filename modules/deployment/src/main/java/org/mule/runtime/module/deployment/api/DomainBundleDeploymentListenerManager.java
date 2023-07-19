/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.api;

import org.mule.api.annotation.NoImplement;

/**
 * Manages which {@link DeploymentListener} are listening for domain bundle deployment notifications.
 */
@NoImplement
public interface DomainBundleDeploymentListenerManager {

  /**
   * Adds a deployment listener for domain bundles
   * <p>
   * If the listener was added before the operation has no effect.
   *
   * @param listener listener to add
   */
  void addDomainBundleDeploymentListener(DeploymentListener listener);

  /**
   * Removes a deployment listener for domain bundles
   * <p>
   * If the listener was not added before the operation has no effect.
   *
   * @param listener listener to be removed.
   */
  void removeDomainBundleDeploymentListener(DeploymentListener listener);
}
