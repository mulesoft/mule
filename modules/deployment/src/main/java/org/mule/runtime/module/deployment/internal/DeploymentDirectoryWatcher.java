/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.ZIP_FILE_SUFFIX;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.sort;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.io.IOCase.INSENSITIVE;
import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeEndIgnoreCase;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.Artifact;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;
import org.mule.runtime.module.deployment.internal.util.ElementAddedEvent;
import org.mule.runtime.module.deployment.internal.util.ElementRemovedEvent;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It's in charge of the whole deployment process.
 * <p/>
 * It will deploy the applications at the container startup process. It will periodically scan the artifact directories in order
 * to process new deployments, remove artifacts that were previously deployed but the anchor file was removed and redeploy those
 * applications which configuration has changed.
 */
public class DeploymentDirectoryWatcher implements Runnable {

  public static final String ARTIFACT_ANCHOR_SUFFIX = "-anchor.txt";
  public static final String CHANGE_CHECK_INTERVAL_PROPERTY = "mule.launcher.changeCheckInterval";
  public static final IOFileFilter JAR_ARTIFACT_FILTER =
      new AndFileFilter(new SuffixFileFilter(JAR_FILE_SUFFIX, INSENSITIVE), FileFileFilter.FILE);
  public static final IOFileFilter ZIP_ARTIFACT_FILTER =
      new AndFileFilter(new SuffixFileFilter(ZIP_FILE_SUFFIX, INSENSITIVE), FileFileFilter.FILE);

  protected static final int DEFAULT_CHANGES_CHECK_INTERVAL_MS = 5000;

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private final ReentrantLock deploymentLock;
  private final ArchiveDeployer<DomainDescriptor, Domain> domainArchiveDeployer;
  protected final ArchiveDeployer<ApplicationDescriptor, Application> applicationArchiveDeployer;
  protected final Supplier<SchedulerService> schedulerServiceSupplier;
  private final ArtifactTimestampListener<Application> applicationTimestampListener;
  private final ArtifactTimestampListener<Domain> domainTimestampListener;
  private final ObservableList<Application> applications;
  private final ObservableList<Domain> domains;
  private final DomainBundleArchiveDeployer domainBundleDeployer;
  private final File appsDir;
  private final File domainsDir;
  private Scheduler artifactDirMonitorScheduler;

  protected volatile boolean dirty;

  public DeploymentDirectoryWatcher(DomainBundleArchiveDeployer domainBundleDeployer,
                                    final ArchiveDeployer<DomainDescriptor, Domain> domainArchiveDeployer,
                                    final ArchiveDeployer<ApplicationDescriptor, Application> applicationArchiveDeployer,
                                    ObservableList<Domain> domains,
                                    ObservableList<Application> applications, Supplier<SchedulerService> schedulerServiceSupplier,
                                    final ReentrantLock deploymentLock) {
    this.domainBundleDeployer = domainBundleDeployer;
    this.appsDir = applicationArchiveDeployer.getDeploymentDirectory();
    this.domainsDir = domainArchiveDeployer.getDeploymentDirectory();
    this.deploymentLock = deploymentLock;
    this.domainArchiveDeployer = domainArchiveDeployer;
    this.applicationArchiveDeployer = applicationArchiveDeployer;
    this.applications = applications;
    this.domains = domains;
    applications.addPropertyChangeListener(e -> {
      if (e instanceof ElementAddedEvent || e instanceof ElementRemovedEvent) {
        if (logger.isDebugEnabled()) {
          logger.debug("Deployed applications set has been modified, flushing state.");
        }
        dirty = true;
      }
    });
    domains.addPropertyChangeListener(e -> {
      if (e instanceof ElementAddedEvent || e instanceof ElementRemovedEvent) {
        if (logger.isDebugEnabled()) {
          logger.debug("Deployed applications set has been modified, flushing state.");
        }
        dirty = true;
      }
    });
    this.schedulerServiceSupplier = schedulerServiceSupplier;
    this.applicationTimestampListener = new ArtifactTimestampListener<>(applications);
    this.domainTimestampListener = new ArtifactTimestampListener<>(domains);
  }

