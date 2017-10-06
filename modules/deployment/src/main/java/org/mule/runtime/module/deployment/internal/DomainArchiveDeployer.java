/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.util.Optional.empty;

import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Archive deployer for domains.
 * <p/>
 * Knows how to deploy / undeploy a domain and a domain bundle (zip with domain + domains apps).
 */
public class DomainArchiveDeployer implements ArchiveDeployer<Domain> {

  private transient final Logger logger = LoggerFactory.getLogger(getClass());

  public static final String DOMAIN_BUNDLE_APPS_FOLDER = "apps";
  private final ArchiveDeployer<Domain> domainDeployer;
  private final DeploymentService deploymentService;
  private final ArchiveDeployer<Application> applicationDeployer;

  public DomainArchiveDeployer(ArchiveDeployer<Domain> domainDeployer, ArchiveDeployer<Application> applicationDeployer,
                               DeploymentService deploymentService) {
    this.domainDeployer = domainDeployer;
    this.applicationDeployer = applicationDeployer;
    this.deploymentService = deploymentService;
  }

  @Override
  public boolean isUpdatedZombieArtifact(String artifactName) {
    // Domains do not manage zombie artifacts
    return true;
  }

  /**
   * Undeploys a domain.
   * <p/>
   * Before undeploying the domain it undeploys the applications associated.
   *
   * @param artifactId domain name to undeploy
   */
  @Override
  public void undeployArtifact(String artifactId) {
    Collection<Application> domainApplications = findApplicationsAssociated(artifactId);
    for (Application domainApplication : domainApplications) {
      applicationDeployer.undeployArtifact(domainApplication.getArtifactName());
    }
    domainDeployer.undeployArtifact(artifactId);
  }

  private Collection<Application> findApplicationsAssociated(String artifactId) {
    Domain domain = deploymentService.findDomain(artifactId);
    Preconditions.checkArgument(domain != null, String.format("Domain %s does not exists", artifactId));
    return findApplicationsAssociated(domain);
  }

  private Collection<Application> findApplicationsAssociated(Domain domain) {
    return deploymentService.findDomainApplications(domain.getArtifactName());
  }

  @Override
  public File getDeploymentDirectory() {
    return domainDeployer.getDeploymentDirectory();
  }

  @Override
  public void setDeploymentListener(CompositeDeploymentListener deploymentListener) {
    domainDeployer.setDeploymentListener(deploymentListener);
  }

  @Override
  public Map<String, Map<URI, Long>> getArtifactsZombieMap() {
    return domainDeployer.getArtifactsZombieMap();
  }

  @Override
  public void setArtifactFactory(ArtifactFactory<Domain> artifactFactory) {
    domainDeployer.setArtifactFactory(artifactFactory);
  }

  @Override
  public void undeployArtifactWithoutUninstall(Domain artifact) {
    throw new NotImplementedException("undeploy without uninstall is not supported for domains");
  }

  @Override
  public Domain deployPackagedArtifact(URI domainArchiveUri, Optional<Properties> appProperties) throws DeploymentException {
    return domainDeployer.deployPackagedArtifact(domainArchiveUri, appProperties);
  }

  @Override
  public Domain deployPackagedArtifact(String zip, Optional<Properties> deploymentProperties) throws DeploymentException {
    return domainDeployer.deployPackagedArtifact(zip, deploymentProperties);
  }

  @Override
  public void redeploy(Domain artifact, Optional<Properties> deploymentProperties) throws DeploymentException {
    Collection<Application> domainApplications = findApplicationsAssociated(artifact);
    for (Application domainApplication : domainApplications) {
      applicationDeployer.undeployArtifactWithoutUninstall(domainApplication);
    }
    try {
      domainDeployer.redeploy(artifact, deploymentProperties);
    } catch (DeploymentException e) {
      logger.warn(String.format("Failure during redeployment of domain %s, domain applications deployment will be skipped",
                                artifact.getArtifactName()));
      throw e;
    }
    for (Application domainApplication : domainApplications) {
      try {
        applicationDeployer.redeploy(domainApplication, deploymentProperties);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Error redeploying application {}", domainApplication.getArtifactName(), e);
        }
      }
    }
  }

  @Override
  public void deployArtifact(Domain artifact, Optional<Properties> deploymentProperties) throws DeploymentException {
    domainDeployer.deployArtifact(artifact, deploymentProperties);
  }

  @Override
  public Domain deployExplodedArtifact(String artifactDir, Optional<Properties> deploymentProperties) {
    return domainDeployer.deployExplodedArtifact(artifactDir, deploymentProperties);
  }
}
