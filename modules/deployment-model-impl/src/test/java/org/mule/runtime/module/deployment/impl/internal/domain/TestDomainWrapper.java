/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.io.File;
import java.util.List;

public class TestDomainWrapper implements Domain {

  private Domain delegate;
  private boolean failOnPurpose;
  private boolean failOnDispose;

  public TestDomainWrapper(Domain delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean containsSharedResources() {
    return delegate.containsSharedResources();
  }

  @Override
  public Registry getRegistry() {
    return delegate.getRegistry();
  }

  @Override
  public File getLocation() {
    return delegate.getLocation();
  }

  @Override
  public ConnectivityTestingService getConnectivityTestingService() {
    return delegate.getConnectivityTestingService();
  }

  @Override
  public MetadataService getMetadataService() {
    return delegate.getMetadataService();
  }

  @Override
  public ValueProviderService getValueProviderService() {
    return delegate.getValueProviderService();
  }

  @Override
  public List<ArtifactPlugin> getArtifactPlugins() {
    return delegate.getArtifactPlugins();
  }

  @Override
  public void setMuleContextListener(MuleContextListener muleContextListener) {
    delegate.setMuleContextListener(muleContextListener);
  }

  @Override
  public void install() throws InstallException {
    delegate.install();
  }

  @Override
  public void init() {
    delegate.init();
  }

  @Override
  public void lazyInit() {
    delegate.lazyInit();
  }

  @Override
  public void lazyInit(boolean disableXmlValidations) {
    delegate.lazyInit(disableXmlValidations);
  }

  @Override
  public void start() throws DeploymentStartException {
    delegate.start();
  }

  @Override
  public void stop() {
    if (failOnPurpose) {
      fail();
    }
    delegate.stop();
  }

  private void fail() {
    throw new RuntimeException("fail on purpose");
  }

  @Override
  public void dispose() {
    if (failOnDispose) {
      fail();
    }
    delegate.dispose();
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
  public DomainDescriptor getDescriptor() {
    return delegate.getDescriptor();
  }

  @Override
  public File[] getResourceFiles() {
    return delegate.getResourceFiles();
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return delegate.getArtifactClassLoader();
  }

  public void setFailOnStop() {
    this.failOnPurpose = true;
  }

  public void setFailOnDispose() {
    this.failOnDispose = true;
  }
}
