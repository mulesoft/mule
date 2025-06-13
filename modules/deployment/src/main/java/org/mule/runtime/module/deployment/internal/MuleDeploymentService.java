/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.ParallelDeploymentDirectoryWatcher.MAX_APPS_IN_PARALLEL_DEPLOYMENT;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleDeploymentService implements DeploymentService {

  public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
  public static final IOFileFilter JAR_ARTIFACT_FILTER =
      new AndFileFilter(new SuffixFileFilter(JAR_FILE_SUFFIX), FileFileFilter.FILE);
  public static final String PARALLEL_DEPLOYMENT_PROPERTY = SYSTEM_PROPERTY_PREFIX + "deployment.parallel";
  private static final int MAX_QUEUED_STARTING_ARTIFACTS = 256;

  private static final Logger LOGGER = LoggerFactory.getLogger(MuleDeploymentService.class);
  // fair lock
  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);
  private final LazyValue<Scheduler> artifactStartExecutor;

  private final ObservableList<Application> applications = new ObservableList<>();
  private final ObservableList<Domain> domains = new ObservableList<>();
  private final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();

  /**
   * TODO: move to setter as in previous version.
   */
  private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainBundleDeploymentListener = new CompositeDeploymentListener();
  private final ArchiveDeployer<DomainDescriptor, Domain> domainDeployer;
  private final DeploymentDirectoryWatcher deploymentDirectoryWatcher;
  private final DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer;
  private final DomainBundleArchiveDeployer domainBundleDeployer;
  private final Supplier<SchedulerService> artifactStartExecutorSupplier;

  public MuleDeploymentService(DefaultDomainFactory domainFactory, DefaultApplicationFactory applicationFactory,
                               Supplier<SchedulerService> artifactStartExecutorSupplier) {
    this.artifactStartExecutorSupplier = artifactStartExecutorSupplier;
    artifactStartExecutor = new LazyValue<>(() -> artifactStartExecutorSupplier.get()
        .customScheduler(config()
            .withName("ArtifactDeployer.start")
            .withMaxConcurrentTasks(useParallelDeployment() ? MAX_APPS_IN_PARALLEL_DEPLOYMENT : 1),
                         MAX_QUEUED_STARTING_ARTIFACTS));
    // TODO MULE-9653 : Migrate domain class loader creation to use ArtifactClassLoaderBuilder which already has support for
    // artifact plugins.
    ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>(artifactStartExecutor);
    ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<>(artifactStartExecutor);

    this.applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications,
                                                            NOP_ARTIFACT_DEPLOYMENT_TEMPLATE,
                                                            new DeploymentMuleContextListenerFactory(applicationDeploymentListener),
                                                            artifactStartExecutorSupplier);
    this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
    this.domainDeployer = createDomainArchiveDeployer(domainFactory, domainMuleDeployer, domains, applicationDeployer,
                                                      applicationDeploymentListener, domainDeploymentListener);
    this.domainDeployer.setDeploymentListener(domainDeploymentListener);

    this.domainBundleDeployer = new DomainBundleArchiveDeployer(domainBundleDeploymentListener, domainDeployer, domains,
                                                                applicationDeployer, applications, domainDeploymentListener,
                                                                applicationDeploymentListener, this);

    if (useParallelDeployment()) {
      if (isDeployingSelectedAppsInOrder()) {
        throw new IllegalArgumentException(format("Deployment parameters '%s' and '%s' cannot be used together",
                                                  DEPLOYMENT_APPLICATION_PROPERTY, PARALLEL_DEPLOYMENT_PROPERTY));
      }
      LOGGER.info("Using parallel deployment");
      this.deploymentDirectoryWatcher =
          new ParallelDeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains,
                                                 applications,
                                                 artifactStartExecutorSupplier, deploymentLock);
    } else {
      this.deploymentDirectoryWatcher =
          new DeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains, applications,
                                         artifactStartExecutorSupplier,
                                         deploymentLock);
    }
  }

  static boolean useParallelDeployment() {
    return getProperties().containsKey(PARALLEL_DEPLOYMENT_PROPERTY);
  }

  private boolean isDeployingSelectedAppsInOrder() {
    return !isEmpty(getProperty(DEPLOYMENT_APPLICATION_PROPERTY));
  }

  @Override
  public void start() {
    start(true);
  }

  /**
   * Starts the service, and optionally starts the directory watcher.
   *
   * @param startDirectoryWatcher whether to start the directory watcher or not.
   */
  public void start(boolean startDirectoryWatcher) {
    DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
    addDeploymentListener(deploymentStatusTracker.getApplicationDeploymentStatusTracker());
    addDomainDeploymentListener(deploymentStatusTracker.getDomainDeploymentStatusTracker());

    StartupSummaryDeploymentListener summaryDeploymentListener =
        new StartupSummaryDeploymentListener(deploymentStatusTracker, this);
    addStartupListener(summaryDeploymentListener);

    if (startDirectoryWatcher) {
      deploymentDirectoryWatcher.start();
    }

    notifyStartupListeners();
  }

  /**
   * Triggers one pass of the directory watcher once in the current thread. It takes the lock in order to avoid the option of
   * "waiting to the next poll" present in the run implementation.
   */
  public void triggerDirectoryWatcher() {
    deploymentLock.lock();
    try {
      deploymentDirectoryWatcher.run();
    } finally {
      deploymentLock.unlock();
    }
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
    deploymentDirectoryWatcher.stop();
    artifactStartExecutor.ifComputed(ExecutorService::shutdownNow);
  }

  @Override
  public Domain findDomain(String domainName) {
    return deploymentDirectoryWatcher.findArtifact(domainName, domains);
  }

  @Override
  public Application findApplication(String appName) {
    return deploymentDirectoryWatcher.findArtifact(appName, applications);
  }

  @Override
  public Collection<Application> findDomainApplications(final String domain) {
    Preconditions.checkArgument(domain != null, "Domain name cannot be null");
    return applications.stream()
        .filter(application -> application.getDomain() != null && application.getDomain().getArtifactName().equals(domain))
        .collect(toList());
  }


  @Override
  public List<Application> getApplications() {
    return unmodifiableList(applications);
  }

  @Override
  public List<Domain> getDomains() {
    return unmodifiableList(domains);
  }

  /**
   * @return URL/lastModified of apps which previously failed to deploy
   */
  public Map<String, Map<URI, Long>> getZombieApplications() {
    return applicationDeployer.getArtifactsZombieMap();
  }

  public Map<String, Map<URI, Long>> getZombieDomains() {
    return domainDeployer.getArtifactsZombieMap();
  }

  public void setAppFactory(ArtifactFactory<ApplicationDescriptor, Application> appFactory) {
    this.applicationDeployer.setArtifactFactory(appFactory);
  }

  @Override
  public ReentrantLock getLock() {
    return deploymentLock;
  }

  @Override
  public void undeploy(String appName) {
    executeSynchronized(() -> applicationDeployer.undeployArtifact(appName));
  }

  @Override
  public void deploy(URI appArchiveUri) throws IOException {
    deploy(appArchiveUri, empty());
  }

  private void deploy(final URI appArchiveUri, final Optional<Properties> deploymentProperties) throws IOException {
    deployTemplateMethod(appArchiveUri, deploymentProperties, getAppsFolder(), applicationDeployer);
  }

  @Override
  public void deploy(URI appArchiveUri, Properties appProperties) throws IOException {
    deploy(appArchiveUri, ofNullable(appProperties));
  }

  @Override
  public void redeploy(String artifactName) {
    redeploy(artifactName, getPersistedDeploymentProperties(artifactName));
  }

  @Override
  public void redeploy(String artifactName, Properties appProperties) {
    redeploy(artifactName, ofNullable(appProperties));
  }

  @Override
  public void redeploy(URI archiveUri, Properties appProperties) throws IOException {
    deployTemplateMethod(archiveUri, ofNullable(appProperties), getAppsFolder(), applicationDeployer);
  }

  @Override
  public void redeploy(URI archiveUri) throws IOException {
    redeploy(archiveUri, null);
  }

  @Override
  public void undeployDomain(String domainName) {
    executeSynchronized(() -> domainDeployer.undeployArtifact(domainName));
  }

  @Override
  public void deployDomain(URI domainArchiveUri) throws IOException {
    deployDomain(domainArchiveUri, empty());
  }

  private void deployDomain(URI domainArchiveUri, Optional<Properties> deploymentProperties) throws IOException {
    deployTemplateMethod(domainArchiveUri, deploymentProperties, getDomainsFolder(), domainDeployer);
  }

  @Override
  public void redeployDomain(String domainName) {
    Optional<Properties> deploymentProperties = getPersistedDeploymentProperties(domainName);
    redeployDomain(domainName, deploymentProperties);
  }

  private void redeployDomain(String domainName, Optional<Properties> deploymentProperties) {
    executeSynchronized(() -> domainDeployer.redeploy(domainName, deploymentProperties));
  }

  @Override
  public void deployDomainBundle(URI domainArchiveUri) throws IOException {
    executeSynchronized(() -> domainBundleDeployer.deployArtifact(domainArchiveUri));
  }

  @Override
  public void addStartupListener(StartupListener listener) {
    this.startupListeners.add(listener);
  }

  @Override
  public void removeStartupListener(StartupListener listener) {
    this.startupListeners.remove(listener);
  }

  @Override
  public void addDeploymentListener(DeploymentListener listener) {
    applicationDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDeploymentListener(DeploymentListener listener) {
    applicationDeploymentListener.removeDeploymentListener(listener);
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
  public void addDomainBundleDeploymentListener(DeploymentListener listener) {
    domainBundleDeploymentListener.addDeploymentListener(listener);
  }

  @Override
  public void removeDomainBundleDeploymentListener(DeploymentListener listener) {
    domainBundleDeploymentListener.removeDeploymentListener(listener);
  }

  public void setDomainFactory(ArtifactFactory<DomainDescriptor, Domain> domainFactory) {
    this.domainDeployer.setArtifactFactory(domainFactory);
  }

  public void undeploy(Application app) {
    applicationDeployer.undeployArtifact(app.getArtifactName());
  }

  public void undeploy(Domain domain) {
    domainDeployer.undeployArtifact(domain.getArtifactName());
  }

  private interface SynchronizedDeploymentAction {

    void execute();

  }

  private <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void deployTemplateMethod(final URI artifactArchiveUri,
                                                                                                    final Optional<Properties> deploymentProperties,
                                                                                                    File artifactDeploymentFolder,
                                                                                                    ArchiveDeployer<D, T> archiveDeployer)
      throws IOException {
    executeSynchronized(() -> {
      try {
        File artifactLocation = toFile(artifactArchiveUri.toURL());
        String fileName = artifactLocation.getName();
        if (fileName.endsWith(".jar")) {
          archiveDeployer.deployPackagedArtifact(artifactArchiveUri, deploymentProperties);
        } else {
          if (!artifactLocation.getParent().equals(artifactDeploymentFolder.getPath())) {
            try {
              copyDirectory(artifactLocation, new File(artifactDeploymentFolder, fileName));
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          }
          archiveDeployer.deployExplodedArtifact(fileName, deploymentProperties);
        }
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(e);
      }
    });
  }

  private void executeSynchronized(SynchronizedDeploymentAction deploymentAction) {
    try {
      if (!deploymentLock.tryLock(0, SECONDS)) {
        LOGGER.debug("Another deployment operation in progress, will skip this cycle. Owner thread: {}",
                     (deploymentLock instanceof DebuggableReentrantLock ? ((DebuggableReentrantLock) deploymentLock).getOwner()
                         : "Unknown"));
        return;
      }
      deploymentAction.execute();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (deploymentLock.isHeldByCurrentThread()) {
        deploymentLock.unlock();
      }
    }
  }

  /**
   * @param serviceManager the manager to do the lookup of the service in.
   * @return the instance of the {@link SchedulerService} from within the given {@code serviceManager}.
   */
  public static SchedulerService findSchedulerService(ServiceRepository serviceManager) {
    final List<Service> services = serviceManager.getServices();
    return (SchedulerService) services.stream().filter(s -> s instanceof SchedulerService).findFirst().get();
  }

  @Override
  public void deployDomain(URI domainArchiveUri, Properties appProperties) throws IOException {
    deployDomain(domainArchiveUri, ofNullable(appProperties));
  }

  private void redeploy(final String artifactName, final Optional<Properties> deploymentProperties) {
    executeSynchronized(() -> {
      try {
        applicationDeployer.redeploy(artifactName, deploymentProperties);
      } catch (DeploymentException e) {
        LOGGER.atDebug()
            .setCause(e)
            .log("Failure while redeploying application: {}", artifactName);
      }
    });
  }

  @Override
  public void redeployDomain(String domainName, Properties deploymentProperties) {
    redeployDomain(domainName, ofNullable(deploymentProperties));
  }

  /**
   * Creates a {@link DomainArchiveDeployer}. Override this method for testing purposes.
   *
   * @param domainFactory                 the domainFactory to provide to the {@link DomainArchiveDeployer}.
   * @param domainMuleDeployer            the domainMuleDeployer to provide to the {@link DomainArchiveDeployer}.
   * @param domains                       the domains that this DeploymentService manages.
   * @param applicationDeployer           the applicationDeployer to provide to the {@link DomainArchiveDeployer}.
   * @param applicationDeploymentListener the applicationDeployer listener to provide to the {@link DomainDeploymentTemplate}.
   * @param domainDeploymentListener      the domainDeploymentListener to provide to the
   *                                      {@link DeploymentMuleContextListenerFactory}
   * @return the DomainArchiveDeployer.
   */
  protected DomainArchiveDeployer createDomainArchiveDeployer(DefaultDomainFactory domainFactory,
                                                              ArtifactDeployer<Domain> domainMuleDeployer,
                                                              ObservableList<Domain> domains,
                                                              DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer,
                                                              CompositeDeploymentListener applicationDeploymentListener,
                                                              DeploymentListener domainDeploymentListener) {
    return new DomainArchiveDeployer(new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains,
                                                                  new DomainDeploymentTemplate(applicationDeployer,
                                                                                               this,
                                                                                               applicationDeploymentListener),
                                                                  new DeploymentMuleContextListenerFactory(domainDeploymentListener),
                                                                  artifactStartExecutorSupplier),
                                     applicationDeployer, this);

  }

}
