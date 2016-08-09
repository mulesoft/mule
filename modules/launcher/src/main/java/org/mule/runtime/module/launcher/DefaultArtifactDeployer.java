/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.module.launcher.artifact.DeployableArtifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultArtifactDeployer<T extends DeployableArtifact> implements ArtifactDeployer<T> {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  public void deploy(T artifact) {
    try {
      artifact.install();
      artifact.init();
      artifact.start();
    } catch (Throwable t) {
      artifact.dispose();

      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = String.format("Failed to deploy artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
    }
  }

  public void undeploy(T artifact) {
    try {
      tryToStopArtifact(artifact);
      tryToDisposeArtifact(artifact);
    } catch (Throwable t) {
      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = String.format("Failed to undeployArtifact artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(MessageFactory.createStaticMessage(msg), t);
    }
  }

  private void tryToDisposeArtifact(T artifact) {
    try {
      artifact.dispose();
    } catch (Throwable t) {
      logger.error(String.format(
                                 "Unable to cleanly dispose artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                                 artifact.getArtifactName()),
                   t);
    }
  }

  private void tryToStopArtifact(T artifact) {

    try {
      artifact.stop();
    } catch (Throwable t) {
      logger.error(String.format("Unable to cleanly stop artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                                 artifact.getArtifactName()),
                   t);
    }
  }

}
