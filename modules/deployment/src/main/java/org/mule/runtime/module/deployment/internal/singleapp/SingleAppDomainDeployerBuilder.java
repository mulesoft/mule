/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.CompositeDeploymentListener;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DeploymentMuleContextListenerFactory;
import org.mule.runtime.module.deployment.internal.DomainArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DomainDeploymentTemplate;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.List;

import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;

/**
 * A builder for a domain {@link DefaultArchiveDeployer} to be used in single app mode.
 */
public class SingleAppDomainDeployerBuilder {

  private ObservableList<Domain> domains;
  private ArtifactDeployer<Domain> domainArtifactDeployer;
  private DefaultDomainFactory domainFactory;
  private DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer;
  private CompositeDeploymentListener applicationDeploymentListener;
  private CompositeDeploymentListener domainDeploymentListener;
  private DeploymentService deploymentService;
  private ObservableList<Application> applications;
  private DefaultApplicationFactory applicationFactory;
  private ArtifactDeployer<Application> applicationArtifactDeployer;

  public static SingleAppDomainDeployerBuilder getSingleAppDomainDeployerBuilder() {
    return new SingleAppDomainDeployerBuilder();
  }

  private SingleAppDomainDeployerBuilder() {}

  public DomainArchiveDeployer build() {
    return new DomainArchiveDeployer(new DefaultArchiveDeployer<>(domainArtifactDeployer, domainFactory, domains,
                                                                  new DomainDeploymentTemplate(new DefaultArchiveDeployer<>(applicationArtifactDeployer,
                                                                                                                            applicationFactory,
                                                                                                                            applications,
                                                                                                                            NOP_ARTIFACT_DEPLOYMENT_TEMPLATE,
                                                                                                                            new DeploymentMuleContextListenerFactory(applicationDeploymentListener)),
                                                                                               deploymentService,
                                                                                               applicationDeploymentListener),
                                                                  new DeploymentMuleContextListenerFactory(
                                                                                                           domainDeploymentListener)),
                                     applicationDeployer, deploymentService);

  }

  public SingleAppDomainDeployerBuilder withDomains(List<Domain> domains) {
    this.domains = new ObservableList<>(domains);
    return this;
  }

  public SingleAppDomainDeployerBuilder withApplications(List<Application> applications) {
    this.applications = new ObservableList<>(applications);
    return this;
  }

  public SingleAppDomainDeployerBuilder withApplicationFactory(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
    return this;
  }

  public SingleAppDomainDeployerBuilder withApplicationArtifactDeployer(ArtifactDeployer<Application> applicationArtifactDeployer) {
    this.applicationArtifactDeployer = applicationArtifactDeployer;
    return this;
  }

  public SingleAppDomainDeployerBuilder withDomainArtifactDeployer(ArtifactDeployer<Domain> domainArtifactDeployer) {
    this.domainArtifactDeployer = domainArtifactDeployer;
    return this;
  }

  public SingleAppDomainDeployerBuilder withDomainFactory(DefaultDomainFactory domainFactory) {
    this.domainFactory = domainFactory;
    return this;
  }

  public SingleAppDomainDeployerBuilder withApplicationDeploymentListener(CompositeDeploymentListener applicationDeploymentListener) {
    this.applicationDeploymentListener = applicationDeploymentListener;
    return this;
  }

  public SingleAppDomainDeployerBuilder withDomainDeploymentListener(CompositeDeploymentListener domainDeploymentListener) {
    this.domainDeploymentListener = domainDeploymentListener;
    return this;
  }

  public SingleAppDomainDeployerBuilder withApplicationDeployer(DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer) {
    this.applicationDeployer = applicationDeployer;
    return this;
  }

  public SingleAppDomainDeployerBuilder withDeploymentService(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
    return this;
  }

}
