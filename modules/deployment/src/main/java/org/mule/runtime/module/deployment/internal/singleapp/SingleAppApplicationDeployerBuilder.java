/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.internal.CompositeDeploymentListener;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DefaultArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DeploymentMuleContextListenerFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.List;
import java.util.function.Supplier;

/**
 * A builder for an application {@link DefaultArchiveDeployer} to be used in single app mode.
 */
public class SingleAppApplicationDeployerBuilder {

  private final Supplier<SchedulerService> artifactStartExecutorSupplier;
  private ObservableList<Application> applications;
  private DefaultApplicationFactory applicationFactory;
  private DefaultArtifactDeployer applicationDeployer;
  private CompositeDeploymentListener applicationDeploymentListener;

  private SingleAppApplicationDeployerBuilder(Supplier<SchedulerService> artifactStartExecutorSupplier) {
    this.artifactStartExecutorSupplier = artifactStartExecutorSupplier;
  }

  public static SingleAppApplicationDeployerBuilder getSingleAppApplicationDeployerBuilder(Supplier<SchedulerService> artifactStartExecutorSupplier) {
    return new SingleAppApplicationDeployerBuilder(artifactStartExecutorSupplier);
  }

  public DefaultArchiveDeployer<ApplicationDescriptor, Application> build() {
    return new DefaultArchiveDeployer<>(applicationDeployer, applicationFactory, applications,
                                        NOP_ARTIFACT_DEPLOYMENT_TEMPLATE,
                                        new DeploymentMuleContextListenerFactory(applicationDeploymentListener),
                                        artifactStartExecutorSupplier);
  }

  public SingleAppApplicationDeployerBuilder withApplications(List<Application> applications) {
    this.applications = new ObservableList<>(applications);
    return this;
  }

  public SingleAppApplicationDeployerBuilder withApplicationFactory(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
    return this;
  }

  public SingleAppApplicationDeployerBuilder withApplicationDeployer(DefaultArtifactDeployer<Application> applicationDeployer) {
    this.applicationDeployer = applicationDeployer;
    return this;
  }

  public SingleAppApplicationDeployerBuilder withApplicationDeploymentListener(CompositeDeploymentListener applicationDeploymentListener) {
    this.applicationDeploymentListener = applicationDeploymentListener;
    return this;
  }
}
