/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DEPLOYMENT_FAILED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultMuleApplication;

import java.util.*;

/**
 * Utility to hook callbacks just before and after a domain zip is redeployed in Mule.
 */
public final class DomainDeploymentTemplate implements ArtifactDeploymentTemplate {

  private Collection<Application> domainApplications = Collections.emptyList();
  private Map<Application, ApplicationStatus> appStatusPreRedeployment;
  private final DefaultArchiveDeployer<Application> applicationDeployer;
  private final DeploymentService deploymentservice;
  private final CompositeDeploymentListener applicationDeploymentListener;
  private final List<ArtifactStoppedDeploymentListener> artifactStoppedDeploymentListeners = new ArrayList<>();

  public DomainDeploymentTemplate(DefaultArchiveDeployer<Application> applicationDeployer, DeploymentService deploymentservice,
                                  CompositeDeploymentListener applicationDeploymentListener) {
    this.applicationDeployer = applicationDeployer;
    this.deploymentservice = deploymentservice;
    this.applicationDeploymentListener = applicationDeploymentListener;
  }

  /**
   * Undeploys all applications that use this domain.
   */
  @Override
  public void preRedeploy(Artifact domain) {
    if (domain instanceof Domain) {
      appStatusPreRedeployment = new HashMap<>();
      domainApplications = deploymentservice.findDomainApplications(domain.getArtifactName());
      for (Application domainApplication : domainApplications) {
        appStatusPreRedeployment.put(domainApplication, domainApplication.getStatus());
        applicationDeploymentListener.onRedeploymentStart(domainApplication.getArtifactName());
        applicationDeployer.undeployArtifactWithoutUninstall(domainApplication);
      }
    }
  }

  /**
   * Deploys applications that were undeployed when {@link #preRedeploy(Artifact)} was called.
   */
  @Override
  public void postRedeploy(Artifact domain) {
    if (domain != null && !domainApplications.isEmpty()) {
      RuntimeException firstException = null;
      for (Application domainApplication : domainApplications) {
        applicationDeployer.preTrackArtifact(domainApplication);
        if (applicationDeployer.isUpdatedZombieArtifact(domainApplication.getArtifactName())) {
          try {
            applicationDeployer.deployExplodedArtifact(domainApplication.getArtifactName(),
                                                       getProperties(appStatusPreRedeployment.get(domainApplication)));
            applicationDeploymentListener.onRedeploymentSuccess(domainApplication.getArtifactName());
          } catch (RuntimeException e) {
            applicationDeploymentListener.onRedeploymentFailure(domainApplication.getArtifactName(), e);
            if (firstException == null) {
              firstException = e;
            }
          }
        }
      }
      if (firstException != null) {
        throw firstException;
      }
    }
    domainApplications = Collections.emptyList();
  }

  private Optional<Properties> getProperties(ApplicationStatus applicationStatus) {
    Properties properties = new Properties();
    boolean startArtifact = applicationStatus.equals(STARTED) || applicationStatus.equals(DEPLOYMENT_FAILED);
    properties.setProperty(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY, valueOf(startArtifact));
    return of(properties);
  }
}
