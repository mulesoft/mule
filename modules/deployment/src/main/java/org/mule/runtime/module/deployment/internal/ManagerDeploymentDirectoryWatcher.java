/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.PARALLEL_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.useParallelDeployment;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the deployment directory needed.
 *
 * @since 4.7.0
 */
public class ManagerDeploymentDirectoryWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagerDeploymentDirectoryWatcher.class);

  private final ReentrantLock deploymentLock;
  private final ObservableList<Application> applications;
  private final ObservableList<Domain> domains;
  private final ArchiveDeployer<DomainDescriptor, Domain> domainDeployer;
  private final DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer;
  private final DomainBundleArchiveDeployer domainBundleDeployer;
  private final Supplier<SchedulerService> artifactStartExecutorSupplier;

  public ManagerDeploymentDirectoryWatcher(ReentrantLock deploymentLock, ObservableList<Application> applications,
                                           ObservableList<Domain> domains,
                                           ArchiveDeployer<DomainDescriptor, Domain> domainDeployer,
                                           DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer,
                                           DomainBundleArchiveDeployer domainBundleDeployer,
                                           Supplier<SchedulerService> artifactStartExecutorSupplier) {
    this.deploymentLock = deploymentLock;
    this.applications = applications;
    this.domains = domains;
    this.domainDeployer = domainDeployer;
    this.applicationDeployer = applicationDeployer;
    this.domainBundleDeployer = domainBundleDeployer;
    this.artifactStartExecutorSupplier = artifactStartExecutorSupplier;
  }

  public DeploymentDirectoryWatcher getDeploymentDirectoryWatcher() {
    if (useParallelDeployment()) {
      if (isDeployingSelectedAppsInOrder()) {
        throw new IllegalArgumentException(format("Deployment parameters '%s' and '%s' cannot be used together",
                                                  DEPLOYMENT_APPLICATION_PROPERTY, PARALLEL_DEPLOYMENT_PROPERTY));
      }
      LOGGER.info("Using parallel deployment");
      return new ParallelDeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains,
                                                    applications, artifactStartExecutorSupplier, deploymentLock);
    } else {
      if (isSingleDeployment()) {
        return new SingleDeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains,
                                                    applications, artifactStartExecutorSupplier, deploymentLock);
      } else {
        return new DeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains,
                                              applications, artifactStartExecutorSupplier, deploymentLock);
      }
    }
  }

  private boolean isDeployingSelectedAppsInOrder() {
    return !isEmpty(getProperty(DEPLOYMENT_APPLICATION_PROPERTY));
  }

  private boolean isSingleDeployment() {
    return getProperties().containsKey(SINGLE_DEPLOYMENT_PROPERTY);
  }
}
