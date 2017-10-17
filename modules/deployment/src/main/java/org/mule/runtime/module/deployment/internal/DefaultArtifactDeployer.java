/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.Boolean.valueOf;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultArtifactDeployer<T extends DeployableArtifact> implements ArtifactDeployer<T> {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  public void deploy(T artifact) {
    try {
      artifact.install();
      doInit(artifact);
      artifact.start();
    } catch (Throwable t) {
      artifact.dispose();

      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = String.format("Failed to deploy artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(I18nMessageFactory.createStaticMessage(msg), t);
    }
  }

  /**
   * Initializes the artifact by taking into account deployment properties
   * {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_DEPLOYMENT_PROPERTY}
   * and {@link org.mule.runtime.core.api.config.MuleDeploymentProperties#MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY}.
   * 
   * @param artifact the T artifact to be initialized
   */
  private void doInit(T artifact) {
    boolean lazyInit = false;
    boolean enableXmlValidations = false;
    if (artifact.getDescriptor().getDeploymentProperties().isPresent()) {
      Properties deploymentProperties = artifact.getDescriptor().getDeploymentProperties().get();
      lazyInit = valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "false"));
      enableXmlValidations = valueOf((String) deploymentProperties.getOrDefault(
                                                                                MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY,
                                                                                "false"));
    }

    if (lazyInit) {
      artifact.lazyInit(!enableXmlValidations);
    } else {
      artifact.init();
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
      throw new DeploymentException(I18nMessageFactory.createStaticMessage(msg), t);
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
