/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.core.internal.context.ArtifactStoppedListener;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactoryUtils;

import java.util.*;

import static java.lang.String.valueOf;
import static java.util.Optional.of;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DEPLOYMENT_FAILED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;

/**
 * Utility to hook callbacks just before and after a domain zip is redeployed in Mule.
 */
public final class DomainDeploymentTemplate implements ArtifactDeploymentTemplate {

  private final DefaultArchiveDeployer<Application> applicationDeployer;
  private final DeploymentService deploymentservice;
  private final CompositeDeploymentListener applicationDeploymentListener;
  private final Map<String, ArtifactStoppedListener> artifactStoppedMuleContextListeners = new HashMap<>();
  private Collection<Application> domainApplications = Collections.emptyList();
  private Map<Application, ApplicationStatus> appStatusPreRedeployment;

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
        ArtifactFactoryUtils.withArtifactMuleContext(domainApplication, muleContext -> {
          Optional<ArtifactStoppedListener> optionalArtifactStoppedListener =
              Optional.ofNullable(((DefaultMuleContext) muleContext).getRegistry().lookupObject(ARTIFACT_STOPPED_LISTENER));
          optionalArtifactStoppedListener.ifPresent(artifactStoppedListener -> artifactStoppedMuleContextListeners
              .put(domainApplication.getArtifactName(), artifactStoppedListener));
        });
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
            Optional<DeployableArtifact> optDeployableArtifact =
                Optional.ofNullable(applicationDeployer.deployExplodedArtifact(domainApplication.getArtifactName(),
                                                                               getProperties(appStatusPreRedeployment
                                                                                   .get(domainApplication))));
            optDeployableArtifact
                .ifPresent(deployableArtifact -> ArtifactFactoryUtils.withArtifactMuleContext(deployableArtifact, muleContext -> {
                  MuleRegistry muleRegistry = ((DefaultMuleContext) muleContext).getRegistry();
                  ArtifactStoppedListener artifactStoppedListener =
                      artifactStoppedMuleContextListeners.get(deployableArtifact.getArtifactName());
                  artifactStoppedListener.mustPersist(true);
                  muleRegistry.registerObject(ARTIFACT_STOPPED_LISTENER, artifactStoppedListener);
                }));
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
