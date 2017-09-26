/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.api;

/**
 * Manages which {@link DeploymentListener} are listening for domain bundle deployment notifications.
 */

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
