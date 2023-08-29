/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.containsType;
import static org.mule.runtime.core.internal.logging.LogUtil.log;

import static java.lang.String.format;

import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;

import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;

import java.util.function.Supplier;

import org.mule.runtime.module.deployment.internal.DeploymentMuleContextListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DeploymentService} that only allows to deploy one unpacked app.
 *
 * @since 4.6.0
 */
public class SingleAppDeploymentService extends AbstractSingleAppDeploymentService {

  public static SingleAppDeploymentService createSingleAppDeploymentService(ArtifactDeployer<DeployableArtifact<ApplicationDescriptor>> artifactDeployer,
                                                                            Supplier<Application> applicationSupplier) {
    return new SingleAppDeploymentService(artifactDeployer, applicationSupplier);
  }

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private final ArtifactDeployer<DeployableArtifact<ApplicationDescriptor>> applicationDeployer;

  private final Supplier<Application> applicationSupplier;


  private SingleAppDeploymentService(ArtifactDeployer<DeployableArtifact<ApplicationDescriptor>> applicationDeployer,
                                     Supplier<Application> applicationSupplier) {
    this.applicationDeployer = applicationDeployer;
    this.applicationSupplier = applicationSupplier;
  }

  @Override
  public void start() {
    Application artifact = resolveApplication();

    try {
      applicationDeploymentListener.onDeploymentStart(artifact.getArtifactName());
      applicationDeployer.deploy(artifact);
      setApplication(artifact);
      applicationDeploymentListener.onDeploymentSuccess(artifact.getArtifactName());
    } catch (Throwable t) {
      String artifactName = artifact.getArtifactName();
      if (containsType(t, DeploymentStartException.class)) {
        log(miniSplash(format("Failed to deploy artifact '%s', see artifact's log for details", artifactName)));
        logger.error(t.getMessage());
      } else {
        log(miniSplash(format("Failed to deploy artifact '%s', see below", artifactName)));
        logger.error(t.getMessage(), t);
      }

      applicationDeploymentListener.onDeploymentFailure(artifactName, t);

      if (t instanceof DeploymentException) {
        throw (DeploymentException) t;
      } else {
        throw new DeploymentException(createStaticMessage("Failed to deploy artifact: " + artifactName), t);
      }
    }

    notifyStartupListeners();
  }

  private Application resolveApplication() {
    try {
      Application application = applicationSupplier.get();
      application.setMuleContextListener(new DeploymentMuleContextListenerFactory(applicationDeploymentListener)
          .create(application.getArtifactName()));
      return application;
    } catch (Exception e) {
      throw new DeploymentException(createStaticMessage("Failed to find single app artifact"), e);
    }
  }

}
