/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.System.getProperties;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.deployment.internal.ArtifactDeploymentTemplate.NOP_ARTIFACT_DEPLOYMENT_TEMPLATE;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;
import org.mule.runtime.core.config.StartupContext;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainFactory;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MuleDeploymentService implements DeploymentService {

  public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
  public static final IOFileFilter ZIP_ARTIFACT_FILTER =
      new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX), FileFileFilter.FILE);
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
  private final ArchiveDeployer<Domain> domainDeployer;
  private final DeploymentDirectoryWatcher deploymentDirectoryWatcher;
  private DefaultArchiveDeployer<Application> applicationDeployer;

  public MuleDeploymentService(DefaultDomainFactory domainFactory, DefaultApplicationFactory applicationFactory) {
    // TODO MULE-9653 : Migrate domain class loader creation to use ArtifactClassLoaderBuilder which already has support for
    // artifact plugins.
    domainFactory.setMuleContextListenerFactory(new DeploymentMuleContextListenerFactory(domainDeploymentListener));
    applicationFactory.setMuleContextListenerFactory(new DeploymentMuleContextListenerFactory(applicationDeploymentListener));

    ArtifactDeployer<Application> applicationMuleDeployer = new DefaultArtifactDeployer<>();
    ArtifactDeployer<Domain> domainMuleDeployer = new DefaultArtifactDeployer<>();

    this.applicationDeployer = new DefaultArchiveDeployer<>(applicationMuleDeployer, applicationFactory, applications,
                                                            NOP_ARTIFACT_DEPLOYMENT_TEMPLATE);
    this.applicationDeployer.setDeploymentListener(applicationDeploymentListener);
    this.domainDeployer = new DomainArchiveDeployer(
                                                    new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains,
                                                                                 new DomainDeploymentTemplate(applicationDeployer,
                                                                                                              this)),
                                                    applicationDeployer, this);
    this.domainDeployer.setDeploymentListener(domainDeploymentListener);

    if (useParallelDeployment()) {
      if (isDeployingSelectedAppsInOrder()) {
        throw new IllegalArgumentException("Deployment parameters 'app' and '" + PARALLEL_DEPLOYMENT_PROPERTY
            + "' cannot be used together");
      }
      logger.info("Using parallel deployment");
      this.deploymentDirectoryWatcher =
          new ParallelDeploymentDirectoryWatcher(domainDeployer, applicationDeployer, domains, applications, deploymentLock);
    } else {
      this.deploymentDirectoryWatcher =
          new DeploymentDirectoryWatcher(domainDeployer, applicationDeployer, domains, applications, deploymentLock);
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
    return CollectionUtils.select(applications, new Predicate() {

      @Override
      public boolean evaluate(Object object) {
        return ((Application) object).getDomain().getArtifactName().equals(domain);
      }
    });
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
  Map<URL, Long> getZombieApplications() {
    return applicationDeployer.getArtifactsZombieMap();
  }

  Map<URL, Long> getZombieDomains() {
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
  public void deploy(URL appArchiveUrl) throws IOException {
    executeSynchronized(() -> applicationDeployer.deployPackagedArtifact(appArchiveUrl));
  }

  @Override
  public void redeploy(String artifactName) {
    executeSynchronized(() -> {
      try {
        applicationDeployer.redeploy(findApplication(artifactName));
      } catch (DeploymentException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Failure while redeploying application: " + artifactName, e);
        }
      }
    });
  }

  @Override
  public void undeployDomain(String domainName) {
    executeSynchronized(() -> domainDeployer.undeployArtifact(domainName));
  }

  @Override
  public void deployDomain(URL domainArchiveUrl) throws IOException {
    executeSynchronized(() -> domainDeployer.deployPackagedArtifact(domainArchiveUrl));
  }

  @Override
  public void redeployDomain(String domainName) {
    executeSynchronized(() -> domainDeployer.redeploy(findDomain(domainName)));
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

  public void setDomainFactory(DomainFactory domainFactory) {
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
}
