/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static java.util.Optional.empty;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Provides parallel deployment of Mule applications.
 *
 * @since 3.8.2
 */
public class ParallelDeploymentDirectoryWatcher extends DeploymentDirectoryWatcher {

  private Scheduler threadPoolExecutor;

  public ParallelDeploymentDirectoryWatcher(
                                            DomainBundleArchiveDeployer domainBundleDeployer,
                                            ArchiveDeployer<Domain> domainArchiveDeployer,
                                            ArchiveDeployer<Application> applicationArchiveDeployer,
                                            ObservableList<Domain> domains, ObservableList<Application> applications,
                                            Supplier<SchedulerService> schedulerServiceSupplier, ReentrantLock deploymentLock) {
    super(domainBundleDeployer, domainArchiveDeployer, applicationArchiveDeployer, domains, applications,
          schedulerServiceSupplier, deploymentLock);
  }

  @Override
  protected void deployPackedApps(String[] zips) {
    if (zips.length == 0) {
      return;
    }

    List<Callable<Object>> tasks = new ArrayList<>(zips.length);
    for (final String zip : zips) {
      tasks.add(() -> {
        try {
          applicationArchiveDeployer.deployPackagedArtifact(zip, empty());
        } catch (Exception e) {
          // Ignore and continue
        }
        return null;
      });
    }

    waitForTasksToFinish(tasks);
  }


  @Override
  protected void deployExplodedApps(String[] apps) {
    List<Callable<Object>> tasks = new ArrayList<>(apps.length);

    for (final String addedApp : apps) {
      if (applicationArchiveDeployer.isUpdatedZombieArtifact(addedApp)) {
        tasks.add(() -> {
          try {
            applicationArchiveDeployer.deployExplodedArtifact(addedApp, empty());
          } catch (Exception e) {
            // Ignore and continue
          }
          return null;
        });
      }
    }

    if (!tasks.isEmpty()) {
      waitForTasksToFinish(tasks);
    }
  }

  private void waitForTasksToFinish(List<Callable<Object>> tasks) {
    this.threadPoolExecutor =
        schedulerServiceSupplier.get().ioScheduler(config().withName("parallelDeployment").withMaxConcurrentTasks(20));
    try {
      final List<Future<Object>> futures = threadPoolExecutor.invokeAll(tasks);

      for (Future<Object> future : futures) {
        try {
          future.get();
        } catch (ExecutionException e) {
          // Ignore and continue with the next one
        }
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      threadPoolExecutor.stop();
    }
  }
}