  /**
   * Starts the process of deployment / undeployment of artifact.
   * <p/>
   * It will schedule a task for periodically scan the deployment directories.
   */
  public void start() {
    deploymentLock.lock();
    deleteAllAnchors();

    String appString = getProperty(DEPLOYMENT_APPLICATION_PROPERTY);

    try {
      if (appString == null) {
        // Deploys all the artifact already installed
        run();

        // only start the monitor thread if we launched in default mode without explicitly
        // stated applications to launch
        scheduleChangeMonitor();
      } else {
        String[] explodedDomains = listFiles(domainsDir, DIRECTORY);
        String[] packagedDomains = listFiles(domainsDir, JAR_ARTIFACT_FILTER);

        deployPackedDomains(packagedDomains);
        deployExplodedDomains(explodedDomains);
        String[] apps = appString.split(":");
        apps = removeDuplicateAppNames(apps);

        for (String app : apps) {
          try {
            File applicationFile = new File(appsDir, app + JAR_FILE_SUFFIX);

            if (applicationFile.exists() && applicationFile.isFile()) {
              applicationArchiveDeployer.deployPackagedArtifact(app + JAR_FILE_SUFFIX, empty());
            } else {
              if (applicationArchiveDeployer.isUpdatedZombieArtifact(app)) {
                applicationArchiveDeployer.deployExplodedArtifact(app, empty());
              }
            }
          } catch (Exception e) {
            // Ignore and continue
          }
        }
        log(miniSplash("Mule is up and running in a fixed app set mode"));
      }
    } finally {
      if (deploymentLock.isHeldByCurrentThread()) {
        deploymentLock.unlock();
      }
    }
  }

  /**
   * Stops the deployment scan service.
   */
  public void stop() {
    stopAppDirMonitorTimer();

    deploymentLock.lock();
    try {
      notifyStopListeners();
      stopArtifacts(applications);
      stopArtifacts(domains);
    } finally {
      deploymentLock.unlock();
    }
  }

  private void stopArtifacts(List<? extends DeployableArtifact> artifacts) {
    Collections.reverse(artifacts);

    for (DeployableArtifact artifact : artifacts) {
      try {
        artifact.stop();
        artifact.dispose();
      } catch (Throwable t) {
        logger.error("Error stopping artifact {}", artifact.getArtifactName(), t);
      }
    }
  }

  static int getChangesCheckIntervalMs() {
    try {
      String value = getProperty(CHANGE_CHECK_INTERVAL_PROPERTY);
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return DEFAULT_CHANGES_CHECK_INTERVAL_MS;
    }
  }

  private void scheduleChangeMonitor() {
    final int reloadIntervalMs = getChangesCheckIntervalMs();
    SchedulerConfig schedulerConfig = SchedulerConfig.config()
        .withName("Mule.app.deployer.monitor")
        .withPriority(MIN_PRIORITY)
        .withMaxConcurrentTasks(1);

    artifactDirMonitorScheduler = schedulerServiceSupplier.get().customScheduler(schedulerConfig);
    artifactDirMonitorScheduler.scheduleWithFixedDelay(this, reloadIntervalMs, reloadIntervalMs, MILLISECONDS);

    log(miniSplash(format("Mule is up and kicking (every %dms)", reloadIntervalMs)));
  }

  protected void deployPackedApps(String[] zips) {
    for (String zip : zips) {
      try {
        final String artifactName = removeEndIgnoreCase(zip, JAR_FILE_SUFFIX);
        Optional<Properties> deploymentProperties = getPersistedDeploymentProperties(artifactName);
        applicationArchiveDeployer.deployPackagedArtifact(zip, deploymentProperties);
      } catch (Exception e) {
        // Ignore and continue
      }
    }
  }

