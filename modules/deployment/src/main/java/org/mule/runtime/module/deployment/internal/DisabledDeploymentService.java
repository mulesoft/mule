/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.module.deployment.internal.util.DebuggableReentrantLock;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link DeploymentService} that does not allow deployment of applications.
 */
public class DisabledDeploymentService implements DeploymentService {

  private final ReentrantLock deploymentLock = new DebuggableReentrantLock(true);

  private final List<DeploymentListener> applicationDeploymentListeners = new CopyOnWriteArrayList<>();
  private final List<DeploymentListener> domainDeploymentListeners = new CopyOnWriteArrayList<>();

  private final List<StartupListener> startupListeners = new CopyOnWriteArrayList<>();

  private final List<Application> applications = new CopyOnWriteArrayList<>();
  private final List<Domain> domains = new CopyOnWriteArrayList<>();


  @Override
  public void addDeploymentListener(DeploymentListener listener) {
    checkArgument(listener != null, "Listener cannot be null");
    applicationDeploymentListeners.add(listener);
  }

  @Override
  public void removeDeploymentListener(DeploymentListener listener) {
    checkArgument(listener != null, "Listener cannot be null");
    applicationDeploymentListeners.remove(listener);
  }

  @Override
  public Application findApplication(String appName) {
    checkArgument(appName != null, "Application name cannot be null");
    return applications.stream().filter(app -> app.getArtifactName().contains(appName)).findFirst().orElse(null);
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
    this.startupListeners.add(listener);
  }

  @Override
  public void removeStartupListener(StartupListener listener) {
    this.startupListeners.remove(listener);
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
  public void deployDomainBundle(URI domainArchiveUri) throws IOException {
    throw new UnsupportedOperationException("Domain bundle deploy operation not supported");
  }

  @Override
  public void start() {
    // Nothing to do.
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
    domainDeploymentListeners.add(listener);
  }

  @Override
  public void removeDomainDeploymentListener(DeploymentListener listener) {
    domainDeploymentListeners.remove(listener);
  }

  public void addApplication(Application application) {
    applications.add(application);
  }

  public void removeApplication(Application application) {
    applications.remove(application);
  }

  public void addDomain(Domain domain) {
    domains.add(domain);
  }

  public void removeDomain(Domain domain) {
    domains.remove(domain);
  }

  public List<StartupListener> getStartupListeners() {
    return startupListeners;
  }

  public List<DeploymentListener> getApplicationDeploymentListeners() {
    return applicationDeploymentListeners;
  }

  public List<DeploymentListener> getDomainDeploymentListeners() {
    return domainDeploymentListeners;
  }
}
