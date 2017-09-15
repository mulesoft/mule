/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.application.ApplicationPolicyManager;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.DeployableArtifactWrapper;

import java.io.File;
import java.io.IOException;

/**
 * Decorates the target deployer to properly switch out context classloader for deployment one where applicable. E.g. init() phase
 * may load custom classes for an application, which must be executed with deployment (app) classloader in the context, and not
 * Mule system classloader.
 */
public class ApplicationWrapper extends DeployableArtifactWrapper<Application, ApplicationDescriptor> implements Application {

  protected ApplicationWrapper(Application delegate) throws IOException {
    super(delegate);
  }

  public String getAppName() {
    return getArtifactName();
  }

  @Override
  public Registry getRegistry() {
    return getMuleContext().getRegistry().get(OBJECT_REGISTRY);
  }

  @Override
  public Domain getDomain() {
    return getDelegate().getDomain();
  }

  @Override
  public ApplicationStatus getStatus() {
    return getDelegate().getStatus();
  }

  @Override
  public RegionClassLoader getRegionClassLoader() {
    return getDelegate().getRegionClassLoader();
  }

  @Override
  public ApplicationPolicyManager getPolicyManager() {
    return getDelegate().getPolicyManager();
  }

  @Override
  public String toString() {
    return String.format("%s(%s)", getClass().getName(), getDelegate());
  }

  public Application getDelegate() {
    return super.getDelegate();
  }

  @Override
  public File getLocation() {
    return getDelegate().getLocation();
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
}