  protected void deployExplodedApps(String[] apps) {
    for (String addedApp : apps) {
      try {
        Optional<Properties> deploymentProperties = getPersistedDeploymentProperties(addedApp);
        applicationArchiveDeployer.deployExplodedArtifact(addedApp, deploymentProperties);
      } catch (DeploymentException e) {
        // Ignore and continue
      }
    }
  }

  // Cycle is:
  // undeployArtifact removed apps
  // undeployArtifact removed domains
  // deploy domain bundles
  // deploy domain archives
  // deploy domain exploded
  // redeploy modified apps
  // deploy archives apps
  // deploy exploded apps
  @Override
  public void run() {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Checking for changes...");
      }
      // use non-barging lock to preserve fairness, according to javadocs
      // if there's a lock present - wait for next poll to do anything
      if (!deploymentLock.tryLock(0, SECONDS)) {
        if (logger.isDebugEnabled()) {
          logger.debug("Another deployment operation in progress, will skip this cycle. Owner thread: "
              + ((DebuggableReentrantLock) deploymentLock).getOwner());
        }
        return;
      }

      undeployRemovedApps();

      undeployRemovedDomains();

      deployDomainBundles();

      // list new domains
      String[] domains = listFiles(domainsDir, DIRECTORY);
      final String[] domainZips = listFiles(domainsDir, JAR_ARTIFACT_FILTER);

      redeployModifiedDomains();

      deployPackedDomains(domainZips);

      // re-scan exploded domains and update our state, as deploying Mule domains archives might have added some
      if (domainZips.length > 0 || dirty) {
        domains = listFiles(domainsDir, DIRECTORY);
      }

      deployExplodedDomains(domains);

      redeployModifiedApplications();

      // list new apps
      String[] apps = listFiles(appsDir, DIRECTORY);
      final String[] appZips = listFiles(appsDir, JAR_ARTIFACT_FILTER);

      deployPackedApps(appZips);

      // re-scan exploded apps and update our state, as deploying Mule app archives might have added some
      if (appZips.length > 0 || dirty) {
        apps = listFiles(appsDir, DIRECTORY);
      }

