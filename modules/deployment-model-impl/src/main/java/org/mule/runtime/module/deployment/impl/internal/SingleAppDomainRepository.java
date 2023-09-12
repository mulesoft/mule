/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.deployment.model.api.DeploymentStartException;
import org.mule.runtime.deployment.model.api.InstallException;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;

import java.io.File;
import java.util.List;

/**
 * A {@link DomainRepository} for single app domain mode. This repository will only provide a default blank domain with multiple
 * functionalities unsupported in this mode.
 *
 * @since 4.6.0
 */
public class SingleAppDomainRepository implements DomainRepository {

  public static final String NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE = "Not supported in single app mode";
  private final ArtifactClassLoaderResolver artifactClassLoaderResolver;

  public SingleAppDomainRepository(ArtifactClassLoaderResolver artifactClassLoaderResolver) {
    this.artifactClassLoaderResolver = artifactClassLoaderResolver;
  }

  @Override
  public Domain getDomain(String name) throws DomainNotFoundException {
    return new DefaultSingleAppDomain(name, artifactClassLoaderResolver);
  }

  @Override
  public boolean contains(String name) {
    throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
  }

  @Override
  public Domain getCompatibleDomain(BundleDescriptor descriptor) {
    throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
  }

  @Override
  public boolean containsCompatible(BundleDescriptor descriptor) {
    return true;
  }

  private static class DefaultSingleAppDomain implements Domain {

    public static final DomainDescriptor DESCRIPTOR = new DomainDescriptor(DomainDescriptor.DEFAULT_DOMAIN_NAME);
    private final ArtifactClassLoaderResolver artifactClassLoaderResolver;
    private final String name;

    public DefaultSingleAppDomain(String name, ArtifactClassLoaderResolver artifactClassLoaderResolver) {
      this.name = name;
      this.artifactClassLoaderResolver = artifactClassLoaderResolver;
    }

    @Override
    public String getArtifactName() {
      return name;
    }

    @Override
    public String getArtifactId() {
      return name;
    }

    @Override
    public File[] getResourceFiles() {
      return new File[0];
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader() {
      return artifactClassLoaderResolver.createDomainClassLoader(DESCRIPTOR);
    }

    @Override
    public void install() throws InstallException {
      // Nothing to do.
    }

    @Override
    public void init() {
      // Nothing to do.
    }

    @Override
    public void initTooling() {
      // Nothing to do.
    }

    @Override
    public void lazyInit() {
      // Nothing to do.
    }

    @Override
    public void lazyInit(boolean disableXmlValidations) {
      // Nothing to do.
    }

    @Override
    public void lazyInitTooling(boolean disableXmlValidations) {
      // Nothing to do.
    }

    @Override
    public void start() throws DeploymentStartException {
      // Nothing to do.
    }

    @Override
    public void stop() {
      // Nothing to do.
    }

    @Override
    public DomainDescriptor getDescriptor() {
      return DESCRIPTOR;
    }

    @Override
    public void dispose() {
      // Nothing to do.
    }

    @Override
    public ArtifactContext getArtifactContext() {
      return null;
    }

    @Override
    public Registry getRegistry() {
      throw new UnsupportedOperationException("Not supported in single app mode");
    }

    @Override
    public File getLocation() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
    }

    @Override
    public ConnectivityTestingService getConnectivityTestingService() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
    }

    @Override
    public MetadataService getMetadataService() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
    }

    @Override
    public ValueProviderService getValueProviderService() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_IN_SINGLE_APP_MODE_ERROR_MESSAGE);
    }

    @Override
    public List<ArtifactPlugin> getArtifactPlugins() {
      return emptyList();
    }

    @Override
    public void setMuleContextListener(MuleContextListener muleContextListener) {
      // Nothing to do.
    }

    @Override
    public boolean containsSharedResources() {
      return false;
    }
  }
}
