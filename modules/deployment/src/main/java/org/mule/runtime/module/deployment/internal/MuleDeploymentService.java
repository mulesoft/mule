/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.System.getProperties;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppsFolder;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.internal.config.StartupContext;
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
import org.mule.runtime.module.service.api.manager.ServiceManager;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.NotImplementedException;
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
    this.domainDeployer = new DomainArchiveDeployer(new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains,
                                                                                 new DomainDeploymentTemplate(applicationDeployer,
                                                                                                              this),
                                                                                 new DeploymentMuleContextListenerFactory(
                                                                                                                          domainDeploymentListener)),
                                                    applicationDeployer, this);
    this.domainDeployer.setDeploymentListener(domainDeploymentListener);

    this.domainBundleDeployer = new DomainBundleArchiveDeployer(domainBundleDeploymentListener, domainDeployer, domains,
                                                                applicationDeployer, applications);

    if (useParallelDeployment()) {
      if (isDeployingSelectedAppsInOrder()) {
        throw new IllegalArgumentException("Deployment parameters 'app' and '" + PARALLEL_DEPLOYMENT_PROPERTY
            + "' cannot be used together");
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
    final Map<String, Object> options = StartupContext.get().getStartupOptions();
    String appString = (String) options.get("app");

    return !isEmpty(appString);
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
    return CollectionUtils.select(applications, object -> ((Application) object).getDomain().getArtifactName().equals(domain));
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
    executeSynchronized(() -> {
      try {
        File appLocation = toFile(appArchiveUri.toURL());
        String fileName = appLocation.getName();
        if (fileName.endsWith(".jar")) {
          applicationDeployer.deployPackagedArtifact(appArchiveUri, deploymentProperties);
        } else {
          if (!appLocation.getParent().equals(getAppsFolder())) {
            try {
              copyDirectory(appLocation, new File(getAppsFolder(), fileName));
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          }
          applicationDeployer.deployExplodedArtifact(fileName, deploymentProperties);
        }
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(e);
      }
    });
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
  public void undeployDomain(String domainName) {
    executeSynchronized(() -> domainDeployer.undeployArtifact(domainName));
  }

  @Override
  public void deployDomain(URI domainArchiveUri) throws IOException {
    deployDomain(domainArchiveUri, empty());
  }

  private void deployDomain(URI domainArchiveUri, Optional<Properties> properties) {
    executeSynchronized(() -> domainDeployer.deployPackagedArtifact(domainArchiveUri, properties));
  }

  @Override
  public void redeployDomain(String domainName) {
    redeployDomain(domainName, empty());
  }

  private void redeployDomain(String domainName, Optional<Properties> deploymentProperties) {
    executeSynchronized(() -> domainDeployer.redeploy(findDomain(domainName), deploymentProperties));
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
  public static SchedulerService findSchedulerService(ServiceManager serviceManager) {
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
        applicationDeployer.redeploy(findApplication(artifactName), deploymentProperties);
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
}
