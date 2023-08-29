/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.internal.CompositeDeploymentListener;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract {@link DeploymentService} for single app env.
 *
 * @since 4.6.0
 */
public abstract class AbstractSingleAppDeploymentService implements DeploymentService {

  public static final String NO_DOMAIN_SUPPORT_IN_SINGLE_APP_MODE_ERROR_MESSAGE = "Single app mode does not support domains.";

  protected final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();

  protected final CompositeDeploymentListener applicationDeploymentListener = new CompositeDeploymentListener();

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

  private Application application;

  // fair lock
  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

  protected void notifyStartupListeners() {
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
    if (application == null) {
      logger.warn("No application has been deployed.");
    } else {
      try {
        application.stop();
        application.dispose();
      } catch (Throwable t) {
        logger.error("Error stopping artifact {}", application.getArtifactName(), t);
      }
    }
  }

  @Override
  public Domain findDomain(String domainName) {
    return null;
  }

  @Override
  public Application findApplication(String appName) {
    return application.getArtifactName().equals(appName) ? application : null;
  }

  @Override
  public Collection<Application> findDomainApplications(final String domain) {
    return emptyList();
  }


  @Override
  public List<Application> getApplications() {
    return singletonList(application);
  }

  @Override
  public List<Domain> getDomains() {
    return emptyList();
  }

  @Override
  public ReentrantLock getLock() {
    return deploymentLock;
  }

  @Override
  public void undeploy(String appName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deploy(URI appArchiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deploy(URI appArchiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(String artifactName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(String artifactName, Properties appProperties) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(URI archiveUri, Properties appProperties) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeploy(URI archiveUri) throws IOException {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void undeployDomain(String domainName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployDomain(URI domainArchiveUri) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void redeployDomain(String domainName) {
    throw new UnsupportedOperationException("");
  }

  @Override
  public void deployDomainBundle(URI domainArchiveUri) {
    throw new UnsupportedOperationException("");
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
    // Nothing to do.
  }

  @Override
  public void removeDomainDeploymentListener(DeploymentListener listener) {
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
  public void deployDomain(URI domainArchiveUri, Properties appProperties) {
    throw new UnsupportedOperationException(NO_DOMAIN_SUPPORT_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
  }

  @Override
  public void redeployDomain(String domainName, Properties deploymentProperties) {
    throw new UnsupportedOperationException(NO_DOMAIN_SUPPORT_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
  }

  public void setApplication(Application application) {
    this.application = application;
  }
}
