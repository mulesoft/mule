/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;

import static java.util.Arrays.stream;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provides single deployment of Mule applications.
 *
 * @since 4.7.0
 */
public class SingleDeploymentDirectoryWatcher extends DeploymentDirectoryWatcher {

  private String artifact;

  public SingleDeploymentDirectoryWatcher(DomainBundleArchiveDeployer domainBundleDeployer,
                                          ArchiveDeployer<DomainDescriptor, Domain> domainArchiveDeployer,
                                          ArchiveDeployer<ApplicationDescriptor, Application> applicationArchiveDeployer,
                                          ObservableList<Domain> domains, ObservableList<Application> applications,
                                          Supplier<SchedulerService> schedulerServiceSupplier, ReentrantLock deploymentLock) {
    super(domainBundleDeployer, domainArchiveDeployer, applicationArchiveDeployer, domains, applications,
          schedulerServiceSupplier, deploymentLock);
  }

  @Override
  protected void deployPackedApps(String[] zips) {
    if (isThereAnArtifactToDeploy(zips)) {
      String artifactToDeploy = zips[0];
      if (noArtifactHasBeenDeployed()) {
        setArtifact(artifactToDeploy);
      }
      if (isTheDeployedArtifactEqualsTo(artifactToDeploy)) {
        super.deployPackedApps(new String[] {artifactToDeploy});
      }
    }
  }

  @Override
  protected void deployExplodedApps(String[] apps) {
    if (isThereAnArtifactToDeploy(apps)) {
      String artifactToDeploy = apps[0];
      if (noArtifactHasBeenDeployed()) {
        setArtifact(artifactToDeploy);
      }
      if (isTheDeployedArtifactEqualsTo(artifactToDeploy)) {
        super.deployExplodedApps(new String[] {artifactToDeploy});
      }
    }
  }

  private boolean isThereAnArtifactToDeploy(String[] artifacts) {
    return stream(artifacts).findFirst().isPresent();
  }

  private boolean noArtifactHasBeenDeployed() {
    return this.artifact == null;
  }

  private void setArtifact(String artifact) {
    this.artifact = artifact.replace(JAR_FILE_SUFFIX, "");
  }

  private boolean isTheDeployedArtifactEqualsTo(String artifact) {
    return this.artifact.equals(artifact) || this.artifact.equals(artifact.replace(JAR_FILE_SUFFIX, ""));
  }
}
