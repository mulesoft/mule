/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
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
  public void onArtifactCreated(String artifactName, CustomizationService customizationService) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onArtifactCreated(artifactName, customizationService);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onArtifactCreated", t);
      }
    }
  }

  @Override
  public void onArtifactInitialised(String artifactName, Registry registry) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onArtifactInitialised(artifactName, registry);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onArtifactInitialised", t);
      }
    }
  }

  @Override
  public void onArtifactStarted(String artifactName, Registry registry) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onArtifactStarted(artifactName, registry);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onArtifactStarted", t);
      }
    }
  }

  @Override
  public void onArtifactStopped(String artifactName, Registry registry) {
    for (DeploymentListener listener : deploymentListeners) {
      try {
        listener.onArtifactStopped(artifactName, registry);
      } catch (Throwable t) {
        logNotificationProcessingError(artifactName, listener, "onArtifactStopped", t);
      }
    }
  }

  private void logNotificationProcessingError(String appName, DeploymentListener listener, String notification, Throwable error) {
    logger.error(String.format("Listener '%s' failed to process notification '%s' for application '%s'", listener, notification,
                               appName),
                 error);
  }
}
