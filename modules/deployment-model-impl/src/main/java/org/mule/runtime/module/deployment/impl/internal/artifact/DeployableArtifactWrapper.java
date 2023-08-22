/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.deployment.model.api.DeployableArtifact;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment one where applicable. E.g. init() phase
 * may load custom classes for an application, which must be executed with deployment (app) classloader in the context, and not
 * Mule system classloader.
 */
public class DeployableArtifactWrapper<T extends DeployableArtifact<D>, D extends DeployableArtifactDescriptor>
    implements DeployableArtifact<D> {

  private final T delegate;

  protected DeployableArtifactWrapper(T artifact) throws IOException {
    this.delegate = artifact;
  }

  @Override
  public void dispose() {
    executeWithinArtifactClassLoader(delegate::dispose);
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return delegate.getArtifactClassLoader();
  }

  @Override
  public Registry getRegistry() {
    return delegate.getArtifactContext() == null ? null : delegate.getArtifactContext().getRegistry();
  }

  @Override
  public ArtifactContext getArtifactContext() {
    return delegate.getArtifactContext();
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

  @Override
  public ValueProviderService getValueProviderService() {
    return getDelegate().getValueProviderService();
  }

  @Override
  public List<ArtifactPlugin> getArtifactPlugins() {
    return delegate.getArtifactPlugins();
  }

  @Override
  public void init() {
    executeWithinArtifactClassLoader(delegate::init);
  }

  @Override
  public void initTooling() {
    executeWithinArtifactClassLoader(delegate::initTooling);
  }

  @Override
  public void lazyInit() {
    getDelegate().lazyInit();
  }

  @Override
  public void lazyInit(boolean disableXmlValidations) {
    getDelegate().lazyInit(disableXmlValidations);
  }

  @Override
  public void lazyInitTooling(boolean disableXmlValidations) {
    getDelegate().lazyInitTooling(disableXmlValidations);
  }

  @Override
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

  @Override
  public void setMuleContextListener(MuleContextListener muleContextListener) {
    delegate.setMuleContextListener(muleContextListener);
  }

  @Override
  public void start() throws DeploymentStartException {
    executeWithinArtifactClassLoader(delegate::start);
  }

  @Override
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
