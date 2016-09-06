/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.api;

/**
 * Manages which {@link DeploymentListener} are listening for deployment notifications.
 */
public interface DeploymentListenerManager {

  void addDeploymentListener(DeploymentListener listener);

  void removeDeploymentListener(DeploymentListener listener);
}