      // Sorts apps to ensure they are always deployed in the same order
      sort(apps);
      deployExplodedApps(apps);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        // preserve the flag for the thread
        currentThread().interrupt();
      } else {
        logger.error("Exception processing deployment watch dir.", e);
      }
    } finally {
      if (deploymentLock.isHeldByCurrentThread()) {
        deploymentLock.unlock();
      }
      dirty = false;
    }
  }

  private void deployDomainBundles() {
    final String[] domainBundles = listFiles(domainsDir, ZIP_ARTIFACT_FILTER);

    for (String domainBundle : domainBundles) {
      try {
        File domainBundleFile = new File(getDomainsFolder(), domainBundle);
        domainBundleDeployer.deployArtifact(domainBundleFile.toURI());
      } catch (Exception e) {
        // Ignore and continue
      }
    }
  }

  private String[] listFiles(File directory, FilenameFilter filter) {
    String[] files = directory.list(filter);
    if (files == null) {
      throw new IllegalStateException(format("We got a null while listing the contents of director '%s'. Some common " +
          "causes for this is a lack of permissions to the directory or that it's being deleted concurrently",
                                             directory.getName()));
    }
    return files;
  }

  public <D extends DeployableArtifactDescriptor, T extends Artifact<D>> T findArtifact(String artifactName,
                                                                                        ObservableList<T> artifacts) {
    return artifacts.stream()
        .filter(artifact -> artifact.getArtifactName().equals(artifactName))
        .findFirst()
        .orElse(null);
  }

  private void undeployRemovedDomains() {
    undeployRemovedArtifacts(domainsDir, domains, domainArchiveDeployer);
  }

  private void undeployRemovedApps() {
    undeployRemovedArtifacts(appsDir, applications, applicationArchiveDeployer);
  }

  private <D extends DeployableArtifactDescriptor> void undeployRemovedArtifacts(File artifactDir,
                                                                                 ObservableList<? extends Artifact<D>> artifacts,
                                                                                 ArchiveDeployer<D, ? extends Artifact<D>> archiveDeployer) {
    // we care only about removed anchors
    String[] currentAnchors = listFiles(artifactDir, new SuffixFileFilter(ARTIFACT_ANCHOR_SUFFIX));
    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append(format("Current anchors:%n"));
      for (String currentAnchor : currentAnchors) {
        sb.append(format("  %s%n", currentAnchor));
      }
      logger.debug(sb.toString());
    }

    final Set<String> currentAnchorsSet = stream(currentAnchors).collect(toSet());
    Collection<String> deletedAnchors = stream(findExpectedAnchorFiles(artifacts))
        .filter(a -> !currentAnchorsSet.contains(a))
        .collect(toList());

    if (logger.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder();
      sb.append(format("Deleted anchors:%n"));
      for (String deletedAnchor : deletedAnchors) {
        sb.append(format("  %s%n", deletedAnchor));
      }
      logger.debug(sb.toString());
    }

    for (String deletedAnchor : deletedAnchors) {
      String artifactName = removeEnd(deletedAnchor, ARTIFACT_ANCHOR_SUFFIX);
      try {
        if (findArtifact(artifactName, artifacts) != null) {
          archiveDeployer.undeployArtifact(artifactName);
        } else if (logger.isDebugEnabled()) {
          logger.debug(format("Artifact [%s] has already been undeployed via API", artifactName));
        }
      } catch (Throwable t) {
        logger.error("Failed to undeployArtifact artifact: " + artifactName, t);
      }
    }
  }

  /**
   * Returns the list of anchor file names for the deployed apps
   *
   * @return a non null list of file names
   */
  private <D extends DeployableArtifactDescriptor> String[] findExpectedAnchorFiles(ObservableList<? extends Artifact<D>> artifacts) {
    String[] anchors = new String[artifacts.size()];
    int i = 0;
    for (Artifact<D> artifact : artifacts) {
      anchors[i++] = artifact.getArtifactName() + ARTIFACT_ANCHOR_SUFFIX;
    }
    return anchors;
  }

  private void deployExplodedDomains(String[] domains) {
    for (String addedDomain : domains) {
      try {
        if (domainArchiveDeployer.isUpdatedZombieArtifact(addedDomain)) {
          domainArchiveDeployer.deployExplodedArtifact(addedDomain, empty());
        }
      } catch (DeploymentException e) {
        logger.error("Error deploying domain '{}'", addedDomain, e);
      }
    }
  }

  private void deployPackedDomains(String[] zips) {
    for (String zip : zips) {
      try {
        domainArchiveDeployer.deployPackagedArtifact(zip, empty());
      } catch (Exception e) {
        // Ignore and continue
      }
    }
  }

  private void deleteAllAnchors() {
    deleteAnchorsFromDirectory(domainsDir);
    deleteAnchorsFromDirectory(appsDir);
  }

  private void deleteAnchorsFromDirectory(final File directory) {
    // Deletes any leftover anchor files from previous shutdowns
    String[] anchors = listFiles(directory, new SuffixFileFilter(ARTIFACT_ANCHOR_SUFFIX));
    for (String anchor : anchors) {
      // ignore result
      new File(directory, anchor).delete();
    }
  }

  private String[] removeDuplicateAppNames(String[] apps) {
    List<String> appNames = new LinkedList<>();

    for (String appName : apps) {
      if (!appNames.contains(appName)) {
        appNames.add(appName);
      }
    }

    return appNames.toArray(new String[appNames.size()]);
  }

  private void redeployModifiedDomains() {
    Collection<String> redeployableDomains = getArtifactsToRedeploy(domains, domainTimestampListener);
    redeployModifiedArtifacts(redeployableDomains, domainArchiveDeployer);
  }

  private void redeployModifiedApplications() {
    Collection<String> redeployableApplications = getArtifactsToRedeploy(applications, applicationTimestampListener);
    redeployModifiedArtifacts(redeployableApplications, applicationArchiveDeployer);
  }

  private <D extends DeployableArtifactDescriptor, T extends DeployableArtifact<D>> Collection<String> getArtifactsToRedeploy(Collection<T> collection,
                                                                                                                              ArtifactTimestampListener<T> artifactTimestampListener) {
    return collection.stream()
        .filter(artifact -> artifact.getDescriptor().isRedeploymentEnabled())
        .filter(artifactTimestampListener::isArtifactResourceUpdated)
        .map(DeployableArtifact::getArtifactName)
        .collect(toList());
  }

  private <D extends DeployableArtifactDescriptor, T extends Artifact<D>> void redeployModifiedArtifacts(Collection<String> artifactNames,
                                                                                                         ArchiveDeployer<D, T> artifactArchiveDeployer) {
    for (String artifactName : artifactNames) {
      try {
        artifactArchiveDeployer.redeploy(artifactName, empty());
      } catch (DeploymentException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Error redeploying artifact {}", artifactName, e);
        }
      }
    }
  }

  private void stopAppDirMonitorTimer() {
    if (artifactDirMonitorScheduler != null) {
      artifactDirMonitorScheduler.shutdown();
      try {
        artifactDirMonitorScheduler.awaitTermination(getChangesCheckIntervalMs(), MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class ArtifactTimestampListener<T extends Artifact> implements PropertyChangeListener {

    private final Map<String, ArtifactResourcesTimestamp<T>> artifactConfigResourcesTimestaps = new HashMap<>();

    public ArtifactTimestampListener(ObservableList<T> artifacts) {
      artifacts.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (event instanceof ElementAddedEvent) {
        Artifact artifactAdded = (T) event.getNewValue();
        artifactConfigResourcesTimestaps.put(artifactAdded.getArtifactName(), new ArtifactResourcesTimestamp<T>(artifactAdded));
      } else if (event instanceof ElementRemovedEvent) {
        Artifact artifactRemoved = (T) event.getNewValue();
        artifactConfigResourcesTimestaps.remove(artifactRemoved.getArtifactName());
      }
    }

    public boolean isArtifactResourceUpdated(T artifact) {
      ArtifactResourcesTimestamp<T> applicationResourcesTimestamp =
          artifactConfigResourcesTimestaps.get(artifact.getArtifactName());
      return !applicationResourcesTimestamp.resourcesHaveSameTimestamp();
    }
  }

  private static class ArtifactResourcesTimestamp<T extends Artifact> {

    private final Map<String, Long> timestampsPerResource = new HashMap<>();

    public ArtifactResourcesTimestamp(final Artifact artifact) {
      for (File configResourceFile : artifact.getResourceFiles()) {
        timestampsPerResource.put(configResourceFile.getAbsolutePath(), configResourceFile.lastModified());
      }
      File descriptorFile =
          new File(((DeployableArtifactDescriptor) artifact.getDescriptor()).getArtifactLocation(),
                   ArtifactDescriptor.MULE_ARTIFACT_JSON_DESCRIPTOR_LOCATION);
      if (descriptorFile.exists()) {
        timestampsPerResource.put(descriptorFile.getAbsolutePath(), descriptorFile.lastModified());
      }
    }

    public boolean resourcesHaveSameTimestamp() {
      return timestampsPerResource.entrySet().stream().noneMatch(entry -> {
        File trackedFile = new File(entry.getKey());
        long originalTimestamp = entry.getValue();
        long currentTimestamp = trackedFile.lastModified();
        if (originalTimestamp != currentTimestamp) {
          timestampsPerResource.put(entry.getKey(), currentTimestamp);
          return true;
        }
        return false;
      });
    }
  }

  private void notifyStopListeners() {
    for (Application application : applications) {
      applicationArchiveDeployer.doNotPersistArtifactStop(application);
    }
    for (Domain domain : domains) {
      domainArchiveDeployer.doNotPersistArtifactStop(domain);
    }
  }
}
