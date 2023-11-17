/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.lang.System.getenv;
import static java.lang.System.getProperty;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Paths.get;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.environment.singleapp.api.SingleAppStarter;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * A {@link DeploymentService} that does not allow deployment of applications. This is needed for single app mode because
 * components like the mule agent use the the deployment service to query the apps.
 *
 * @since 4.7.0.
 */
public class SingleAppEnvironmentManager implements DeploymentService, SingleAppStarter {

  public static final String MULE_APP_PATH = resolve("MULE_APP_PATH");

  private static final String MULE_DOMAIN_PATH = resolve("MULE_DOMAIN_PATH");

  private static final String MULE_APP_PROPERTIES_PATH = resolve("MULE_APP_PROPERTIES_PATH");

  private static final Logger LOGGER = LoggerFactory.getLogger(SingleAppEnvironmentManager.class);

  private static final Scheduler SINGLE_SCHEDULER = new DisabledDeploymentServiceScheduler();
  private DefaultDomainFactory domainFactory;
  private DefaultApplicationFactory applicationFactory;

  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

  private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();

  private final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();

  private final ObservableList<Application> applications = new ObservableList<>();
  private final ObservableList<Domain> domains = new ObservableList<>();


  public SingleAppEnvironmentManager() {}

  public SingleAppEnvironmentManager(DefaultDomainFactory domainFactory,
                                     DefaultApplicationFactory applicationFactory) {
    this.domainFactory = domainFactory;
    this.applicationFactory = applicationFactory;
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
    return unmodifiableList(applications);
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
    throw new UnsupportedOperationException("Application deploy operation not supported");
  }

  @Override
  public void deploy(URI appArchiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("Application deploy operation not supported");
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
  public void deployDomain(URI domainArchiveUri) {
    throw new UnsupportedOperationException("Domain deploy operation not supported");
  }

  @Override
  public void deployDomain(URI domainArchiveUri, Properties deploymentProperties) {
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
  public void deployDomainBundle(URI domainArchiveUri) {
    throw new UnsupportedOperationException("Domain bundle deploy operation not supported");
  }

  @Override
  public void start() {
    // Nothing to do.
    // The deployment service is intended to be removed in the future for
    // single app environment.
  }

  @Override
  public void startApp() {
    if (MULE_DOMAIN_PATH == null) {
      throw new SingleAppEnvironmentException("No mule domain path for single app");
    }

    if (MULE_APP_PATH == null) {
      throw new SingleAppEnvironmentException("No mule app path for single app");
    }

    if (domainFactory == null) {
      throw new SingleAppEnvironmentException("Domain factory not set");
    }

    if (applicationFactory == null) {
      throw new SingleAppEnvironmentException("Application factory not set");
    }

    DeploymentService deploymentService = new MuleDeploymentService(domainFactory,
                                                                    applicationFactory,
                                                                    new LazyValue<>(SINGLE_SCHEDULER),
                                                                    applications,
                                                                    domains,
                                                                    null);

    deploymentService.addDeploymentListener(new SingleAppDeploymentListener(applicationDeploymentListener));

    try {
      Properties properties = new Properties();
      try (InputStream inputStream = newInputStream(get(MULE_APP_PROPERTIES_PATH))) {
        properties.load(inputStream);
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("No app properties file was found");
        }
      }
      deploymentService.addDeploymentListener(applicationDeploymentListener);
      deploymentService.addDomainDeploymentListener(domainDeploymentListener);
      deploymentService.deployDomain(new File(MULE_DOMAIN_PATH).toURI());
      deploymentService.deploy(new File(MULE_APP_PATH).toURI(), properties);
      startupListeners.forEach(StartupListener::onAfterStartup);
    } catch (Exception e) {
      throw new SingleAppEnvironmentException("Error deploying app: ", e);
    }
  }

  @Override
  public void stop() {
    // Nothing to do.
  }

  @Override
  public void addDomainBundleDeploymentListener(DeploymentListener listener) {
    // Nothing to do.
  }

  @Override
  public void removeDomainBundleDeploymentListener(DeploymentListener listener) {
    // Nothing to do.
  }

  @Override
  public void addDomainDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDomainDeploymentListener(DeploymentListener listener) {
    domainDeploymentListener.removeDeploymentListener(listener);
  }

  public void addApplication(Application application) {
    applications.add(application);
  }

  public void addDomain(Domain domain) {
    domains.add(domain);
  }

  public List<StartupListener> getStartupListeners() {
    return startupListeners;
  }

  public DeploymentListener getApplicationDeploymentListener() {
    return applicationDeploymentListener;
  }

  public DeploymentListener getDomainDeploymentListener() {
    return domainDeploymentListener;
  }

  private static class DisabledDeploymentServiceScheduler implements Scheduler {

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

  private static class SingleAppDeploymentListener implements DeploymentListener {

    private final DeploymentListener deploymentListener;

    public SingleAppDeploymentListener(DeploymentListener deploymentListener) {
      this.deploymentListener = deploymentListener;
    }

    public void onDeploymentStart(String artifactName) {
      deploymentListener.onDeploymentStart(artifactName);
    }

    public void onDeploymentSuccess(String artifactName) {
      deploymentListener.onDeploymentSuccess(artifactName);
    }

    public void onDeploymentFailure(String artifactName, Throwable cause) {
      deploymentListener.onDeploymentFailure(artifactName, cause);
    }

    public void onUndeploymentStart(String artifactName) {
      deploymentListener.onUndeploymentStart(artifactName);
    }

    public void onUndeploymentSuccess(String artifactName) {
      deploymentListener.onUndeploymentSuccess(artifactName);
    }

    public void onUndeploymentFailure(String artifactName, Throwable cause) {
      deploymentListener.onUndeploymentFailure(artifactName, cause);
    }

    public void onRedeploymentStart(String artifactName) {
      deploymentListener.onRedeploymentStart(artifactName);
    }

    public void onRedeploymentSuccess(String artifactName) {
      deploymentListener.onRedeploymentSuccess(artifactName);
    }

    public void onRedeploymentFailure(String artifactName, Throwable cause) {
      deploymentListener.onRedeploymentFailure(artifactName, cause);
    }

    public void onArtifactCreated(String artifactName, CustomizationService customizationService) {
      deploymentListener.onArtifactCreated(artifactName, customizationService);
    }

    public void onArtifactInitialised(String artifactName, Registry registry) {
      deploymentListener.onArtifactInitialised(artifactName, registry);
    }

    public void onArtifactStarted(String artifactName, Registry registry) {
      deploymentListener.onArtifactStarted(artifactName, registry);
    }

    public void onArtifactStopped(String artifactName, Registry registry) {
      deploymentListener.onArtifactStopped(artifactName, registry);
    }
  }

  @Inject
  public void setDomainFactory(DefaultDomainFactory domainFactory) {
    this.domainFactory = domainFactory;
  }

  @Inject
  public void setApplicationFactory(DefaultApplicationFactory applicationFactory) {
    this.applicationFactory = applicationFactory;
  }

  public static class SingleAppEnvironmentException extends RuntimeException {

    public SingleAppEnvironmentException(String message) {
      super(message);
    }

    public SingleAppEnvironmentException(String message, Throwable e) {
      super(message, e);
    }
  }

  private static String resolve(String property) {
    return defaultIfBlank(getProperty(property), getenv(property));
  }
}
