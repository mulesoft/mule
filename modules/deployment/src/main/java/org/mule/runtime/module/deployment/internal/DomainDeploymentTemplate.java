/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.empty;

import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.Artifact;

import java.util.Collection;
import java.util.Collections;

/**
 * Utility to hook callbacks just before and after a domain zip is redeployed in Mule.
 */
public final class DomainDeploymentTemplate implements ArtifactDeploymentTemplate {

  private Collection<Application> domainApplications = Collections.emptyList();
  private final DefaultArchiveDeployer<Application> applicationDeployer;
  private final DeploymentService deploymentservice;

  public DomainDeploymentTemplate(DefaultArchiveDeployer<Application> applicationDeployer, DeploymentService deploymentservice) {
    this.applicationDeployer = applicationDeployer;
    this.deploymentservice = deploymentservice;
  }

  /**
   * Undeploys all applications that use this domain.
   */
  @Override
  public void preRedeploy(Artifact domain) {
    if (domain instanceof Domain) {
      domainApplications = deploymentservice.findDomainApplications(domain.getArtifactName());
      for (Application domainApplication : domainApplications) {
        applicationDeployer.undeployArtifactWithoutUninstall(domainApplication);
      }
    }
  }

  /**
   * Deploys applications that were undeployed when {@link #preRedeploy(Artifact)} was called..
   */
  @Override
  public void postRedeploy(Artifact domain) {
    if (domain != null && !domainApplications.isEmpty()) {
      for (Application domainApplication : domainApplications) {
        applicationDeployer.preTrackArtifact(domainApplication);
        if (applicationDeployer.isUpdatedZombieArtifact(domainApplication.getArtifactName())) {
          applicationDeployer.deployExplodedArtifact(domainApplication.getArtifactName(), empty());
        }
      }
    }
    domainApplications = Collections.emptyList();
  }
}
