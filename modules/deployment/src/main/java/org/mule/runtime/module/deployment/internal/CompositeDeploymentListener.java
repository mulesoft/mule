/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentListenerManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeDeploymentListener implements DeploymentListener, DeploymentListenerManager {

  private transient final Logger logger = LoggerFactory.getLogger(getClass());

  private List<DeploymentListener> deploymentListeners = new CopyOnWriteArrayList<DeploymentListener>();

  @Override
  public void addDeploymentListener(DeploymentListener listener) {
    this.deploymentListeners.add(listener);
  }

  @Override
  public void removeDeploymentListener(DeploymentListener listener) {
    this.deploymentListeners.remove(listener);
  }

  @Override
  public void onDeploymentStart(String artifactName) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onDeploymentStart(artifactName);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onDeploymentStart", t);
      }
    }
  }

  @Override
  public void onDeploymentSuccess(String appName) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onDeploymentSuccess(appName);
      } catch (Throwable t) {
        logNotificationProcessingError(appName, listener, "onDeploymentSuccess", t);
      }
    }
  }

  @Override
  public void onDeploymentFailure(String artifactName, Throwable cause) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onDeploymentFailure(artifactName, cause);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onDeploymentFailure", t);
      }
    }
  }

  @Override
  public void onUndeploymentStart(String artifactName) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onUndeploymentStart(artifactName);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onUndeploymentStart", t);
      }
    }
  }

  @Override
  public void onUndeploymentSuccess(String artifactName) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onUndeploymentSuccess(artifactName);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onUndeploymentSuccess", t);
      }
    }
  }

  @Override
  public void onUndeploymentFailure(String artifactName, Throwable cause) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onUndeploymentFailure(artifactName, cause);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onUndeploymentFailure", t);
      }
    }
  }

  @Override
  public void onMuleContextCreated(String artifactName, MuleContext context, CustomizationService customizationService) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onMuleContextCreated(artifactName, context, customizationService);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onMuleContextCreated", t);
      }
    }
  }

  @Override
  public void onMuleContextInitialised(String artifactName, MuleContext context) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onMuleContextInitialised(artifactName, context);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onMuleContextInitialised", t);
      }
    }
  }

  @Override
  public void onMuleContextConfigured(String artifactName, MuleContext context) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onMuleContextConfigured(artifactName, context);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onMuleContextConfigured", t);
      }
    }
  }

  private void logNotificationProcessingError(String appName, DeploymentListener listener, String notification, Throwable error) {
    logger.error(String.format("Listener '%s' failed to process notification '%s' for application '%s'", listener, notification,
                               appName),
                 error);
  }
}
