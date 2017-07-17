/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.apache.commons.lang3.StringUtils.removeEndIgnoreCase;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.splash.SplashScreen.miniSplash;
import static org.mule.runtime.module.reboot.api.MuleContainerBootstrapUtils.getMuleAppsDir;

import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.module.artifact.Artifact;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;
import org.mule.runtime.module.deployment.internal.util.ObservableList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanPropertyValueEqualsPredicate;
import org.apache.commons.beanutils.BeanToPropertyValueTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deployer of an artifact within mule container. - Keeps track of deployed artifacts - Avoid already deployed artifacts to be
 * redeployed - Deploys, undeploys, redeploys packaged and exploded artifacts
 */
public class DefaultArchiveDeployer<T extends DeployableArtifact> implements ArchiveDeployer<T> {

  public static final String ARTIFACT_NAME_PROPERTY = "artifactName";
  public static final String JAR_FILE_SUFFIX = ".jar";
  private static final Logger logger = LoggerFactory.getLogger(DefaultArchiveDeployer.class);

  private final ArtifactDeployer<T> deployer;
  private final ArtifactArchiveInstaller artifactArchiveInstaller;
  private final Map<String, ZombieFile> artifactZombieMap = new HashMap<String, ZombieFile>();
  private final File artifactDir;
  private final ObservableList<T> artifacts;
  private final ArtifactDeploymentTemplate deploymentTemplate;
  private ArtifactFactory<T> artifactFactory;
  private DeploymentListener deploymentListener = new NullDeploymentListener();
  private MuleContextListenerFactory muleContextListenerFactory;


  public DefaultArchiveDeployer(final ArtifactDeployer deployer, final ArtifactFactory artifactFactory,
                                final ObservableList<T> artifacts,
                                ArtifactDeploymentTemplate deploymentTemplate,
                                MuleContextListenerFactory muleContextListenerFactory) {
    this.deployer = deployer;
    this.artifactFactory = artifactFactory;
    this.artifacts = artifacts;
    this.deploymentTemplate = deploymentTemplate;
    this.artifactDir = artifactFactory.getArtifactDir();
    this.artifactArchiveInstaller = new ArtifactArchiveInstaller(artifactDir);
    this.muleContextListenerFactory = muleContextListenerFactory;
  }

  @Override
  public T deployPackagedArtifact(String zip) throws DeploymentException {
    URI uri;
    File artifactZip;
    try {
      final String artifactName = removeEndIgnoreCase(zip, JAR_FILE_SUFFIX);
      artifactZip = new File(artifactDir, zip);
      uri = artifactZip.toURI();
      return deployPackagedArtifact(uri, artifactName);
    } catch (DeploymentException e) {
      throw e;
    } catch (Exception e) {
      throw new DeploymentException(createStaticMessage("Failed to deploy from zip: " + zip), e);
    }
  }

  @Override
  public T deployExplodedArtifact(String artifactDir) throws DeploymentException {
    if (!isUpdatedZombieArtifact(artifactDir)) {
      return null;
    }

    return deployExplodedApp(artifactDir);
  }

  @Override
  public boolean isUpdatedZombieArtifact(String artifactName) {
    @SuppressWarnings("rawtypes")
    Collection<String> deployedAppNames = collect(artifacts, new BeanToPropertyValueTransformer(ARTIFACT_NAME_PROPERTY));

    if (deployedAppNames.contains(artifactName) && (!artifactZombieMap.containsKey(artifactName))) {
      return false;
    }

    ZombieFile zombieFile = artifactZombieMap.get(artifactName);

    if ((zombieFile != null) && (!zombieFile.updatedZombieApp())) {
      return false;
    }
    return true;
  }

