/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DeploymentUtils.deployExplodedDomains;

import static java.lang.System.getenv;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.CompositeDeploymentListener;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DefaultArtifactDeployer;
import org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher;
import org.mule.runtime.module.deployment.internal.DeploymentFileResolver;
import org.mule.runtime.module.deployment.internal.DomainArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DomainBundleArchiveDeployer;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DeploymentService} that allows only the deployment of one application.
 *
 * @since 4.7.0.
 */
public class SingleAppDeploymentService implements DeploymentService, Startable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleAppDeploymentService.class);
  public static final String MULE_APPS_PATH_ENV = "MULE_APPS_PATH";

  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

  private static final Scheduler SINGLE_SCHEDULER = new StartUpDeploymentServiceScheduler();

  private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();

  private final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();
  private final DomainArchiveDeployer domainDeployer;

  private final ObservableList<Domain> domains = new ObservableList<>();
  private final ObservableList<Application> applications = new ObservableList<>();
  private final DeploymentFileResolver fileResolver;
  private final Supplier<SchedulerService> artifactStartExecutorSupplier;

  private DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer;
  private Consumer<Throwable> deploymentErrorConsumer = t -> {
  };
  private DeploymentDirectoryWatcher deploymentDirectoryWatcher;
  private final CompositeDeploymentListener domainBundleDeploymentListener = new CompositeDeploymentListener();

  public SingleAppDeploymentService(SingleAppDomainDeployerBuilder singleAppDomainDeployerBuilder,
                                    SingleAppApplicationDeployerBuilder applicationDeployerBuilder,
                                    DeploymentFileResolver fileResolver,
                                    List<Application> applications,
                                    List<Domain> domains,
                                    Supplier<SchedulerService> artifactStartExecutorSupplier) {
    this.applications.addAll(applications);
    this.domains.addAll(domains);
    this.fileResolver = fileResolver;
    this.artifactStartExecutorSupplier = artifactStartExecutorSupplier;

    LazyValue<Scheduler> artifactStartExecutor = new LazyValue<>(SINGLE_SCHEDULER);
    ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>(artifactStartExecutor);

    this.domainDeployer = singleAppDomainDeployerBuilder
        .withDomains(this.domains)
        .withDeploymentService(this)
        .withDomainArtifactDeployer(new DefaultArtifactDeployer<>(artifactStartExecutor))
        .withDomainDeploymentListener(domainDeploymentListener)
        .withApplications(applications)
        .withApplicationDeploymentListener(applicationDeploymentListener)
        .withApplicationArtifactDeployer(applicationMuleDeployer)
        .build();

    this.applicationDeployer = applicationDeployerBuilder
        .withApplicationDeployer(new DefaultArtifactDeployer<>(artifactStartExecutor))
        .withApplicationDeploymentListener(applicationDeploymentListener)
        .withApplications(this.applications)
        .build();

    this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
  }



  @Override
  public void addDeploymentListener(DeploymentListener listener) {
    checkArgument(listener != null, "Listener cannot be null");
    applicationDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDeploymentListener(DeploymentListener listener) {
    checkArgument(listener != null, "Listener cannot be null");
    applicationDeploymentListener.removeDeploymentListener(listener);
  }

  @Override
  public Application findApplication(String appName) {
    checkArgument(appName != null, "Application name cannot be null");
    return applications.stream().filter(app -> app.getArtifactName().equals(appName)).findFirst().orElse(null);
  }

  @Override
  public List<Application> getApplications() {
    return applications;
  }

  @Override
  public Domain findDomain(String domainName) {
    checkArgument(domainName != null, "Domain name cannot be null");
    return domains.stream().filter(domain -> domain.getArtifactName().contains(domainName)).findFirst().orElse(null);
  }

  @Override
  public Collection<Application> findDomainApplications(String domainName) {
    checkArgument(domainName != null, "Domain name cannot be null");
    return applications.stream()
        .filter(application -> application.getDomain() != null && application.getDomain().getArtifactName().equals(domainName))
        .collect(toList());
  }

  @Override
  public List<Domain> getDomains() {
    return unmodifiableList(domains);
  }

  @Override
  public void addStartupListener(StartupListener listener) {
    startupListeners.add(listener);
  }

  @Override
  public void removeStartupListener(StartupListener listener) {
    startupListeners.remove(listener);
  }

  @Override
  public ReentrantLock getLock() {
    return deploymentLock;
  }

  @Override
  public void undeploy(String appName) {
    throw new UnsupportedOperationException("Application undeploy operation not supported");
  }

  @Override
  public void deploy(URI appArchiveUri) throws IOException {
    deploy(appArchiveUri, empty());
  }

  @Override
  public void deploy(URI appArchiveUri, Properties appProperties) throws IOException {
    deploy(appArchiveUri, ofNullable(appProperties));
  }

  private synchronized void deploy(final URI appUri, final Optional<Properties> deploymentProperties) throws IOException {
    if (!applications.isEmpty()) {
      throw new UnsupportedOperationException("A deployment cannot be done if there is an already deployed app in single app mode.");
    }

    try {
      File artifactLocation = fileResolver.resolve(appUri);
      String fileName = artifactLocation.getName();
      if (fileName.endsWith(".jar")) {
        applicationDeployer.deployPackagedArtifact(appUri, deploymentProperties);
      } else {
        if (!artifactLocation.getParent().equals(appUri.getPath())) {
          try {
            copyDirectory(artifactLocation, new File(getAppsFolder(), fileName));
          } catch (IOException e) {
            throw new MuleRuntimeException(e);
          }
        }
        applicationDeployer.deployExplodedArtifact(fileName, deploymentProperties);
      }
    } catch (Throwable t) {
      deploymentErrorConsumer.accept(t);
    }
  }

  @Override
  public void redeploy(String artifactName) {
    throw new UnsupportedOperationException("Application redeploy operation not supported");
  }

  @Override
  public void redeploy(String artifactName, Properties appProperties) {
    throw new UnsupportedOperationException("Application redeploy operation not supported");
  }

  @Override
  public void redeploy(URI archiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("Application redeploy operation not supported");
  }

  @Override
  public void redeploy(URI archiveUri) throws IOException {
    throw new UnsupportedOperationException("Application redeploy operation not supported");
  }

  @Override
  public void undeployDomain(String domainName) {
    throw new UnsupportedOperationException("Domain undeploy operation not supported");
  }

  @Override
  public void deployDomain(URI domainArchiveUri) throws IOException {
    throw new UnsupportedOperationException("Domain deploy operation not supported");
  }

  @Override
  public void deployDomain(URI domainArchiveUri, Properties deploymentProperties) throws IOException {
    throw new UnsupportedOperationException("Domain deploy operation not supported");
  }

  @Override
  public void redeployDomain(String domainName, Properties deploymentProperties) {
    throw new UnsupportedOperationException("Domain redeploy operation not supported");
  }

  @Override
  public void redeployDomain(String domainName) {
    throw new UnsupportedOperationException("Domain redeploy operation not supported");
  }

  @Override
  public void deployDomainBundle(URI domainArchiveUri) throws IOException {
    throw new UnsupportedOperationException("Domain bundle deploy operation not supported");
  }

  @Override
  public void start() {
    try {
      String muleAppsPath = getenv(MULE_APPS_PATH_ENV);
      if (muleAppsPath != null) {
        if (muleAppsPath.toLowerCase().endsWith(JAR_FILE_SUFFIX)) {
          throw new IllegalArgumentException("Invalid Mule app path: '" + muleAppsPath + "'");
        }
        startDeployment(new File(muleAppsPath).toURI());
      } else {
        startDeploymentDirectoryWatcher();
      }
      notifyStartupListeners();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Error on starting single app mode"), e);
    }
  }

  private void startDeployment(URI appUri) throws IOException {
    deployExplodedDomains(domainDeployer);
    deploy(appUri);
  }

  private void startDeploymentDirectoryWatcher() {
    this.deploymentDirectoryWatcher =
        resolveDeploymentDirectoryWatcher();

    applicationDeploymentListener.addDeploymentListener(new DeploymentListener() {

      @Override
      public void onDeploymentSuccess(String artifactName) {
        deploymentDirectoryWatcher.stop();
      }

      @Override
      public void onDeploymentFailure(String artifactName, Throwable cause) {
        deploymentErrorConsumer.accept(cause);
      }
    });

    this.deploymentDirectoryWatcher.start();
  }

  protected DeploymentDirectoryWatcher resolveDeploymentDirectoryWatcher() {
    return new DeploymentDirectoryWatcher(new DomainBundleArchiveDeployer(domainBundleDeploymentListener, domainDeployer, domains,
                                                                          applicationDeployer, applications,
                                                                          domainDeploymentListener,
                                                                          applicationDeploymentListener, this),
                                          this.domainDeployer,
                                          applicationDeployer,
                                          domains,
                                          applications,
                                          artifactStartExecutorSupplier,
                                          deploymentLock,
                                          false);
  }

  public void notifyStartupListeners() {
    for (StartupListener listener : startupListeners) {
      try {
        listener.onAfterStartup();
      } catch (Throwable t) {
        LOGGER.error("Error executing startup listener {}", listener, t);
      }
    }
  }


  @Override
  public void stop() {
    deploymentDirectoryWatcher.stop(true);
  }

  @Override
  public void addDomainBundleDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDomainBundleDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.removeDeploymentListener(listener);
  }

  @Override
  public void addDomainDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDomainDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.removeDeploymentListener(listener);
  }

  @Override
  public void onDeploymentError(Consumer<Throwable> deploymentErrorConsumer) {
    this.deploymentErrorConsumer = deploymentErrorConsumer;
  }

  public DeploymentListener getApplicationDeploymentListener() {
    return applicationDeploymentListener;
  }

  public DeploymentListener getDomainDeploymentListener() {
    return domainDeploymentListener;
  }

  public List<StartupListener> getStartupListeners() {
    return startupListeners;
  }

  private static class StartUpDeploymentServiceScheduler implements Scheduler {

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
      throw new UnsupportedOperationException("");
    }

    @Override
    public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
      throw new UnsupportedOperationException("");
    }

    @Override
    public void stop() {
      // Nothing to do.
    }

    @Override
    public String getName() {
      return "Disabled Deployment Service Scheduler";
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("");
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("Schedule with delay not supported");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      throw new UnsupportedOperationException("Schedule with delay not supported");
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException("Schedule with delay not supported");
    }

    @Override
    public void shutdown() {
      // Nothing to do.
    }

    @Override
    public List<Runnable> shutdownNow() {
      return null;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException("Await termination not supported");
    }


    @Override
    public <T> Future<T> submit(Callable<T> task) {
      try {
        return immediateFuture(task.call());
      } catch (Exception e) {
        return immediateFailedFuture(e);
      }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
      try {
        task.run();
        return immediateFuture(result);
      } catch (Exception e) {
        return immediateFailedFuture(e);
      }
    }

    @Override
    public Future<?> submit(Runnable task) {
      try {
        task.run();
        return immediateFuture(null);
      } catch (Exception e) {
        return immediateFailedFuture(e);
      }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
      return tasks.stream().map(this::submit).collect(toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
      return tasks.stream().map(this::submit).collect(toList());
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException {
      try {
        return tasks.iterator().next().call();
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws ExecutionException {
      try {
        return tasks.iterator().next().call();
      } catch (Exception e) {
        throw new ExecutionException(e);
      }
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }
}
