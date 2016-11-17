/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.io.File;
import java.io.IOException;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment one where applicable. E.g. init() phase
 * may load custom classes for an application, which must be executed with deployment (app) classloader in the context, and not
 * Mule system classloader.
 */
public class DeployableArtifactWrapper<T extends DeployableArtifact<D>, D extends DeployableArtifactDescriptor>
    implements DeployableArtifact<D> {

  private T delegate;

  protected DeployableArtifactWrapper(T artifact) throws IOException {
    this.delegate = artifact;
  }

  public void dispose() {
    executeWithinArtifactClassLoader(delegate::dispose);
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return delegate.getArtifactClassLoader();
  }

  public MuleContext getMuleContext() {
    return delegate.getMuleContext();
  }

  @Override
  public File getLocation() {
    return delegate.getLocation();
  }

  @Override
  public ConnectivityTestingService getConnectivityTestingService() {
    return getDelegate().getConnectivityTestingService();
  }

  @Override
  public MetadataService getMetadataService() {
    return getDelegate().getMetadataService();
  }

  public void init() {
    executeWithinArtifactClassLoader(delegate::init);
  }

  @Override
  public void lazyInit() {
    getDelegate().lazyInit();
  }

  public void install() throws InstallException {
    executeWithinArtifactClassLoader(delegate::install);
  }

  @Override
  public String getArtifactName() {
    return delegate.getArtifactName();
  }

  @Override
  public String getArtifactId() {
    return delegate.getArtifactId();
  }

  @Override
  public D getDescriptor() {
    return delegate.getDescriptor();
  }

  @Override
  public File[] getResourceFiles() {
    return delegate.getResourceFiles();
  }

  public void start() throws DeploymentStartException {
    executeWithinArtifactClassLoader(delegate::start);
  }

  public void stop() {
    executeWithinArtifactClassLoader(delegate::stop);
  }

  private void executeWithinArtifactClassLoader(ArtifactAction artifactAction) {
    ClassLoader classLoader = getArtifactClassLoader() != null ? getArtifactClassLoader().getClassLoader()
        : Thread.currentThread().getContextClassLoader();

    withContextClassLoader(classLoader, artifactAction::execute);
  }

  public String getAppName() {
    return getArtifactName();
  }

  @Override
  public String toString() {
    return String.format("%s(%s)", getClass().getName(), delegate);
  }

  public T getDelegate() {
    return delegate;
  }

  private interface ArtifactAction {

    void execute();
  }
}