  @Override
  public void undeployArtifact(String artifactId) {
    ZombieFile zombieFile = artifactZombieMap.get(artifactId);
    if ((zombieFile != null)) {

      if (zombieFile.exists()) {
        return;
      } else {
        artifactZombieMap.remove(artifactId);
      }

    }

    T artifact = (T) find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactId));
    undeploy(artifact);
  }

  @Override
  public File getDeploymentDirectory() {
    return artifactFactory.getArtifactDir();
  }

  @Override
  public T deployPackagedArtifact(URI artifactAchivedUri) throws DeploymentException {
    T artifact;

    try {
      try {

        artifact = installFrom(artifactAchivedUri);
        trackArtifact(artifact);
      } catch (Throwable t) {
        File artifactArchive = new File(artifactAchivedUri);
        String artifactName = removeEndIgnoreCase(artifactArchive.getName(), JAR_FILE_SUFFIX);

        // error text has been created by the deployer already
        logDeploymentFailure(t, artifactName);

        addZombieFile(artifactName, artifactArchive);

        deploymentListener.onDeploymentFailure(artifactName, t);

        throw t;
      }

      deployArtifact(artifact);
      return artifact;
    } catch (Throwable t) {
      if (t instanceof DeploymentException) {
        // re-throw
        throw ((DeploymentException) t);
      }

      final String msg = "Failed to deploy from URI: " + artifactAchivedUri;
      throw new DeploymentException(createStaticMessage(msg), t);
    }
  }

  private void logDeploymentFailure(Throwable t, String artifactName) {
    final String msg = miniSplash(format("Failed to deploy artifact '%s', see below", artifactName));
    logger.error(msg, t);
  }

  @Override
  public Map<URI, Long> getArtifactsZombieMap() {
    Map<URI, Long> result = new HashMap<URI, Long>();

    for (String artifact : artifactZombieMap.keySet()) {
      ZombieFile file = artifactZombieMap.get(artifact);
      result.put(file.uri, file.originalTimestamp);
    }
    return result;
  }

  @Override
  public void setArtifactFactory(final ArtifactFactory<T> artifactFactory) {
    this.artifactFactory = artifactFactory;
  }

  @Override
  public void undeployArtifactWithoutUninstall(T artifact) {
    logRequestToUndeployArtifact(artifact);
    try {
      deploymentListener.onUndeploymentStart(artifact.getArtifactName());
      deployer.undeploy(artifact);
      deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());
    } catch (DeploymentException e) {
      deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
      throw e;
    }
  }

  ArtifactDeployer getDeployer() {
    return deployer;
  }

  @Override
  public void setDeploymentListener(CompositeDeploymentListener deploymentListener) {
    this.deploymentListener = deploymentListener;
  }

  private T deployPackagedArtifact(final URI artifactUri, String artifactName) throws IOException {
    ZombieFile zombieFile = artifactZombieMap.get(artifactName);
    if (zombieFile != null) {
      if (zombieFile.isFor(artifactUri) && !zombieFile.updatedZombieApp()) {
        // Skips the file because it was already deployed with failure
        return null;
      }
    }

    // check if this artifact is running first, undeployArtifact it then
    T artifact = (T) find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
    if (artifact != null) {
      deploymentTemplate.preRedeploy(artifact);
      undeployArtifact(artifactName);
    }

    T deployedAtifact = deployPackagedArtifact(artifactUri);
    deploymentTemplate.postRedeploy(deployedAtifact);
    return deployedAtifact;
  }

  private T deployExplodedApp(String addedApp) throws DeploymentException {
    if (logger.isInfoEnabled()) {
      logger.info("================== New Exploded Artifact: " + addedApp);
    }

    T artifact;
    try {
      artifact = createArtifact(new File(getMuleAppsDir(), addedApp));

      // add to the list of known artifacts first to avoid deployment loop on failure
      trackArtifact(artifact);
    } catch (Throwable t) {
      final File artifactDir1 = artifactDir;
      File artifactDir = new File(artifactDir1, addedApp);

      addZombieFile(addedApp, artifactDir);

      String msg = miniSplash(format("Failed to deploy exploded artifact: '%s', see below", addedApp));
      logger.error(msg, t);

      deploymentListener.onDeploymentFailure(addedApp, t);

      if (t instanceof DeploymentException) {
        throw (DeploymentException) t;
      } else {
        msg = "Failed to deploy artifact: " + addedApp;
        throw new DeploymentException(createStaticMessage(msg), t);
      }
    }

    deployArtifact(artifact);
    return artifact;
  }

  @Override
  public void deployArtifact(T artifact) throws DeploymentException {
    try {
      // add to the list of known artifacts first to avoid deployment loop on failure
      trackArtifact(artifact);

      deploymentListener.onDeploymentStart(artifact.getArtifactName());
      deployer.deploy(artifact);

      artifactArchiveInstaller.createAnchorFile(artifact.getArtifactName());
      deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
      artifactZombieMap.remove(artifact.getArtifactName());
    } catch (Throwable t) {
      // error text has been created by the deployer already
      String msg = miniSplash(format("Failed to deploy artifact '%s', see below", artifact.getArtifactName()));
      logger.error(msg, t);

      addZombieApp(artifact);

      deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
      if (t instanceof DeploymentException) {
        throw (DeploymentException) t;
      } else {
        msg = "Failed to deploy artifact: " + artifact.getArtifactName();
        throw new DeploymentException(createStaticMessage(msg), t);
      }
    }
  }

  private void addZombieApp(Artifact artifact) {
    File resourceFile = artifact.getResourceFiles()[0];

    if (resourceFile.exists()) {
      try {
        artifactZombieMap.put(artifact.getArtifactName(), new ZombieFile(resourceFile));
      } catch (Exception e) {
        // ignore resource
      }
    }
  }

  private void addZombieFile(String artifactName, File marker) {
    // no sync required as deploy operations are single-threaded
    if (marker == null) {
      return;
    }

    if (!marker.exists()) {
      return;
    }

    try {
      artifactZombieMap.put(artifactName, new ZombieFile(marker));
    } catch (Exception e) {
      logger.debug(format("Failed to mark an exploded artifact [%s] as a zombie", marker.getName()), e);
    }
  }

  private T findArtifact(String artifactName) {
    return (T) find(artifacts, new BeanPropertyValueEqualsPredicate(ARTIFACT_NAME_PROPERTY, artifactName));
  }

  private void trackArtifact(T artifact) {
    preTrackArtifact(artifact);

    artifacts.add(artifact);
  }

  public void preTrackArtifact(T artifact) {
    T previousArtifact = findArtifact(artifact.getArtifactName());
    artifacts.remove(previousArtifact);
  }

  private void undeploy(T artifact) {
    logRequestToUndeployArtifact(artifact);
    try {
      deploymentListener.onUndeploymentStart(artifact.getArtifactName());

      artifacts.remove(artifact);
      deployer.undeploy(artifact);
      artifactArchiveInstaller.desinstallArtifact(artifact.getArtifactName());

      deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());

      logArtifactUndeployed(artifact);
    } catch (RuntimeException e) {
      deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
      throw e;
    }
  }

  private void logRequestToUndeployArtifact(T artifact) {
    if (logger.isInfoEnabled()) {
      logger.info("================== Request to Undeploy Artifact: " + artifact.getArtifactName());
    }
  }

  private void logArtifactUndeployed(T artifact) {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Undeployed artifact '%s'", artifact.getArtifactName())));
    }
  }

  private T installFrom(URI uri) throws IOException {
    File artifactLocation = artifactArchiveInstaller.installArtifact(uri);
    T artifact = createArtifact(artifactLocation);
    return artifact;
  }

  private T createArtifact(File artifactLocation) throws IOException {
    T artifact = artifactFactory.createArtifact(artifactLocation);
    artifact.setMuleContextListener(muleContextListenerFactory.create(artifact.getArtifactName()));
    return artifact;
  }

  @Override
  public void redeploy(T artifact) throws DeploymentException {
    if (logger.isInfoEnabled()) {
      logger.info(miniSplash(format("Redeploying artifact '%s'", artifact.getArtifactName())));
    }

    deploymentListener.onUndeploymentStart(artifact.getArtifactName());
    try {
      deployer.undeploy(artifact);
      deploymentListener.onUndeploymentSuccess(artifact.getArtifactName());
    } catch (Throwable e) {
      // TODO make the exception better
      deploymentListener.onUndeploymentFailure(artifact.getArtifactName(), e);
    }

    deploymentListener.onDeploymentStart(artifact.getArtifactName());
    try {
      artifact = createArtifact(artifact.getLocation());
      trackArtifact(artifact);

      deployer.deploy(artifact);
      artifactArchiveInstaller.createAnchorFile(artifact.getArtifactName());
      deploymentListener.onDeploymentSuccess(artifact.getArtifactName());
    } catch (Throwable t) {
      try {
        logDeploymentFailure(t, artifact.getArtifactName());
        addZombieApp(artifact);
        if (t instanceof DeploymentException) {
          throw (DeploymentException) t;
        }
        String msg = "Failed to deploy artifact: " + artifact.getArtifactName();
        throw new DeploymentException(createStaticMessage(msg), t);
      } finally {
        deploymentListener.onDeploymentFailure(artifact.getArtifactName(), t);
      }
    }

    artifactZombieMap.remove(artifact.getArtifactName());
  }

  private static class ZombieFile {

    URI uri;
    Long originalTimestamp;
    File file;

    private ZombieFile(File file) {
      this.file = file;
      originalTimestamp = file.lastModified();
      uri = file.toURI();
    }

    public boolean isFor(URI uri) {
      return this.uri.equals(uri);
    }

    public boolean updatedZombieApp() {
      return originalTimestamp != file.lastModified();
    }

    public boolean exists() {
      return file.exists();
    }
  }
}
