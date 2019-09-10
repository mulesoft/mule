/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.DEPLOYMENT_APPLICATION_PROPERTY;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());
  // fair lock
  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

  private final ObservableList<Application> applications = new ObservableList<>();
  private final ObservableList<Domain> domains = new ObservableList<>();
  private final List<StartupListener> startupListeners = new ArrayList<>();

  /**
   * TODO: move to setter as in previous version.
   */
  private final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainDeploymentListener = new CompositeDeploymentListener();
  private final CompositeDeploymentListener domainBundleDeploymentListener = new CompositeDeploymentListener();
  private final ArchiveDeployer<Domain> domainDeployer;
  private final DeploymentDirectoryWatcher deploymentDirectoryWatcher;
  private DefaultArchiveDeployer<Application> applicationDeployer;
  private DomainBundleArchiveDeployer domainBundleDeployer;

  public MuleDeploymentService(DefaultDomainFactory domainFactory, DefaultApplicationFactory applicationFactory,
                               Supplier<SchedulerService> schedulerServiceSupplier) {
    // TODO MULE-9653 : Migrate domain class loader creation to use ArtifactClassLoaderBuilder which already has support for
    // artifact plugins.
    ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>();
    ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<>();

    this.applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications,
                                                            NOP_ARTIFACT_DEPLOYMENT_TEMPLATE,
                                                            new DeploymentMuleContextListenerFactory(applicationDeploymentListener));
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
      logger.info("Using parallel deployment");
      this.deploymentDirectoryWatcher =
          new ParallelDeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains,
                                                 applications,
                                                 schedulerServiceSupplier, deploymentLock);
    } else {
      this.deploymentDirectoryWatcher =
          new DeploymentDirectoryWatcher(domainBundleDeployer, this.domainDeployer, applicationDeployer, domains, applications,
                                         schedulerServiceSupplier,
                                         deploymentLock);
    }
  }

  private boolean useParallelDeployment() {
    return getProperties().containsKey(PARALLEL_DEPLOYMENT_PROPERTY);
  }

  private boolean isDeployingSelectedAppsInOrder() {
    return !isEmpty(System.getProperty(DEPLOYMENT_APPLICATION_PROPERTY));
  }

  @Override
  public void start() {
    DeploymentStatusTracker deploymentStatusTracker = new DeploymentStatusTracker();
    addDeploymentListener(deploymentStatusTracker.getApplicationDeploymentStatusTracker());
    addDomainDeploymentListener(deploymentStatusTracker.getDomainDeploymentStatusTracker());

    StartupSummaryDeploymentListener summaryDeploymentListener =
        new StartupSummaryDeploymentListener(deploymentStatusTracker, this);
    addStartupListener(summaryDeploymentListener);

    deploymentDirectoryWatcher.start();

    for (StartupListener listener : startupListeners) {
      try {
        listener.onAfterStartup();
      } catch (Throwable t) {
        logger.error("Error executing startup listener {}", listener, t);
      }
    }
  }

  @Override
  public void stop() {
    deploymentDirectoryWatcher.stop();
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
    return Collections.unmodifiableList(applications);
  }

  @Override
  public List<Domain> getDomains() {
    return Collections.unmodifiableList(domains);
  }

  /**
   * @return URL/lastModified of apps which previously failed to deploy
   */
  Map<String, Map<URI, Long>> getZombieApplications() {
    return applicationDeployer.getArtifactsZombieMap();
  }

  Map<String, Map<URI, Long>> getZombieDomains() {
    return domainDeployer.getArtifactsZombieMap();
  }

  public void setAppFactory(ArtifactFactory<Application> appFactory) {
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
    redeploy(artifactName, empty());
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
    redeployDomain(domainName, empty());
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

  public void setDomainFactory(ArtifactFactory<Domain> domainFactory) {
    this.domainDeployer.setArtifactFactory(domainFactory);
  }

  void undeploy(Application app) {
    applicationDeployer.undeployArtifact(app.getArtifactName());
  }

  void undeploy(Domain domain) {
    domainDeployer.undeployArtifact(domain.getArtifactName());
  }

  private interface SynchronizedDeploymentAction {

    void execute();

  }

  private void deployTemplateMethod(final URI artifactArchiveUri, final Optional<Properties> deploymentProperties,
                                    File artifactDeploymentFolder, ArchiveDeployer archiveDeployer)
      throws IOException {
    executeSynchronized(() -> {
      try {
        File artifactLocation = toFile(artifactArchiveUri.toURL());
        String fileName = artifactLocation.getName();
        if (fileName.endsWith(".jar")) {
          archiveDeployer.deployPackagedArtifact(artifactArchiveUri, deploymentProperties);
        } else {
          if (!artifactLocation.getParent().equals(artifactDeploymentFolder)) {
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
        if (logger.isDebugEnabled()) {
          logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: " +
              (deploymentLock instanceof DebuggableReentrantLock ? ((DebuggableReentrantLock) deploymentLock).getOwner()
                  : "Unknown"));
        }
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
        if (logger.isDebugEnabled()) {
          logger.debug("Failure while redeploying application: " + artifactName, e);
        }
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
   * @param domainFactory the domainFactory to provide to the {@link DomainArchiveDeployer}.
   * @param domainMuleDeployer the domainMuleDeployer to provide to the {@link DomainArchiveDeployer}.
   * @param domains the domains that this DeploymentService manages.
   * @param applicationDeployer the applicationDeployer to provide to the {@link DomainArchiveDeployer}.
   * @param applicationDeploymentListener the applicationDeployer listener to provide to the {@link DomainDeploymentTemplate}.
   * @param domainDeploymentListener the domainDeploymentListener to provide to the {@link DeploymentMuleContextListenerFactory}
   *
   * @return the DomainArchiveDeployer.
   */
  protected DomainArchiveDeployer createDomainArchiveDeployer(DefaultDomainFactory domainFactory,
                                                              ArtifactDeployer domainMuleDeployer, ObservableList<Domain> domains,
                                                              DefaultArchiveDeployer<Application> applicationDeployer,
                                                              CompositeDeploymentListener applicationDeploymentListener,
                                                              DeploymentListener domainDeploymentListener) {
    return new DomainArchiveDeployer(new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains,
                                                                  new DomainDeploymentTemplate(applicationDeployer,
                                                                                               this,
                                                                                               applicationDeploymentListener),
                                                                  new DeploymentMuleContextListenerFactory(
                                                                                                           domainDeploymentListener)),
                                     applicationDeployer, this);

  }

}
