/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils;
import org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;

public class DefaultArtifactDeployer<T extends DeployableArtifact> implements ArtifactDeployer<T> {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void deploy(T artifact, boolean startArtifact) {
    try {
      artifact.install();
      doInit(artifact);
      if (startArtifact && shouldStartArtifact(artifact)) {
        artifact.start();
      }

      ArtifactStoppedDeploymentListener artifactStoppedDeploymentListener =
          new ArtifactStoppedDeploymentListener(artifact.getArtifactName());
      ArtifactFactoryUtils.withArtifactMuleContext(artifact, muleContext -> {
        MuleRegistry muleRegistry = ((DefaultMuleContext) muleContext).getRegistry();
        muleRegistry.registerObject(ARTIFACT_STOPPED_LISTENER, artifactStoppedDeploymentListener);
      });
    } catch (Throwable t) {
      artifact.dispose();

      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = format("Failed to deploy artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(createStaticMessage(msg), t);
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
      enableXmlValidations =
          valueOf((String) deploymentProperties.getOrDefault(MULE_LAZY_INIT_ENABLE_XML_VALIDATIONS_DEPLOYMENT_PROPERTY,
                                                             "false"));
    }

    if (lazyInit) {
      artifact.lazyInit(!enableXmlValidations);
    } else {
      artifact.init();
    }
  }

  @Override
  public void undeploy(T artifact) {
    try {
      tryToStopArtifact(artifact);
      tryToDisposeArtifact(artifact);
    } catch (Throwable t) {
      if (t instanceof DeploymentException) {
        throw ((DeploymentException) t);
      }

      final String msg = format("Failed to undeployArtifact artifact [%s]", artifact.getArtifactName());
      throw new DeploymentException(createStaticMessage(msg), t);
    }
  }

  private void tryToDisposeArtifact(T artifact) {
    try {
      artifact.dispose();
    } catch (Throwable t) {
      logger.error(format("Unable to cleanly dispose artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                          artifact.getArtifactName()),
                   t);
    }
  }

  private void tryToStopArtifact(T artifact) {

    try {
      artifact.stop();
    } catch (Throwable t) {
      logger.error(format("Unable to cleanly stop artifact '%s'. Restart Mule if you get errors redeploying this artifact",
                          artifact.getArtifactName()),
                   t);
    }
  }

  private Boolean shouldStartArtifact(T artifact) {
    Properties deploymentProperties = null;
    try {
      deploymentProperties = DeploymentPropertiesUtils.resolveDeploymentProperties(artifact.getArtifactName(), Optional.empty());
    } catch (IOException e) {
      logger.error("Failed to load deployment property for artifact "
          + artifact.getArtifactName(), e);
    }
    return deploymentProperties != null
        && Boolean.parseBoolean(deploymentProperties.getProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, "true"));
  }
}
